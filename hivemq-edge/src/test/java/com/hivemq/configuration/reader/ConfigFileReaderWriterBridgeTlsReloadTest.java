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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.security.tls.TLSConfigBridge;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

/// A bridge TLS configuration Edge cannot carry out, met when the configuration file is read: on a
/// reload the file is rejected and the node keeps the configuration it has, at start-up it is still
/// refused. Before, the refusal surfaced while the configuration was applied, the watcher read it as
/// a change needing a restart and exited the node, and the restarted node refused the same file.
class ConfigFileReaderWriterBridgeTlsReloadTest {

    private static final @NotNull String VALID_TLS =
            "<tls-checks>ALL</tls-checks><tls-truststore><builtin>JVM-cacerts</builtin></tls-truststore>";

    @TempDir
    File folder;

    private @NotNull ListAppender<ILoggingEvent> tlsLog;
    private @NotNull Logger tlsLogger;

    @BeforeEach
    void captureTlsLog() {
        tlsLogger = (Logger) LoggerFactory.getLogger(TLSConfigBridge.class);
        tlsLog = new ListAppender<>();
        tlsLog.start();
        tlsLogger.addAppender(tlsLog);
    }

    @AfterEach
    void releaseTlsLog() {
        tlsLogger.detachAppender(tlsLog);
    }

    /// A reload whose bridge sets the hostname check twice is rejected, and the running
    /// configuration is kept.
    @Test
    void givenARunningConfiguration_whenReloadedWithTwoHostnameSettings_thenTheFileIsRejectedAndTheConfigurationKept()
            throws Exception {
        final ConfigFileReaderWriter reader = reader(null);
        assertThat(reader.loadConfigFromXML(write("running.xml", config(bridge("plant-a", VALID_TLS)))))
                .isEqualTo(ConfigFileReaderWriter.ReloadOutcome.APPLIED);
        final HiveMQConfigEntity running = reader.getConfigEntity();

        final File changed =
                write("changed.xml", config(bridge("plant-a", VALID_TLS + "<verify-hostname>false</verify-hostname>")));

        assertThat(reader.loadConfigFromXML(changed)).isEqualTo(ConfigFileReaderWriter.ReloadOutcome.REJECTED_INVALID);
        assertThat(reader.getConfigEntity()).isSameAs(running);
    }

    /// A reload declaring material that names no file or built-in is rejected, and the running
    /// configuration is kept.
    @Test
    void givenADeclaredMaterialThatNamesNothing_whenReloaded_thenTheFileIsRejectedAndTheConfigurationKept()
            throws Exception {
        final ConfigFileReaderWriter reader = reader(null);
        assertThat(reader.loadConfigFromXML(write("running.xml", config(bridge("plant-a", VALID_TLS)))))
                .isEqualTo(ConfigFileReaderWriter.ReloadOutcome.APPLIED);
        final HiveMQConfigEntity running = reader.getConfigEntity();

        final File changed = write(
                "changed.xml",
                config("<tls-materials><tls-material><name>plant</name><tls-truststore><name>ca</name>"
                        + "</tls-truststore></tls-material></tls-materials>"
                        + bridgesOf(bridge("plant-a", VALID_TLS))));

        assertThat(reader.loadConfigFromXML(changed)).isEqualTo(ConfigFileReaderWriter.ReloadOutcome.REJECTED_INVALID);
        assertThat(reader.getConfigEntity()).isSameAs(running);
    }

    /// At start-up there is no configuration to keep, so the same file is refused.
    @Test
    void givenTwoHostnameSettings_whenTheNodeStarts_thenItRefusesTheConfiguration() throws Exception {
        final File file =
                write("start.xml", config(bridge("plant-a", VALID_TLS + "<verify-hostname>false</verify-hostname>")));

        assertThatThrownBy(() -> reader(file).applyConfig()).isInstanceOf(UnrecoverableException.class);
    }

