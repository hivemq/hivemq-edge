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
package com.hivemq.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.api.resources.impl.BridgeResourceImpl;
import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.api.model.Bridge;
import com.hivemq.edge.api.model.Status;
import com.hivemq.edge.api.model.TlsConfiguration;
import com.hivemq.security.tls.MaterialRef;
import com.hivemq.security.tls.TLSConfig;
import com.hivemq.security.tls.TLSConstants;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// What the editor's round trip does to a bridge's TLS settings. The bridge is read from
/// configuration text, served as the REST model (`BridgeUtils.convert`), and the model is converted
/// back as a PUT body is (`BridgeResourceImpl.convertTls`). A body served back unchanged keeps the
/// settings the bridge already has, new-style settings included, rather than replacing them with
/// the compatibility default; a changed body replaces them with what its fields say.
class BridgeUtilsTlsRoundTripTest {

    private static final @NotNull String BRIDGE_ID = "rest-bridge";

    @TempDir
    File temporaryFolder;

    /// The body the editor was served, sent back as it was, keeps the very settings the bridge has.
    @Test
    void givenTheServedBodyUnchanged_whenSaved_thenTheBridgeKeepsItsSettings() throws Exception {
        final MqttBridge bridge = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>SELF_SIGNED</tls-checks>
                    <tls-allowlist><file>/tls/pinned.txt</file></tls-allowlist>
                </tls>
                """);
        final TlsConfiguration served =
                BridgeUtils.convert(bridge, new Status()).getTlsConfiguration();

        final BridgeTls saved = BridgeResourceImpl.convertTls(BRIDGE_ID, served, bridge.getBridgeTls());

        assertThat(saved).isSameAs(bridge.getBridgeTls());
    }

    /// The same holds for a bridge written in the superseded fields only.
    @Test
    void givenOnlyTheSupersededFieldsAndTheServedBodyUnchanged_whenSaved_thenTheBridgeKeepsItsSettings()
            throws Exception {
        final MqttBridge bridge = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <keystore>
                        <path>/tls/client.jks</path>
                        <password>store-pw</password>
                        <private-key-password>key-pw</private-key-password>
                    </keystore>
                    <truststore><path>/tls/trust.jks</path><password>trust-pw</password></truststore>
                </tls>
                """);
        final TlsConfiguration served =
                BridgeUtils.convert(bridge, new Status()).getTlsConfiguration();

        final BridgeTls saved = BridgeResourceImpl.convertTls(BRIDGE_ID, served, bridge.getBridgeTls());

