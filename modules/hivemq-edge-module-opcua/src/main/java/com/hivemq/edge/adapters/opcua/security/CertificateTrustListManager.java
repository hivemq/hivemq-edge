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

import org.eclipse.milo.opcua.stack.core.security.TrustListManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.jetbrains.annotations.NotNull;

import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.List;

public class CertificateTrustListManager implements TrustListManager {

    private final @NotNull List<X509Certificate> trustedCerts;
    private final @NotNull DateTime dateTime;

    public CertificateTrustListManager(final @NotNull List<X509Certificate> trustedCerts) {
        this.trustedCerts = List.copyOf(trustedCerts);
        this.dateTime = DateTime.now();
    }

    @Override
    public @NotNull List<X509CRL> getIssuerCrls() {
        return List.of();
    }

    @Override
    public void setIssuerCrls(final @NotNull List<X509CRL> issuerCrls) {
        //no-op
    }

    @Override
    public @NotNull List<X509CRL> getTrustedCrls() {
        return List.of();
    }

    @Override
    public void setTrustedCrls(final @NotNull List<X509CRL> trustedCrls) {
        //no-op
    }

    @Override
    public @NotNull List<X509Certificate> getIssuerCertificates() {
        //allowed for chain building, but not "trusted"
        return List.of();
    }

    @Override
    public void setIssuerCertificates(final @NotNull List<X509Certificate> issuerCertificates) {
        //no-op
    }

    @Override
    public @NotNull List<X509Certificate> getTrustedCertificates() {
        //"trusted" certs
        return List.copyOf(trustedCerts);
    }

    @Override
    public void setTrustedCertificates(final @NotNull List<X509Certificate> trustedCertificates) {
        //no-op
    }

    @Override
    public void addIssuerCertificate(final @NotNull X509Certificate certificate) {
        //no-op
    }

    @Override
    public void addTrustedCertificate(final @NotNull X509Certificate certificate) {
        //no-op
    }

    @Override
    public boolean removeIssuerCertificate(final @NotNull ByteString thumbprint) {
        return false;
    }

    @Override
    public boolean removeTrustedCertificate(final @NotNull ByteString thumbprint) {
        return false;
    }

    @Override
    public @NotNull DateTime getLastUpdateTime() {
        return dateTime;
    }
}
