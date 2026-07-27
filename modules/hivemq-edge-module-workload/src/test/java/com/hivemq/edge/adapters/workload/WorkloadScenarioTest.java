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
package com.hivemq.edge.adapters.workload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Unit layer for the scenario language: proves that what a scenario PROMISES is what the parsed model COMMANDS. Fast
 * and engine-free — the wired e2e (hivemq-edge-test) and the black-box QA suite (hivemq-testsuite) verify execution
 * against a real runtime; this layer verifies the instrument's own semantics, so a scenario typo or a parsing
 * regression can never silently reshape what a test believes it configured.
 */
class WorkloadScenarioTest {

    private static final @org.jetbrains.annotations.NotNull Random NO_NOISE = new Random(0);

    // ── the "*" scoping rule ────────────────────────────────────────────────────────────────────────────────────────

    /**
     * The documented scoping surprise: a named tag entry shadows the {@code "*"} wildcard ENTIRELY — there is no
     * field-level inheritance. {@code bad} declares only a verify fault, so its wave is the built-in constant-0
     * default, NOT the wildcard's sine.
     */
    @Test
    void aNamedTagShadowsTheWildcardEntirely_noFieldLevelInheritance() {
        final WorkloadScenario s = WorkloadScenario.parseOrEmpty("{\"tags\":{\"bad\":{\"verify\":\"permanent\"},"
                + "\"*\":{\"wave\":\"sine\",\"periodMs\":1000,\"amplitude\":2,\"offset\":10}}}");

        // the named entry's own field applies…
        assertThat(s.faultFor("bad").verify()).isEqualTo("permanent");
        // …but every unset field comes from the LANGUAGE defaults, not from "*": constant 0, at any instant
        assertThat(s.waveKind("bad")).isEqualTo("constant");
        assertThat(s.valueFor("bad", 250, NO_NOISE)).isCloseTo(0.0, within(1e-9));
        // while an unnamed tag still gets the wildcard's sine (peak at period/4: offset+amplitude = 12)
        assertThat(s.valueFor("unnamed", 250, NO_NOISE)).isCloseTo(12.0, within(1e-9));
    }

    @Test
    void anUnnamedTagFallsBackToTheWildcardEntry() {
        final WorkloadScenario s =
                WorkloadScenario.parseOrEmpty("{\"tags\":{\"*\":{\"wave\":\"constant\",\"value\":42}}}");

        assertThat(s.waveKind("anything")).isEqualTo("constant");
        assertThat(s.valueFor("anything", 0, NO_NOISE)).isCloseTo(42.0, within(1e-9));
        assertThat(s.faultFor("anything").verify()).isEqualTo("success");
    }

    @Test
    void noWildcardAndNoNamedEntry_yieldsTheHealthyDefaults() {
        final WorkloadScenario s = WorkloadScenario.parseOrEmpty("{\"tags\":{\"only\":{\"wave\":\"counter\"}}}");

        // "other" is named nowhere and there is no "*": healthy fault, constant-0 wave
        assertThat(s.faultFor("other").verify()).isEqualTo("success");
        assertThat(s.faultFor("other").poll()).isEqualTo("value");
        assertThat(s.faultFor("other").write()).isEqualTo("success");
        assertThat(s.valueFor("other", 1234, NO_NOISE)).isCloseTo(0.0, within(1e-9));
    }

    // ── parsing the whole program ───────────────────────────────────────────────────────────────────────────────────

