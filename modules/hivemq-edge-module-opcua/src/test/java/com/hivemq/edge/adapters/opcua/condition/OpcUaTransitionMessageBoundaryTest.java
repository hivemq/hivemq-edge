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
package com.hivemq.edge.adapters.opcua.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterPublishBuilder;
import com.hivemq.adapter.sdk.api.ProtocolPublishResult;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaSubscriptionLifecycleHandler;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterTagStreamingServiceImpl;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishServiceImpl;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import com.hivemq.protocols.northbound.NorthboundTagConsumer;
import com.hivemq.protocols.northbound.SingleTagConsumer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Review-07 finding 6: a batch of condition transitions is still one MQTT publication per transition.
 * <p>
 * The OPC UA callback deliberately builds one list for everything Milo delivered in a publishing cycle.
 * That is compatible with the external contract only because the production streaming service hands the
 * list to {@link TagManager}, which invokes each {@link SingleTagConsumer} once per data point, and the real
 * northbound consumer sends one MQTT message per invocation.
 * <p>
 * This crosses every one of those boundaries. A flat payload capture could prove only that the values
 * survived; it could not distinguish three independent sends from one combined MQTT message.
 */
class OpcUaTransitionMessageBoundaryTest {

    private static final @NotNull String ADAPTER_ID = "test-adapter-id";
    private static final @NotNull String TAG_NAME = "boiler-high-temp";
    private static final @NotNull String MQTT_TOPIC = "alarms/boiler/high-temperature";

    @Test
    void severalTransitionsFromOneServerCallbackBecomeSeparateMqttPublishesInOrder() throws Exception {
        final OpcuaTag tag = conditionTag();
        final TagManager tagManager = new TagManager();
        final var streamingService = new ProtocolAdapterTagStreamingServiceImpl(ADAPTER_ID, tagManager, ignored -> {});

        final List<String> consumerInvocations = new ArrayList<>();
        tagManager.addConsumer(new RecordingConsumer(consumerInvocations));

        final ObjectMapper objectMapper = new ObjectMapper();
        final ProtocolAdapterWrapper protocolAdapter = mock(ProtocolAdapterWrapper.class);
        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        when(protocolAdapter.getId()).thenReturn(ADAPTER_ID);
        when(protocolAdapter.getAdapter()).thenReturn(mock(ProtocolAdapter.class));
        when(protocolAdapter.getAdapterInformation()).thenReturn(adapterInformation);
        when(adapterInformation.getProtocolId()).thenReturn("opcua");

        final ProtocolAdapterPublishServiceImpl publishService = mock(ProtocolAdapterPublishServiceImpl.class);
        final ProtocolAdapterPublishBuilder publishBuilder = mock(ProtocolAdapterPublishBuilder.class);
        when(publishService.createPublish()).thenReturn(publishBuilder);
        when(publishBuilder.withTopic(anyString())).thenReturn(publishBuilder);
        when(publishBuilder.withQoS(anyInt())).thenReturn(publishBuilder);
        when(publishBuilder.withPayload(any(byte[].class))).thenReturn(publishBuilder);
        when(publishBuilder.withAdapter(any())).thenReturn(publishBuilder);
        // Left outstanding so the first-publication event is irrelevant to this synchronous boundary test.
        when(publishBuilder.send()).thenReturn(new CompletableFuture<ProtocolPublishResult>());

        final NorthboundMapping mapping =
                new NorthboundMapping(TAG_NAME, MQTT_TOPIC, 1, false, false, false, List.of(), null);
        tagManager.addConsumer(new NorthboundTagConsumer(
                mapping,
                protocolAdapter,
                objectMapper,
                publishService,
                mock(ProtocolAdapterMetricsService.class),
                mock(EventService.class)));

        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.getDynamicEncodingContext()).thenReturn(DefaultEncodingContext.INSTANCE);
        final var handler = new OpcUaSubscriptionLifecycleHandler(
                mock(ProtocolAdapterMetricsService.class),
                streamingService,
                new FakeEventService(),
                ADAPTER_ID,
                List.of(tag),
                client,
                config());

