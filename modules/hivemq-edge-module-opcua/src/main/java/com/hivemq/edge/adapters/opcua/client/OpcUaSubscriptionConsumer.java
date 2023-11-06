/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.client;

import com.hivemq.edge.adapters.opcua.OpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.OpcUaException;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsHelper;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaSubscriptionConsumer implements Consumer<UaSubscription> {
    private static final Logger log = LoggerFactory.getLogger(OpcUaSubscriptionConsumer.class);

    private final @NotNull OpcUaAdapterConfig.@NotNull Subscription subscription;
    private final @NotNull ReadValueId readValueId;
    private final @NotNull ProtocolAdapterPublishService adapterPublishService;
    private final @NotNull EventService eventService;
    private final @NotNull CompletableFuture<Void> resultFuture;
    private final @NotNull OpcUaClient opcUaClient;
    private final @NotNull Map<UInteger, OpcUaAdapterConfig.Subscription> subscriptionMap;
    private final @NotNull ProtocolAdapterMetricsHelper metricsHelper;
    private final @NotNull String adapterId;

    public OpcUaSubscriptionConsumer(
            final @NotNull OpcUaAdapterConfig.Subscription subscription,
            final @NotNull ReadValueId readValueId,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @Nullable EventService eventService,
            final @NotNull CompletableFuture<Void> resultFuture,
            final @NotNull OpcUaClient opcUaClient,
            final @NotNull Map<UInteger, OpcUaAdapterConfig.Subscription> subscriptionMap,
            final @NotNull ProtocolAdapterMetricsHelper metricsHelper,
            final @NotNull String adapterId) {
        this.subscription = subscription;
        this.readValueId = readValueId;
        this.adapterPublishService = adapterPublishService;
        this.eventService = eventService;
        this.resultFuture = resultFuture;
        this.opcUaClient = opcUaClient;
        this.subscriptionMap = subscriptionMap;
        this.metricsHelper = metricsHelper;
        this.adapterId = adapterId;
    }

    @Override
    public void accept(final UaSubscription uaSubscription) {

        subscriptionMap.put(uaSubscription.getSubscriptionId(), subscription);
        // create a new client handle, these have to be unique for each handle.
        UInteger clientHandle = uaSubscription.nextClientHandle();

        MonitoringParameters parameters = new MonitoringParameters(clientHandle,
                (double) subscription.getPublishingInterval(),
                null,
                uint(subscription.getServerQueueSize()),
                true);

        MonitoredItemCreateRequest request =
                new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);

        UaSubscription.ItemCreationCallback onItemCreated =
                (item, id) -> item.setValueConsumer(new OpcUaDataValueConsumer(subscription,
                        adapterPublishService,
                        opcUaClient,
                        readValueId.getNodeId(),
                        metricsHelper,
                        adapterId,
                        eventService));

        uaSubscription.createMonitoredItems(TimestampsToReturn.Both, List.of(request), onItemCreated)
                .thenAccept(items -> {
                    for (UaMonitoredItem item : items) {
                        if (item.getStatusCode().isGood()) {
                            if (log.isDebugEnabled()) {
                                log.debug("OPC-UA subscription created for nodeId={}",
                                        item.getReadValueId().getNodeId());
                            }
                        } else {
                            log.warn("OPC-UA subscription failed for nodeId={} (status={})",
                                    item.getReadValueId().getNodeId(),
                                    item.getStatusCode());
                            throw new OpcUaException("OPC-UA subscription failed for nodeId `" +
                                    item.getReadValueId().getNodeId() +
                                    "` (status '" +
                                    item.getStatusCode() +
                                    "')");
                        }
                    }
                    resultFuture.complete(null);
                })
                .exceptionally(monitorThrowable -> {
                    resultFuture.completeExceptionally(monitorThrowable);
                    return null;
                });
    }
}
