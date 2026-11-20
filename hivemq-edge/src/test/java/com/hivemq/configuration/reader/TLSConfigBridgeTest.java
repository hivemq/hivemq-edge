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
package com.hivemq.configuration.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.bridge.BridgeTlsEntity;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.security.tls.FileSet;
import com.hivemq.security.tls.MaterialRef;
import com.hivemq.security.tls.Secret;
import com.hivemq.security.tls.TLSConfig;
import com.hivemq.security.tls.TLSConfigBridge;
import com.hivemq.security.tls.TLSConstants;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// A bridge's TLS configuration, read from real configuration text through the real reader.
///
/// Parsed rather than hand-built, because the element names and the schema are exactly what could be
/// wrong: these are the first hyphenated TLS elements a bridge has had, and a name that does not
/// match what an operator types would bind to nothing.
class TLSConfigBridgeTest extends AbstractConfigurationTest {

    // ----- the superseded spelling keeps working -----

    @Test
    void whenOnlyTheOlderElementsAreWritten_thenTheyStillWork() throws Exception {
        final TLSConfigBridge config = configFrom("""
                <tls>
                    <enabled>true</enabled>
                    <keystore>
                        <path>/etc/edge/client.jks</path>
                        <password>store-pw</password>
                        <private-key-password>key-pw</private-key-password>
                    </keystore>
                    <truststore>
                        <path>/etc/edge/trust.jks</path>
                        <password>trust-pw</password>
                    </truststore>
                </tls>
                """);

        final MaterialRef.Inline identity =
                (MaterialRef.Inline) config.identity().orElseThrow();
        assertThat(identity.files().files()).containsExactly("/etc/edge/client.jks");
        assertThat(identity.password()).isEqualTo(new Secret.Literal("store-pw"));
        assertThat(identity.entryPassword()).isEqualTo(new Secret.Literal("key-pw"));

        final MaterialRef.Inline truststore =
                (MaterialRef.Inline) config.truststore().orElseThrow();
        assertThat(truststore.files().files()).containsExactly("/etc/edge/trust.jks");
    }

    @Test
    void whenNoChecksAreWritten_thenTheBridgeValidatesWhatItAlwaysDid() throws Exception {
        // A path built to the configured authorities, validity enforced, no revocation, no key
        // usage, hostname on. Changing any of these would change what an existing bridge checks.
        final TLSConfigBridge config = configFrom("""
                <tls>
                    <enabled>true</enabled>
                    <truststore><path>/t.jks</path><password>pw</password></truststore>
                </tls>
                """);

        assertThat(config.trustMode()).isEqualTo(TLSConstants.TrustMode.CHAIN);
        assertThat(config.hostname()).isEqualTo(TLSConstants.HostnameCheck.HOSTNAME);
        assertThat(config.validity()).isEqualTo(TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER);
        assertThat(config.revocation()).isEqualTo(TLSConstants.RevocationCheck.NONE);
        assertThat(config.keyUsage()).isEqualTo(TLSConstants.KeyUsageCheck.NONE);
        // A bridge never performs the application-identifier check: MQTT sends no such identifier.
        assertThat(config.sanUri()).isEqualTo(TLSConstants.SanUriCheck.NONE);
    }

    @Test
    void whenHostnameVerificationIsTurnedOff_thenOnlyThatCheckChanges() throws Exception {
        final TLSConfigBridge config = configFrom("""
                <tls>
                    <enabled>true</enabled>
                    <verify-hostname>false</verify-hostname>
                    <truststore><path>/t.jks</path><password>pw</password></truststore>
                </tls>
                """);

        assertThat(config.hostname()).isEqualTo(TLSConstants.HostnameCheck.NONE);
        assertThat(config.validity()).isEqualTo(TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER);
        assertThat(config.trustMode()).isEqualTo(TLSConstants.TrustMode.CHAIN);
    }

