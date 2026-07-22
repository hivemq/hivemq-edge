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
package com.hivemq.protocols.v2.southbound;

import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.persistence.mappings.SouthboundMapping;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.InternalWritingContext;
import com.hivemq.protocols.InternalWritingContextImpl;
import com.hivemq.protocols.v2.config.SouthboundMappingEntity;
import com.hivemq.protocols.v2.northbound.ProtocolAdapterPublishIdentity;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the southbound MQTT&rarr;adapter write wiring for one v2 adapter (EDG-824 #3) — the
 * southbound counterpart of the northbound consumer registry. It subscribes the adapter's southbound mappings
 * through the reused {@link InternalProtocolAdapterWritingService} (the same seam v1 uses; a no-op when the service
 * is unavailable in this edition) with a {@link SouthboundWriteBridge} that forwards each arriving write into the
 * wrapper mailbox.
 * <p>
 * The v2 config carries only the {@code topic}/{@code tag-name} pair, so the legacy {@link SouthboundMapping} is
 * built with the default field mapping and a permissive schema — the tag's declared schema governs the value at the
 * adapter boundary, not here.
 */
public final class SouthboundWriterRegistry implements AutoCloseable {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SouthboundWriterRegistry.class);

    /** Accepts any JSON payload; the write value's shape is the adapter's contract, not the transport's. */
    private static final @NotNull String PERMISSIVE_SCHEMA = "{}";

    private final @NotNull String adapterId;
    private final @NotNull InternalProtocolAdapterWritingService writingService;
    private final @NotNull ProtocolAdapterMetricsService metricsService;
    private final @NotNull SouthboundWriteBridge bridge;

    private @NotNull List<InternalWritingContext> activeContexts = List.of();

    /**
     * @param adapterId        the owning adapter's id.
     * @param information      the owning adapter's v2 type information.
     * @param writingService   the reused writing service that owns the MQTT subscription machinery.
     * @param metricsService   the per-adapter v1 metrics service.
     * @param wrapperSender    the owning wrapper's mailbox sender.
     * @param dataPointFactory the reused v1 factory write values are carried with.
     * @param nodes            the adapter's configured node/tag pairs.
     * @param mappings         the adapter's southbound mappings.
     */
    public SouthboundWriterRegistry(
            final @NotNull String adapterId,
            final @NotNull com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation information,
            final @NotNull InternalProtocolAdapterWritingService writingService,
            final @NotNull ProtocolAdapterMetricsService metricsService,
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull List<NodeTagPair> nodes,
            final @NotNull List<SouthboundMappingEntity> mappings) {
        this.adapterId = adapterId;
        this.writingService = writingService;
        this.metricsService = metricsService;
        this.bridge = new SouthboundWriteBridge(
                new ProtocolAdapterPublishIdentity(adapterId, information),
                wrapperSender,
                dataPointFactory,
                byTagName(nodes));
        updateMappings(mappings, nodes);
    }

    /**
     * Replace the active southbound wiring after a reload: stop the current subscriptions, refresh the bridge's
     * node set, and start writing for the new mappings (none declared &rarr; nothing started).
     *
     * @param mappings the adapter's current southbound mappings.
     * @param nodes    the adapter's current node/tag pairs.
     */
    @SuppressWarnings("FutureReturnValueIgnored") // the start is fire-and-forget; failure is logged in whenComplete
    public void updateMappings(
            final @NotNull List<SouthboundMappingEntity> mappings, final @NotNull List<NodeTagPair> nodes) {
        stopActive();
        bridge.updateNodes(byTagName(nodes));
        if (mappings.isEmpty()) {
            return;
        }
        final List<InternalWritingContext> contexts = mappings.stream()
                .map(mapping -> (InternalWritingContext) new InternalWritingContextImpl(new SouthboundMapping(
                        mapping.getTagName(),
                        mapping.getTopic(),
                        FieldMapping.DEFAULT_FIELD_MAPPING,
                        PERMISSIVE_SCHEMA)))
                .toList();
        activeContexts = contexts;
        writingService.startWritingAsync(bridge, metricsService, contexts).whenComplete((success, error) -> {
            if (error != null) {
                log.error("Failed to start southbound writing for v2 adapter '{}'", adapterId, error);
            } else if (!Boolean.TRUE.equals(success)) {
                log.warn(
                        "Southbound writing for v2 adapter '{}' was not started: the writing service is "
                                + "unavailable in this edition or configuration",
                        adapterId);
            }
        });
    }

    @Override
    public void close() {
        stopActive();
    }

    private void stopActive() {
        if (!activeContexts.isEmpty()) {
            writingService.stopWriting(bridge, activeContexts);
            activeContexts = List.of();
        }
    }

    private static @NotNull Map<String, Node> byTagName(final @NotNull List<NodeTagPair> nodes) {
        final Map<String, Node> byTagName = new HashMap<>();
        for (final NodeTagPair pair : nodes) {
            byTagName.put(pair.tag().name(), pair.node());
        }
        return byTagName;
    }
}
