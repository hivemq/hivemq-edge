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
package com.hivemq.protocols.v2.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.Configurator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class ProtocolAdapterExtractorTest {

    @Test
    void registerConsumer_notifiesImmediatelyWithTheCurrentConfigs() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        final AtomicReference<ProtocolAdapterConfigUpdate> received = new AtomicReference<>();

        extractor.registerConsumer(received::set);

        assertThat(received.get().adapters()).isEmpty();
        assertThat(received.get().rejected()).isEmpty();
    }

    @Test
    void validSection_isAppliedAndTheConsumerIsNotified() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        final AtomicReference<ProtocolAdapterConfigUpdate> received = new AtomicReference<>();
        extractor.registerConsumer(received::set);

        final Configurator.ConfigResult result = extractor.updateConfig(config(adapter("chaos-1")));

        assertThat(result).isEqualTo(Configurator.ConfigResult.SUCCESS);
        assertThat(extractor.getAllConfigs()).hasSize(1);
        assertThat(extractor.getAdapterByAdapterId("chaos-1")).isPresent();
        assertThat(received.get().adapters()).hasSize(1);
        assertThat(received.get().adapters().getFirst().getAdapterId()).isEqualTo("chaos-1");
        assertThat(received.get().rejected()).isEmpty();
    }

    @Test
    void emptySection_appliesSuccessfullyWithNoAdapters() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();

        assertThat(extractor.updateConfig(new HiveMQConfigEntity())).isEqualTo(Configurator.ConfigResult.SUCCESS);
        assertThat(extractor.getAllConfigs()).isEmpty();
    }

    // EDG-824 #4: a bad adapter is scoped to itself — the section applies, the sibling runs, the bad adapter is
    // surfaced in the rejected list instead of rejecting the section (which used to shut the whole node down).
    @Test
    void invalidNewAdapter_isScopedToThatAdapterAndSurfacedAsRejected() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        final AtomicReference<ProtocolAdapterConfigUpdate> received = new AtomicReference<>();
        extractor.registerConsumer(received::set);

        final ProtocolAdapterEntity invalid = adapterWithTimeouts("chaos-2", 5_000, 10_000); // violates S32
        final Configurator.ConfigResult result = extractor.updateConfig(config(adapter("chaos-1"), invalid));

        assertThat(result).isEqualTo(Configurator.ConfigResult.SUCCESS);
        assertThat(extractor.getAllConfigs()).hasSize(1);
        assertThat(extractor.getAllConfigs().getFirst().getAdapterId()).isEqualTo("chaos-1");
        assertThat(received.get().rejected()).hasSize(1);
        assertThat(received.get().rejected().getFirst().entity().getAdapterId()).isEqualTo("chaos-2");
        assertThat(received.get().rejected().getFirst().reason()).contains("watchdog-timeout-millis");
    }

    // EDG-824 #4 — reproduces the QA MalformedAdapterConfigDoesNotCrashTheNodeTest tripwire input exactly: a NEW
    // adapter with an empty <protocol-id> declared alongside a healthy sibling in the same (re)load. The empty
    // protocol-id is scoped to the rejected list; the healthy sibling is applied and the section succeeds — proving
    // the node is never shut down by the malformed adapter. (The sibling-survival mechanism is also covered by
    // invalidNewAdapter_isScopedToThatAdapterAndSurfacedAsRejected using an S32 violation; this pins the tripwire's
    // exact empty-protocol-id malformation.)
    @Test
    void newAdapterWithEmptyProtocolId_isRejectedWhileTheHealthySiblingIsApplied() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        final AtomicReference<ProtocolAdapterConfigUpdate> received = new AtomicReference<>();
        extractor.registerConsumer(received::set);

        final ProtocolAdapterEntity emptyProtocolId = adapterWithProtocolId("bad-one", "");
        final Configurator.ConfigResult result =
                extractor.updateConfig(config(adapter("healthy-one"), emptyProtocolId));

        assertThat(result).isEqualTo(Configurator.ConfigResult.SUCCESS);
        assertThat(extractor.getAllConfigs()).hasSize(1);
        assertThat(extractor.getAllConfigs().getFirst().getAdapterId()).isEqualTo("healthy-one");
        assertThat(received.get().rejected()).hasSize(1);
        assertThat(received.get().rejected().getFirst().entity().getAdapterId()).isEqualTo("bad-one");
        assertThat(received.get().rejected().getFirst().reason()).contains("protocol-id");
    }

    // EDG-824 #4 (transactional per-adapter rejection): an invalid replacement for a previously-accepted adapter
    // keeps the previously-applied configuration running untouched — it is neither removed nor marked rejected.
    @Test
    void invalidReplacement_keepsThePreviouslyAppliedConfiguration() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        final ProtocolAdapterEntity original = adapter("chaos-1");
        assertThat(extractor.updateConfig(config(original))).isEqualTo(Configurator.ConfigResult.SUCCESS);

        final ProtocolAdapterEntity invalidReplacement = adapterWithTimeouts("chaos-1", 5_000, 10_000);
        final AtomicReference<ProtocolAdapterConfigUpdate> received = new AtomicReference<>();
        extractor.registerConsumer(received::set);
        assertThat(extractor.updateConfig(config(invalidReplacement))).isEqualTo(Configurator.ConfigResult.SUCCESS);

        assertThat(extractor.getAllConfigs()).hasSize(1);
        assertThat(extractor.getAllConfigs().getFirst()).isEqualTo(original);
        assertThat(received.get().adapters()).containsExactly(original);
        assertThat(received.get().rejected()).isEmpty();
    }

    // Sam #4: a duplicate adapter-id makes the document ambiguous. There is NO "keep the first declaration" winner
    // any more — neither declaration is applied. For a NEW id, a single ERROR adapter is surfaced (below); for an
    // already-running id, the last-known-good configuration is kept (below). The result must be identical for every
    // ordering of the duplicates, for all validity combinations (valid/invalid).

    @Test
    void duplicateNewId_bothValid_rejectsAsAmbiguousInAnyOrder() {
        assertNewIdDuplicateRejectedInEveryOrder(
                adapterWithTimeouts("dup", 30_000, 10_000), adapterWithTimeouts("dup", 60_000, 10_000));
    }

    @Test
    void duplicateNewId_invalidThenValid_rejectsAsAmbiguousInAnyOrder() {
        assertNewIdDuplicateRejectedInEveryOrder(
                adapterWithTimeouts("dup", 5_000, 10_000), adapterWithTimeouts("dup", 30_000, 10_000));
    }

    @Test
    void duplicateNewId_validThenInvalid_rejectsAsAmbiguousInAnyOrder() {
        assertNewIdDuplicateRejectedInEveryOrder(
                adapterWithTimeouts("dup", 30_000, 10_000), adapterWithTimeouts("dup", 5_000, 10_000));
    }

    @Test
    void duplicateNewId_bothInvalid_rejectsAsAmbiguousInAnyOrder() {
        assertNewIdDuplicateRejectedInEveryOrder(
                adapterWithTimeouts("dup", 5_000, 10_000), adapterWithTimeouts("dup", 4_000, 10_000));
    }

    @Test
    void duplicateRunningId_bothValid_keepsPreviousInAnyOrder() {
        assertRunningIdDuplicateKeepsPreviousInEveryOrder(
                adapterWithTimeouts("dup", 60_000, 10_000), adapterWithTimeouts("dup", 45_000, 10_000));
    }

    @Test
    void duplicateRunningId_invalidThenValid_keepsPreviousInAnyOrder() {
        assertRunningIdDuplicateKeepsPreviousInEveryOrder(
                adapterWithTimeouts("dup", 5_000, 10_000), adapterWithTimeouts("dup", 60_000, 10_000));
    }

    @Test
    void duplicateRunningId_validThenInvalid_keepsPreviousInAnyOrder() {
        assertRunningIdDuplicateKeepsPreviousInEveryOrder(
                adapterWithTimeouts("dup", 60_000, 10_000), adapterWithTimeouts("dup", 5_000, 10_000));
    }

    @Test
    void duplicateRunningId_bothInvalid_keepsPreviousInAnyOrder() {
        assertRunningIdDuplicateKeepsPreviousInEveryOrder(
                adapterWithTimeouts("dup", 5_000, 10_000), adapterWithTimeouts("dup", 4_000, 10_000));
    }

    @Test
    void adapterWithoutAnAdapterId_isDroppedWithoutRejectingTheSection() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        final AtomicReference<ProtocolAdapterConfigUpdate> received = new AtomicReference<>();
        extractor.registerConsumer(received::set);

        final ProtocolAdapterEntity anonymous = adapterWithId("");
        final Configurator.ConfigResult result = extractor.updateConfig(config(anonymous, adapter("chaos-1")));

        assertThat(result).isEqualTo(Configurator.ConfigResult.SUCCESS);
        assertThat(extractor.getAllConfigs()).hasSize(1);
        assertThat(extractor.getAllConfigs().getFirst().getAdapterId()).isEqualTo("chaos-1");
        assertThat(received.get().rejected()).isEmpty();
    }

    // A previously-rejected adapter whose configuration is fixed on the next reload becomes a normal accepted
    // adapter — the rejection is not sticky.
    @Test
    void fixedReplacementOfARejectedAdapter_isAccepted() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        extractor.updateConfig(config(adapterWithTimeouts("chaos-1", 5_000, 10_000)));
        assertThat(extractor.getAllConfigs()).isEmpty();

        final AtomicReference<ProtocolAdapterConfigUpdate> received = new AtomicReference<>();
        extractor.registerConsumer(received::set);
        assertThat(extractor.updateConfig(config(adapter("chaos-1")))).isEqualTo(Configurator.ConfigResult.SUCCESS);

        assertThat(extractor.getAllConfigs()).hasSize(1);
        assertThat(received.get().adapters()).hasSize(1);
        assertThat(received.get().rejected()).isEmpty();
    }

    @Test
    void sync_roundTripsTheLoadedSectionVerbatim() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        extractor.updateConfig(config(adapter("chaos-1")));

        final HiveMQConfigEntity target = new HiveMQConfigEntity();
        target.getV2().getProtocolAdapters().add(adapter("stale"));
        extractor.sync(target);

        assertThat(target.getV2().getProtocolAdapters()).hasSize(1);
        assertThat(target.getV2().getProtocolAdapters().getFirst().getAdapterId())
                .isEqualTo("chaos-1");
    }

    @Test
    void needsRestartWithConfig_isFalse() {
        assertThat(new ProtocolAdapterExtractor().needsRestartWithConfig(new HiveMQConfigEntity()))
                .isFalse();
    }

    @Test
    void everyUpdate_notifiesTheConsumer() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        final AtomicInteger notifications = new AtomicInteger();
        extractor.registerConsumer(update -> notifications.incrementAndGet());
        final int notificationsAfterRegistration = notifications.get();

        extractor.updateConfig(config(adapter("chaos-1")));
        extractor.updateConfig(config(adapterWithTimeouts("chaos-1", 5_000, 10_000)));

        assertThat(notifications.get()).isEqualTo(notificationsAfterRegistration + 2);
    }

    // Both entities must share the same (new, not-yet-running) adapter-id. Asserts that whichever order the two
    // ambiguous duplicates appear in, the outcome is identical: nothing is applied and exactly one adapter is
    // rejected — no "winner" is ever selected.
    private static void assertNewIdDuplicateRejectedInEveryOrder(
            final @NotNull ProtocolAdapterEntity a, final @NotNull ProtocolAdapterEntity b) {
        assertNewIdDuplicateRejected(a, b);
        assertNewIdDuplicateRejected(b, a);
    }

    private static void assertNewIdDuplicateRejected(
            final @NotNull ProtocolAdapterEntity first, final @NotNull ProtocolAdapterEntity second) {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        final AtomicReference<ProtocolAdapterConfigUpdate> received = new AtomicReference<>();
        extractor.registerConsumer(received::set);

        final Configurator.ConfigResult result = extractor.updateConfig(config(first, second));

        assertThat(result).isEqualTo(Configurator.ConfigResult.SUCCESS);
        assertThat(extractor.getAllConfigs()).isEmpty();
        assertThat(received.get().adapters()).isEmpty();
        assertThat(received.get().rejected()).hasSize(1);
        assertThat(received.get().rejected().getFirst().entity().getAdapterId()).isEqualTo("dup");
        assertThat(received.get().rejected().getFirst().reason()).containsIgnoringCase("duplicate");
    }

    // Both entities must share an adapter-id that is already running from a prior updateConfig. Asserts that whichever
    // order the two ambiguous duplicates appear in, the previously-applied (last-known-good) configuration is kept
    // untouched and nothing is rejected.
    private static void assertRunningIdDuplicateKeepsPreviousInEveryOrder(
            final @NotNull ProtocolAdapterEntity a, final @NotNull ProtocolAdapterEntity b) {
        assertRunningIdDuplicateKeepsPrevious(a, b);
        assertRunningIdDuplicateKeepsPrevious(b, a);
    }

    private static void assertRunningIdDuplicateKeepsPrevious(
            final @NotNull ProtocolAdapterEntity first, final @NotNull ProtocolAdapterEntity second) {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        final ProtocolAdapterEntity previous = adapter("dup");
        assertThat(extractor.updateConfig(config(previous))).isEqualTo(Configurator.ConfigResult.SUCCESS);
        assertThat(extractor.getAllConfigs()).containsExactly(previous);

        final AtomicReference<ProtocolAdapterConfigUpdate> received = new AtomicReference<>();
        extractor.registerConsumer(received::set);
        final Configurator.ConfigResult result = extractor.updateConfig(config(first, second));

        assertThat(result).isEqualTo(Configurator.ConfigResult.SUCCESS);
        assertThat(extractor.getAllConfigs()).containsExactly(previous);
        assertThat(received.get().adapters()).containsExactly(previous);
        assertThat(received.get().rejected()).isEmpty();
    }

    private static @NotNull HiveMQConfigEntity config(final @NotNull ProtocolAdapterEntity... adapters) {
        final HiveMQConfigEntity entity = new HiveMQConfigEntity();
        entity.getV2().getProtocolAdapters().addAll(List.of(adapters));
        return entity;
    }

    private static @NotNull ProtocolAdapterEntity adapter(final @NotNull String adapterId) {
        return adapterWithTimeouts(adapterId, 30_000, 10_000);
    }

    private static @NotNull ProtocolAdapterEntity adapterWithId(final @NotNull String adapterId) {
        return adapterWithTimeouts(adapterId, 30_000, 10_000);
    }

    private static @NotNull ProtocolAdapterEntity adapterWithProtocolId(
            final @NotNull String adapterId, final @NotNull String protocolId) {
        return new ProtocolAdapterEntity(
                adapterId,
                protocolId,
                2,
                true,
                false,
                false,
                Map.of(),
                new RetryPolicyEntity(),
                30_000,
                10_000,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());
    }

    private static @NotNull ProtocolAdapterEntity adapterWithTimeouts(
            final @NotNull String adapterId, final long watchdogMillis, final long commandMillis) {
        return new ProtocolAdapterEntity(
                adapterId,
                "chaos",
                2,
                true,
                false,
                false,
                Map.of(),
                new RetryPolicyEntity(),
                watchdogMillis,
                commandMillis,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());
    }
}