    @Test
    void whenNoTruststoreIsWritten_thenTheRuntimesOwnAuthoritiesAreUsed() throws Exception {
        // What a bridge to a cloud broker relies on: no truststore meant no trust manager, which
        // the JDK reads as its own authorities.
        final TLSConfigBridge config = configFrom("""
                <tls><enabled>true</enabled></tls>
                """);

        final MaterialRef.Inline truststore =
                (MaterialRef.Inline) config.truststore().orElseThrow();
        assertThat(truststore.files().builtins()).containsExactly(FileSet.JVM_CACERTS);
    }

    @Test
    void whenAStoreIsNamedWithAPkcs12Suffix_thenThatFormatIsUsed() throws Exception {
        // The older spelling has no format element, so the format comes from the file name.
        final TLSConfigBridge config = configFrom("""
                <tls>
                    <enabled>true</enabled>
                    <keystore><path>/c.p12</path><password>p</password><private-key-password>k</private-key-password></keystore>
                    <truststore><path>/t.PFX</path><password>q</password></truststore>
                </tls>
                """);

        assertThat(((MaterialRef.Inline) config.identity().orElseThrow()).format())
                .isEqualTo("p12");
        // Case-insensitive, unlike the shipped version: an upper-case suffix is the same file.
        assertThat(((MaterialRef.Inline) config.truststore().orElseThrow()).format())
                .isEqualTo("p12");
    }

    @Test
    void whenAStoreHasNoKnownSuffix_thenAJavaKeyStoreIsAssumed() throws Exception {
        final TLSConfigBridge config = configFrom("""
                <tls>
                    <enabled>true</enabled>
                    <truststore><path>/etc/edge/secrets/truststore</path><password>q</password></truststore>
                </tls>
                """);

        assertThat(((MaterialRef.Inline) config.truststore().orElseThrow()).format())
                .isEqualTo("jks");
    }

    // ----- the superseding spelling -----

    @Test
    void whenAPemIdentityIsWritten_thenItsThreeFilesReachTheMaterial() throws Exception {
        // The gap the newer spelling closes: PEM has no container, so the key, the certificate and
        // the intermediates are named separately.
        final TLSConfigBridge config = configFrom("""
                <tls>
                    <enabled>true</enabled>
                    <tls-identity>
                        <format>pem</format>
                        <key-file>/etc/edge/client.key</key-file>
                        <certificate-file>/etc/edge/client.crt</certificate-file>
                        <chain-file>/etc/edge/chain.crt</chain-file>
                        <key-password-file>/run/secrets/key-pw</key-password-file>
                    </tls-identity>
                    <tls-truststore><file>/etc/edge/ca.pem</file><format>pem</format><skip-integrity-check>true</skip-integrity-check></tls-truststore>
                </tls>
                """);

        final MaterialRef.Inline identity =
                (MaterialRef.Inline) config.identity().orElseThrow();
        assertThat(identity.format()).isEqualTo("pem");
        assertThat(identity.files().files())
                .containsExactly("/etc/edge/client.key", "/etc/edge/client.crt", "/etc/edge/chain.crt");
        assertThat(identity.entryPassword()).isEqualTo(new Secret.FromFile("/run/secrets/key-pw"));
    }

    @Test
    void whenATruststoreNamesSeveralSources_thenAllOfThemReachTheMaterial() throws Exception {
        final TLSConfigBridge config = configFrom("""
                <tls>
                    <enabled>true</enabled>
                    <tls-truststore>
                        <file>/plant-ca.pem</file>
                        <directory><location>/ca.d</location><may-be-empty>true</may-be-empty></directory>
                        <pattern><location>/etc/*.pem</location></pattern>
                        <builtin>JVM-cacerts</builtin>
                        <format>pem</format>
                        <skip-integrity-check>true</skip-integrity-check>
                    </tls-truststore>
                </tls>
                """);

        final MaterialRef.Inline truststore =
                (MaterialRef.Inline) config.truststore().orElseThrow();
        assertThat(truststore.files().files()).containsExactly("/plant-ca.pem");
        assertThat(truststore.files().directories()).containsExactly(new FileSet.Directory("/ca.d", true));
        assertThat(truststore.files().patterns()).containsExactly(new FileSet.Pattern("/etc/*.pem", false));
        assertThat(truststore.files().builtins()).containsExactly(FileSet.JVM_CACERTS);
    }

