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

import com.hivemq.edge.adapters.opcua.config.EffectiveChecks;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for {@code trustMode=ALLOW_LIST}: trust is established by the presented certificate's
 * SHA-256 fingerprint appearing in an allow-list authored offline by an operator.
 *
 * <p>This is chain validation with a hash as the trust anchor instead of a certificate — the same
 * manual act of trust, in a form a human can transcribe. It is the honest answer for an environment
 * with no CA, and unlike accepting any certificate it still detects the server being replaced.
 *
 * <p>The allow-list is never written to. There is no first-use capture, by design: a fingerprint
 * enters the list only because a human put it there, so an attacker present at first connect cannot
 * enrol themselves, and the trust anchor can be deployed as a read-only secret.
 */
public class AllowListCertificateValidator implements CertificateValidator {

    private static final @NotNull Logger log = LoggerFactory.getLogger(AllowListCertificateValidator.class);

    private final @NotNull Set<String> allowedFingerprints;
    private final @NotNull EffectiveChecks checks;
    private final @NotNull String endpointUri;

    public AllowListCertificateValidator(
            final @NotNull Set<String> allowedFingerprints,
            final @NotNull EffectiveChecks checks,
            final @NotNull String endpointUri) {
        this.allowedFingerprints = Set.copyOf(allowedFingerprints);
        this.checks = checks;
        this.endpointUri = endpointUri;
    }

    @Override
    public void validateCertificateChain(
            final @NotNull List<X509Certificate> certificateChain,
            final @Nullable String applicationUri,
            final String @Nullable [] validHostnames)
            throws UaException {

        if (certificateChain.isEmpty()) {
            throw new UaException(StatusCodes.Bad_SecurityChecksFailed, "server presented an empty certificate chain");
        }
        final X509Certificate endEntity = certificateChain.get(0);

        final String fingerprint;
        try {
            fingerprint = CertificateFingerprints.fingerprintOf(endEntity);
        } catch (final CertificateEncodingException e) {
            throw new UaException(
                    StatusCodes.Bad_SecurityChecksFailed, "server certificate could not be encoded for fingerprinting");
        }

        if (!allowedFingerprints.contains(fingerprint)) {
            // The seen fingerprint is logged in the exact form the allow-list file accepts, so an
            // operator who recognises the server can add it out of band without extracting the
            // certificate first. It is logged on rejection only - never added automatically.
            log.warn(
                    "OPC UA adapter endpoint '{}': the server certificate was rejected, its SHA-256 fingerprint is "
                            + "not in the configured allow-list. Subject='{}', issuer='{}', fingerprint={}. If this "
                            + "is the expected server, add the fingerprint to the allow-list file, then restart the "
                            + "adapter - the allow-list is read once at adapter start.",
                    endpointUri,
                    endEntity.getSubjectX500Principal().getName(),
                    endEntity.getIssuerX500Principal().getName(),
                    CertificateFingerprints.toDisplayForm(fingerprint));
            throw new UaException(
                    StatusCodes.Bad_SecurityChecksFailed,
                    "server certificate fingerprint is not in the configured allow-list");
        }

        CertificateChecks.apply(checks, endEntity, applicationUri, validHostnames);
    }
}