    /// The check that reads the file builds each bridge's TLS a first time, and the bridge is built
    /// from it afterwards; only the second may warn, or every warning a bridge earns is logged twice.
    @Test
    void givenABridgeWithoutATruststore_whenTheFileIsRead_thenTheWarningIsLoggedOnce() throws Exception {
        final ConfigFileReaderWriter reader = reader(null);

        assertThat(reader.loadConfigFromXML(write("no-truststore.xml", config(bridge("cloud", "")))))
                .isEqualTo(ConfigFileReaderWriter.ReloadOutcome.APPLIED);

        assertThat(tlsLog.list).hasSize(1);
    }

    /// The remedy the no-truststore warning gives must be one the schema accepts: `builtin` is a child
    /// of `tls-truststore` only, including for a bridge written in the superseded spelling, where
    /// `truststore` takes a path and a password.
    @Test
    void givenABridgeWithoutATruststore_whenItsTlsIsBuilt_thenTheWarningNamesTheElementThatTakesABuiltin()
            throws Exception {
        reader(null).loadConfigFromXML(write("no-truststore.xml", config(bridge("cloud", ""))));

        assertThat(tlsLog.list)
                .singleElement()
                .extracting(ILoggingEvent::getFormattedMessage)
                .asString()
                .contains("<tls-truststore><builtin>JVM-cacerts</builtin></tls-truststore>")
                .doesNotContain("<truststore>");
    }

    /// A clash inside one declaration is refused by the same check as a bridge's own fault.
    @Test
    void givenTwoDeclaredEntriesOfOneName_whenReloaded_thenTheFileIsRejectedAndTheConfigurationKept() throws Exception {
        final ConfigFileReaderWriter reader = reader(null);
        assertThat(reader.loadConfigFromXML(write("running.xml", config(bridge("plant-a", VALID_TLS)))))
                .isEqualTo(ConfigFileReaderWriter.ReloadOutcome.APPLIED);
        final HiveMQConfigEntity running = reader.getConfigEntity();

        final File changed = write(
                "changed.xml",
                config("<tls-materials><tls-material><name>plant</name>"
                        + "<tls-truststore><name>ca</name><builtin>JVM-cacerts</builtin></tls-truststore>"
                        + "<tls-truststore><name>ca</name><builtin>JVM-cacerts</builtin></tls-truststore>"
                        + "</tls-material></tls-materials>" + bridgesOf(bridge("plant-a", VALID_TLS))));

        assertThat(reader.loadConfigFromXML(changed)).isEqualTo(ConfigFileReaderWriter.ReloadOutcome.REJECTED_INVALID);
        assertThat(reader.getConfigEntity()).isSameAs(running);
    }

    // --- fixtures -----------------------------------------------------------------------------------

    private static @NotNull ConfigFileReaderWriter reader(final File file) {
        final SystemInformation systemInformation = mock(SystemInformation.class);
        when(systemInformation.isConfigFragmentBase64Zip()).thenReturn(false);
        return new ConfigFileReaderWriter(systemInformation, new ConfigurationFile(file), List.of());
    }

    private static @NotNull String config(final @NotNull String body) {
        final String inner = body.startsWith("<mqtt-bridge>") ? bridgesOf(body) : body;
        return "<?xml version=\"1.0\"?>\n<hivemq>" + inner + "</hivemq>\n";
    }

    private static @NotNull String bridgesOf(final @NotNull String bridges) {
        return "<mqtt-bridges>" + bridges + "</mqtt-bridges>";
    }

    private static @NotNull String bridge(final @NotNull String id, final @NotNull String tls) {
        return "<mqtt-bridge><id>" + id + "</id><remote-broker><host>broker.example.com</host><port>8883</port>"
                + "<tls><enabled>true</enabled>" + tls + "</tls></remote-broker>"
                + "<forwarded-topics><forwarded-topic><filters><mqtt-topic-filter>" + id
                + "/#</mqtt-topic-filter></filters></forwarded-topic></forwarded-topics></mqtt-bridge>";
    }

    private @NotNull File write(final @NotNull String name, final @NotNull String content) throws Exception {
        final File file = new File(folder, name);
        Files.writeString(file.toPath(), content);
        return file;
    }
}
