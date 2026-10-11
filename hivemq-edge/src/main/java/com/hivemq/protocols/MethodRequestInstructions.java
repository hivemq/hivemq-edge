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
package com.hivemq.protocols;

import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/// Adds the one field-mapping instruction that carries a method request's `context` to the adapter.
///
/// ## What the context is
///
/// Everything a method request carries *besides its arguments*: which method to call, the caller's correlation
/// token, and the address it wants the answer on. Edge gathers those -- from the payload and from the MQTT 5
/// properties -- validates them, and puts the result in one MQTT user property before the message reaches the
/// data policy. See `ReactiveWriter` in the southbound module for the assembly, which is also where a request
/// that fails validation is quarantined.
///
/// ## Why an instruction is needed at all
///
/// A southbound field mapping is a **whitelist**. The generated transform starts from an empty object and copies
/// across only what an instruction names, so anything no instruction mentions is dropped before the adapter sees
/// it. The default mapping is a single `value -> value` copy. One instruction therefore has to name the context,
/// or it never arrives -- and a request whose method cannot be named cannot be dispatched at all.
///
/// ## Why it is synthetic, and why it is now exactly one
///
/// This is not an instruction a user writes. The mapping editor offers the writable leaves of a tag's southbound
/// schema as destinations, and `context` is declared read-only there precisely so it is not offered.
///
/// It replaces three identity copies -- `$.methodName -> $.methodName` and the same for `correlationData` and
/// `responseTopic` -- whose only job was to stop the whitelist deleting those fields. Three consequences of that
/// shape have gone with it:
///
///   - **The flags no longer work by omission.** They used to: switching `responseTopic` off simply added no
///     instruction, and the field was deleted. Which made a *configured* instruction naming the field enough to
///     defeat the switch, so the stored flag described nothing. The flags are now read where the context is
///     assembled, and a mapping cannot contradict them.
///   - **Nothing has to be reserved.** `correlationData` and `responseTopic` are no longer destinations at the
///     message root, so a configured instruction cannot collide with one.
///   - **Order no longer matters.** One destination, written once.
///
/// @see com.hivemq.edge.modules.bidirectional.write.ScriptCreator for the `@.` source kind this relies on
public final class MethodRequestInstructions {

    private MethodRequestInstructions() {}

    /// The user property Edge puts a method request's assembled context in.
    ///
    /// Part of the contract between three places: whatever assembles the property, the instruction that copies
    /// it, and the quarantine republish that strips it again. `INTERNAL_` says what happens to a caller who
    /// sets it themselves -- Edge overwrites it. Compared exactly everywhere: `String.equals` on the Java side,
    /// `===` in the generated script, and MQTT itself treats a user property name as an opaque UTF-8 string
    /// with no case folding.
    public static final @NotNull String CONTEXT_USER_PROPERTY = "INTERNAL_REQUEST_RESPONSE_FIELDS";

    /// Where the context lands in the payload the adapter receives: a sibling of `value`.
    public static final @NotNull String CONTEXT_FIELD = "context";

    /// The marker that makes an instruction's source a user property rather than a payload path.
    ///
    /// Duplicated from the script generator rather than shared, because that class is in the commercial
    /// repository and this one is not. Two characters, in one constant on each side, both documented as the
    /// same contract.
    private static final @NotNull String USER_PROPERTY_PREFIX = "@.";

    /// The field mapping to hand the script generator, given the mapping's own.
    ///
    /// Returns the original untouched when the mapping carries no method calls, which is every southbound mapping
    /// that exists today -- so the generated transform for those is byte-identical to what it was.
    ///
    /// The mapping's three method-call flags are deliberately *not* parameters any more. They decide what the
    /// context contains, which is settled where it is assembled; here there is nothing to decide, because the
    /// instruction is the same whatever the context holds.
    ///
    /// @param configured  the instructions the user configured.
    /// @param methodCalls whether this mapping carries method calls at all.
    public static @NotNull FieldMapping augment(final @NotNull FieldMapping configured, final boolean methodCalls) {

        if (!methodCalls) {
            return configured;
        }

        final List<Instruction> kept = new ArrayList<>(configured.getInstructions());

        // A configured instruction already writing `context` wins, and nothing is appended: two instructions
        // writing one destination would make the result depend on their order. Not reachable from the mapping
        // editor, which declares the field read-only, but a hand-written configuration file is not filtered
        // that way.
        final boolean alreadyWritten =
                kept.stream().anyMatch(instruction -> CONTEXT_FIELD.equals(instruction.toDestinationJsonPath()));
        if (!alreadyWritten) {
            kept.add(new Instruction(USER_PROPERTY_PREFIX + CONTEXT_USER_PROPERTY, "$." + CONTEXT_FIELD, null));
        }

        return new FieldMapping(kept);
    }
}
