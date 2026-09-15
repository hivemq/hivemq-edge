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
import java.util.List;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Validator for {@code trustMode=ANY_CERT}: no trust is established — any certificate is accepted —
 * but the remaining configured checks are still enforced on it.
 *
 * <p>This is the cell of the matrix the stock validators do not cover: the default validator always
 * builds a certification path, and the insecure validator skips everything. Keeping the other axes
 * live under {@code ANY_CERT} is what makes them genuinely orthogonal.
 *
 * <p>WARNING: without a trust decision, a connection using this validator remains vulnerable to
 * man-in-the-middle attacks. Asserting an identity does not establish provenance — an attacker who can
 * present any certificate can present one carrying the expected application URI and hostname.
 */
public class CheckOnlyCertificateValidator implements CertificateValidator {

    private final @NotNull EffectiveChecks checks;

    public CheckOnlyCertificateValidator(final @NotNull EffectiveChecks checks) {
        this.checks = checks;
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
        CertificateChecks.apply(checks, certificateChain.get(0), applicationUri, validHostnames);
    }
}
