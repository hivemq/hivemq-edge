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

import static com.hivemq.security.tls.TLSFixtures.bridgeTlsEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/// How a bridge's written TLS settings become the material references and checks it reads: when two
/// configurations are the same, which store type is used, which preset a bridge is on, and which
/// written material is refused before any file is opened.
class TLSConfigBridgeMaterialTest {

    private static final @NotNull String BRIDGE_ID = "a-bridge";
    private static final @NotNull String PASSWORD = "changeit";

    private static void assertBridgeRefused(final @NotNull String tls, final @NotNull String expected) {
        assertThatThrownBy(() -> bridge(tls))
                .isInstanceOf(TLSConfigBridge.InvalidTlsConfigException.class)
                .hasMessageContaining(BRIDGE_ID)
                .hasMessageContaining(expected);
    }

    private static @NotNull TLSConfigBridge bridge(final @NotNull String tls) throws Exception {
        return new TLSConfigBridge(BRIDGE_ID, bridgeTlsEntity(tls));
    }

    /// When two bridge configurations read from text are equal.
    @Nested
    class Equality {

        private static final @NotNull String WRITTEN = """
                <tls>
                    <enabled>true</enabled>
                    <tls-identity>
                        <format>pem</format>
                        <key-file>/etc/edge/client.key</key-file>
                        <certificate-file>/etc/edge/client.crt</certificate-file>
                    </tls-identity>
                    <tls-truststore><file>/etc/edge/ca.pem</file><format>pem</format><skip-integrity-check>true</skip-integrity-check></tls-truststore>
                </tls>
                """;

        /// Proves that the same text read twice gives equal configurations with equal hashes.
        @Test
        void whenTheSameTextIsReadTwice_thenTheTwoAreEqualWithEqualHashes() throws Exception {
            final TLSConfigBridge first = bridge(WRITTEN);
            final TLSConfigBridge second = bridge(WRITTEN);

            assertThat(first).isEqualTo(second);
            assertThat(first.hashCode()).isEqualTo(second.hashCode());
        }

        /// Proves that a different file, enabled flag or cipher suite makes the configurations differ.
        @Test
        void whenASourceOrACheckDiffers_thenTheTwoAreNotEqual() throws Exception {
            final TLSConfigBridge written = bridge(WRITTEN);

            assertThat(written).isNotEqualTo(bridge(WRITTEN.replace("/etc/edge/ca.pem", "/etc/edge/other-ca.pem")));
            assertThat(written).isNotEqualTo(bridge(WRITTEN.replace("<enabled>true", "<enabled>false")));
            assertThat(written)
                    .isNotEqualTo(bridge(WRITTEN.replace(
                            "<enabled>true</enabled>",
                            "<enabled>true</enabled><cipher-suites>"
                                    + "<cipher-suite>TLS_AES_128_GCM_SHA256</cipher-suite></cipher-suites>")));
        }
    }

    /// The container type the REST API states for each store, against the type the file name suggests.
    @Nested
    class StatedStoreType {

        /// Proves that a stated store type wins over the type the file name suggests.
        @Test
        void whenAKeystoreTypeContradictsTheFileName_thenTheStatedTypeIsUsed() throws Exception {
            final TLSConfigBridge config = withTypes("/tls/client.p12", "JKS", "/tls/authority.jks", "pkcs12");

            assertThat(formatOf(config.identity())).isEqualTo("jks");
            assertThat(formatOf(config.truststore())).isEqualTo("p12");
        }

        /// Proves that an absent or blank type falls back to the file name, giving the configuration
        /// the settings without types give.
        @Test
        void whenNoTypeIsStated_thenTheTypeIsGuessedFromTheFileNameAsBefore() throws Exception {
            final TLSConfigBridge stated = withTypes("/tls/client.p12", null, "/tls/authority.jks", " ");
            final TLSConfigBridge guessed = TLSConfigBridge.fromSupersededSettings(
                    BRIDGE_ID,
                    "/tls/client.p12",
                    PASSWORD,
                    PASSWORD,
                    "/tls/authority.jks",
                    PASSWORD,
                    List.of(),
                    List.of(),
                    true,
                    10);

            assertThat(formatOf(stated.identity())).isEqualTo("p12");
            assertThat(formatOf(stated.truststore())).isEqualTo("jks");
            assertThat(stated).isEqualTo(guessed);
        }

