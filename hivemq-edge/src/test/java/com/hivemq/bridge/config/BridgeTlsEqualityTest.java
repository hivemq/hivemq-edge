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
package com.hivemq.bridge.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// When two bridges' TLS settings are equal. `BridgeService.updateBridges` restarts a running bridge
/// only when `equals` says it changed, so a changed TLS policy, a rotated truststore reference, and a
/// switch between the compatibility preset (`tls-checks` left out, or `TLS_BRIDGE_DEFAULT`) and
/// `tls-checks-full` writing the preset's five values must all compare unequal, while the same
/// configuration read twice compares equal. The preset validates the dates of a directly trusted
/// certificate only as the superseded settings did; the written checks validate them as written.
/// Bridges are read from real configuration text, one file per reading, so that the effective
/// settings are separate objects as they are on a reload.
class BridgeTlsEqualityTest {

    private static final @NotNull String BRIDGE_ID = "tls-bridge";

    /// The five checks `TLS_BRIDGE_DEFAULT` sets for a bridge that leaves `verify-hostname` at its
    /// default, written out.
    private static final @NotNull String PRESET_VALUES_WRITTEN = """
            <tls>
                <enabled>true</enabled>
                <tls-checks-full>
                    <trust-mode>CHAIN</trust-mode>
                    <hostname>HOSTNAME</hostname>
                    <validity>NOT_BEFORE_OR_AFTER</validity>
                    <revocation>NONE</revocation>
                    <key-usage>NONE</key-usage>
                </tls-checks-full>
            </tls>
            """;

    @TempDir
    File temporaryFolder;

    private int readings;

    /// The same configuration read twice gives equal TLS settings and equal bridges.
    @Test
    void givenTheSameConfigurationReadTwice_whenComparingTheBridges_thenTheyAreEqual() throws Exception {
        final String tls = """
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>ALL</tls-checks>
                    <tls-truststore><file>/tls/authority.pem</file><format>pem</format></tls-truststore>
                </tls>
                """;
        final MqttBridge first = readBridge(tls);
        final MqttBridge second = readBridge(tls);

        assertThat(first.getBridgeTls()).isNotNull();
        assertThat(first.getBridgeTls()).isEqualTo(second.getBridgeTls());
        assertThat(first).isEqualTo(second);
    }

    /// Settings written in the superseded fields only, read twice, are equal.
    @Test
    void givenTheSupersededSettingsReadTwice_whenComparingTheBridgeTls_thenTheyAreEqual() throws Exception {
        final String tls = """
                <tls>
                    <enabled>true</enabled>
                    <truststore><path>/tls/trust.jks</path><password>pw</password></truststore>
                </tls>
                """;

        assertThat(readBridge(tls).getBridgeTls()).isEqualTo(readBridge(tls).getBridgeTls());
    }

