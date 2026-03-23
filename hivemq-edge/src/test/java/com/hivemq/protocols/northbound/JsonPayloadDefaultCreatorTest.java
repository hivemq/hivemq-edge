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
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.edge.modules.adapters.data.AbstractProtocolAdapterJsonPayload;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterPublisherJsonPayload;
import com.hivemq.edge.modules.adapters.data.TagSample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonPayloadDefaultCreatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonPayloadDefaultCreator creator;

    @Mock
    private PollingContext pollingContext;

    @BeforeEach
    void setUp() {
        creator = new JsonPayloadDefaultCreator();
    }

    @Test
    void convertToJson_withTimestampAndTagName_producesExpectedJson() throws Exception {
        final DataPoint dataPoint = createDataPoint("temperature", 42.5);
        when(pollingContext.getIncludeTimestamp()).thenReturn(true);
        when(pollingContext.getIncludeTagNames()).thenReturn(true);
        when(pollingContext.getUserProperties()).thenReturn(List.of());

        final List<byte[]> result = creator.convertToJson(List.of(dataPoint), pollingContext, objectMapper);

        assertThat(result).hasSize(1);
        final JsonNode json = objectMapper.readTree(result.getFirst());
        assertThat(json.has("timestamp")).isTrue();
        assertThat(json.get("timestamp").asLong()).isPositive();
        assertThat(json.get("value").asDouble()).isEqualTo(42.5);
        assertThat(json.get("tagName").asText()).isEqualTo("temperature");
    }

    @Test
    void convertToJson_withoutTimestamp_noTimestampInJson() throws Exception {
        final DataPoint dataPoint = createDataPoint("sensor", "on");
        when(pollingContext.getIncludeTimestamp()).thenReturn(false);
        when(pollingContext.getIncludeTagNames()).thenReturn(true);
        when(pollingContext.getUserProperties()).thenReturn(List.of());

        final List<byte[]> result = creator.convertToJson(List.of(dataPoint), pollingContext, objectMapper);

        assertThat(result).hasSize(1);
        final JsonNode json = objectMapper.readTree(result.getFirst());
        assertThat(json.has("timestamp")).isFalse();
        assertThat(json.get("value").asText()).isEqualTo("on");
        assertThat(json.get("tagName").asText()).isEqualTo("sensor");
    }

    @Test
    void convertToJson_withoutTagNames_noTagNameInJson() throws Exception {
        final DataPoint dataPoint = createDataPoint("pressure", 1013);
        when(pollingContext.getIncludeTimestamp()).thenReturn(false);
        when(pollingContext.getIncludeTagNames()).thenReturn(false);
        when(pollingContext.getUserProperties()).thenReturn(List.of());

        final List<byte[]> result = creator.convertToJson(List.of(dataPoint), pollingContext, objectMapper);

        assertThat(result).hasSize(1);
        final JsonNode json = objectMapper.readTree(result.getFirst());
        assertThat(json.has("tagName")).isFalse();
        assertThat(json.get("value").asInt()).isEqualTo(1013);
    }

    @Test
    void convertToJson_multipleDataPoints_producesMultiplePayloads() throws Exception {
        final DataPoint dp1 = createDataPoint("temp", 20);
        final DataPoint dp2 = createDataPoint("humidity", 65);
        when(pollingContext.getIncludeTimestamp()).thenReturn(false);
        when(pollingContext.getIncludeTagNames()).thenReturn(true);
        when(pollingContext.getUserProperties()).thenReturn(List.of());

        final List<byte[]> result = creator.convertToJson(List.of(dp1, dp2), pollingContext, objectMapper);

        assertThat(result).hasSize(2);
        final JsonNode json0 = objectMapper.readTree(result.get(0));
        final JsonNode json1 = objectMapper.readTree(result.get(1));
        assertThat(json0.get("tagName").asText()).isEqualTo("temp");
        assertThat(json1.get("tagName").asText()).isEqualTo("humidity");
    }

    @Test
    void convertToJson_emptyDataPoints_producesEmptyList() {
        when(pollingContext.getIncludeTimestamp()).thenReturn(false);

        final List<byte[]> result = creator.convertToJson(List.of(), pollingContext, objectMapper);

        assertThat(result).isEmpty();
    }

    @Test
    void convertToJson_withUserProperties_includesUserProperties() throws Exception {
        final DataPoint dataPoint = createDataPoint("tag1", "value1");
        final MqttUserProperty userProperty = new MqttUserProperty("key", "val");
        when(pollingContext.getIncludeTimestamp()).thenReturn(false);
        when(pollingContext.getIncludeTagNames()).thenReturn(false);
        when(pollingContext.getUserProperties()).thenReturn(List.of(userProperty));

        final List<byte[]> result = creator.convertToJson(List.of(dataPoint), pollingContext, objectMapper);

        assertThat(result).hasSize(1);
        final JsonNode json = objectMapper.readTree(result.getFirst());
        assertThat(json.has("mqttUserProperties")).isTrue();
        assertThat(json.get("mqttUserProperties")).hasSize(1);
        assertThat(json.get("mqttUserProperties").get(0).get("name").asText()).isEqualTo("key");
        assertThat(json.get("mqttUserProperties").get(0).get("value").asText()).isEqualTo("val");
    }

    @Test
    void convertToJson_withEmptyUserProperties_noUserPropertiesInJson() throws Exception {
        final DataPoint dataPoint = createDataPoint("tag1", "value1");
        when(pollingContext.getIncludeTimestamp()).thenReturn(false);
        when(pollingContext.getIncludeTagNames()).thenReturn(false);
        when(pollingContext.getUserProperties()).thenReturn(List.of());

        final List<byte[]> result = creator.convertToJson(List.of(dataPoint), pollingContext, objectMapper);

        assertThat(result).hasSize(1);
        final JsonNode json = objectMapper.readTree(result.getFirst());
        assertThat(json.has("mqttUserProperties")).isFalse();
    }

    @Test
    void createTagSample_withIncludeTagName_setsTagName() {
        final DataPoint dataPoint = createDataPoint("myTag", 123);

        final TagSample sample = JsonPayloadDefaultCreator.createTagSample(dataPoint, true);

        assertThat(sample.getTagName()).isEqualTo("myTag");
        assertThat(sample.getTagValue()).isEqualTo(123);
    }

    @Test
    void createTagSample_withoutIncludeTagName_tagNameIsNull() {
        final DataPoint dataPoint = createDataPoint("myTag", 123);

        final TagSample sample = JsonPayloadDefaultCreator.createTagSample(dataPoint, false);

        assertThat(sample.getTagName()).isNull();
        assertThat(sample.getTagValue()).isEqualTo(123);
    }

    @Test
    void convertAdapterSampleToPublishes_withTimestamp_payloadsHaveTimestamp() {
        final DataPoint dataPoint = createDataPoint("tag", "val");
        when(pollingContext.getIncludeTimestamp()).thenReturn(true);
        when(pollingContext.getIncludeTagNames()).thenReturn(false);
        when(pollingContext.getUserProperties()).thenReturn(List.of());

        final List<AbstractProtocolAdapterJsonPayload> payloads =
                creator.convertAdapterSampleToPublishes(List.of(dataPoint), pollingContext);

        assertThat(payloads).hasSize(1);
        assertThat(payloads.getFirst().getTimestamp()).isNotNull().isPositive();
    }

    @Test
    void convertAdapterSampleToPublishes_withoutTimestamp_payloadsHaveNullTimestamp() {
        final DataPoint dataPoint = createDataPoint("tag", "val");
        when(pollingContext.getIncludeTimestamp()).thenReturn(false);
        when(pollingContext.getIncludeTagNames()).thenReturn(false);
        when(pollingContext.getUserProperties()).thenReturn(List.of());

        final List<AbstractProtocolAdapterJsonPayload> payloads =
                creator.convertAdapterSampleToPublishes(List.of(dataPoint), pollingContext);

        assertThat(payloads).hasSize(1);
        assertThat(payloads.getFirst().getTimestamp()).isNull();
    }

    @Test
    void decoratePayloadMessage_withUserProperties_setsUserProperties() {
        final MqttUserProperty prop = new MqttUserProperty("propName", "propValue");
        when(pollingContext.getUserProperties()).thenReturn(List.of(prop));

        final ProtocolAdapterPublisherJsonPayload payload =
                new ProtocolAdapterPublisherJsonPayload(null, new TagSample(null, "v"));

        final AbstractProtocolAdapterJsonPayload result = creator.decoratePayloadMessage(payload, pollingContext);

        assertThat(result.getMqttUserProperties()).containsExactly(prop);
    }

    @Test
    void decoratePayloadMessage_withEmptyUserProperties_doesNotSetUserProperties() {
        when(pollingContext.getUserProperties()).thenReturn(List.of());

        final ProtocolAdapterPublisherJsonPayload payload =
                new ProtocolAdapterPublisherJsonPayload(null, new TagSample(null, "v"));

        final AbstractProtocolAdapterJsonPayload result = creator.decoratePayloadMessage(payload, pollingContext);

        assertThat(result.getMqttUserProperties()).isNull();
    }

    @Test
    void convertToJson_singlePayload_serializesCorrectly() throws Exception {
        final ProtocolAdapterPublisherJsonPayload payload =
                new ProtocolAdapterPublisherJsonPayload(1234L, new TagSample("tag", "hello"));

        final byte[] bytes = creator.convertToJson(payload, objectMapper);

        final JsonNode json = objectMapper.readTree(bytes);
        assertThat(json.get("timestamp").asLong()).isEqualTo(1234L);
        assertThat(json.get("tagName").asText()).isEqualTo("tag");
        assertThat(json.get("value").asText()).isEqualTo("hello");
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
