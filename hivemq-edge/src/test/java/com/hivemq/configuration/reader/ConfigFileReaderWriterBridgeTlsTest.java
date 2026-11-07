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
import com.hivemq.configuration.entity.bridge.MqttBridgeEntity;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.security.tls.TLSConfig;
import com.hivemq.security.tls.TLSConstants;
import java.nio.file.Files;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// The bridge `<tls>` element through the real, schema-validating configuration reader: which
/// spellings it accepts, which it refuses, and what a write-back returns to the file. A write-back
/// of a configuration that uses none of the new TLS settings must still start the previous release,
/// whose schema does not know `tls-materials`, so such a file carries no `tls-materials` element.
class ConfigFileReaderWriterBridgeTlsTest extends AbstractConfigurationTest {

    /// Material declaring two entries of each list type, so that a reference to either resolves and
    /// only the second reference itself can refuse the file.
    private static final @NotNull String SHARED_MATERIAL = """
            <tls-materials>
                <tls-material>
                    <name>shared</name>
                    <tls-truststore><name>first</name><file>/tls/first.pem</file><format>pem</format></tls-truststore>
                    <tls-truststore><name>second</name><file>/tls/second.pem</file><format>pem</format></tls-truststore>
                    <tls-allowlist>
                        <name>first</name>
                        <uuid>00000000-0000-0000-0000-000000000001</uuid>
                        <file>/tls/first.txt</file>
                    </tls-allowlist>
                    <tls-allowlist>
                        <name>second</name>
                        <uuid>00000000-0000-0000-0000-000000000002</uuid>
                        <file>/tls/second.txt</file>
                    </tls-allowlist>
                    <tls-revocationlist><name>first</name><file>/tls/first.crl</file></tls-revocationlist>
                    <tls-revocationlist><name>second</name><file>/tls/second.crl</file></tls-revocationlist>
                </tls-material>
            </tls-materials>
            """;

    /// A bridge written with the superseded `keystore` and `truststore` elements only.
    private static final @NotNull String SUPERSEDED_ONLY = """
            <hivemq>
                <mqtt-bridges>
                    <mqtt-bridge>
                        <id>downgrade-bridge</id>
                        <remote-broker>
                            <host>broker.example</host>
                            <port>8883</port>
                            <tls>
                                <enabled>true</enabled>
                                <keystore>
                                    <path>/tls/client.jks</path>
                                    <password>store-pw</password>
                                    <private-key-password>key-pw</private-key-password>
                                </keystore>
                                <truststore>
                                    <path>/tls/trust.jks</path>
                                    <password>trust-pw</password>
                                </truststore>
                            </tls>
                        </remote-broker>
                    </mqtt-bridge>
                </mqtt-bridges>
            </hivemq>
            """;

    // --- a single-valued material element written twice is refused ---------------------------------

    /// A truststore naming two references would silently use the second; the file is refused.
    @Test
    void givenATruststoreWithTwoReferenceNames_whenRead_thenRefused() throws Exception {
        assertRefused(bridgeWithTls(SHARED_MATERIAL, """
                <enabled>true</enabled>
                <tls-truststore>
                    <reference-name>shared::first</reference-name>
                    <reference-name>shared::second</reference-name>
                </tls-truststore>
                """));
    }

    /// Two formats on one truststore.
    @Test
    void givenATruststoreWithTwoFormats_whenRead_thenRefused() throws Exception {
        assertRefused(bridgeWithTls("""
                <enabled>true</enabled>
                <tls-truststore>
                    <file>/tls/authority.pem</file>
                    <format>jks</format>
                    <format>pem</format>
                </tls-truststore>
                """));
    }

    /// Two passwords on one truststore.
    @Test
    void givenATruststoreWithTwoPasswords_whenRead_thenRefused() throws Exception {
        assertRefused(bridgeWithTls("""
                <enabled>true</enabled>
                <tls-truststore>
                    <file>/tls/authority.jks</file>
                    <format>jks</format>
                    <password>first</password>
                    <password>second</password>
                </tls-truststore>
                """));
    }

