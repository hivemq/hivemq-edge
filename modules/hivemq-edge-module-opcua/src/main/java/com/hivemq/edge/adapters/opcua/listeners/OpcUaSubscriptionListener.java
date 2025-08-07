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
package com.hivemq.edge.adapters.opcua.listeners;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.Constants;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;

public class OpcUaSubscriptionListener implements OpcUaSubscription.SubscriptionListener {

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull ProtocolAdapterTagStreamingService tagStreamingService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    final Map<OpcuaTag, Boolean> tagToFirstSeen = new ConcurrentHashMap<>();
    private final @NotNull Map<NodeId, OpcuaTag> nodeIdToTag;
    private final @NotNull OpcUaClient client;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull Gson gson;

    public OpcUaSubscriptionListener(
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull ProtocolAdapterTagStreamingService tagStreamingService,
            final @NotNull EventService eventService,
            final @NotNull String adapterId,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull OpcUaClient client,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull Gson gson) {
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.tagStreamingService = tagStreamingService;
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.client = client;
        this.dataPointFactory = dataPointFactory;
        this.gson = gson;
        nodeIdToTag = tags.stream()
                .collect(Collectors.toMap(tag -> NodeId.parse(tag.getDefinition().getNode()), Function.identity()));
    }

    @Override
    public void onKeepAliveReceived(final @NotNull OpcUaSubscription subscription) {
        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_KEEPALIVE_COUNT);
        OpcUaSubscription.SubscriptionListener.super.onKeepAliveReceived(subscription);
    }

    @Override
    public void onTransferFailed(
            final @NotNull OpcUaSubscription subscription,
            final @NotNull StatusCode status) {
        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_TRANSFER_FAILED_COUNT);
        OpcUaSubscription.SubscriptionListener.super.onTransferFailed(subscription, status);
    }

    @Override
    public void onDataReceived(
            final @NotNull OpcUaSubscription subscription,
            final @NotNull List<OpcUaMonitoredItem> items,
            final @NotNull List<DataValue> values) {
        for (int i = 0; i < items.size(); i++) {
            final var tag = nodeIdToTag.get(items.get(i).getReadValueId().getNodeId());
            final String tn = tag.getName();
            if (null == tagToFirstSeen.putIfAbsent(tag, true)) {
                eventService.createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                        .withSeverity(Event.SEVERITY.INFO)
                        .withMessage(String.format("Adapter '%s' took first sample for tag '%s'",
                                adapterId,
                                tn))
                        .fire();
            }
            try {
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_RECEIVED_COUNT);
                final var currentValue = values.get(i);
                final String payload = extractPayload(client, currentValue, gson, tag.getDefinition().isCollectAllProperties());
                tagStreamingService.feed(tn, List.of(dataPointFactory.createJsonDataPoint(tn, payload)));
            } catch (final Throwable e) {
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_ERROR_COUNT);
                throw new RuntimeException(e);
            }
        }
    }

    private static @NotNull String extractPayload(
            final @NotNull OpcUaClient client,
            final @NotNull DataValue value,
            final @NotNull Gson gson,
            final boolean collectAllProperties) throws UaException {

        if (value.getValue().getValue() == null) {
            return "";
        }
        final var jsonObject = OpcUaToJsonConverter.convertPayload(client.getDynamicEncodingContext(), value, gson);

        return jsonObject
                .map(json -> {
                    final var ret = new JsonObject();
                    ret.add("value", json);
                    if(collectAllProperties) {
                        if(value.getServerPicoseconds() != null) {
                            ret.addProperty("serverPicoseconds", value.getServerPicoseconds().longValue());
                        } else {
                            ret.addProperty("serverPicoseconds", 0);
                        }
                        if(value.getSourcePicoseconds() != null) {
                            ret.addProperty("sourcePicoseconds", value.getSourcePicoseconds().longValue());
                        } else {
                            ret.addProperty("sourcePicoseconds", 0);
                        }
                        if(value.getSourceTime() != null) {
                            ret.addProperty("sourceTime", value.getSourceTime().getUtcTime());
                        } else {
                            ret.addProperty("sourceTime", 0);
                        }
                        if(value.getServerTime() != null) {
                            ret.addProperty("serverTime", value.getServerTime().getUtcTime());
                        } else {
                            ret.addProperty("serverTime", 0);
                        }
                    }
                    return ret;
                })
                .map(gson::toJson)
                .orElseGet(() -> gson.toJson(new JsonObject()));

    }
}
