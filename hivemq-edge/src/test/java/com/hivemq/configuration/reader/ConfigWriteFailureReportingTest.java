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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ch.qos.logback.classic.Level;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;

/**
 * EDG-882 review v04, finding 1.5 — what an operator is told when the configuration cannot be written.
 * <p>
 * {@code writeConfigWithSync} catches everything and carries on, deliberately: the running node is
 * correct and unchanged, and throwing here would decide, for every configuration endpoint at once, what a
 * REST call should answer when the file cannot be persisted. That decision is not this ticket's to make.
 * What is this ticket's is that the outcome used to be a message-less {@code UnrecoverableException}
 * stack trace under a generic "sync failed" line, which reads like a crash and says nothing about the
 * consequence — while the REST call that triggered it answered success.
 * <p>
 * Every deliberate refusal on the write path — a credential that cannot be kept out of the file,
 * protections that cannot be reproduced — arrives here. The reason is logged by whichever check made the
 * decision; this pins the line that states what it means for the file on disk.
 */
public class ConfigWriteFailureReportingTest extends AbstractConfigurationTest {

    private static final @NotNull String CONFIG =
            "<hivemq>\n<mqtt-bridges>\n    <mqtt-bridge>\n        <id>edg-882-reporting-bridge</id>\n"
                    + "        <remote-broker>\n            <host>testhost</host>\n"
                    + "        </remote-broker>\n        <forwarded-topics>\n            <forwarded-topic>\n"
                    + "                <filters>\n                    <mqtt-topic-filter>plant/#</mqtt-topic-filter>\n"
                    + "                </filters>\n                <destination>{#}</destination>\n"
                    + "                <max-qos>1</max-qos>\n            </forwarded-topic>\n"
                    + "        </forwarded-topics>\n    </mqtt-bridge>\n</mqtt-bridges></hivemq>";

    private @NotNull LogbackCapturingAppender logCapture;
    private @NotNull Path config;

    @BeforeEach
    public void captureTheWriterLog() {
        logCapture = LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(ConfigFileReaderWriter.class));
        config = Path.of(xmlFile.getPath());
        assumeTrue(
                config.getFileSystem().supportedFileAttributeViews().contains("posix"),
                "the write is made to fail through directory permissions");
        assumeTrue(!"root".equals(System.getProperty("user.name")), "a superuser is not refused by mode bits");
    }

    @AfterEach
    public void restoreTheDirectory() throws IOException {
        Files.setPosixFilePermissions(config.getParent(), PosixFilePermissions.fromString("rwx------"));
        LogbackCapturingAppender.Factory.cleanUp();
    }

    private @NotNull List<String> errorsLogged() {
        return logCapture.getCapturedLogs().stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .map(event -> event.getFormattedMessage())
                .toList();
    }

    /**
     * The write fails, the caller is not told, and the log says so: the node is correct, the file is not,
     * and a restart would lose the change. Provoked by taking write permission off the configuration
     * directory, which is the same path every deliberate refusal takes to get here.
     */
    @Test
    public void whenTheConfigurationCannotBeWritten_thenTheLogSaysTheFileNoLongerMatchesTheNode() throws IOException {
        Files.writeString(config, CONFIG);
        reader.applyConfig();
        Files.setPosixFilePermissions(config.getParent(), PosixFilePermissions.fromString("r-x------"));

        assertThatCode(() -> reader.writeConfigWithSync())
                .as("the caller must not see the failure, which is exactly why the log has to carry it")
                .doesNotThrowAnyException();

        assertThat(errorsLogged())
                .as("the operator is told the file is now stale, not only that something failed")
                .anySatisfy(message -> assertThat(message)
                        .contains("config.xml")
                        .contains("restart")
                        .contains("keeps running on the configuration it already holds"));
    }

    /** And the configuration file it could not replace is still the one it was, byte for byte. */
    @Test
    public void whenTheConfigurationCannotBeWritten_thenTheFileOnDiskIsUntouched() throws IOException {
        Files.writeString(config, CONFIG);
        reader.applyConfig();
        Files.setPosixFilePermissions(config.getParent(), PosixFilePermissions.fromString("r-x------"));

        reader.writeConfigWithSync();

        Files.setPosixFilePermissions(config.getParent(), PosixFilePermissions.fromString("rwx------"));
        assertThat(Files.readString(config)).isEqualTo(CONFIG);
    }

    /** An ordinary write logs none of this. */
    @Test
    public void whenTheConfigurationIsWritten_thenNothingIsReported() throws IOException {
        Files.writeString(config, CONFIG);
        reader.applyConfig();

        reader.writeConfigWithSync();

        assertThat(errorsLogged()).isEmpty();
        assertThat(Files.readString(config)).contains("edg-882-reporting-bridge");
    }
}