        /// Proves that a type a bridge cannot read is refused with the bridge and the field named.
        @Test
        void whenATypeIsNotOneABridgeReads_thenTheSettingsAreRefusedNamingTheField() {
            assertThatThrownBy(() -> withTypes("/tls/client.p12", null, "/tls/authority.pem", "PEM"))
                    .isInstanceOf(TLSConfigBridge.InvalidTlsConfigException.class)
                    .hasMessageContaining(BRIDGE_ID)
                    .hasMessageContaining("truststoreType 'PEM'");
        }

        /// Proves that the warning about falling back to the runtime's authorities names the bridge.
        @Test
        void whenNoTruststoreIsNamed_thenTheRuntimeAuthoritiesWarningNamesTheBridge() throws Exception {
            final Logger logger = (Logger) LoggerFactory.getLogger(TLSConfigBridge.class);
            final ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            try {
                withTypes(null, null, null, null);
            } finally {
                logger.detachAppender(appender);
            }

            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anySatisfy(message -> assertThat(message).startsWith("Bridge '" + BRIDGE_ID + "':"));
        }

        private static @Nullable String formatOf(final @NotNull Optional<MaterialRef> reference) {
            assertThat(reference).get().isInstanceOf(MaterialRef.Inline.class);
            return ((MaterialRef.Inline) reference.orElseThrow()).format();
        }

        private @NotNull TLSConfigBridge withTypes(
                final @Nullable String keystorePath,
                final @Nullable String keystoreType,
                final @Nullable String truststorePath,
                final @Nullable String truststoreType)
                throws TLSConfigBridge.InvalidTlsConfigException {
            return TLSConfigBridge.fromSupersededSettings(
                    BRIDGE_ID,
                    keystorePath,
                    PASSWORD,
                    PASSWORD,
                    keystoreType,
                    truststorePath,
                    PASSWORD,
                    truststoreType,
                    List.of(),
                    List.of(),
                    true,
                    10);
        }
    }

    /// Whether a bridge is on the preset that keeps what it validated before upgrading.
    @Nested
    class CompatibilityPreset {

        /// Proves that a bridge writing neither a preset nor checks is on the compatibility preset.
        @Test
        void whenNeitherPresetNorChecksAreWritten_thenTheBridgeIsOnTheCompatibilityPreset() throws Exception {
            assertThat(bridge("<tls><enabled>true</enabled></tls>").isCompatibilityPreset())
                    .isTrue();
        }

        /// Proves that the preset's name is read regardless of case, separator and surrounding space.
        @Test
        void whenTheCompatibilityPresetIsNamedInAnotherSpelling_thenTheBridgeIsOnIt() throws Exception {
            assertThat(bridge("<tls><enabled>true</enabled><tls-checks> tls-bridge-default </tls-checks></tls>")
                            .isCompatibilityPreset())
                    .isTrue();
        }

        /// Proves that naming another preset leaves the compatibility preset.
        @Test
        void whenAnotherPresetIsNamed_thenTheBridgeIsNotOnTheCompatibilityPreset() throws Exception {
            assertThat(bridge("<tls><enabled>true</enabled><tls-checks>STANDARD</tls-checks></tls>")
                            .isCompatibilityPreset())
                    .isFalse();
        }

