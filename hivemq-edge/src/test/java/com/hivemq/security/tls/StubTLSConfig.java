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
package com.hivemq.security.tls;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/// A TLS configuration holding the checks and material references a test gives, for tests that build
/// a [TLSPolicy] from material in hand or read the references through one. It is always enabled, with
/// no protocol or cipher restriction.
record StubTLSConfig(
        @NotNull TLSConstants.TrustMode trustMode,
        @NotNull TLSConstants.SanUriCheck sanUri,
        @NotNull TLSConstants.HostnameCheck hostname,
        @NotNull TLSConstants.ValidityCheck validity,
        @NotNull TLSConstants.RevocationCheck revocation,
        @NotNull TLSConstants.KeyUsageCheck keyUsage,
        @NotNull Optional<MaterialRef> identity,
        @NotNull Optional<MaterialRef> truststore,
        @NotNull Optional<MaterialRef> allowList,
        @NotNull Optional<MaterialRef> revocationList)
        implements TLSConfig {

    @Override
    public @NotNull List<String> cipherSuites() {
        return List.of();
    }

    @Override
    public @NotNull Duration handshakeTimeout() {
        return Duration.ZERO;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public @NotNull List<String> protocols() {
        return List.of();
    }

    /// Chain trust, with every check but validity off and no material named.
    static @NotNull StubTLSConfig chain() {
        return of(TLSConstants.TrustMode.CHAIN);
    }

    /// The given trust mode, with every check but validity off and no material named, so a test turns
    /// on only what it is about.
    static @NotNull StubTLSConfig of(final @NotNull TLSConstants.TrustMode trustMode) {
        return new StubTLSConfig(
                trustMode,
                TLSConstants.SanUriCheck.NONE,
                TLSConstants.HostnameCheck.NONE,
                TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER,
                TLSConstants.RevocationCheck.NONE,
                TLSConstants.KeyUsageCheck.NONE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    /// The given trust mode, with every certificate check at its strictest, revocation off, and no
    /// material named.
    static @NotNull StubTLSConfig strictest(final @NotNull TLSConstants.TrustMode trustMode) {
        return new StubTLSConfig(
                trustMode,
                TLSConstants.SanUriCheck.STRICTEST,
                TLSConstants.HostnameCheck.STRICTEST,
                TLSConstants.ValidityCheck.STRICTEST,
                TLSConstants.RevocationCheck.NONE,
                TLSConstants.KeyUsageCheck.STRICTEST,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    @NotNull
    StubTLSConfig withAllowList(final @NotNull MaterialRef reference) {
        return new StubTLSConfig(
                trustMode,
                sanUri,
                hostname,
                validity,
                revocation,
                keyUsage,
                identity,
                truststore,
                Optional.of(reference),
                revocationList);
    }

    @NotNull
    StubTLSConfig withHostname(final @NotNull TLSConstants.HostnameCheck check) {
        return new StubTLSConfig(
                trustMode,
                sanUri,
                check,
                validity,
                revocation,
                keyUsage,
                identity,
                truststore,
                allowList,
                revocationList);
    }

    @NotNull
    StubTLSConfig withIdentity(final @NotNull MaterialRef reference) {
        return new StubTLSConfig(
                trustMode,
                sanUri,
                hostname,
                validity,
                revocation,
                keyUsage,
                Optional.of(reference),
                truststore,
                allowList,
                revocationList);
    }

    @NotNull
    StubTLSConfig withKeyUsage(final @NotNull TLSConstants.KeyUsageCheck check) {
        return new StubTLSConfig(
                trustMode,
                sanUri,
                hostname,
                validity,
                revocation,
                check,
                identity,
                truststore,
                allowList,
                revocationList);
    }

    @NotNull
    StubTLSConfig withRevocation(final @NotNull TLSConstants.RevocationCheck check) {
        return new StubTLSConfig(
                trustMode,
                sanUri,
                hostname,
                validity,
                check,
                keyUsage,
                identity,
                truststore,
                allowList,
                revocationList);
    }

    @NotNull
    StubTLSConfig withRevocationList(final @NotNull MaterialRef reference) {
        return new StubTLSConfig(
                trustMode,
                sanUri,
                hostname,
                validity,
                revocation,
                keyUsage,
                identity,
                truststore,
                allowList,
                Optional.of(reference));
    }

    @NotNull
    StubTLSConfig withTruststore(final @NotNull MaterialRef reference) {
        return new StubTLSConfig(
                trustMode,
                sanUri,
                hostname,
                validity,
                revocation,
                keyUsage,
                identity,
                Optional.of(reference),
                allowList,
                revocationList);
    }

    @NotNull
    StubTLSConfig withValidity(final @NotNull TLSConstants.ValidityCheck check) {
        return new StubTLSConfig(
                trustMode,
                sanUri,
                hostname,
                check,
                revocation,
                keyUsage,
                identity,
                truststore,
                allowList,
                revocationList);
    }
}