    /// Two integrity switches on one truststore.
    @Test
    void givenATruststoreWithTwoIntegritySwitches_whenRead_thenRefused() throws Exception {
        assertRefused(bridgeWithTls("""
                <enabled>true</enabled>
                <tls-truststore>
                    <file>/tls/authority.jks</file>
                    <format>jks</format>
                    <skip-integrity-check>false</skip-integrity-check>
                    <skip-integrity-check>true</skip-integrity-check>
                </tls-truststore>
                """));
    }

    /// Two references on one allow-list.
    @Test
    void givenAnAllowListWithTwoReferenceUuids_whenRead_thenRefused() throws Exception {
        assertRefused(bridgeWithTls(SHARED_MATERIAL, """
                <enabled>true</enabled>
                <tls-checks>SELF_SIGNED</tls-checks>
                <tls-allowlist>
                    <reference-uuid>00000000-0000-0000-0000-000000000001</reference-uuid>
                    <reference-uuid>00000000-0000-0000-0000-000000000002</reference-uuid>
                </tls-allowlist>
                """));
    }

    /// Two references on one revocation list.
    @Test
    void givenARevocationListWithTwoReferenceNames_whenRead_thenRefused() throws Exception {
        assertRefused(bridgeWithTls(SHARED_MATERIAL, """
                <enabled>true</enabled>
                <tls-revocationlist>
                    <reference-name>shared::first</reference-name>
                    <reference-name>shared::second</reference-name>
                </tls-revocationlist>
                """));
    }

    /// The same rule holds for material declared once in `tls-materials`.
    @Test
    void givenDeclaredMaterialWithTwoNames_whenRead_thenRefused() throws Exception {
        assertRefused("""
                <hivemq>
                    <tls-materials>
                        <tls-material>
                            <name>shared</name>
                            <tls-truststore>
                                <name>first</name>
                                <name>second</name>
                                <file>/tls/authority.pem</file>
                                <format>pem</format>
                            </tls-truststore>
                        </tls-material>
                    </tls-materials>
                </hivemq>
                """);
    }

    /// The elements that merge into one set still repeat freely.
    @Test
    void givenATruststoreWithTwoFilesAndTheRuntimeAuthorities_whenRead_thenAccepted() throws Exception {
        final BridgeTlsEntity tls = readTlsEntity(bridgeWithTls("""
                <enabled>true</enabled>
                <tls-checks>ALL</tls-checks>
                <tls-truststore>
                    <file>/tls/first.pem</file>
                    <file>/tls/second.pem</file>
                    <builtin>JVM-cacerts</builtin>
                    <format>pem</format>
                </tls-truststore>
                <tls-revocationlist>
                    <file>/tls/first.crl</file>
                    <file>/tls/second.crl</file>
                </tls-revocationlist>
                """));

        assertThat(tls.getTlsTruststore().getFiles()).containsExactly("/tls/first.pem", "/tls/second.pem");
        assertThat(tls.getTlsRevocationlist().getFiles()).containsExactly("/tls/first.crl", "/tls/second.crl");
    }

    // --- the spellings of preset and check values the reference promises ---------------------------

    /// A preset in lower case with a hyphen for the underscore.
    @Test
    void givenAPresetInLowerCaseWithAHyphen_whenRead_thenItIsThePreset() throws Exception {
        final TLSConfig tls = readEffectiveTls(bridgeWithTls("""
                <enabled>true</enabled>
                <tls-checks>self-signed</tls-checks>
                <tls-allowlist><file>/tls/pinned.txt</file></tls-allowlist>
                """));

        assertThat(tls.trustMode()).isEqualTo(TLSConstants.TrustMode.ALLOW_LIST);
    }

    /// A preset in mixed case, with the underscore.
    @Test
    void givenAPresetInMixedCase_whenRead_thenItIsThePreset() throws Exception {
        final TLSConfig tls = readEffectiveTls(bridgeWithTls("""
                <enabled>true</enabled>
                <tls-checks>No_Verification</tls-checks>
                """));

        assertThat(tls.trustMode()).isEqualTo(TLSConstants.TrustMode.ANY_CERT);
        assertThat(tls.validity()).isEqualTo(TLSConstants.ValidityCheck.NONE);
    }

