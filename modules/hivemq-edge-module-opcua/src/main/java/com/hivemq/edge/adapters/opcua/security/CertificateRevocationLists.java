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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CRL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Certificate revocation lists, and the file or directory that holds them.
 *
 * <p>Without these the {@code revocation} axis cannot be satisfied for any chain that runs through a
 * CA: the underlying stack decides revocation while it builds the certification path, and it asks the
 * trust-list manager for the CRLs to decide it with. Handing it none means the status of every issuing
 * CA is "unknown", which fails closed as {@code Bad_CertificateRevocationUnknown} — a refusal no
 * amount of correct configuration elsewhere could lift.
 *
 * <p>Read once at adapter start and never written, exactly like the fingerprint allow-list: the file
 * can be a read-only artifact, and refreshing it is a deliberate restart rather than something that
 * happens under a running adapter.
 *
 * <p>Accepts PEM or DER, and either a single file or a directory of them — a CA that publishes one
 * CRL per issuer is the common case, and making the operator concatenate them by hand would be a
 * transcription step with no upside.
 */
public final class CertificateRevocationLists {

    private static final @NotNull String CERTIFICATE_FACTORY_TYPE = "X.509";

    private CertificateRevocationLists() {}

    /**
     * Loads every CRL under {@code path}, which may be a single file or a directory of files.
     *
     * @throws IOException if the path cannot be read, with a message that names the file and the fix
     * @throws IllegalArgumentException if nothing at the path parses as a CRL
     */
    public static @NotNull List<X509CRL> load(final @NotNull Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException(("Certificate revocation list '%s' does not exist. Point the path at a CRL file "
                            + "or at a directory of them, or set revocation=NONE if the deployment has no "
                            + "revocation infrastructure.")
                    .formatted(path));
        }
        if (!Files.isReadable(path)) {
            throw new IOException(("Certificate revocation list '%s' is not readable by the Edge process. Correct "
                            + "the file permissions or the path.")
                    .formatted(path));
        }

        final List<Path> files = filesUnder(path);
        if (files.isEmpty()) {
            throw new IllegalArgumentException(("Certificate revocation list directory '%s' is empty. Put the CA's "
                            + "CRL in it, point the path at a single CRL file instead, or set revocation=NONE.")
                    .formatted(path));
        }

        final List<X509CRL> revocationLists = new ArrayList<>();
        for (final Path file : files) {
            revocationLists.addAll(parse(file));
        }

        if (revocationLists.isEmpty()) {
            // Only reachable for a directory whose files all parsed to nothing; a single file that
            // yields nothing is reported against that file, by name, in parse().
            throw new IllegalArgumentException(("Certificate revocation list directory '%s' contains no CRLs. "
                            + "Every file in it was read successfully but none of them is a CRL.")
                    .formatted(path));
        }
        return List.copyOf(revocationLists);
    }

    /** The regular files to read: the path itself, or a directory's entries in a stable order. */
    private static @NotNull List<Path> filesUnder(final @NotNull Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            return List.of(path);
        }
        try (var entries = Files.list(path)) {
            return entries.filter(Files::isRegularFile)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }

    private static @NotNull List<X509CRL> parse(final @NotNull Path file) throws IOException {
        final Collection<? extends CRL> parsed;
        try (InputStream in = Files.newInputStream(file)) {
            // generateCRLs reads PEM and DER alike, and a PEM file holding several CRLs yields all of
            // them. It throws rather than returning empty for content it cannot make sense of.
            parsed = CertificateFactory.getInstance(CERTIFICATE_FACTORY_TYPE).generateCRLs(in);
        } catch (final IOException e) {
            throw new IOException(
                    "Certificate revocation list '%s' could not be read: %s".formatted(file, e.getMessage()), e);
        } catch (final java.security.cert.CRLException | java.security.cert.CertificateException e) {
            // The operator-facing half of the message matters more than the provider's wording: the
            // usual cause is a certificate or a truststore pointed at by mistake.
            throw new IllegalArgumentException(
                    ("Certificate revocation list '%s' is not a CRL. Expected PEM or DER "
                                    + "revocation-list content; a certificate or keystore file is not one. Provider "
                                    + "reported: %s")
                            .formatted(file, e.getMessage()),
                    e);
        }

        final List<X509CRL> x509 = parsed.stream()
                .filter(X509CRL.class::isInstance)
                .map(X509CRL.class::cast)
                .toList();
        if (x509.isEmpty()) {
            throw new IllegalArgumentException(("Certificate revocation list '%s' contains no X.509 CRLs. Expected "
                            + "PEM or DER revocation-list content.")
                    .formatted(file));
        }
        return x509;
    }
}
