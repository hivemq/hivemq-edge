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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.eclipse.milo.opcua.stack.core.security.TrustListManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;

import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.List;

public class CertificateTrustListManager implements TrustListManager {

    private final @NotNull ImmutableList<X509Certificate> trustedCerts;

    public CertificateTrustListManager(final @NotNull List<X509Certificate> trustedCerts) {
        this.trustedCerts = ImmutableList.copyOf(trustedCerts);
    }

    @Override
    public ImmutableList<X509CRL> getIssuerCrls() {
        return ImmutableList.of();
    }

    @Override
    public ImmutableList<X509CRL> getTrustedCrls() {
        return ImmutableList.of();
    }

    @Override
    public ImmutableList<X509Certificate> getIssuerCertificates() {
        //allowed for chain building, but not "trusted"
        return ImmutableList.of();
    }

    @Override
    public ImmutableList<X509Certificate> getTrustedCertificates() {
        //"trusted" certs
        return ImmutableList.copyOf(trustedCerts);
    }

    @Override
    public ImmutableList<X509Certificate> getRejectedCertificates() {
        return ImmutableList.of();
    }

    @Override
    public void setIssuerCrls(final List<X509CRL> issuerCrls) {
        //no-op
    }

    @Override
    public void setTrustedCrls(final List<X509CRL> trustedCrls) {
        //no-op
    }

    @Override
    public void setIssuerCertificates(final List<X509Certificate> issuerCertificates) {
        //no-op
    }

    @Override
    public void setTrustedCertificates(final List<X509Certificate> trustedCertificates) {
        //no-op
    }

    @Override
    public void addIssuerCertificate(final X509Certificate certificate) {
        //no-op
    }

    @Override
    public void addTrustedCertificate(final X509Certificate certificate) {
        //no-op
    }

    @Override
    public void addRejectedCertificate(final X509Certificate certificate) {
        //no-op
    }

    @Override
    public boolean removeIssuerCertificate(final ByteString thumbprint) {
        return false;
    }

    @Override
    public boolean removeTrustedCertificate(final ByteString thumbprint) {
        return false;
    }

    @Override
    public boolean removeRejectedCertificate(final ByteString thumbprint) {
        return false;
    }
}