    @Test
    void whenAPresetIsWritten_thenItSetsAllFiveChecks() throws Exception {
        final TLSConfigBridge config = configFrom("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>ALL</tls-checks>
                    <tls-truststore><file>/ca.pem</file><format>pem</format><skip-integrity-check>true</skip-integrity-check></tls-truststore>
                    <tls-revocationlist><file>/crl.pem</file></tls-revocationlist>
                </tls>
                """);

        assertThat(config.trustMode()).isEqualTo(TLSConstants.TrustMode.CHAIN);
        assertThat(config.hostname()).isEqualTo(TLSConstants.HostnameCheck.HOSTNAME);
        assertThat(config.revocation()).isEqualTo(TLSConstants.RevocationCheck.REQUIRE_CRLS);
        assertThat(config.keyUsage()).isEqualTo(TLSConstants.KeyUsageCheck.SERVER_AUTH);
    }

    /// One preset name means one set of values across Edge, so STANDARD on a bridge sets what
    /// STANDARD on an adapter sets, for the four checks a bridge has. `hostname=NONE` is the row
    /// that is easy to get wrong: a bridge checked the hostname before this model existed, and
    /// carrying that into STANDARD would give the name a second meaning. A bridge keeps the old
    /// behaviour through TLS_BRIDGE_DEFAULT, which is what a bridge writing no checks gets.
    @Test
    void standardSetsWhatItSetsOnEveryOtherSurface() throws Exception {
        final TLSConfigBridge config = configFrom("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>STANDARD</tls-checks>
                    <tls-truststore><file>/ca.pem</file><format>pem</format><skip-integrity-check>true</skip-integrity-check></tls-truststore>
                    <tls-revocationlist><file>/crl.pem</file></tls-revocationlist>
                </tls>
                """);

        assertThat(config.trustMode()).isEqualTo(TLSConstants.TrustMode.CHAIN);
        assertThat(config.hostname()).isEqualTo(TLSConstants.HostnameCheck.NONE);
        assertThat(config.validity()).isEqualTo(TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER);
        assertThat(config.revocation()).isEqualTo(TLSConstants.RevocationCheck.REQUIRE_CRLS);
        assertThat(config.keyUsage()).isEqualTo(TLSConstants.KeyUsageCheck.NONE);
    }

    @Test
    void whenTheChecksAreWrittenIndividually_thenOmittedOnesTakeTheStrictestValue() throws Exception {
        // A forgotten entry must yield more verification, never less.
        final TLSConfigBridge config = configFrom("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks-full><hostname>NONE</hostname></tls-checks-full>
                    <tls-truststore><file>/ca.pem</file><format>pem</format><skip-integrity-check>true</skip-integrity-check></tls-truststore>
                    <tls-revocationlist><file>/crl.pem</file></tls-revocationlist>
                </tls>
                """);

        assertThat(config.hostname()).isEqualTo(TLSConstants.HostnameCheck.NONE);
        assertThat(config.trustMode()).isEqualTo(TLSConstants.TrustMode.STRICTEST);
        assertThat(config.validity()).isEqualTo(TLSConstants.ValidityCheck.STRICTEST);
        assertThat(config.revocation()).isEqualTo(TLSConstants.RevocationCheck.STRICTEST);
        assertThat(config.keyUsage()).isEqualTo(TLSConstants.KeyUsageCheck.STRICTEST);
    }

    @Test
    void whenFingerprintTrustIsUsed_thenTheAllowListReachesTheMaterial() throws Exception {
        final TLSConfigBridge config = configFrom("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>SELF_SIGNED</tls-checks>
                    <tls-allowlist><file>/pinned.txt</file></tls-allowlist>
                </tls>
                """);

        assertThat(config.trustMode()).isEqualTo(TLSConstants.TrustMode.ALLOW_LIST);
        assertThat(((MaterialRef.Inline) config.allowList().orElseThrow())
                        .files()
                        .files())
                .containsExactly("/pinned.txt");
    }

