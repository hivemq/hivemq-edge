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
        final AtomicReference<List<ProtocolAdapterEntity>> received = new AtomicReference<>();

        extractor.registerConsumer(received::set);

        assertThat(received.get()).isEmpty();
    }

    @Test
    void validSection_isAppliedAndTheConsumerIsNotified() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        final AtomicReference<List<ProtocolAdapterEntity>> received = new AtomicReference<>();
        extractor.registerConsumer(received::set);

        final Configurator.ConfigResult result = extractor.updateConfig(config(adapter("chaos-1")));

        assertThat(result).isEqualTo(Configurator.ConfigResult.SUCCESS);
        assertThat(extractor.getAllConfigs()).hasSize(1);
        assertThat(extractor.getAdapterByAdapterId("chaos-1")).isPresent();
        assertThat(received.get()).hasSize(1);
        assertThat(received.get().getFirst().getAdapterId()).isEqualTo("chaos-1");
    }

    @Test
    void emptySection_appliesSuccessfullyWithNoAdapters() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();

        assertThat(extractor.updateConfig(new HiveMQConfigEntity())).isEqualTo(Configurator.ConfigResult.SUCCESS);
        assertThat(extractor.getAllConfigs()).isEmpty();
    }

    // S32 at the load boundary: a watchdog that does not outlast the command timeout fails the whole section.
    @Test
    void invalidSection_isRejectedAndLeavesThePreviousConfigUntouched() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        final AtomicInteger notifications = new AtomicInteger();
        extractor.registerConsumer(configs -> notifications.incrementAndGet());

        assertThat(extractor.updateConfig(config(adapter("chaos-1")))).isEqualTo(Configurator.ConfigResult.SUCCESS);
        final int notificationsAfterValid = notifications.get();

        final ProtocolAdapterEntity invalid = adapterWithTimeouts("chaos-2", 5_000, 10_000);
        assertThat(extractor.updateConfig(config(invalid))).isEqualTo(Configurator.ConfigResult.ERROR);

        // previous good config retained; the rejected section never reached the consumer
        assertThat(extractor.getAllConfigs()).hasSize(1);
        assertThat(extractor.getAllConfigs().getFirst().getAdapterId()).isEqualTo("chaos-1");
        assertThat(notifications.get()).isEqualTo(notificationsAfterValid);
    }

    @Test
    void duplicateAdapterIds_areRejected() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();

        final Configurator.ConfigResult result = extractor.updateConfig(config(adapter("chaos-1"), adapter("chaos-1")));

        assertThat(result).isEqualTo(Configurator.ConfigResult.ERROR);
        assertThat(extractor.getAllConfigs()).isEmpty();
    }

    @Test
    void sync_roundTripsTheLoadedSectionVerbatim() {
        final ProtocolAdapterExtractor extractor = new ProtocolAdapterExtractor();
        extractor.updateConfig(config(adapter("chaos-1")));

        final HiveMQConfigEntity target = new HiveMQConfigEntity();
        target.getV2ProtocolAdapterConfig().add(adapter("stale"));
        extractor.sync(target);

        assertThat(target.getV2ProtocolAdapterConfig()).hasSize(1);
        assertThat(target.getV2ProtocolAdapterConfig().getFirst().getAdapterId())
                .isEqualTo("chaos-1");
    }

    @Test
    void needsRestartWithConfig_isFalse() {
        assertThat(new ProtocolAdapterExtractor().needsRestartWithConfig(new HiveMQConfigEntity()))
                .isFalse();
    }

    private static @NotNull HiveMQConfigEntity config(final @NotNull ProtocolAdapterEntity... adapters) {
        final HiveMQConfigEntity entity = new HiveMQConfigEntity();
        entity.getV2ProtocolAdapterConfig().addAll(List.of(adapters));
        return entity;
    }

    private static @NotNull ProtocolAdapterEntity adapter(final @NotNull String adapterId) {
        return adapterWithTimeouts(adapterId, 30_000, 10_000);
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