    /// Check values in mixed case, with the separators left out or replaced.
    @Test
    void givenCheckValuesInEachSpelling_whenRead_thenEachIsTheCheck() throws Exception {
        final TLSConfig tls = readEffectiveTls(bridgeWithTls("""
                <enabled>true</enabled>
                <tls-checks-full>
                    <trust-mode>Chain</trust-mode>
                    <hostname>hostname</hostname>
                    <validity>NotBeforeOrAfter</validity>
                    <revocation>require-crls</revocation>
                    <key-usage>server_auth</key-usage>
                </tls-checks-full>
                <tls-truststore><file>/tls/authority.pem</file><format>pem</format></tls-truststore>
                """));

        assertThat(tls.trustMode()).isEqualTo(TLSConstants.TrustMode.CHAIN);
        assertThat(tls.hostname()).isEqualTo(TLSConstants.HostnameCheck.HOSTNAME);
        assertThat(tls.validity()).isEqualTo(TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER);
        assertThat(tls.revocation()).isEqualTo(TLSConstants.RevocationCheck.REQUIRE_CRLS);
        assertThat(tls.keyUsage()).isEqualTo(TLSConstants.KeyUsageCheck.SERVER_AUTH);
    }

    /// The reference's own example spellings of one value.
    @Test
    void givenTheValidityValueWrittenWithUnderscoresInLowerCase_whenRead_thenItIsTheCheck() throws Exception {
        final TLSConfig tls = readEffectiveTls(bridgeWithTls("""
                <enabled>true</enabled>
                <tls-checks-full>
                    <validity>not_before_or_after</validity>
                </tls-checks-full>
                """));

        assertThat(tls.validity()).isEqualTo(TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER);
    }

    /// A word that is no preset is still refused.
    @Test
    void givenAnUnknownPreset_whenRead_thenRefused() throws Exception {
        assertRefused(bridgeWithTls("""
                <enabled>true</enabled>
                <tls-checks>STRICT</tls-checks>
                """));
    }

    /// A word that is no check value is still refused.
    @Test
    void givenAnUnknownCheckValue_whenRead_thenRefused() throws Exception {
        assertRefused(bridgeWithTls("""
                <enabled>true</enabled>
                <tls-checks-full>
                    <validity>sometimes</validity>
                </tls-checks-full>
                """));
    }

    // --- what a write-back returns to the file ------------------------------------------------------

    /// An empty `tls-materials` declares nothing and is not written back.
    @Test
    void givenAnEmptyTlsMaterialsElement_whenWrittenBack_thenTheFileHasNone() throws Exception {
        writeBack("""
                <hivemq>
                    <tls-materials/>
                </hivemq>
                """);

        assertThat(elementsInTheFileNamed("tls-materials")).isZero();
    }

    /// Declared material is written back.
    @Test
    void givenDeclaredMaterial_whenWrittenBack_thenItIsKept() throws Exception {
        writeBack("""
                <hivemq>
                    <tls-materials>
                        <tls-material>
                            <name>shared</name>
                            <tls-truststore>
                                <name>authorities</name>
                                <file>/tls/authority.pem</file>
                                <format>pem</format>
                            </tls-truststore>
                        </tls-material>
                    </tls-materials>
                </hivemq>
                """);
        final HiveMQConfigEntity reread = reader.applyConfig();

        assertThat(reread.getTlsMaterials()).hasSize(1);
        assertThat(reread.getTlsMaterials().getFirst().getName()).isEqualTo("shared");
    }