        final OpcUaMonitoredItem item = itemFor(tag);
        final List<String> transitions = List.of("active", "acknowledged", "inactive");
        try {
            // One Milo callback is one server publishing cycle and one DataPointListBuilder.publish(). All
            // three transitions therefore enter TagManager together, in this order.
            handler.onEventReceived(
                    mock(OpcUaSubscription.class),
                    List.of(item, item, item),
                    List.of(
                            transition("event-1", transitions.get(0)),
                            transition("event-2", transitions.get(1)),
                            transition("event-3", transitions.get(2))));
        } finally {
            handler.abandon();
        }

        assertThat(consumerInvocations)
                .as("TagManager must invoke a tag consumer once per transition and preserve callback order")
                .containsExactlyElementsOf(transitions);
        verify(publishService, times(transitions.size())).createPublish();
        verify(publishBuilder, times(transitions.size())).withTopic(MQTT_TOPIC);
        verify(publishBuilder, times(transitions.size())).send();

        final ArgumentCaptor<byte[]> payloads = ArgumentCaptor.forClass(byte[].class);
        verify(publishBuilder, times(transitions.size())).withPayload(payloads.capture());
        assertThat(payloads.getAllValues().stream()
                        .map(bytes -> messageFromMqttPayload(objectMapper, bytes))
                        .toList())
                .as("each consumer invocation must become its own MQTT payload, in transition order")
                .containsExactlyElementsOf(transitions);
    }

    private static @NotNull OpcuaTag conditionTag() {
        return new OpcuaTag(
                TAG_NAME,
                "a condition tag",
                new OpcuaTagDefinition(
                        "ns=2;s=Boiler1.HighTemp", OpcuaTagKind.CONDITION, OpcuaConditionType.ALARM_CONDITION));
    }

    private static @NotNull OpcUaMonitoredItem itemFor(final @NotNull OpcuaTag tag) {
        final OpcUaMonitoredItem item = mock(OpcUaMonitoredItem.class);
        when(item.getUserObject()).thenReturn(Optional.of(tag));
        return item;
    }

    /** One ordinary alarm transition, with the payload field that makes order directly observable. */
    private static Variant @NotNull [] transition(final @NotNull String eventId, final @NotNull String message) {
        final Variant[] fields =
                new Variant[OpcuaConditionType.ALARM_CONDITION.selectedFields().size()];
        fields[OpcuaConditionType.BASE_EVENT_FIELDS.indexOf("EventId")] =
                Variant.of(new ByteString(eventId.getBytes(StandardCharsets.UTF_8)));
        fields[OpcuaConditionType.BASE_EVENT_FIELDS.indexOf("EventType")] = Variant.of(NodeIds.AlarmConditionType);
        fields[OpcuaConditionType.BASE_EVENT_FIELDS.indexOf("Message")] = Variant.of(LocalizedText.english(message));
        return fields;
    }

    private static @NotNull String messageFrom(final @NotNull DataPoint dataPoint) {
        return ((JsonNode) dataPoint.getTagValue()).path("Message").path("text").asText();
    }

    private static @NotNull String messageFromMqttPayload(
            final @NotNull ObjectMapper objectMapper, final byte @NotNull [] payload) {
        try {
            return objectMapper
                    .readTree(payload)
                    .path("value")
                    .path("Message")
                    .path("text")
                    .asText();
        } catch (final Exception e) {
            throw new AssertionError("the real northbound consumer produced invalid JSON", e);
        }
    }

    private static @NotNull OpcUaSpecificAdapterConfig config() {
        return new OpcUaSpecificAdapterConfig(
                "opc.tcp://localhost:4840",
                false,
                null,
                null,
                null,
                OpcUaToMqttConfig.defaultOpcUaToMqttConfig(),
                null,
                ConnectionOptions.defaultConnectionOptions());
    }

    private static final class RecordingConsumer implements SingleTagConsumer {

        private final @NotNull List<String> invocations;

        private RecordingConsumer(final @NotNull List<String> invocations) {
            this.invocations = invocations;
        }

        @Override
        public @NotNull String getTagName() {
            return TAG_NAME;
        }

        @Override
        public @Nullable String getScope() {
            return ADAPTER_ID;
        }

        @Override
        public void accept(final @NotNull DataPoint dataPoint) {
            invocations.add(messageFrom(dataPoint));
        }
    }
}