    /// The preset's five values written out, read twice, are equal and hash alike.
    @Test
    void givenTheSameChecksWrittenOutReadTwice_whenComparingTheBridgeTls_thenTheyAreEqualWithEqualHashes()
            throws Exception {
        final BridgeTls first = readBridge(PRESET_VALUES_WRITTEN).getBridgeTls();
        final BridgeTls second = readBridge(PRESET_VALUES_WRITTEN).getBridgeTls();

        assertThat(first).isNotNull();
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    /// The compatibility preset, read twice, is equal and hashes alike.
    @Test
    void givenTheCompatibilityPresetReadTwice_whenComparingTheBridgeTls_thenTheyAreEqualWithEqualHashes()
            throws Exception {
        final String tls = "<tls><enabled>true</enabled></tls>";
        final BridgeTls first = readBridge(tls).getBridgeTls();
        final BridgeTls second = readBridge(tls).getBridgeTls();

        assertThat(first).isNotNull();
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    /// Naming the preset is the same configuration as leaving the checks out.
    @Test
    void givenThePresetNamedInAnotherSpelling_whenComparingWithTheChecksLeftOut_thenTheyAreEqual() throws Exception {
        final BridgeTls implicit =
                readBridge("<tls><enabled>true</enabled></tls>").getBridgeTls();
        final BridgeTls named = readBridge(
                        "<tls><enabled>true</enabled><tls-checks> tls-bridge-default </tls-checks></tls>")
                .getBridgeTls();

        assertThat(implicit).isNotNull();
        assertThat(implicit).isEqualTo(named);
        assertThat(implicit.hashCode()).isEqualTo(named.hashCode());
    }

    /// The preset and its five values written out enforce the same checks, yet compare unequal: only
    /// the preset tells them apart, and a bridge moved from one to the other must be restarted.
    @Test
    void givenThePresetAndItsFiveValuesWrittenOut_whenComparingTheBridgeTls_thenTheyAreNotEqual() throws Exception {
        final BridgeTls preset =
                readBridge("<tls><enabled>true</enabled></tls>").getBridgeTls();
        final BridgeTls written = readBridge(PRESET_VALUES_WRITTEN).getBridgeTls();

        assertThat(preset).isNotNull();
        assertThat(written).isNotNull();
        assertThat(preset.getEffectiveTls()).isNotNull();
        assertThat(written.getEffectiveTls()).isNotNull();
        assertThat(BridgeTls.isCompatibilityPreset(preset.getConfiguredTls()))
                .as("the first reading is on the compatibility preset")
                .isTrue();
        assertThat(BridgeTls.isCompatibilityPreset(written.getConfiguredTls()))
                .as("the second reading is not")
                .isFalse();
        assertThat(written.getEffectiveTls().enforcesSameChecks(preset.getEffectiveTls()))
                .as("the five values themselves are the same, so only the preset tells the two apart")
                .isTrue();
        assertThat(preset)
                .as("a bridge moved from the preset to its values written out must be restarted by the reload")
                .isNotEqualTo(written);
        assertThat(written).isNotEqualTo(preset);
    }

    /// The same holds for the bridges themselves.
    @Test
    void givenThePresetAndItsFiveValuesWrittenOut_whenComparingTheBridges_thenTheyAreNotEqual() throws Exception {
        final MqttBridge running = readBridge("<tls><enabled>true</enabled></tls>");
        final MqttBridge reloaded = readBridge(PRESET_VALUES_WRITTEN);

        assertThat(running)
                .as("BridgeService.updateBridges keeps the running bridge when equals says nothing changed")
                .isNotEqualTo(reloaded);
    }

    /// Checks changed from no verification to all make the TLS settings unequal.
    @Test
    void givenTheChecksChangeFromNoVerificationToAll_whenComparingTheBridgeTls_thenTheyAreNotEqual() throws Exception {
        final BridgeTls loosened = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>NO_VERIFICATION</tls-checks>
                </tls>
                """).getBridgeTls();
        final BridgeTls tightened = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>ALL</tls-checks>
                </tls>
                """).getBridgeTls();

        assertThat(loosened).isNotNull();
        assertThat(tightened).isNotNull();
        assertThat(loosened.getEffectiveTls()).isNotNull();
        assertThat(tightened.getEffectiveTls()).isNotNull();
        assertThat(loosened.getEffectiveTls().trustMode())
                .as("the two readings really differ in their effective policy")
                .isNotEqualTo(tightened.getEffectiveTls().trustMode());
        assertThat(loosened)
                .as("a changed TLS policy must make the bridge's TLS settings unequal, so reload restarts it")
                .isNotEqualTo(tightened);
    }

    /// A rotated truststore reference makes the TLS settings unequal.
    @Test
    void givenTheTruststoreReferenceChanges_whenComparingTheBridgeTls_thenTheyAreNotEqual() throws Exception {
        final BridgeTls before = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>ALL</tls-checks>
                    <tls-truststore><file>/tls/old-authority.pem</file><format>pem</format></tls-truststore>
                </tls>
                """).getBridgeTls();
        final BridgeTls after = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>ALL</tls-checks>
                    <tls-truststore><file>/tls/new-authority.pem</file><format>pem</format></tls-truststore>
                </tls>
                """).getBridgeTls();

        assertThat(before).isNotNull();
        assertThat(after).isNotNull();
        assertThat(before.getEffectiveTls()).isNotNull();
        assertThat(after.getEffectiveTls()).isNotNull();
        assertThat(before.getEffectiveTls().truststore())
                .as("the two readings really differ in their truststore")
                .isNotEqualTo(after.getEffectiveTls().truststore());
        assertThat(before)
                .as("a rotated truststore reference must make the bridge's TLS settings unequal, "
                        + "so reload restarts it")
                .isNotEqualTo(after);
    }

    /// A bridge reloaded with changed checks does not equal the running one.
    @Test
    void givenTheChecksChange_whenComparingTheBridges_thenTheyAreNotEqual() throws Exception {
        final MqttBridge running = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>NO_VERIFICATION</tls-checks>
                </tls>
                """);
        final MqttBridge reloaded = readBridge("""
                <tls>
                    <enabled>true</enabled>
                    <tls-checks>ALL</tls-checks>
                </tls>
                """);

        assertThat(running)
                .as("a bridge reloaded with a changed TLS policy must not equal the running one, "
                        + "or BridgeService.updateBridges keeps the old policy")
                .isNotEqualTo(reloaded);
    }

    /// Reads one bridge with the given `tls` block through the real configuration reader, from a
    /// file of its own.
    private @NotNull MqttBridge readBridge(final @NotNull String tls) throws Exception {
        final File file = new File(temporaryFolder, "config-" + readings++ + ".xml");
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
        return reader.getBridgeExtractor().getBridges().getFirst();
    }
}
