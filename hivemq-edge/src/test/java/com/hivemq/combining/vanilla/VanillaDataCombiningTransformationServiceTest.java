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
package com.hivemq.combining.vanilla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataCombiningDestination;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.common.i18n.StringTemplate;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.services.PrePublishProcessorService;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VanillaDataCombiningTransformationServiceTest {
    private static final UUID DEFAULT_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final String EMPTY_OBJECT = "{}";
    private static final String TOPIC_DESTINATION = "topic/destination";

    @Mock
    private @NotNull PrePublishProcessorService prePublishProcessorService;

    @Mock
    private @NotNull PUBLISH publish;

    @Mock
    private @NotNull DataCombining dataCombining;

    @Mock
    private @NotNull DataCombiningDestination dataCombiningDestination;

    @Mock
    @SuppressWarnings("DoNotMock")
    private @NotNull ListenableFuture<PublishingResult> listenableFuture;

    private @NotNull ArgumentCaptor<PUBLISH> publishCaptor;

    private @NotNull VanillaDataCombiningTransformationService service;

    @BeforeEach
    public void setUp() {
        publishCaptor = ArgumentCaptor.forClass(PUBLISH.class);
        service = new VanillaDataCombiningTransformationService(prePublishProcessorService);
        when(dataCombining.id()).thenReturn(DEFAULT_UUID);
        when(dataCombining.destination()).thenReturn(dataCombiningDestination);
        when(dataCombiningDestination.topic()).thenReturn(TOPIC_DESTINATION);
        when(publish.getHivemqId()).thenReturn("hivemq-id");
        when(publish.getQoS()).thenReturn(QoS.AT_LEAST_ONCE);
        when(publish.getUserProperties()).thenReturn(Mqtt5UserProperties.NO_USER_PROPERTIES);
        when(prePublishProcessorService.publish(any(), any(), any())).thenReturn(listenableFuture);
    }

    @Test
    public void whenPayloadHasZeroLength_thenSkipsPublish() {
        when(publish.getPayload()).thenReturn(new byte[0]);
        assertThat(service.applyMappings(publish, dataCombining).isDone()).isTrue();
        verify(prePublishProcessorService, never()).publish(publishCaptor.capture(), any(), any());
    }

    @Test
    public void whenInstructionsAreEmpty_thenPublishesEmptyObject() {
        when(publish.getPayload()).thenReturn(EMPTY_OBJECT.getBytes());
        when(dataCombining.instructions()).thenReturn(List.of());
        assertThat(service.applyMappings(publish, dataCombining).isDone()).isFalse();
        verify(prePublishProcessorService, times(1)).publish(publishCaptor.capture(), any(), any());
        assertThat(new String(publishCaptor.getValue().getPayload())).isEqualTo(EMPTY_OBJECT);
    }

    @Test
    public void when1FilterMatches_thenPublishPasses() {
        when(publish.getPayload()).thenReturn("""
                {
                  "TOPIC_FILTER:topic/a": {
                    "a": 1
                  }
                }""".getBytes());
        when(dataCombining.instructions())
                .thenReturn(List.of(new Instruction(
                        "$.a",
                        "dest.a",
                        new DataIdentifierReference("topic/a", DataIdentifierReference.Type.TOPIC_FILTER))));
        assertThat(service.applyMappings(publish, dataCombining).isDone()).isFalse();
        verify(prePublishProcessorService, times(1)).publish(publishCaptor.capture(), any(), any());
        assertThat(new String(publishCaptor.getValue().getPayload())).isEqualTo("""
                {"dest":{"a":1}}""");
    }

    @Test
    public void when2FiltersMatch_thenPublishPasses() {
        when(publish.getPayload()).thenReturn("""
                {
                  "TOPIC_FILTER:topic/a": {
                    "a": 1
                  },
                  "TOPIC_FILTER:topic/b": {
                    "b": 2
                  }
                }""".getBytes());
        when(dataCombining.instructions())
                .thenReturn(List.of(
                        new Instruction(
                                "$.a",
                                "dest.a",
                                new DataIdentifierReference("topic/a", DataIdentifierReference.Type.TOPIC_FILTER)),
                        new Instruction(
                                "$.b",
                                "dest.b",
                                new DataIdentifierReference("topic/b", DataIdentifierReference.Type.TOPIC_FILTER))));
        assertThat(service.applyMappings(publish, dataCombining).isDone()).isFalse();
        verify(prePublishProcessorService, times(1)).publish(publishCaptor.capture(), any(), any());
        assertThat(new String(publishCaptor.getValue().getPayload())).isEqualTo("""
                {"dest":{"a":1,"b":2}}""");
    }

    @Test
    public void when1TagMatches_thenPublishPasses() {
        when(publish.getPayload()).thenReturn(EMPTY_OBJECT.getBytes());
        when(dataCombining.instructions())
                .thenReturn(List.of(new Instruction(
                        "$.value",
                        "dest.tag1",
                        new DataIdentifierReference("TAG1", DataIdentifierReference.Type.TAG))));
        assertThat(service.applyMappings(publish, dataCombining).isDone()).isFalse();
        verify(prePublishProcessorService, times(1)).publish(publishCaptor.capture(), any(), any());
        assertThat(new String(publishCaptor.getValue().getPayload())).isEqualTo("""
                {"dest":{"tag1":"TAG1"}}""");
    }

    @Test
    public void when2TagsMatch_thenPublishPasses() {
        when(publish.getPayload()).thenReturn(EMPTY_OBJECT.getBytes());
        when(dataCombining.instructions())
                .thenReturn(List.of(
                        new Instruction(
                                "$.value",
                                "dest.tag1",
                                new DataIdentifierReference("TAG1", DataIdentifierReference.Type.TAG)),
                        new Instruction(
                                "$.value",
                                "dest.tag2",
                                new DataIdentifierReference("TAG2", DataIdentifierReference.Type.TAG))));
        assertThat(service.applyMappings(publish, dataCombining).isDone()).isFalse();
        verify(prePublishProcessorService, times(1)).publish(publishCaptor.capture(), any(), any());
        assertThat(new String(publishCaptor.getValue().getPayload())).isEqualTo("""
                {"dest":{"tag1":"TAG1","tag2":"TAG2"}}""");
    }

    @Test
    public void when1AssetMatches_thenPublishPasses() {
        final String assetId = UUID.randomUUID().toString();
        when(publish.getPayload()).thenReturn(EMPTY_OBJECT.getBytes());
        when(dataCombining.instructions())
                .thenReturn(List.of(new Instruction(
                        "$.value",
                        "dest.asset",
                        new DataIdentifierReference(assetId, DataIdentifierReference.Type.PULSE_ASSET))));
        assertThat(service.applyMappings(publish, dataCombining).isDone()).isFalse();
        verify(prePublishProcessorService, times(1)).publish(publishCaptor.capture(), any(), any());
        assertThat(new String(publishCaptor.getValue().getPayload()))
                .isEqualTo(StringTemplate.format("""
                {"dest":{"asset":"${assetId}"}}""", Map.of("assetId", assetId)));
    }

    @Test
    public void when2FiltersAnd2TagsAnd2AssetsMatch_thenPublishPasses() {
        when(publish.getPayload()).thenReturn("""
                {
                  "TOPIC_FILTER:topic/a": {
                    "a": 1
                  },
                  "TOPIC_FILTER:topic/b": {
                    "b": 2
                  }
                }""".getBytes());
        when(dataCombining.instructions())
                .thenReturn(List.of(
                        new Instruction(
                                "$.a",
                                "dest.a",
                                new DataIdentifierReference("topic/a", DataIdentifierReference.Type.TOPIC_FILTER)),
                        new Instruction(
                                "$.b",
                                "dest.b",
                                new DataIdentifierReference("topic/b", DataIdentifierReference.Type.TOPIC_FILTER)),
                        new Instruction(
                                "$.value",
                                "dest.tag1",
                                new DataIdentifierReference("TAG1", DataIdentifierReference.Type.TAG)),
                        new Instruction(
                                "$.value",
                                "dest.tag2",
                                new DataIdentifierReference("TAG2", DataIdentifierReference.Type.TAG)),
                        new Instruction(
                                "$.value",
                                "dest.asset1",
                                new DataIdentifierReference("ASSET1", DataIdentifierReference.Type.PULSE_ASSET)),
                        new Instruction(
                                "$.value",
                                "dest.asset2",
                                new DataIdentifierReference("ASSET2", DataIdentifierReference.Type.PULSE_ASSET))));
        assertThat(service.applyMappings(publish, dataCombining).isDone()).isFalse();
        verify(prePublishProcessorService, times(1)).publish(publishCaptor.capture(), any(), any());
        assertThat(new String(publishCaptor.getValue().getPayload())).isEqualTo("""
                {"dest":{"a":1,"b":2,"tag1":"TAG1","tag2":"TAG2","asset1":"ASSET1","asset2":"ASSET2"}}""");
    }

    @Test
    public void when2FiltersOverlap_thenLast1WinsAndPublishPasses() {
        when(publish.getPayload()).thenReturn("""
                {
                  "TOPIC_FILTER:topic/a": {
                    "a": 1
                  },
                  "TOPIC_FILTER:topic/b": {
                    "b": 2
                  }
                }""".getBytes());
        when(dataCombining.instructions())
                .thenReturn(List.of(
                        new Instruction(
                                "$.a",
                                "dest.x",
                                new DataIdentifierReference("topic/a", DataIdentifierReference.Type.TOPIC_FILTER)),
                        new Instruction(
                                "$.b",
                                "dest.x",
                                new DataIdentifierReference("topic/b", DataIdentifierReference.Type.TOPIC_FILTER))));
        assertThat(service.applyMappings(publish, dataCombining).isDone()).isFalse();
        verify(prePublishProcessorService, times(1)).publish(publishCaptor.capture(), any(), any());
        assertThat(new String(publishCaptor.getValue().getPayload())).isEqualTo("""
                {"dest":{"x":2}}""");
    }

    @Test
    public void when2TagsOverlap_thenLast1WinsAndPublishPasses() {
        when(publish.getPayload()).thenReturn(EMPTY_OBJECT.getBytes());
        when(dataCombining.instructions())
                .thenReturn(List.of(
                        new Instruction(
                                "$.value",
                                "dest.tag",
                                new DataIdentifierReference("TAG1", DataIdentifierReference.Type.TAG)),
                        new Instruction(
                                "$.value",
                                "dest.tag",
                                new DataIdentifierReference("TAG2", DataIdentifierReference.Type.TAG))));
        assertThat(service.applyMappings(publish, dataCombining).isDone()).isFalse();
        verify(prePublishProcessorService, times(1)).publish(publishCaptor.capture(), any(), any());
        assertThat(new String(publishCaptor.getValue().getPayload())).isEqualTo("""
                {"dest":{"tag":"TAG2"}}""");
    }

    @Test
    public void whenSingleAndDoubleQuotesAreInTopic_thenPublishPasses() {
        when(publish.getPayload()).thenReturn("""
                {
                  "TOPIC_FILTER:topic/single'quote": {
                    "a": 1
                  },
                  "TOPIC_FILTER:topic/double\\"quote": {
                    "b": 2
                  }
                }""".getBytes());
        when(dataCombining.instructions())
                .thenReturn(List.of(
                        new Instruction(
                                "$.a",
                                "dest.a",
                                new DataIdentifierReference(
                                        "topic/single'quote", DataIdentifierReference.Type.TOPIC_FILTER)),
                        new Instruction(
                                "$.b",
                                "dest.b",
                                new DataIdentifierReference(
                                        "topic/double\"quote", DataIdentifierReference.Type.TOPIC_FILTER))));
        assertThat(service.applyMappings(publish, dataCombining).isDone()).isFalse();
        verify(prePublishProcessorService, times(1)).publish(publishCaptor.capture(), any(), any());
        assertThat(new String(publishCaptor.getValue().getPayload())).isEqualTo("""
                {"dest":{"a":1,"b":2}}""");
    }

    @Test
    public void whenDestinationIsFullJsonPath_thenNoDollarIsInTheResult() {
        when(publish.getPayload()).thenReturn("""
                {
                  "TOPIC_FILTER:topic/a": {
                    "a": 1
                  },
                  "TOPIC_FILTER:topic/b": {
                    "b": 2
                  }
                }""".getBytes());
        when(dataCombining.instructions())
                .thenReturn(List.of(
                        new Instruction(
                                "$.a",
                                "$.dest.a",
                                new DataIdentifierReference("topic/a", DataIdentifierReference.Type.TOPIC_FILTER)),
                        new Instruction(
                                "$.b",
                                "$.dest.b",
                                new DataIdentifierReference("topic/b", DataIdentifierReference.Type.TOPIC_FILTER))));
        assertThat(service.applyMappings(publish, dataCombining).isDone()).isFalse();
        verify(prePublishProcessorService, times(1)).publish(publishCaptor.capture(), any(), any());
        assertThat(new String(publishCaptor.getValue().getPayload())).isEqualTo("""
                {"dest":{"a":1,"b":2}}""");
    }
}