    @Test
    void aFullScenarioParses_connectMisbehaveControlDirTimelineAndPerTagBehaviors() {
        final WorkloadScenario s = WorkloadScenario.parseOrEmpty("{"
                + "\"connect\":\"fail\","
                + "\"misbehave\":\"verify-partial\","
                + "\"controlDir\":\"/tmp/wl-unit\","
                + "\"tags\":{\"flappy\":{\"verify\":\"transient-then-success\",\"transientCount\":3,\"reason\":\"boom\"}},"
                + "\"timeline\":[{\"atMs\":5000,\"action\":\"disconnect\"},{\"atMs\":9000,\"action\":\"mute\",\"tag\":\"flappy\"}]"
                + "}");

        assertThat(s.connect()).isEqualTo("fail");
        assertThat(s.misbehave()).isEqualTo("verify-partial");
        assertThat(s.controlDir()).isEqualTo("/tmp/wl-unit");
        assertThat(s.faultFor("flappy").verify()).isEqualTo("transient-then-success");
        assertThat(s.faultFor("flappy").transientCount()).isEqualTo(3);
        assertThat(s.faultFor("flappy").reason()).isEqualTo("boom");
        assertThat(s.timeline()).hasSize(2);
        assertThat(s.timeline().get(0).atMs()).isEqualTo(5000);
        assertThat(s.timeline().get(0).action()).isEqualTo("disconnect");
        assertThat(s.timeline().get(1).action()).isEqualTo("mute");
        assertThat(s.timeline().get(1).tag()).isEqualTo("flappy");
    }

    @Test
    void theScenarioIsExtractedFromTheAdapterConfigMapUnderTheScenarioKey() {
        final WorkloadScenario s = WorkloadScenario.parseOrEmpty(Map.of("scenario", "{\"connect\":\"no-response\"}"));

        assertThat(s.connect()).isEqualTo("no-response");
    }

    @Test
    void unparseableJsonFallsBackToTheEmptyScenario_neverThrows() {
        final WorkloadScenario s = WorkloadScenario.parseOrEmpty("{this is not json");

        // the adapter must stay runnable: healthy defaults, control channel disabled
        assertThat(s.connect()).isEqualTo("succeed");
        assertThat(s.misbehave()).isEmpty();
        assertThat(s.controlDir()).isEmpty();
        assertThat(s.timeline()).isEmpty();
        assertThat(s.faultFor("any").verify()).isEqualTo("success");
    }

    @Test
    void aNullOrBlankScenarioYieldsTheEmptyScenario() {
        for (final WorkloadScenario s :
                new WorkloadScenario[] {WorkloadScenario.parseOrEmpty(null), WorkloadScenario.parseOrEmpty("  ")}) {
            assertThat(s.connect()).isEqualTo("succeed");
            assertThat(s.controlDir()).isEmpty();
        }
    }

    // ── wave math (pure functions of elapsed time) ──────────────────────────────────────────────────────────────────

    @Test
    void sineEvaluatesOffsetPlusAmplitudeTimesSineOfElapsedOverPeriod() {
        final WorkloadScenario s = WorkloadScenario.parseOrEmpty(
                "{\"tags\":{\"t\":{\"wave\":\"sine\",\"periodMs\":1000,\"amplitude\":2,\"offset\":10}}}");

        assertThat(s.valueFor("t", 0, NO_NOISE)).isCloseTo(10.0, within(1e-9)); // sin(0) = 0
        assertThat(s.valueFor("t", 250, NO_NOISE)).isCloseTo(12.0, within(1e-9)); // sin(π/2) = 1 → offset+amplitude
        assertThat(s.valueFor("t", 750, NO_NOISE)).isCloseTo(8.0, within(1e-9)); // sin(3π/2) = -1
    }

    @Test
    void rampEvaluatesOffsetPlusSlopeTimesElapsedSeconds() {
        final WorkloadScenario s =
                WorkloadScenario.parseOrEmpty("{\"tags\":{\"t\":{\"wave\":\"ramp\",\"slopePerSec\":2,\"offset\":1}}}");

        assertThat(s.valueFor("t", 0, NO_NOISE)).isCloseTo(1.0, within(1e-9));
        assertThat(s.valueFor("t", 1500, NO_NOISE)).isCloseTo(4.0, within(1e-9)); // 1 + 2·1.5
    }

    @Test
    void anUnknownWaveKindFallsBackToTheConstantDefault_notAnError() {
        final WorkloadScenario s =
                WorkloadScenario.parseOrEmpty("{\"tags\":{\"t\":{\"wave\":\"sineee\",\"value\":7}}}");

        // unknown enum values must degrade loudly-but-safely (WARN + default semantics), never break the device
        assertThat(s.valueFor("t", 123, NO_NOISE)).isCloseTo(7.0, within(1e-9));
    }
}