    /// Every superseding element and a reference come back from a write-back as they were read.
    @Test
    void givenEverySupersedingElement_whenWrittenBack_thenTheTlsElementIsUnchanged() throws Exception {
        Files.writeString(xmlFile.toPath(), """
                <hivemq>
                    <tls-materials>
                        <tls-material>
                            <name>shared</name>
                            <tls-allowlist>
                                <name>pins</name>
                                <file>/tls/pinned.txt</file>
                            </tls-allowlist>
                        </tls-material>
                    </tls-materials>
                    <mqtt-bridges>
                        <mqtt-bridge>
                            <id>bridge</id>
                            <remote-broker>
                                <host>broker.example</host>
                                <tls>
                                    <enabled>true</enabled>
                                    <tls-checks-full>
                                        <trust-mode>ALLOW_LIST</trust-mode>
                                        <hostname>NONE</hostname>
                                        <validity>NOT_BEFORE_OR_AFTER</validity>
                                        <revocation>NONE</revocation>
                                    </tls-checks-full>
                                    <tls-identity>
                                        <key-file>/tls/client.key</key-file>
                                        <certificate-file>/tls/client.pem</certificate-file>
                                        <format>pem</format>
                                    </tls-identity>
                                    <tls-allowlist><reference-name>shared::pins</reference-name></tls-allowlist>
                                </tls>
                            </remote-broker>
                        </mqtt-bridge>
                    </mqtt-bridges>
                </hivemq>
                """);
        final BridgeTlsEntity read = reader.applyConfig()
                .getBridgeConfig()
                .getFirst()
                .getRemoteBroker()
                .getTls();

        reader.writeConfigWithSync();
        final BridgeTlsEntity reread = reader.applyConfig()
                .getBridgeConfig()
                .getFirst()
                .getRemoteBroker()
                .getTls();

        assertThat(reread).isEqualTo(read);
        assertThat(reread.getTlsAllowlist().getReferenceName()).isEqualTo("shared::pins");
    }

    /// A bridge with only the superseded elements is still written in that spelling.
    @Test
    void givenOnlyTheSupersededElements_whenWrittenBack_thenTheyAreKept() throws Exception {
        writeBack(bridgeWithTls("""
                <enabled>true</enabled>
                <keystore>
                    <path>/tls/client.jks</path>
                    <password>store-pw</password>
                    <private-key-password>key-pw</private-key-password>
                </keystore>
                <truststore><path>/tls/trust.jks</path><password>trust-pw</password></truststore>
                """));

        final BridgeTlsEntity reread = reader.applyConfig()
                .getBridgeConfig()
                .getFirst()
                .getRemoteBroker()
                .getTls();

        assertThat(reread.getKeyStore()).isNotNull();
        assertThat(reread.getKeyStore().getPath()).isEqualTo("/tls/client.jks");
        assertThat(reread.getTrustStore()).isNotNull();
        assertThat(reread.getTrustStore().getPath()).isEqualTo("/tls/trust.jks");
        assertThat(reread.hasSupersedingElements()).isFalse();
    }

    /// The `ALL` preset, a truststore and a revocation list survive a write-back.
    @Test
    void givenTheAllPresetATruststoreAndARevocationList_whenWrittenBack_thenTheyAreKept() throws Exception {
        writeBack("""
                <hivemq>
                    <mqtt-bridges>
                        <mqtt-bridge>
                            <id>bridge-with-tls-material</id>
                            <remote-broker>
                                <host>broker.example</host>
                                <tls>
                                    <enabled>true</enabled>
                                    <tls-checks>ALL</tls-checks>
                                    <tls-truststore>
                                        <file>/tls/bridge-authority.pem</file>
                                        <format>pem</format>
                                        <skip-integrity-check>true</skip-integrity-check>
                                    </tls-truststore>
                                    <tls-revocationlist><file>/tls/bridge-revocations.pem</file></tls-revocationlist>
                                </tls>
                            </remote-broker>
                        </mqtt-bridge>
                    </mqtt-bridges>
                </hivemq>
                """);

        final BridgeTlsEntity tls = reader.applyConfig()
                .getBridgeConfig()
                .getFirst()
                .getRemoteBroker()
                .getTls();

        assertThat(tls.getTlsChecks()).isEqualTo("ALL");
        assertThat(tls.getTlsTruststore().getFiles()).containsExactly("/tls/bridge-authority.pem");
        assertThat(tls.getTlsRevocationlist().getFiles()).containsExactly("/tls/bridge-revocations.pem");
    }

