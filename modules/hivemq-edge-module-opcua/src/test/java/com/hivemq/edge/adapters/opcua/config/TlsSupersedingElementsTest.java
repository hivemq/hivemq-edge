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
package com.hivemq.edge.adapters.opcua.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.security.tls.FileSet;
import com.hivemq.security.tls.MaterialRef;
import com.hivemq.security.tls.Secret;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The superseding `tls-*` material elements, which reach what the older `keystore`, `truststore`,
/// `allowList` and `revocationList` cannot express: PEM, several files, directories, glob patterns,
/// the runtime's own authorities, passwords read from files, and references to shared material.
///
/// Also that a configuration mixing the two spellings is refused rather than given a precedence.
class TlsSupersedingElementsTest {

    @Test
    void whenAPemIdentityNamesItsThreeFiles_thenAllThreeReachTheMaterial() throws Exception {
        // The gap the superseding spelling exists to close: PEM has no container, so the key, the
        // certificate and the intermediates are named separately and the older element's single
        // 'path' cannot hold them.
        final TLSConfigOpcUA config = new TLSConfigOpcUA(Tls.superseding(
                true,
                TlsChecks.NO_VERIFICATION,
                null,
                new TlsIdentity(
                        MaterialFormat.PEM,
                        null,
                        "/etc/edge/client.key",
                        "/etc/edge/client.crt",
                        "/etc/edge/chain.crt",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "key-pw",
                        null,
                        null,
                        null,
                        java.util.Map.of()),
                null,
                null,
                null));

        final MaterialRef.Inline identity =
                (MaterialRef.Inline) config.identity().orElseThrow();
        assertThat(identity.format()).isEqualTo("pem");
        assertThat(identity.files().files())
                .containsExactly("/etc/edge/client.key", "/etc/edge/client.crt", "/etc/edge/chain.crt");
        assertThat(identity.entryPassword()).isEqualTo(new Secret.Literal("key-pw"));
    }

    @Test
    void whenAnIdentityNamesAPasswordFile_thenItIsReadFromThere() throws Exception {
        final TLSConfigOpcUA config = new TLSConfigOpcUA(Tls.superseding(
                true,
                TlsChecks.NO_VERIFICATION,
                null,
                new TlsIdentity(
                        MaterialFormat.P12,
                        "/etc/edge/client.p12",
                        null,
                        null,
                        null,
                        null,
                        "/run/secrets/store-pw",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        java.util.Map.of()),
                null,
                null,
                null));

        final MaterialRef.Inline identity =
                (MaterialRef.Inline) config.identity().orElseThrow();
        assertThat(identity.password()).isEqualTo(new Secret.FromFile("/run/secrets/store-pw"));
    }

    @Test
    void whenAnIdentityNamesBothAPasswordAndAPasswordFile_thenItIsRefused() {
        assertThatThrownBy(() -> new TLSConfigOpcUA(Tls.superseding(
                        true,
                        TlsChecks.NO_VERIFICATION,
                        null,
                        new TlsIdentity(
                                MaterialFormat.P12,
                                "/etc/edge/client.p12",
                                null,
                                null,
                                null,
                                "written",
                                "/run/secrets/store-pw",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                java.util.Map.of()),
                        null,
                        null,
                        null)))
                .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                .hasMessageContaining("tls-identity")
                .hasMessageContaining("password file");
    }

    @Test
    void whenATruststoreNamesSeveralSources_thenAllOfThemReachTheMaterial() throws Exception {
        // A surface trusting a plant authority and the public ones together names both and gets the
        // union, which the older element's single container cannot express.
        final TLSConfigOpcUA config = new TLSConfigOpcUA(Tls.superseding(
                true,
                TlsChecks.STANDARD,
                null,
                null,
                new TlsTruststore(
                        MaterialFormat.PEM,
                        List.of("/etc/edge/plant-ca.pem"),
                        List.of(new FileLocation("/etc/edge/ca.d", true)),
                        List.of(new FileLocation("/etc/edge/*.pem")),
                        List.of(FileSet.JVM_CACERTS),
                        null,
                        null,
                        true,
                        null,
                        null),
                null,
                null));

        final MaterialRef.Inline truststore =
                (MaterialRef.Inline) config.truststore().orElseThrow();
        assertThat(truststore.format()).isEqualTo("pem");
        assertThat(truststore.files().files()).containsExactly("/etc/edge/plant-ca.pem");
        assertThat(truststore.files().directories()).containsExactly(new FileSet.Directory("/etc/edge/ca.d", true));
        assertThat(truststore.files().patterns()).containsExactly(new FileSet.Pattern("/etc/edge/*.pem", false));
        assertThat(truststore.files().builtins()).containsExactly(FileSet.JVM_CACERTS);
        // skip-integrity-check is carried as the waiver, which the reading stage opens unchecked
        // rather than with an empty password.
        assertThat(truststore.password()).isEqualTo(new Secret.Waived());
    }

