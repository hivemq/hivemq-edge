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
package com.hivemq.protocols.v2.northbound;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.metrics.InternalProtocolAdapterMetricsService;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import com.hivemq.protocols.northbound.NorthboundTagConsumer;
import com.hivemq.protocols.v2.config.NorthboundMappingEntity;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Owns the {@link NorthboundTagConsumer}s for one v2 adapter.
 * <p>
 * The v2 config currently carries only a tag/topic pair, so the legacy {@link NorthboundMapping} is built with the
 * same defaults the legacy mapping model uses: QoS 2, timestamp included, tag names and metadata omitted.
 * <p>
 * <b>Construction acquires nothing.</b> A registry starts empty and its owner calls {@link #updateMappings} to wire
 * it — deliberately, so the owner can take responsibility for closing it <i>before</i> the first consumer exists.
 * Building the consumers in the constructor put them beyond the reach of any rollback: a failure on the second
 * mapping left the first one live in the {@link TagManager} while the half-built registry was never returned to
 * anyone who could close it (Sam round 3, finding 4).
 */
public final class NorthboundTagConsumerRegistry implements AutoCloseable {

    private static final int DEFAULT_QOS = QoS.EXACTLY_ONCE.getQosNumber();

    private final @NotNull String adapterId;
    private final @NotNull String protocolId;
    private final @NotNull ProtocolAdapter publishIdentity;
    private final @NotNull TagManager tagManager;
    private final @NotNull NorthboundConsumerFactory consumerFactory;
    private final @NotNull InternalProtocolAdapterMetricsService metricsService;
    private final @NotNull List<NorthboundTagConsumer> consumers = new ArrayList<>();

    public NorthboundTagConsumerRegistry(
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapterInformation information,
            final @NotNull TagManager tagManager,
            final @NotNull NorthboundConsumerFactory consumerFactory,
            final @NotNull InternalProtocolAdapterMetricsService metricsService) {
        this.adapterId = adapterId;
        this.protocolId = information.protocolId();
        this.publishIdentity = new ProtocolAdapterPublishIdentity(adapterId, information);
        this.tagManager = tagManager;
        this.consumerFactory = consumerFactory;
        this.metricsService = metricsService;
    }

    /**
     * Replace the active consumers with the ones the given mappings call for. A failure part-way through leaves the
     * consumers built so far registered here, so {@link #close()} still removes every one of them.
     *
     * @param mappings the adapter's current northbound mappings.
     */
    public void updateMappings(final @NotNull List<NorthboundMappingEntity> mappings) {
        removeConsumers();
        for (final NorthboundMappingEntity mapping : mappings) {
            final NorthboundTagConsumer consumer = consumerFactory.build(
                    adapterId, publishIdentity, protocolId, toNorthboundMapping(mapping), metricsService);
            // Recorded before it is handed to the tag manager: a throw from addConsumer must not leave a consumer
            // this registry cannot remove. removeConsumer on a never-added consumer is a no-op.
            consumers.add(consumer);
            tagManager.addConsumer(consumer);
        }
    }

    @Override
    public void close() {
        removeConsumers();
        metricsService.clearAll();
    }

    private void removeConsumers() {
        consumers.forEach(tagManager::removeConsumer);
        consumers.clear();
    }

    private static @NotNull NorthboundMapping toNorthboundMapping(final @NotNull NorthboundMappingEntity mapping) {
        return new NorthboundMapping(
                mapping.getTagName(), mapping.getTopic(), DEFAULT_QOS, false, true, false, List.of(), null);
    }
}