    @Test
    void whenMaterialIsReferenced_thenTheDeclaredMaterialIsUsed() throws Exception {
        // A bridge is the first surface where a reference resolves end to end: the declared material
        // and the bridge are read together, so nothing has to carry one to the other.
        final BridgeTls bridge = bridgeFrom("""
                <tls>
                    <enabled>true</enabled>
                    <tls-identity><reference-name>plant::gateway</reference-name></tls-identity>
                    <tls-truststore><reference-uuid>a3f1c8e2-7b44-4e91-9c2d-6f5a81b30e47</reference-uuid></tls-truststore>
                </tls>
                """, """
                <tls-materials>
                    <tls-material>
                        <name>plant</name>
                        <tls-identity>
                            <name>gateway</name>
                            <format>p12</format>
                            <file>/etc/edge/gateway.p12</file>
                            <password>secret</password>
                        </tls-identity>
                        <tls-truststore>
                            <name>authorities</name>
                            <uuid>a3f1c8e2-7b44-4e91-9c2d-6f5a81b30e47</uuid>
                            <format>pem</format>
                            <file>/etc/edge/plant-ca.pem</file>
                            <skip-integrity-check>true</skip-integrity-check>
                        </tls-truststore>
                    </tls-material>
                </tls-materials>
                """);

        final TLSConfig effective = bridge.getEffectiveTls();
        assertThat(effective).isNotNull();
        // Resolved, not merely parsed: what reaches the connection names files, as if the operator
        // had written the material inside the bridge.
        final MaterialRef.Inline identity =
                (MaterialRef.Inline) effective.identity().orElseThrow();
        assertThat(identity.files().files()).containsExactly("/etc/edge/gateway.p12");
        final MaterialRef.Inline truststore =
                (MaterialRef.Inline) effective.truststore().orElseThrow();
        assertThat(truststore.files().files()).containsExactly("/etc/edge/plant-ca.pem");
    }

    // ----- what is refused -----
    //
    // A bridge whose TLS cannot be carried out refuses the whole configuration, as an invalid bridge
    // id does. So these assert that reading the file fails; the messages are pinned by the direct
    // cases further down, which build the entity and call the derivation themselves.

    @Test
    void whenBothSpellingsNameMaterial_thenTheConfigurationIsRefused() throws Exception {
        assertRefused("""
                <tls>
                    <enabled>true</enabled>
                    <keystore><path>/c.jks</path><password>p</password><private-key-password>k</private-key-password></keystore>
                    <tls-truststore><file>/ca.pem</file><format>pem</format><skip-integrity-check>true</skip-integrity-check></tls-truststore>
                </tls>
                """);
    }

    @Test
    void whenBothSpellingsOfTheChecksAreWritten_thenTheConfigurationIsRefused() throws Exception {
        assertRefused("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>ALL</tls-checks>
                    <tls-checks-full><hostname>NONE</hostname></tls-checks-full>
                </tls>
                """);
    }

    @Test
    void whenHostnameVerificationIsWrittenBesideTheNewChecks_thenTheConfigurationIsRefused() throws Exception {
        // Both decide the hostname check, and they can contradict each other.
        assertRefused("""
                <tls>
                    <enabled>true</enabled>
                    <verify-hostname>false</verify-hostname>
                    <tls-checks>ALL</tls-checks>
                </tls>
                """);
    }

    @Test
    void whenRevocationIsDemandedWithoutAPath_thenTheConfigurationIsRefused() throws Exception {
        // Revocation is decided while a certification path is built, and fingerprint trust builds
        // none, so the check would quietly do nothing.
        assertRefused("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks-full>
                        <trust-mode>ALLOW_LIST</trust-mode>
                        <revocation>REQUIRE_CRLS</revocation>
                    </tls-checks-full>
                    <tls-allowlist><file>/pinned.txt</file></tls-allowlist>
                </tls>
                """);
    }

