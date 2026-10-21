/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.edge.adapters.opcua.method;

import java.math.BigInteger;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/// Reads a Guid from the spellings a caller may write, and refuses anything else.
///
/// ## Why not `UUID.fromString`
///
/// It pads. `UUID.fromString` splits on hyphens and parses each group as a hexadecimal number, so a group
/// narrower than its canonical width is zero-filled on the left rather than refused -- and the result is a
/// *different, perfectly valid* Guid. A caller who drops one character from
/// `72962b91-fa75-4ae6-8d28-0b404dc7daf6` sends `...-b404dc7daf6` and the device receives the original, a node
/// they did not name; `1-1-1-1-1` is sent as `00000001-0001-0001-0001-000000000001`.
///
/// Nothing downstream can undo that. Once padded, the value is indistinguishable from one the caller meant, so
/// the only question answerable here is whether the value *could* have crossed intact -- the same reasoning the
/// reference gives for refusing a whole number of magnitude 2^53 or more, where rounding also produces a
/// different legal value. Milo's own `NodeId.parse` pads identically for the `ns=2;g=...` text form, which is
/// why {@link #requireCanonical} exists to check such a string before Milo sees it.
///
/// Reported by Miguel as R6-1 on EDG-1087.
///
/// ## The spellings
///
/// The forms below are the text spellings of the 32 hexadecimal digits, told apart by their shape. A caller
/// writing a Guid by hand copies it from a vendor tool, and those disagree about the wrapper: OPC-UA's own
/// `NodeId` text form and our read path write it bare and hyphenated, while UaExpert, .NET and C headers each
/// show one of the others. Accepting all of them costs one pattern each and removes a transcription step; what
/// is *not* accepted is any spelling that cannot be told from another, which is why no compact binary encoding
/// -- base64, base32, base58 -- is read here: a 22-character base64 string is also a valid base58 string, and
/// guessing between them would reintroduce exactly the silent-wrong-value failure this class exists to stop.
public final class Guids {

    private static final @NotNull Pattern HYPHENATED =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private static final @NotNull Pattern BARE = Pattern.compile("[0-9a-f]{32}");

    /// `{0xdddddddd,0xdddd,0xdddd,{0xdd,...}}`, the C struct initialiser. Leading zeros may be omitted in each
    /// number, so every group is padded to its own width after matching -- padding is safe here because the
    /// group count and the widths are fixed by the form itself.
    private static final @NotNull Pattern STRUCT =
            Pattern.compile("\\{0x([0-9a-f]{1,8}),0x([0-9a-f]{1,4}),0x([0-9a-f]{1,4}),\\{"
                    + "0x([0-9a-f]{1,2}),0x([0-9a-f]{1,2}),0x([0-9a-f]{1,2}),0x([0-9a-f]{1,2}),"
                    + "0x([0-9a-f]{1,2}),0x([0-9a-f]{1,2}),0x([0-9a-f]{1,2}),0x([0-9a-f]{1,2})\\}\\}");

    private static final int @NotNull [] STRUCT_WIDTHS = {8, 4, 4, 2, 2, 2, 2, 2, 2, 2, 2};

    /// `2.25.<the 128 bits as one unsigned decimal integer>`, the OID form. The range is checked after parsing;
    /// a leading zero is refused so each Guid has one spelling.
    private static final @NotNull Pattern OID = Pattern.compile("2\\.25\\.(0|[1-9][0-9]{0,38})");

    private Guids() {}

    /// The Guid a string spells, in any of the accepted forms.
    ///
    /// @throws IllegalArgumentException naming the expected forms, if the string is none of them
    public static @NotNull UUID parse(final @NotNull String input) {
        final String text = input.strip();
        final String lower = text.toLowerCase(Locale.ROOT);

        final String inner;
        if (lower.startsWith("urn:uuid:")) {
            inner = lower.substring("urn:uuid:".length());
            // Whitespace removed before the test, not after: the struct form is conventionally written with
            // spaces after its commas and braces, so `{ 0x...` has to reach the struct branch rather than
            // falling through to the braced one.
        } else if (lower.replaceAll("\\s+", "").startsWith("{0x")) {
            return fromStruct(lower, input);
        } else if (lower.startsWith("{") && lower.endsWith("}")) {
            inner = lower.substring(1, lower.length() - 1);
        } else if (lower.startsWith("(") && lower.endsWith(")")) {
            inner = lower.substring(1, lower.length() - 1);
        } else if (lower.startsWith("2.25.") || lower.startsWith("urn:oid:")) {
            return fromOid(lower, input);
        } else {
            inner = lower;
        }

        if (HYPHENATED.matcher(inner).matches()) {
            return fromHex(inner.replace("-", ""));
        }
        if (BARE.matcher(inner).matches()) {
            return fromHex(inner);
        }
        throw refusal(input);
    }

    /// The string itself, once it is a Guid in the hyphenated form and nothing else.
    ///
    /// For a value handed on to something that parses it again -- Milo's `NodeId.parse` for `ns=2;g=...`, which
    /// pads as `UUID.fromString` does. Checking the spelling and passing the original string through keeps one
    /// parser in the path rather than re-rendering a Guid Milo would then re-read.
    ///
    /// @throws IllegalArgumentException if the string is not 8-4-4-4-12 hexadecimal digits
    public static @NotNull String requireCanonical(final @NotNull String input) {
        if (!HYPHENATED.matcher(input.toLowerCase(Locale.ROOT)).matches()) {
            throw new IllegalArgumentException("expected a Guid as 8-4-4-4-12 hexadecimal digits, got '" + input + "'");
        }
        return input;
    }

    private static @NotNull UUID fromHex(final @NotNull String hex) {
        return new UUID(
                new BigInteger(hex.substring(0, 16), 16).longValue(),
                new BigInteger(hex.substring(16), 16).longValue());
    }

    private static @NotNull UUID fromStruct(final @NotNull String lower, final @NotNull String input) {
        final Matcher match = STRUCT.matcher(lower.replaceAll("\\s+", ""));
        if (!match.matches()) {
            throw refusal(input);
        }
        final StringBuilder hex = new StringBuilder(32);
        for (int group = 0; group < STRUCT_WIDTHS.length; group++) {
            final String digits = match.group(group + 1);
            hex.append("0".repeat(STRUCT_WIDTHS[group] - digits.length())).append(digits);
        }
        return fromHex(hex.toString());
    }

    private static @NotNull UUID fromOid(final @NotNull String lower, final @NotNull String input) {
        final String oid = lower.startsWith("urn:oid:") ? lower.substring("urn:oid:".length()) : lower;
        if (!OID.matcher(oid).matches()) {
            throw refusal(input);
        }
        final BigInteger value = new BigInteger(oid.substring("2.25.".length()));
        if (value.bitLength() > 128) {
            throw refusal(input);
        }
        return new UUID(value.shiftRight(64).longValue(), value.longValue());
    }

    private static @NotNull IllegalArgumentException refusal(final @NotNull String input) {
        return new IllegalArgumentException("expected a Guid as 8-4-4-4-12 hexadecimal digits, got '" + input
                + "'. Also accepted: 32 hexadecimal digits, either wrapped in braces or parentheses, "
                + "'urn:uuid:' followed by the hyphenated form, the '{0x...}' struct form, and '2.25.' "
                + "followed by the 128 bits as a decimal number");
    }
}
