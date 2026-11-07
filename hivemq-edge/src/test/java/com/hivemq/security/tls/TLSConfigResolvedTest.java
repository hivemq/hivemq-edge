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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// That a reference into declared material is replaced by the material itself, so nothing after this
/// stage needs to know a reference was involved.
class TLSConfigResolvedTest {

    private static final @NotNull MaterialRef.Inline TRUSTSTORE =
            new MaterialRef.Inline("jks", FileSet.of("/plant/ca.jks"), new Secret.Literal("pw"), null, null);

    @Test
    void whenAReferenceIsResolved_thenTheMaterialReplacesIt() throws Exception {
        final TLSMaterialRegistry registry = registryHolding(TLSMaterialRegistry.Kind.TRUSTSTORE, TRUSTSTORE);
        final StubConfig written = new StubConfig(new MaterialRef.ByName("plant", "authorities"));

        final TLSConfigResolved resolved = new TLSConfigResolved(written, registry);

        assertThat(resolved.truststore()).contains(TRUSTSTORE);
    }

    @Test
    void whenMaterialIsAlreadyInline_thenItIsUnchanged() throws Exception {
        final StubConfig written = new StubConfig(TRUSTSTORE);

        final TLSConfigResolved resolved = new TLSConfigResolved(written, TLSMaterialRegistry.empty());

        assertThat(resolved.truststore()).contains(TRUSTSTORE);
    }

    @Test
    void whenAReferenceNamesNothing_thenResolvingIsRefused() {
        final StubConfig written = new StubConfig(new MaterialRef.ByName("plant", "authorities"));

        assertThatThrownBy(() -> new TLSConfigResolved(written, TLSMaterialRegistry.empty()))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class)
                .hasMessageContaining("authorities");
    }

    @Test
    void whenTheTrustModeWouldNeverReadIt_thenTheReferenceIsStillResolved() {
        // A reference that names nothing is a mistake whether or not the material would have been
        // opened, and an operator who changes the trust mode later should not discover it then.
        final StubConfig written = new StubConfig(new MaterialRef.ByName("plant", "authorities")) {
            @Override
            public @NotNull TLSConstants.TrustMode trustMode() {
                return TLSConstants.TrustMode.ANY_CERT;
            }
        };

        assertThatThrownBy(() -> new TLSConfigResolved(written, TLSMaterialRegistry.empty()))
                .isInstanceOf(TLSMaterialRegistry.InvalidReferenceException.class);
    }

    @Test
    void whenResolved_thenTheChecksPassThroughUnchanged() throws Exception {
        // Resolving a reference says nothing about which checks apply.
        final StubConfig written = new StubConfig(TRUSTSTORE);

        final TLSConfigResolved resolved = new TLSConfigResolved(written, TLSMaterialRegistry.empty());

        assertThat(resolved.trustMode()).isEqualTo(written.trustMode());
        assertThat(resolved.sanUri()).isEqualTo(written.sanUri());
        assertThat(resolved.hostname()).isEqualTo(written.hostname());
        assertThat(resolved.validity()).isEqualTo(written.validity());
        assertThat(resolved.revocation()).isEqualTo(written.revocation());
        assertThat(resolved.keyUsage()).isEqualTo(written.keyUsage());
        assertThat(resolved.isEnabled()).isEqualTo(written.isEnabled());
    }

    @Test
    void whenAPolicyIsBuiltFromAResolvedConfig_thenNoReferenceReachesIt() throws Exception {
        // TLSPolicy refuses a reference, so this is what makes a referenced truststore readable at
        // all. Pinned because resolving in the wrong order fails only at the file-reading stage.
        final TLSMaterialRegistry registry = registryHolding(TLSMaterialRegistry.Kind.TRUSTSTORE, TRUSTSTORE);
        final StubConfig written = new StubConfig(new MaterialRef.ByName("plant", "authorities"));

        final TLSConfigResolved resolved = new TLSConfigResolved(written, registry);

        assertThat(resolved.truststore().orElseThrow()).isInstanceOf(MaterialRef.Inline.class);
    }

    private static @NotNull TLSMaterialRegistry registryHolding(
            final @NotNull TLSMaterialRegistry.Kind kind, final @NotNull MaterialRef.Inline material)
            throws TLSMaterialRegistry.InvalidReferenceException {
        final TLSMaterialRegistry.Builder builder = new TLSMaterialRegistry.Builder();
        final String name = builder.material("plant", null);
        builder.entry(name, kind, "authorities", null, material);
        return builder.build();
    }

    /// A configuration naming one truststore and nothing else, so each test varies one thing.
    private static class StubConfig implements TLSConfig {

        private final @NotNull MaterialRef truststore;

        StubConfig(final @NotNull MaterialRef truststore) {
            this.truststore = truststore;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public @NotNull TLSConstants.TrustMode trustMode() {
            return TLSConstants.TrustMode.CHAIN;
        }

        @Override
        public @NotNull TLSConstants.SanUriCheck sanUri() {
            return TLSConstants.SanUriCheck.APPLICATION_URI;
        }

        @Override
        public @NotNull TLSConstants.HostnameCheck hostname() {
            return TLSConstants.HostnameCheck.HOSTNAME;
        }

        @Override
        public @NotNull TLSConstants.ValidityCheck validity() {
            return TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER;
        }

        @Override
        public @NotNull TLSConstants.RevocationCheck revocation() {
            return TLSConstants.RevocationCheck.NONE;
        }

        @Override
        public @NotNull TLSConstants.KeyUsageCheck keyUsage() {
            return TLSConstants.KeyUsageCheck.SERVER_AUTH;
        }

        @Override
        public @NotNull Optional<MaterialRef> identity() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<MaterialRef> truststore() {
            return Optional.of(truststore);
        }

        @Override
        public @NotNull Optional<MaterialRef> allowList() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<MaterialRef> revocationList() {
            return Optional.empty();
        }

        @Override
        public @NotNull List<String> protocols() {
            return List.of();
        }

        @Override
        public @NotNull List<String> cipherSuites() {
            return List.of();
        }

        @Override
        public @NotNull Duration handshakeTimeout() {
            return Duration.ZERO;
        }
    }
}