    @Test
    void whenFingerprintTrustNamesNoAllowList_thenTheConfigurationIsRefused() throws Exception {
        assertRefused("""
                <tls><enabled>true</enabled><tls-checks>SELF_SIGNED</tls-checks></tls>
                """);
    }

    @Test
    void whenAnIdentityNamesBothAPasswordAndAPasswordFile_thenTheConfigurationIsRefused() throws Exception {
        assertRefused("""
                <tls>
                    <enabled>true</enabled>
                    <tls-identity>
                        <format>p12</format>
                        <file>/c.p12</file>
                        <password>written</password>
                        <password-file>/run/secrets/pw</password-file>
                    </tls-identity>
                </tls>
                """);
    }

    @Test
    void whenAReferenceIsWrittenBothWays_thenTheConfigurationIsRefused() throws Exception {
        assertRefused("""
                <tls>
                    <enabled>true</enabled>
                    <tls-truststore>
                        <reference-name>plant::authorities</reference-name>
                        <reference-uuid>a-uuid</reference-uuid>
                    </tls-truststore>
                </tls>
                """);
    }

    @Test
    void whenAReferenceNamesMaterialThatIsNotDeclared_thenTheConfigurationIsRefused() throws Exception {
        // References resolve for bridges, so one naming nothing is a genuine failure rather than an
        // unreachable capability.
        assertRefused("""
                <tls>
                    <enabled>true</enabled>
                    <tls-truststore><reference-name>plant::authorities</reference-name></tls-truststore>
                </tls>
                """);
    }

    // ----- the refusal messages -----
    //
    // Built from an entity and derived directly, because a configuration the reader refuses gives no
    // message to inspect. What an operator reads is the point of these.

    @Test
    void whenBothSpellingsNameMaterial_thenTheMessageNamesBothAndTheBridge() throws Exception {
        final BridgeTlsEntity tls = entityFrom("""
                <tls>
                    <enabled>true</enabled>
                    <tls-truststore><file>/ca.pem</file><format>pem</format><skip-integrity-check>true</skip-integrity-check></tls-truststore>
                </tls>
                """);
        setKeyStore(tls);

        assertThatThrownBy(() -> new TLSConfigBridge("a-bridge", tls))
                .isInstanceOf(TLSConfigBridge.InvalidTlsConfigException.class)
                .hasMessageContaining("a-bridge")
                .hasMessageContaining("keystore")
                .hasMessageContaining("tls-truststore")
                .hasMessageContaining("mutually exclusive");
    }

    @Test
    void whenHostnameVerificationConflicts_thenTheMessageNamesBothElements() throws Exception {
        final BridgeTlsEntity tls = entityFrom("""
                <tls><enabled>true</enabled><tls-checks>ALL</tls-checks></tls>
                """);
        setVerifyHostname(tls);

        assertThatThrownBy(() -> new TLSConfigBridge("a-bridge", tls))
                .isInstanceOf(TLSConfigBridge.InvalidTlsConfigException.class)
                .hasMessageContaining("verify-hostname")
                .hasMessageContaining("tls-checks");
    }

    @Test
    void whenAnUnknownPresetIsWritten_thenTheMessageNamesThePresetsABridgeHas() throws Exception {
        final BridgeTlsEntity tls = entityFrom("""
                <tls><enabled>true</enabled><tls-checks>STANDARD</tls-checks></tls>
                """);
        setTlsChecks(tls, "APPLICATION_URI");

        assertThatThrownBy(() -> new TLSConfigBridge("a-bridge", tls))
                .isInstanceOf(TLSConfigBridge.InvalidTlsConfigException.class)
                .hasMessageContaining("APPLICATION_URI")
                .hasMessageContaining("TLS_BRIDGE_DEFAULT");
    }

    // ----- helpers -----