    @Test
    void whenAnAllowListNamesADirectory_thenItReachesTheMaterial() throws Exception {
        final TLSConfigOpcUA config = new TLSConfigOpcUA(Tls.superseding(
                true,
                TlsChecks.SELF_SIGNED,
                null,
                null,
                null,
                new TlsFileList(null, List.of(new FileLocation("/etc/edge/pinned.d")), null, null, null),
                null));

        final MaterialRef.Inline allowList =
                (MaterialRef.Inline) config.allowList().orElseThrow();
        assertThat(allowList.files().directories()).containsExactly(new FileSet.Directory("/etc/edge/pinned.d", false));
    }

    @Test
    void whenASupersedingElementReferencesSharedMaterial_thenItNamesTheEntry() throws Exception {
        final TLSConfigOpcUA config = new TLSConfigOpcUA(Tls.superseding(
                true,
                TlsChecks.STANDARD,
                null,
                TlsIdentity.referencing("plant::gateway", null),
                new TlsTruststore(null, null, null, null, null, null, null, null, null, "a-uuid"),
                null,
                null));

        assertThat(config.identity()).contains(new MaterialRef.ByName("plant", "gateway"));
        assertThat(config.truststore()).contains(new MaterialRef.ByUuid("a-uuid"));
    }

    @Test
    void whenBothSpellingsNameMaterial_thenTheConfigurationIsRefused() {
        // Not translatable rather than untidy: there is no rule that could combine a keystore with a
        // tls-truststore and still mean one thing, and a precedence would leave an operator reading
        // elements that do nothing.
        // The canonical constructor, since this is the one configuration that names both spellings
        // and so neither convenience form can express it.
        assertThatThrownBy(() -> new TLSConfigOpcUA(new Tls(
                        true,
                        TlsChecks.STANDARD,
                        null,
                        new Keystore("/etc/edge/client.jks", "pw", "pw"),
                        null,
                        null,
                        null,
                        null,
                        new TlsTruststore(
                                null, List.of("/etc/edge/ca.pem"), null, null, null, null, null, null, null, null),
                        null,
                        null,
                        java.util.Map.of())))
                .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                .hasMessageContaining("keystore")
                .hasMessageContaining("tls-truststore")
                .hasMessageContaining("mutually exclusive");
    }

    @Test
    void whenOnlyTheSupersededSpellingIsUsed_thenItKeepsWorking() throws Exception {
        // Every configuration that exists today uses this spelling, read as JKS with its two
        // passwords, and nothing about it changes.
        final TLSConfigOpcUA config = new TLSConfigOpcUA(new Tls(
                true,
                TlsChecks.STANDARD,
                null,
                new Keystore("/etc/edge/client.jks", "store-pw", "key-pw"),
                new Truststore("/etc/edge/trust.jks", "trust-pw"),
                new AllowList("/etc/edge/pinned.txt"),
                new RevocationList("/etc/edge/crl.pem")));

        final MaterialRef.Inline identity =
                (MaterialRef.Inline) config.identity().orElseThrow();
        assertThat(identity.format()).isEqualTo("jks");
        assertThat(identity.files().files()).containsExactly("/etc/edge/client.jks");
        assertThat(identity.password()).isEqualTo(new Secret.Literal("store-pw"));
        assertThat(identity.entryPassword()).isEqualTo(new Secret.Literal("key-pw"));

        final MaterialRef.Inline truststore =
                (MaterialRef.Inline) config.truststore().orElseThrow();
        assertThat(truststore.format()).isEqualTo("jks");
        assertThat(truststore.files().files()).containsExactly("/etc/edge/trust.jks");
    }

    @Test
    void whenTheSupersedingTruststoreIsAbsent_thenTheRuntimesOwnAuthoritiesAreStillUsed() throws Exception {
        // The compatibility fallback applies to both spellings: a path is built and nothing was named
        // to build it to.
        final TLSConfigOpcUA config = new TLSConfigOpcUA(Tls.superseding(
                true, TlsChecks.STANDARD, null, TlsIdentity.referencing("plant::gateway", null), null, null, null));

        final MaterialRef.Inline truststore =
                (MaterialRef.Inline) config.truststore().orElseThrow();
        assertThat(truststore.files().builtins()).containsExactly(FileSet.JVM_CACERTS);
    }

    @Test
    void whenFingerprintTrustNamesTheSupersedingAllowList_thenItIsAccepted() throws Exception {
        final TLSConfigOpcUA config = new TLSConfigOpcUA(Tls.superseding(
                true,
                TlsChecks.SELF_SIGNED,
                null,
                null,
                null,
                new TlsFileList(List.of("/etc/edge/pinned.txt"), null, null, null, null),
                null));

        assertThat(config.allowList()).isPresent();
    }

    @Test
    void whenAnUnknownEntryIsWritten_thenTheAdapterIsRefused() {
        // The application-wide mapper drops an unknown property silently, so without the trap a
        // misspelled element would leave this material unconfigured.
        assertThatThrownBy(() -> new TLSConfigOpcUA(Tls.superseding(
                        true,
                        TlsChecks.NO_VERIFICATION,
                        null,
                        new TlsIdentity(
                                java.util.Map.of("key-fil", "/etc/edge/client.key"),
                                MaterialFormat.PEM,
                                null,
                                null,
                                "/etc/edge/client.crt",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null),
                        null,
                        null,
                        null)))
                .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                .hasMessageContaining("tls-identity")
                .hasMessageContaining("key-fil");
    }
}
