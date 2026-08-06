/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.security;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * SHA-256 fingerprints of server certificates, and the allow-list file that holds them.
 *
 * <p>The file is human-authored and human-transcribable on purpose: a fingerprint can be e-mailed or
 * read out by a device vendor, which is the enrolment path that actually works in a factory with no
 * CA. Format: one fingerprint per line, hexadecimal, {@code ':'} separators optional, case
 * insensitive, {@code '#'} starts a comment, blank lines ignored.
 */
public final class CertificateFingerprints {

    private static final @NotNull String DIGEST_ALGORITHM = "SHA-256";
    private static final int HEX_LENGTH = 64;

    private CertificateFingerprints() {}

    /** The lower-case hexadecimal SHA-256 fingerprint over the DER encoding of the certificate. */
    public static @NotNull String fingerprintOf(final @NotNull X509Certificate certificate)
            throws CertificateEncodingException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (final NoSuchAlgorithmException e) {
            // SHA-256 is mandated for every JRE; its absence is not a configuration problem.
            throw new IllegalStateException(DIGEST_ALGORITHM + " is not available", e);
        }
        return HexFormat.of().formatHex(digest.digest(certificate.getEncoded()));
    }

    /**
     * Reads the allow-list.
     *
     * <p>A malformed line fails the whole file rather than being skipped: silently dropping a typo'd
     * fingerprint would shrink the allow-list without telling anybody, and the operator would be left
     * debugging a rejected connection whose entry is visibly present in the file.
     *
     * @throws IOException if the file cannot be read.
     * @throws IllegalArgumentException if a line is malformed, the file is not UTF-8, or the file
     *         holds no fingerprint.
     */
    public static @NotNull Set<String> loadAllowList(final @NotNull Path path) throws IOException {
        final List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (final MalformedInputException e) {
            // Named explicitly: the generic could-not-be-read message points at paths and
            // permissions, which is exactly the wrong hunt for an encoding problem.
            throw new IllegalArgumentException(
                    ("Certificate allow-list '%s' is not UTF-8 encoded. Save the file as UTF-8 "
                                    + "(fingerprints are plain hexadecimal, so plain ASCII works too).")
                            .formatted(path),
                    e);
        }
        final Set<String> fingerprints = new LinkedHashSet<>();

        for (int i = 0; i < lines.size(); i++) {
            // A Windows editor saving "UTF-8" typically prepends a byte order mark; left in place it
            // would fail the first fingerprint with an invisible-character message.
            final String line = i == 0 ? stripBom(lines.get(i)) : lines.get(i);
            final String withoutComment = stripComment(line);
            if (withoutComment.isBlank()) {
                continue;
            }
            fingerprints.add(parseFingerprint(withoutComment, i + 1, path));
        }

        if (fingerprints.isEmpty()) {
            throw new IllegalArgumentException(("Certificate allow-list '%s' contains no fingerprints. An empty "
                            + "allow-list would reject every server; remove the ALLOW_LIST trust mode or add the "
                            + "SHA-256 fingerprint of each server certificate to trust.")
                    .formatted(path));
        }
        return Set.copyOf(fingerprints);
    }

    private static @NotNull String stripBom(final @NotNull String line) {
        return !line.isEmpty() && line.charAt(0) == '\uFEFF' ? line.substring(1) : line;
    }

    private static @NotNull String stripComment(final @NotNull String line) {
        final int comment = line.indexOf('#');
        return comment < 0 ? line : line.substring(0, comment);
    }

    private static @NotNull String parseFingerprint(
            final @NotNull String rawEntry, final int lineNumber, final @NotNull Path path) {
        final String candidate =
                rawEntry.trim().replace(":", "").replace(" ", "").toLowerCase(java.util.Locale.ROOT);

        if (candidate.length() != HEX_LENGTH || !candidate.chars().allMatch(CertificateFingerprints::isHexDigit)) {
            throw new IllegalArgumentException(("Certificate allow-list '%s', line %d: '%s' is not a SHA-256 "
                            + "fingerprint. Expected %d hexadecimal characters, ':' separators optional.")
                    .formatted(path, lineNumber, rawEntry.trim(), HEX_LENGTH));
        }
        return candidate;
    }

    private static boolean isHexDigit(final int c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }

    /** Renders a fingerprint in the colon-separated form operators are used to reading. */
    public static @NotNull String toDisplayForm(final @NotNull String fingerprint) {
        final StringBuilder sb = new StringBuilder(fingerprint.length() + fingerprint.length() / 2);
        for (int i = 0; i < fingerprint.length(); i += 2) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(fingerprint, i, Math.min(i + 2, fingerprint.length()));
        }
        return sb.toString();
    }
}