    /// Writes the configuration without loading it, for a case that asserts on the load itself.
    ///
    /// A bridge whose TLS cannot be carried out refuses the whole configuration, as an invalid
    /// bridge id does - a bridge connecting without encryption to a broker the operator configured
    /// encryption for would be worse than not starting. So the refusals below cannot inspect the
    /// parsed entity; they assert that reading the file fails.
    private void write(final @NotNull String tls) throws Exception {
        Files.writeString(xmlFile.toPath(), """
                <?xml version="1.0" encoding="UTF-8" ?>
                <hivemq>
                    <mqtt-bridges>
                        <mqtt-bridge>
                            <id>a-bridge</id>
                            <remote-broker>
                                <host>broker.example.com</host>
                                <port>8883</port>
                %s
                            </remote-broker>
                        </mqtt-bridge>
                    </mqtt-bridges>
                </hivemq>
                """.formatted(tls.indent(32)), StandardCharsets.UTF_8);
    }

    /// The bridge TLS entity the given `tls` block parses to, through the real reader.
    private @NotNull BridgeTlsEntity entityFrom(final @NotNull String tls) throws Exception {
        write(tls);
        final HiveMQConfigEntity config = reader.applyConfig();
        final BridgeTlsEntity entity =
                config.getBridgeConfig().get(0).getRemoteBroker().getTls();
        if (entity == null) {
            throw new AssertionError("the tls element did not bind");
        }
        return entity;
    }

    private @NotNull TLSConfigBridge configFrom(final @NotNull String tls) throws Exception {
        return new TLSConfigBridge("a-bridge", entityFrom(tls));
    }

    /// The bridge's runtime TLS settings, read with the given shared material declared alongside.
    ///
    /// Goes through the extractor rather than the entity, because resolving a reference is what the
    /// extractor does and what this exercises.
    private @NotNull BridgeTls bridgeFrom(final @NotNull String tls, final @NotNull String materials) throws Exception {
        Files.writeString(xmlFile.toPath(), """
                <?xml version="1.0" encoding="UTF-8" ?>
                <hivemq>
                    <mqtt-bridges>
                        <mqtt-bridge>
                            <id>a-bridge</id>
                            <remote-broker>
                                <host>broker.example.com</host>
                                <port>8883</port>
                %s
                            </remote-broker>
                        </mqtt-bridge>
                    </mqtt-bridges>
                %s
                </hivemq>
                """.formatted(tls.indent(32), materials.indent(4)), StandardCharsets.UTF_8);
        reader.applyConfig();
        final BridgeTls bridgeTls = bridgeConfiguration.getBridges().get(0).getBridgeTls();
        if (bridgeTls == null) {
            throw new AssertionError("the bridge has no TLS configuration");
        }
        return bridgeTls;
    }

    /// Asserts that the given configuration is refused when it is read.
    private void assertRefused(final @NotNull String tls) throws Exception {
        write(tls);
        assertThatThrownBy(() -> reader.applyConfig()).isInstanceOf(UnrecoverableException.class);
    }

    /// Puts a key store on an entity the reader accepted, to reach a refusal whose message is the
    /// point. Writing both spellings in the file would have the reader refuse the load instead,
    /// leaving no message to read.
    private static void setKeyStore(final @NotNull BridgeTlsEntity tls) throws Exception {
        final var keystore = com.hivemq.configuration.entity.listener.tls.KeystoreEntity.class
                .getDeclaredConstructor()
                .newInstance();
        final var path = keystore.getClass().getDeclaredField("path");
        path.setAccessible(true);
        path.set(keystore, "/c.jks");
        final var field = BridgeTlsEntity.class.getDeclaredField("keyStore");
        field.setAccessible(true);
        field.set(tls, keystore);
    }

    /// As above, for the hostname element.
    private static void setVerifyHostname(final @NotNull BridgeTlsEntity tls) throws Exception {
        final var field = BridgeTlsEntity.class.getDeclaredField("verifyHostname");
        field.setAccessible(true);
        field.set(tls, Boolean.FALSE);
    }

    /// Sets the preset past the schema, for the one case where the schema would have refused it.
    private static void setTlsChecks(final @NotNull BridgeTlsEntity tls, final @NotNull String preset)
            throws Exception {
        final var field = BridgeTlsEntity.class.getDeclaredField("tlsChecks");
        field.setAccessible(true);
        field.set(tls, preset);
    }
}