        /// Proves that checks written out with the preset's values are not the preset: they read the
        /// same checks, yet the configurations differ.
        @Test
        void whenTheChecksAreWrittenWithThePresetsValues_thenTheBridgeIsNotOnItAndDiffers() throws Exception {
            final TLSConfigBridge preset =
                    bridge("<tls><enabled>true</enabled><tls-checks>TLS_BRIDGE_DEFAULT</tls-checks></tls>");
            final TLSConfigBridge written = bridge("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-checks-full>
                            <trust-mode>CHAIN</trust-mode>
                            <hostname>%s</hostname>
                            <validity>NOT_BEFORE_OR_AFTER</validity>
                            <revocation>NONE</revocation>
                            <key-usage>NONE</key-usage>
                        </tls-checks-full>
                    </tls>
                    """.formatted(preset.hostname()));

            assertThat(written.isCompatibilityPreset()).isFalse();
            assertThat(written.trustMode()).isEqualTo(preset.trustMode());
            assertThat(written.hostname()).isEqualTo(preset.hostname());
            assertThat(written.validity()).isEqualTo(preset.validity());
            assertThat(written.revocation()).isEqualTo(preset.revocation());
            assertThat(written.keyUsage()).isEqualTo(preset.keyUsage());
            assertThat(written).isNotEqualTo(preset);
        }

        /// Proves that a bridge built from the superseded settings is on the compatibility preset.
        @Test
        void whenBuiltFromTheSupersededSettings_thenTheBridgeIsOnTheCompatibilityPreset() throws Exception {
            final TLSConfigBridge config = TLSConfigBridge.fromSupersededSettings(
                    BRIDGE_ID, null, null, null, null, null, List.of(), List.of(), true, 10);

            assertThat(config.isCompatibilityPreset()).isTrue();
        }
    }

    /// A material element that is written but names nothing.
    @Nested
    class PresentButEmpty {

        /// Proves that an identity element naming no file is refused.
        @Test
        void whenTheIdentityNamesNoFile_thenTheBridgeIsRefused() {
            assertBridgeRefused("<tls><enabled>true</enabled><tls-identity/></tls>", "tls-identity");
        }

        /// Proves that a revocation-list element naming nothing is refused.
        @Test
        void whenTheRevocationListNamesNothing_thenTheBridgeIsRefused() {
            assertBridgeRefused("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-truststore><builtin>JVM-cacerts</builtin></tls-truststore>
                        <tls-revocationlist/>
                    </tls>
                    """, "tls-revocationlist");
        }

        /// Proves that an allow-list element naming nothing is refused.
        @Test
        void whenTheAllowListNamesNothing_thenTheBridgeIsRefused() {
            assertBridgeRefused("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-checks-full><trust-mode>ALLOW_LIST</trust-mode><revocation>NONE</revocation></tls-checks-full>
                        <tls-allowlist/>
                    </tls>
                    """, "tls-allowlist");
        }

        /// Proves that a blank file entry is refused rather than silently dropped.
        @Test
        void whenAFileEntryIsBlank_thenTheBridgeIsRefusedRatherThanTheEntryDropped() {
            assertBridgeRefused("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-truststore><file>/ca.pem</file><file></file><format>pem</format><skip-integrity-check>true</skip-integrity-check></tls-truststore>
                    </tls>
                    """, "empty 'file' entry");
        }

        /// Proves that a directory declared as possibly empty is a truststore that names something.
        @Test
        void whenADirectoryMayBeEmpty_thenTheTruststoreIsStillAccepted() throws Exception {
            final TLSConfigBridge config = bridge("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-truststore>
                            <directory><location>/ca.d</location><may-be-empty>true</may-be-empty></directory>
                            <format>pem</format>
                            <skip-integrity-check>true</skip-integrity-check>
                        </tls-truststore>
                    </tls>
                    """);

            final MaterialRef.Inline truststore =
                    (MaterialRef.Inline) config.truststore().orElseThrow();
            assertThat(truststore.files().directories()).containsExactly(new FileSet.Directory("/ca.d", true));
        }
    }

    /// A reference to shared material written beside content of its own.
    @Nested
    class ReferenceBesideInlineContent {

        /// Proves that a truststore reference written with a file is refused.
        @Test
        void whenATruststoreReferenceIsWrittenWithAFile_thenTheBridgeIsRefused() {
            assertBridgeRefused("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-truststore>
                            <reference-name>plant::authorities</reference-name>
                            <file>/narrow-ca.pem</file>
                        </tls-truststore>
                    </tls>
                    """, "references shared material");
        }

        /// Proves that an identity reference written with a key entry is refused.
        @Test
        void whenAnIdentityReferenceIsWrittenWithAKeyEntry_thenTheBridgeIsRefused() {
            assertBridgeRefused("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-identity>
                            <reference-name>plant::gateway</reference-name>
                            <key-entry>other</key-entry>
                        </tls-identity>
                    </tls>
                    """, "key-entry");
        }

        /// Proves that an allow-list reference written with a file is refused.
        @Test
        void whenAnAllowListReferenceIsWrittenWithAFile_thenTheBridgeIsRefused() {
            assertBridgeRefused("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-checks-full><trust-mode>ALLOW_LIST</trust-mode><revocation>NONE</revocation></tls-checks-full>
                        <tls-allowlist>
                            <reference-name>plant::pinned</reference-name>
                            <file>/pinned.txt</file>
                        </tls-allowlist>
                    </tls>
                    """, "references shared material");
        }

