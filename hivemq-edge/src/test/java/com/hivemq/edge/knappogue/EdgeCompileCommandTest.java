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
package com.hivemq.edge.knappogue;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EdgeCompileCommandTest {

    // ── detectSingleInstance ───────────────────────────────────────────────────

    @Test
    void detectSingleInstanceReturnsSingleDirectory(@TempDir final Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("instances").resolve("wolery"));
        assertThat(EdgeCompileCommand.detectSingleInstance(tempDir)).isEqualTo("wolery");
    }

    @Test
    void detectSingleInstanceReturnsNullForMultipleInstances(@TempDir final Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("instances").resolve("wolery"));
        Files.createDirectories(tempDir.resolve("instances").resolve("gloomy"));
        assertThat(EdgeCompileCommand.detectSingleInstance(tempDir)).isNull();
    }

    @Test
    void detectSingleInstanceReturnsNullWhenNoInstancesDir(@TempDir final Path tempDir) {
        assertThat(EdgeCompileCommand.detectSingleInstance(tempDir)).isNull();
    }

    @Test
    void detectSingleInstanceReturnsNullWhenInstancesDirIsEmpty(@TempDir final Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("instances"));
        assertThat(EdgeCompileCommand.detectSingleInstance(tempDir)).isNull();
    }

    @Test
    void detectSingleInstanceIgnoresFilesInInstancesDir(@TempDir final Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("instances"));
        Files.writeString(tempDir.resolve("instances").resolve("readme.txt"), "not a dir");
        assertThat(EdgeCompileCommand.detectSingleInstance(tempDir)).isNull();
    }

    // ── PushOptions.parse — fleet and topic ───────────────────────────────────

    @Test
    void parseDefaultsToNoPush() {
        final var opts = EdgeCompileCommand.PushOptions.parse(new String[] {"compile", "-p", "."}, null);
        assertThat(opts.push()).isFalse();
    }

    @Test
    void parsePushFlagEnablesPush() {
        final var opts = EdgeCompileCommand.PushOptions.parse(new String[] {"--push"}, null);
        assertThat(opts.push()).isTrue();
    }

    @Test
    void parseFleetFlagSetsFleet() {
        // assumes HIVEMQ_COMPILED_CONFIG_TOPIC is not set
        assumeNoTopicEnvOverride();
        final var opts = EdgeCompileCommand.PushOptions.parse(new String[] {"--fleet", "acme", "--push"}, "wolery");
        assertThat(opts.topic()).isEqualTo("HIVEMQ/acme/EDGE/wolery/CONFIG/apply");
    }

    @Test
    void parseInstanceIdIsUsedInDefaultTopic() {
        assumeNoTopicEnvOverride();
        final var opts = EdgeCompileCommand.PushOptions.parse(new String[] {"--push"}, "wolery");
        assertThat(opts.topic()).contains("wolery");
    }

    @Test
    void parseTopicOverrideUsedVerbatim() {
        final var opts = EdgeCompileCommand.PushOptions.parse(new String[] {"--topic", "custom/topic"}, "wolery");
        assertThat(opts.topic()).isEqualTo("custom/topic");
    }

    @Test
    void parseTopicOverrideWithPlaceholders() {
        final var opts = EdgeCompileCommand.PushOptions.parse(
                new String[] {"--fleet", "acme", "--topic", "HIVEMQ/{fleetId}/EDGE/{edgeInstanceId}/CONFIG/apply"},
                "wolery");
        assertThat(opts.topic()).isEqualTo("HIVEMQ/acme/EDGE/wolery/CONFIG/apply");
    }

    @Test
    void parseHostAndPortAreRead() {
        final var opts = EdgeCompileCommand.PushOptions.parse(
                new String[] {"--push", "--host", "192.168.1.10", "--port", "1883"}, null);
        assertThat(opts.host()).isEqualTo("192.168.1.10");
        assertThat(opts.port()).isEqualTo(1883);
    }

    @Test
    void parseInvalidPortUsesDefault() {
        final var opts = EdgeCompileCommand.PushOptions.parse(new String[] {"--port", "notanumber"}, null);
        assertThat(opts.port()).isEqualTo(EdgeCompileCommand.DEFAULT_PORT);
    }

    @Test
    void parsePushFlagsAreStrippedFromRemainingArgs() {
        final var opts = EdgeCompileCommand.PushOptions.parse(
                new String[] {
                    "--push", "--fleet", "acme", "--host", "h", "--port", "1883", "--topic", "t", "compile", "-p", "."
                },
                null);
        assertThat(opts.strippedArgs()).containsExactly("compile", "-p", ".");
    }

    @Test
    void parseInstanceFlagIsNotStripped() {
        // --instance is a compiler flag and must be forwarded to the compiler
        final var opts = EdgeCompileCommand.PushOptions.parse(
                new String[] {"--push", "--instance", "wolery", "-p", "."}, "wolery");
        assertThat(opts.strippedArgs()).containsExactly("--instance", "wolery", "-p", ".");
    }

    @Test
    void parseNullInstanceIdDefaultsToDashInTopic() {
        assumeNoTopicEnvOverride();
        final var opts = EdgeCompileCommand.PushOptions.parse(new String[] {"--fleet", "acme"}, null);
        assertThat(opts.topic()).isEqualTo("HIVEMQ/acme/EDGE/-/CONFIG/apply");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void assumeNoTopicEnvOverride() {
        final String override = System.getenv(KnappogueTopic.TOPIC_OVERRIDE_ENV_VAR);
        org.junit.jupiter.api.Assumptions.assumeTrue(
                override == null || override.isBlank(),
                "Skipped: HIVEMQ_COMPILED_CONFIG_TOPIC is set in the environment");
    }
}
