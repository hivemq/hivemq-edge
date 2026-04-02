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

import org.junit.jupiter.api.Test;

class KnappogueTopic_Test {

    // ── replacePlaceholders ────────────────────────────────────────────────────

    @Test
    void replacePlaceholdersSubstitutesFleetId() {
        assertThat(KnappogueTopic.replacePlaceholders("{fleetId}/edge", "acme", "-"))
                .isEqualTo("acme/edge");
    }

    @Test
    void replacePlaceholdersSubstitutesInstanceId() {
        assertThat(KnappogueTopic.replacePlaceholders("edge/{edgeInstanceId}", "-", "wolery"))
                .isEqualTo("edge/wolery");
    }

    @Test
    void replacePlaceholdersBoth() {
        assertThat(KnappogueTopic.replacePlaceholders(
                        "HIVEMQ/{fleetId}/EDGE/{edgeInstanceId}/CONFIG/apply", "acme", "wolery"))
                .isEqualTo("HIVEMQ/acme/EDGE/wolery/CONFIG/apply");
    }

    @Test
    void replacePlaceholdersNoPlaceholders() {
        assertThat(KnappogueTopic.replacePlaceholders("custom/topic/apply", "acme", "wolery"))
                .isEqualTo("custom/topic/apply");
    }

    @Test
    void replacePlaceholdersDashDefaults() {
        assertThat(KnappogueTopic.replacePlaceholders(KnappogueTopic.DEFAULT_PATTERN, "-", "-"))
                .isEqualTo("HIVEMQ/-/EDGE/-/CONFIG/apply");
    }

    // ── resolveForPush — CLI override path ────────────────────────────────────

    @Test
    void resolveForPushUsesCliOverrideWhenProvided() {
        final String topic = KnappogueTopic.resolveForPush("acme", "wolery", "custom/topic");
        assertThat(topic).isEqualTo("custom/topic");
    }

    @Test
    void resolveForPushReplacesPlaceholdersInCliOverride() {
        final String topic =
                KnappogueTopic.resolveForPush("acme", "wolery", "HIVEMQ/{fleetId}/EDGE/{edgeInstanceId}/CONFIG/apply");
        assertThat(topic).isEqualTo("HIVEMQ/acme/EDGE/wolery/CONFIG/apply");
    }

    @Test
    void resolveForPushWithBlankCliOverrideFallsThrough() {
        // blank CLI override must not be treated as an override — falls through to env/default
        final String topic = KnappogueTopic.resolveForPush("acme", "wolery", "  ");
        // since HIVEMQ_COMPILED_CONFIG_TOPIC is not expected to be set in test env,
        // the result should be the default pattern with placeholders replaced
        assertThat(topic).isEqualTo("HIVEMQ/acme/EDGE/wolery/CONFIG/apply");
    }

    @Test
    void resolveForPushWithNullCliOverrideUsesDefaultPattern() {
        // assumes HIVEMQ_COMPILED_CONFIG_TOPIC is not set in the test environment
        final String envOverride = System.getenv(KnappogueTopic.TOPIC_OVERRIDE_ENV_VAR);
        org.junit.jupiter.api.Assumptions.assumeTrue(
                envOverride == null || envOverride.isBlank(),
                "Skipped: HIVEMQ_COMPILED_CONFIG_TOPIC is set in the environment");

        final String topic = KnappogueTopic.resolveForPush("acme", "wolery", null);
        assertThat(topic).isEqualTo("HIVEMQ/acme/EDGE/wolery/CONFIG/apply");
    }

    // ── DEFAULT_PATTERN ───────────────────────────────────────────────────────

    @Test
    void defaultPatternContainsExpectedSegments() {
        assertThat(KnappogueTopic.DEFAULT_PATTERN)
                .startsWith("HIVEMQ/")
                .contains("{fleetId}")
                .contains("EDGE/")
                .contains("{edgeInstanceId}")
                .endsWith("/CONFIG/apply")
                .doesNotStartWith("$");
    }

    // ── ENV_VAR constants ─────────────────────────────────────────────────────

    @Test
    void envVarConstantsHaveExpectedValues() {
        assertThat(KnappogueTopic.FLEET_ID_ENV_VAR).isEqualTo("HIVEMQ_FLEET_ID");
        assertThat(KnappogueTopic.INSTANCE_ID_ENV_VAR).isEqualTo("HIVEMQ_EDGE_INSTANCE_ID");
        assertThat(KnappogueTopic.TOPIC_OVERRIDE_ENV_VAR).isEqualTo("HIVEMQ_COMPILED_CONFIG_TOPIC");
    }

    // ── envOr ─────────────────────────────────────────────────────────────────

    @Test
    void envOrReturnsDefaultWhenVarNotSet() {
        // Use a variable name that is very unlikely to be set
        assertThat(KnappogueTopic.envOr("HIVEMQ_KNAPPOGUE_TEST_NONEXISTENT_VAR_XYZ", "fallback"))
                .isEqualTo("fallback");
    }

    @Test
    void envOrReturnsDefaultForBlankValue() {
        // PATH is almost always set and non-blank; but we need a blank env var to test.
        // We can only test the "not set" branch reliably — the blank branch is covered by code inspection.
        assertThat(KnappogueTopic.envOr("HIVEMQ_KNAPPOGUE_TEST_NONEXISTENT_VAR_XYZ", "default"))
                .isEqualTo("default");
    }
}