        /// Proves that a reference written alone is kept as a reference.
        @Test
        void whenOnlyTheReferenceIsWritten_thenTheReferenceIsKept() throws Exception {
            final TLSConfigBridge config = bridge("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-truststore><reference-name>plant::authorities</reference-name></tls-truststore>
                    </tls>
                    """);

            assertThat(config.truststore().orElseThrow()).isInstanceOf(MaterialRef.ByName.class);
        }
    }

    /// How a bridge's written password, password file and integrity waiver reach its material.
    @Nested
    class IntegrityCheck {

        /// Proves that a written waiver reaches the material reference.
        @Test
        void whenTheBridgeWaivesTheCheck_thenTheWaiverReachesTheMaterial() throws Exception {
            final TLSConfigBridge config = bridge("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-truststore><file>/ca.jks</file><format>jks</format><skip-integrity-check>true</skip-integrity-check></tls-truststore>
                    </tls>
                    """);

            assertThat(((MaterialRef.Inline) config.truststore().orElseThrow()).password())
                    .isEqualTo(new Secret.Waived());
        }

        /// Proves that a container with neither a password nor the waiver is refused.
        @Test
        void whenAContainerNamesNeitherPasswordNorWaiver_thenTheBridgeIsRefused() {
            assertBridgeRefused("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-truststore><file>/ca.p12</file><format>p12</format></tls-truststore>
                    </tls>
                    """, "neither a password nor skip-integrity-check");
        }

        /// Proves that a password written beside a password file, or beside the waiver, is refused.
        @Test
        void whenBothAPasswordAndTheWaiverAreWritten_thenTheBridgeIsRefused() {
            assertBridgeRefused("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-truststore>
                            <file>/ca.p12</file><format>p12</format>
                            <password>pw</password><password-file>/run/pw</password-file>
                            <skip-integrity-check>true</skip-integrity-check>
                        </tls-truststore>
                    </tls>
                    """, "both a password and a password file");
            assertBridgeRefused("""
                    <tls>
                        <enabled>true</enabled>
                        <tls-truststore>
                            <file>/ca.p12</file><format>p12</format><password>pw</password>
                            <skip-integrity-check>true</skip-integrity-check>
                        </tls-truststore>
                    </tls>
                    """, "both a password and skip-integrity-check");
        }
    }

    /// When a bridge falls back to the runtime's own certificate authorities.
    @Nested
    class RuntimeAuthorities {

        /// The truststore every bridge building a path with no truststore named falls back to.
        private static final @NotNull MaterialRef RUNTIME_AUTHORITIES =
                new MaterialRef.Inline(null, FileSet.ofJvmCacerts(), null, null, null);

        /// Proves that a present but empty `tls-truststore` is not widened to the runtime's own
        /// authorities: the configuration is refused, or at least resolves to something else.
        @Test
        void anEmptyTruststoreElementIsNotWidenedToTheRuntimeAuthorities() throws Exception {
            assertThat(truststoreIfAccepted("<tls><enabled>true</enabled><tls-truststore/></tls>"))
                    .as("an enabled bridge with an empty <tls-truststore/> is refused, not given the JVM authorities")
                    .isNotEqualTo(Optional.of(RUNTIME_AUTHORITIES));
        }

        /// Proves that an omitted truststore still falls back to the runtime's own authorities, which
        /// the bridges connecting to cloud brokers rely on.
        @Test
        void anOmittedTruststoreFallsBackToTheRuntimeAuthorities() throws Exception {
            assertThat(truststoreIfAccepted("<tls><enabled>true</enabled></tls>"))
                    .contains(RUNTIME_AUTHORITIES);
        }

        /// The truststore the bridge resolves to, or empty where the configuration is refused.
        ///
        /// Under the default chain trust an accepted configuration always names a truststore, so empty
        /// here means only the refusal.
        private static @NotNull Optional<MaterialRef> truststoreIfAccepted(final @NotNull String tls) throws Exception {
            try {
                return bridge(tls).truststore();
            } catch (final TLSConfigBridge.InvalidTlsConfigException _) {
                return Optional.empty();
            }
        }
    }
}