    /// A write-back of a configuration that uses no new TLS setting has no `tls-materials` element,
    /// which the previous release's schema would refuse.
    @Test
    void givenAConfigurationWithoutNewTlsSettings_whenItIsWrittenBack_thenTheFileHasNoTlsMaterials() throws Exception {
        writeBack(SUPERSEDED_ONLY);

        assertThat(elementsInTheFileNamed("tls-materials"))
                .as("a write-back of a configuration using no new TLS setting must not contain "
                        + "tls-materials, or the previous release refuses to start on it")
                .isZero();
    }

    /// The same write-back keeps the bridge and its superseded TLS elements.
    @Test
    void givenAConfigurationWithoutNewTlsSettings_whenItIsWrittenBack_thenTheBridgeAndItsSupersededTlsAreKept()
            throws Exception {
        writeBack(SUPERSEDED_ONLY);

        final MqttBridgeEntity bridge = reader.applyConfig().getBridgeConfig().getFirst();
        final BridgeTlsEntity tls = bridge.getRemoteBroker().getTls();

        assertThat(bridge.getId()).isEqualTo("downgrade-bridge");
        assertThat(bridge.getRemoteBroker().getHost()).isEqualTo("broker.example");
        assertThat(tls.getKeyStore()).isNotNull();
        assertThat(tls.getKeyStore().getPath()).isEqualTo("/tls/client.jks");
        assertThat(tls.getTrustStore()).isNotNull();
        assertThat(tls.getTrustStore().getPath()).isEqualTo("/tls/trust.jks");
    }

    // --- helpers ------------------------------------------------------------------------------------

    private static @NotNull String bridgeWithTls(final @NotNull String tls) {
        return bridgeWithTls("", tls);
    }

    private static @NotNull String bridgeWithTls(final @NotNull String materials, final @NotNull String tls) {
        return """
                <hivemq>
                %s
                    <mqtt-bridges>
                        <mqtt-bridge>
                            <id>bridge</id>
                            <remote-broker>
                                <host>broker.example</host>
                                <tls>
                %s
                                </tls>
                            </remote-broker>
                        </mqtt-bridge>
                    </mqtt-bridges>
                </hivemq>
                """.formatted(materials.indent(4), tls.indent(20));
    }

    private void assertRefused(final @NotNull String configuration) throws Exception {
        Files.writeString(xmlFile.toPath(), configuration);
        assertThatThrownBy(reader::applyConfig).isInstanceOf(UnrecoverableException.class);
    }

    /// How many elements of the given name the configuration file holds, as an XML parser reads it.
    /// The reader cannot answer this: it reads an absent and an empty `tls-materials` alike.
    private int elementsInTheFileNamed(final @NotNull String name) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder()
                .parse(xmlFile)
                .getElementsByTagName(name)
                .getLength();
    }

    private @NotNull TLSConfig readEffectiveTls(final @NotNull String configuration) throws Exception {
        Files.writeString(xmlFile.toPath(), configuration);
        reader.applyConfig();
        final BridgeTls tls = bridgeConfiguration.getBridges().getFirst().getBridgeTls();
        assertThat(tls).isNotNull();
        assertThat(tls.getEffectiveTls()).isNotNull();
        return tls.getEffectiveTls();
    }

    private @NotNull BridgeTlsEntity readTlsEntity(final @NotNull String configuration) throws Exception {
        Files.writeString(xmlFile.toPath(), configuration);
        return reader.applyConfig()
                .getBridgeConfig()
                .getFirst()
                .getRemoteBroker()
                .getTls();
    }

    /// Reads the given configuration and writes it back to the configuration file.
    private void writeBack(final @NotNull String configuration) throws Exception {
        Files.writeString(xmlFile.toPath(), configuration);
        reader.applyConfig();
        reader.writeConfigWithSync();
    }
}
