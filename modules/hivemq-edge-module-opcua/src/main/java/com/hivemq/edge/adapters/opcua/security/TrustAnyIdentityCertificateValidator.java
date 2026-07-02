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

import java.security.cert.X509Certificate;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.util.validation.CertificateValidationUtil;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link CertificateValidator} for {@code trustLevel=TRUST}: the trust chain is <b>not</b> built
 * (any server certificate is accepted), but the requested identity checks are still enforced on the
 * end-entity certificate.
 *
 * <p>This is the cell of the validation matrix that Milo's stock validators do not cover:
 * {@link org.eclipse.milo.opcua.stack.core.security.DefaultClientCertificateValidator} always builds
 * the chain, and {@link CertificateValidator.InsecureCertificateValidator} skips everything. Milo
 * exposes the individual identity checks as public static helpers on {@link CertificateValidationUtil}
 * ({@code checkApplicationUri}, {@code checkHostnameOrIpAddress}), which are callable without a chain
 * build; this validator composes them.
 *
 * <p>WARNING: because the chain is not validated, a connection using this validator remains
 * vulnerable to MITM. Identity assertion alone does not establish provenance.
 */
public class TrustAnyIdentityCertificateValidator implements CertificateValidator {

    private final boolean checkApplicationUri;
    private final boolean checkHostname;

    public TrustAnyIdentityCertificateValidator(final boolean checkApplicationUri, final boolean checkHostname) {
        this.checkApplicationUri = checkApplicationUri;
        this.checkHostname = checkHostname;
    }

    @Override
    public void validateCertificateChain(
            final List<X509Certificate> certificateChain,
            final @Nullable String applicationUri,
            final @Nullable String[] validHostnames)
            throws UaException {
        // TRUST: no chain build. The end-entity certificate is at index 0.
        final X509Certificate endEntity = certificateChain.get(0);

        // Mirror DefaultClientCertificateValidator: a check whose input is not supplied by Milo
        // (null) cannot be enforced and is skipped.
        if (checkApplicationUri && applicationUri != null) {
            CertificateValidationUtil.checkApplicationUri(endEntity, applicationUri);
        }
        if (checkHostname && validHostnames != null) {
            CertificateValidationUtil.checkHostnameOrIpAddress(endEntity, validHostnames);
        }
    }
}
