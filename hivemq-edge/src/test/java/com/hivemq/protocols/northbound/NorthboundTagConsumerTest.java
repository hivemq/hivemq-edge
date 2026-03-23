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
package com.hivemq.protocols.northbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterPublishBuilder;
import com.hivemq.adapter.sdk.api.ProtocolPublishResult;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.EventBuilder;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishServiceImpl;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NorthboundTagConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NorthboundMapping pollingContext;
    @Mock
    private ProtocolAdapterWrapper protocolAdapter;
    @Mock
    private ProtocolAdapterPublishServiceImpl publishService;
    @Mock
    private ProtocolAdapterMetricsService metricsService;
    @Mock
    private EventService eventService;
    @Mock
    private ProtocolAdapterPublishBuilder publishBuilder;
    @Mock
    private ProtocolAdapter adapter;
    @Mock
    private ProtocolAdapterInformation adapterInformation;
    @Mock
    private EventBuilder eventBuilder;

    private NorthboundTagConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NorthboundTagConsumer(
                pollingContext,
                protocolAdapter,
                objectMapper,
                publishService,
                metricsService,
                eventService);
    }

    private void setupPollingContext(final String topic, final int qos) {
        when(pollingContext.getMqttTopic()).thenReturn(topic);
        when(pollingContext.getMqttQos()).thenReturn(qos);
    }

    private void setupPublishBuilder() {
        when(publishService.createPublish()).thenReturn(publishBuilder);
        when(publishBuilder.withTopic(anyString())).thenReturn(publishBuilder);
        when(publishBuilder.withQoS(anyInt())).thenReturn(publishBuilder);
        when(publishBuilder.withPayload(any(byte[].class))).thenReturn(publishBuilder);
        when(publishBuilder.withAdapter(any())).thenReturn(publishBuilder);
        when(publishBuilder.send()).thenReturn(CompletableFuture.completedFuture(ProtocolPublishResult.DELIVERED));
        when(protocolAdapter.getAdapter()).thenReturn(adapter);
    }

    private void setupEventBuilder() {
        when(protocolAdapter.getId()).thenReturn("adapter-1");
        when(protocolAdapter.getAdapterInformation()).thenReturn(adapterInformation);
        when(adapterInformation.getProtocolId()).thenReturn("modbus");
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);
        when(eventBuilder.withSeverity(any())).thenReturn(eventBuilder);
        when(eventBuilder.withTimestamp(any())).thenReturn(eventBuilder);
        when(eventBuilder.withMessage(anyString())).thenReturn(eventBuilder);
        when(eventBuilder.withPayload(any(Payload.ContentType.class), anyString())).thenReturn(eventBuilder);
    }

    @Test
    void getTagName_returnsPollingContextTagName() {
        when(pollingContext.getTagName()).thenReturn("my-tag");

        assertThat(consumer.getTagName()).isEqualTo("my-tag");
    }

    @Test
    void accept_withSimpleDataPoint_publishesJsonWithValueAndTagName() throws Exception {
        setupPollingContext("test/topic", 1);
        setupPublishBuilder();
        setupEventBuilder();

        final DataPoint dataPoint = createDataPoint("sensor1", 42);

        consumer.accept(dataPoint);

        final ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(publishBuilder).withPayload(payloadCaptor.capture());
        verify(publishBuilder).withTopic("test/topic");
        verify(publishBuilder).withQoS(1);
        verify(publishBuilder).send();

        final JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(json.get("value").asInt()).isEqualTo(42);
        assertThat(json.get("tagName").asText()).isEqualTo("sensor1");
    }

    @Test
    void accept_withStringValue_publishesStringValue() throws Exception {
        setupPollingContext("test/topic", 0);
        setupPublishBuilder();
        setupEventBuilder();

        final DataPoint dataPoint = createDataPoint("status", "active");

        consumer.accept(dataPoint);

        final ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(publishBuilder).withPayload(payloadCaptor.capture());

        final JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(json.get("value").asText()).isEqualTo("active");
        assertThat(json.get("tagName").asText()).isEqualTo("status");
    }

    @Test
    void accept_withDataPointWithMetadata_usesJsonNodeValueAndTimestamp() throws Exception {
        setupPollingContext("test/topic", 0);
        setupPublishBuilder();
        setupEventBuilder();

        final ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("tagName", "rich-tag");
        root.put("value", 99.9);
        root.put("timestamp", 1234567890L);
        final DataPointWithMetadata dpMeta = new DataPointWithMetadata(root);

        consumer.accept(dpMeta);

        final ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(publishBuilder).withPayload(payloadCaptor.capture());

        final JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(json.get("value").asDouble()).isEqualTo(99.9);
        assertThat(json.get("timestamp").asLong()).isEqualTo(1234567890L);
        assertThat(json.get("tagName").asText()).isEqualTo("rich-tag");
    }

    @Test
    void accept_withJsonEncodedValue_parsesJsonString() throws Exception {
        setupPollingContext("test/topic", 0);
        setupPublishBuilder();
        setupEventBuilder();

        final DataPoint dataPoint = new DataPoint() {
            @Override
            public @org.jetbrains.annotations.NotNull Object getTagValue() {
                return "{\"nested\":true}";
            }

            @Override
            public boolean treatTagValueAsJson() {
                return true;
            }

            @Override
            public @org.jetbrains.annotations.NotNull String getTagName() {
                return "json-tag";
            }
        };

        consumer.accept(dataPoint);

        final ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(publishBuilder).withPayload(payloadCaptor.capture());

        final JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(json.get("value").get("nested").asBoolean()).isTrue();
        assertThat(json.get("tagName").asText()).isEqualTo("json-tag");
    }

    @Test
    void accept_onPublishSuccess_incrementsSuccessMetrics() {
        setupPollingContext("test/topic", 0);
        setupPublishBuilder();
        setupEventBuilder();

        consumer.accept(createDataPoint("tag", 1));

        verify(metricsService).incrementReadPublishSuccess();
        verify(metricsService, never()).incrementReadPublishFailure();
    }

    @Test
    void accept_onPublishFailure_incrementsFailureMetrics() {
        setupPollingContext("test/topic", 0);
        when(publishService.createPublish()).thenReturn(publishBuilder);
        when(publishBuilder.withTopic(anyString())).thenReturn(publishBuilder);
        when(publishBuilder.withQoS(anyInt())).thenReturn(publishBuilder);
        when(publishBuilder.withPayload(any(byte[].class))).thenReturn(publishBuilder);
        when(publishBuilder.withAdapter(any())).thenReturn(publishBuilder);
        when(publishBuilder.send()).thenReturn(CompletableFuture.failedFuture(new RuntimeException("publish failed")));
        when(protocolAdapter.getAdapter()).thenReturn(adapter);

        consumer.accept(createDataPoint("tag", 1));

        verify(metricsService).incrementReadPublishFailure();
        verify(metricsService, never()).incrementReadPublishSuccess();
    }

    @Test
    void accept_firstPublishSuccess_firesEvent() {
        setupPollingContext("test/topic", 0);
        setupPublishBuilder();
        setupEventBuilder();

        consumer.accept(createDataPoint("tag", 1));

        verify(eventService).createAdapterEvent("adapter-1", "modbus");
        verify(eventBuilder).fire();
    }

    @Test
    void accept_secondPublishSuccess_doesNotFireEventAgain() {
        setupPollingContext("test/topic", 0);
        setupPublishBuilder();
        setupEventBuilder();

        consumer.accept(createDataPoint("tag", 1));
        consumer.accept(createDataPoint("tag", 2));

        // event is fired only once (on the first publish)
        verify(eventBuilder).fire();
    }

    @Test
    void accept_invalidQos_throwsIllegalArgumentException() {
        when(pollingContext.getMqttTopic()).thenReturn("test/topic");
        when(pollingContext.getMqttQos()).thenReturn(3);

        assertThatThrownBy(() -> consumer.accept(createDataPoint("tag", 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accept_negativeQos_throwsIllegalArgumentException() {
        when(pollingContext.getMqttTopic()).thenReturn("test/topic");
        when(pollingContext.getMqttQos()).thenReturn(-1);

        assertThatThrownBy(() -> consumer.accept(createDataPoint("tag", 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accept_nullDataPoint_throwsNullPointerException() {
        assertThatThrownBy(() -> consumer.accept(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void accept_withInvalidJsonString_doesNotThrow() {
        setupPollingContext("test/topic", 0);

        final DataPoint dataPoint = new DataPoint() {
            @Override
            public @org.jetbrains.annotations.NotNull Object getTagValue() {
                return "not valid json{{{";
            }

            @Override
            public boolean treatTagValueAsJson() {
                return true;
            }

            @Override
            public @org.jetbrains.annotations.NotNull String getTagName() {
                return "bad-json-tag";
            }
        };

        // The exception is caught internally and logged
        consumer.accept(dataPoint);

        // No publish should happen since the exception is caught before sending
        verify(publishService, never()).createPublish();
    }

    private static DataPoint createDataPoint(final String tagName, final Object tagValue) {
        return new DataPoint() {
            @Override
            public @org.jetbrains.annotations.NotNull Object getTagValue() {
                return tagValue;
            }

            @Override
            public @org.jetbrains.annotations.NotNull String getTagName() {
                return tagName;
            }
        };
    }
}