        assertThat(saved).isSameAs(bridge.getBridgeTls());
    }

    /// A bridge written in the superseded fields only comes back from the round trip with every
    /// effective check and store it had.
    @Test
    void givenOnlyTheSupersededFields_whenTheBridgeRoundTripsThroughTheRestModel_thenItsTlsIsUnchanged()
            throws Exception {
        final MqttBridge bridge = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <keystore>
                        <path>/tls/client.jks</path>
                        <password>store-pw</password>
                        <private-key-password>key-pw</private-key-password>
                    </keystore>
                    <truststore><path>/tls/trust.jks</path><password>trust-pw</password></truststore>
                </tls>
                """);

        final TLSConfig roundTripped = roundTrip(bridge);

        assertThat(roundTripped.trustMode())
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().trustMode());
        assertThat(roundTripped.hostname())
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().hostname());
        assertThat(roundTripped.validity())
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().validity());
        assertThat(roundTripped.revocation())
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().revocation());
        assertThat(roundTripped.keyUsage())
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().keyUsage());
        assertThat(roundTripped.identity())
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().identity());
        assertThat(roundTripped.truststore())
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().truststore());
    }

    /// A file-configured allow-list pin survives the round trip: the trust mode stays the allow-list
    /// and the list itself is kept.
    @Test
    void givenAnAllowListPin_whenTheBridgeRoundTripsThroughTheRestModel_thenThePinIsKept() throws Exception {
        final MqttBridge bridge = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>SELF_SIGNED</tls-checks>
                    <tls-allowlist><file>/tls/pinned.txt</file></tls-allowlist>
                </tls>
                """);
        assertThat(bridge.getBridgeTls().getEffectiveTls().allowList()).isPresent();

        final TLSConfig roundTripped = roundTrip(bridge);

        assertThat(roundTripped.trustMode())
                .as("a REST save must keep the allow-list trust mode of a file-configured bridge")
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().trustMode());
        assertThat(roundTripped.allowList())
                .as("a REST save must keep the allow-list of a file-configured bridge")
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().allowList());
    }

    /// A file-configured private-authority truststore survives the round trip.
    @Test
    void givenAPrivateAuthorityTruststore_whenTheBridgeRoundTripsThroughTheRestModel_thenItIsKept() throws Exception {
        final MqttBridge bridge = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>ALL</tls-checks>
                    <tls-truststore><file>/tls/private-authority.pem</file><format>pem</format></tls-truststore>
                </tls>
                """);

        final TLSConfig roundTripped = roundTrip(bridge);

        assertThat(roundTripped.truststore())
                .as("a REST save must keep the private-authority truststore of a file-configured bridge")
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().truststore());
    }

    /// The revocation and key-usage checks of the strict preset survive the round trip.
    @Test
    void givenAStrictPreset_whenTheBridgeRoundTripsThroughTheRestModel_thenTheChecksAreKept() throws Exception {
        final MqttBridge bridge = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>ALL</tls-checks>
                    <tls-truststore><file>/tls/private-authority.pem</file><format>pem</format></tls-truststore>
                </tls>
                """);

        final TLSConfig roundTripped = roundTrip(bridge);

        assertThat(roundTripped.revocation())
                .as("a REST save must keep the revocation check of a file-configured bridge")
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().revocation());
        assertThat(roundTripped.keyUsage())
                .as("a REST save must keep the key-usage check of a file-configured bridge")
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().keyUsage());
    }

    /// A file-configured mTLS client identity survives the round trip.
    @Test
    void givenAClientIdentity_whenTheBridgeRoundTripsThroughTheRestModel_thenTheIdentityIsKept() throws Exception {
        final MqttBridge bridge = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>ALL</tls-checks>
                    <tls-identity><file>/tls/client.p12</file><format>p12</format><password>pw</password></tls-identity>
                    <tls-truststore><file>/tls/private-authority.pem</file><format>pem</format></tls-truststore>
                </tls>
                """);
        assertThat(bridge.getBridgeTls().getEffectiveTls().identity()).isPresent();

        final TLSConfig roundTripped = roundTrip(bridge);

        assertThat(roundTripped.identity())
                .as("a REST save must keep the mTLS identity of a file-configured bridge")
                .isEqualTo(bridge.getBridgeTls().getEffectiveTls().identity());
    }

    /// A body naming another truststore is what the operator asked for: the bridge takes the new
    /// store with the checks of the superseded fields, and its allow-list no longer applies.
    @Test
    void givenAChangedBody_whenSaved_thenTheBodyReplacesTheSettings() throws Exception {
        final MqttBridge bridge = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>SELF_SIGNED</tls-checks>
                    <tls-allowlist><file>/tls/pinned.txt</file></tls-allowlist>
                </tls>
                """);
        final Bridge served = BridgeUtils.convert(bridge, new Status());
        final TlsConfiguration changed = served.getTlsConfiguration()
                .truststorePath("/tls/new-authority.jks")
                .truststorePassword("trust-pw");

        final BridgeTls saved = BridgeResourceImpl.convertTls(BRIDGE_ID, changed, bridge.getBridgeTls());

        assertThat(saved).isNotNull().isNotSameAs(bridge.getBridgeTls());
        assertThat(saved.getTruststorePath()).isEqualTo("/tls/new-authority.jks");
        assertThat(saved.getConfiguredTls()).isNull();
        final TLSConfig effective = saved.getEffectiveTls();
        assertThat(effective).isNotNull();
        assertThat(effective.trustMode()).isEqualTo(TLSConstants.TrustMode.CHAIN);
        assertThat(effective.allowList()).isEmpty();
        assertThat(effective.truststore()).isPresent();
    }

    /// The type served for a store is the type the bridge reads it as, whatever the case of its
    /// name: the editor sends the served type back, and a stated type wins over the file name.
    @Test
    void givenAStoreNamedInCapitals_whenServed_thenItsTypeIsTheTypeItIsReadAs() throws Exception {
        final MqttBridge bridge = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <keystore>
                        <path>/tls/CLIENT.P12</path>
                        <password>store-pw</password>
                        <private-key-password>key-pw</private-key-password>
                    </keystore>
                </tls>
                """);

        final TlsConfiguration served =
                BridgeUtils.convert(bridge, new Status()).getTlsConfiguration();

        assertThat(served.getKeystoreType()).isEqualTo("PKCS12");
        assertThat(bridge.getBridgeTls().getEffectiveTls().identity())
                .get()
                .isInstanceOfSatisfying(MaterialRef.Inline.class, inline -> assertThat(inline.format())
                        .isEqualTo("p12"));
    }

    /// The effective TLS settings of the bridge after the editor's GET, then PUT of the same body.
    /// The PUT replaces this bridge, so its conversion is given the bridge's current TLS settings, as
    /// `BridgeResourceImpl.updateBridge` gives them.
    private static @NotNull TLSConfig roundTrip(final @NotNull MqttBridge bridge) {
        final Bridge served = BridgeUtils.convert(bridge, new Status());
        final BridgeTls saved =
                BridgeResourceImpl.convertTls(BRIDGE_ID, served.getTlsConfiguration(), bridge.getBridgeTls());
        assertThat(saved).isNotNull();
        final TLSConfig effective = saved.getEffectiveTls();
        assertThat(effective).isNotNull();
        return effective;
    }

    /// A bridge read from the given `tls` block through the real configuration reader, with effective
    /// TLS settings.
    private @NotNull MqttBridge readBridge(final @NotNull String tls) throws Exception {
        final File file = new File(temporaryFolder, "config.xml");
        Files.writeString(file.toPath(), """
                <?xml version="1.0" encoding="UTF-8" ?>
                <hivemq>
                    <mqtt-bridges>
                        <mqtt-bridge>
                            <id>%s</id>
                            <remote-broker>
                                <host>broker.example.com</host>
                                <port>8883</port>
                %s
                            </remote-broker>
                        </mqtt-bridge>
                    </mqtt-bridges>
                </hivemq>
                """.formatted(BRIDGE_ID, tls.indent(32)), StandardCharsets.UTF_8);
        final ConfigFileReaderWriter reader =
                new ConfigFileReaderWriter(new SystemInformationImpl(false), new ConfigurationFile(file), List.of());
        reader.applyConfig();
        final MqttBridge bridge = reader.getBridgeExtractor().getBridges().getFirst();
        assertThat(bridge.getBridgeTls()).isNotNull();
        assertThat(bridge.getBridgeTls().getEffectiveTls()).isNotNull();
        return bridge;
    }
}
