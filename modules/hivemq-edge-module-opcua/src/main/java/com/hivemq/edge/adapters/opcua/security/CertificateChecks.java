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
import java.security.cert.X509Certificate;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.util.validation.CertificateValidationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies the per-certificate checks of an {@link EffectiveChecks} to an end-entity certificate,
 * without building a certification path.
 *
 * <p>This is the piece that keeps the axes orthogonal when no chain is built. The stack's default
 * validator runs these checks only as part of path validation, but exposes each of them as a public
 * static helper, so the same checks are available under {@code ALLOW_LIST} and {@code ANY_CERT}.
 *
 * <p>Revocation is deliberately absent: it is decidable only while a path is being built, so the
 * configuration layer rejects any request for it without a chain rather than letting it pass here
 * unnoticed.
 */
final class CertificateChecks {

    private CertificateChecks() {}

    /**
     * @param applicationUri the application URI announced by the server, or {@code null} if the stack
     *     did not supply one.
     * @param validHostnames the endpoint hostnames, or {@code null} if the stack did not supply any.
     */
    static void apply(
            final @NotNull EffectiveChecks checks,
            final @NotNull X509Certificate endEntity,
            final @Nullable String applicationUri,
            final String @Nullable [] validHostnames)
            throws UaException {

        // A check whose input the stack did not supply cannot be enforced; skipping it mirrors what
        // the default validator does in the same situation.
        if (checks.isSanUri() && applicationUri != null) {
            CertificateValidationUtil.checkApplicationUri(endEntity, applicationUri);
        }
        if (checks.isHostname() && validHostnames != null) {
            CertificateValidationUtil.checkHostnameOrIpAddress(endEntity, validHostnames);
        }
        if (checks.isValidity()) {
            CertificateValidationUtil.checkValidity(endEntity, true);
        }
        if (checks.isKeyUsage()) {
            CertificateValidationUtil.checkEndEntityKeyUsage(endEntity);
        }
        if (checks.isExtendedKeyUsage()) {
            // The remote end entity is the server, so the extended key usage must permit server
            // authentication, not client authentication.
            CertificateValidationUtil.checkEndEntityExtendedKeyUsage(endEntity, false);
        }
    }
}
