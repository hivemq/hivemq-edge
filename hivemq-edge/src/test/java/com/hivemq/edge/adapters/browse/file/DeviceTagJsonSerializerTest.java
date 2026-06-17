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
package com.hivemq.edge.adapters.browse.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import com.hivemq.edge.adapters.browse.model.FieldMappingInstruction;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DeviceTagJsonSerializerTest {

    private final DeviceTagJsonSerializer serializer = new DeviceTagJsonSerializer();

    @Test
    void roundTrip_allFields() throws IOException {
        final DeviceTagRow row = DeviceTagRow.builder()
                .nodePath("/Objects/Data/Int32")
                .namespaceUri("urn:test")
                .namespaceIndex(2)
                .nodeId("ns=2;i=100")
                .dataType("Int32")
                .accessLevel("READ_WRITE")
                .nodeDescription("A test node")
                .tagName("my-tag")
                .tagNameDefault("adapter-int32")
                .tagDescription("Tag description")
                .northboundTopic("adapter/data/int32")
                .northboundTopicDefault("adapter/data/int32")
                .southboundTopic("adapter/write/data/int32")
                .southboundTopicDefault("adapter/write/data/int32")
                .southboundFieldMapping(List.of(
                        new FieldMappingInstruction("value", "value"),
                        new FieldMappingInstruction("status", "quality")))
                .maxQos(1)
                .messageExpiryInterval(3600L)
                .includeTimestamp(true)
                .includeTagNames(false)
                .includeMetadata(true)
                .mqttUserProperties(Map.of("key1", "val1"))
                .build();

        final byte[] json = serializer.serialize(List.of(row));
        final List<DeviceTagRow> result = serializer.deserialize(json);

        assertThat(result).hasSize(1);
        final DeviceTagRow deserialized = result.getFirst();
        assertThat(deserialized.getNodePath()).isEqualTo(row.getNodePath());
        assertThat(deserialized.getNamespaceUri()).isEqualTo(row.getNamespaceUri());
        assertThat(deserialized.getNamespaceIndex()).isEqualTo(row.getNamespaceIndex());
        assertThat(deserialized.getNodeId()).isEqualTo(row.getNodeId());
        assertThat(deserialized.getDataType()).isEqualTo(row.getDataType());
        assertThat(deserialized.getAccessLevel()).isEqualTo(row.getAccessLevel());
        assertThat(deserialized.getNodeDescription()).isEqualTo(row.getNodeDescription());
        assertThat(deserialized.getTagName()).isEqualTo(row.getTagName());
        assertThat(deserialized.getTagNameDefault()).isEqualTo(row.getTagNameDefault());
        assertThat(deserialized.getTagDescription()).isEqualTo(row.getTagDescription());
        assertThat(deserialized.getNorthboundTopic()).isEqualTo(row.getNorthboundTopic());
        assertThat(deserialized.getSouthboundTopic()).isEqualTo(row.getSouthboundTopic());
        assertThat(deserialized.getMaxQos()).isEqualTo(1);
        assertThat(deserialized.getMessageExpiryInterval()).isEqualTo(3600L);
        assertThat(deserialized.getIncludeTimestamp()).isTrue();
        assertThat(deserialized.getIncludeTagNames()).isFalse();
        assertThat(deserialized.getIncludeMetadata()).isTrue();
        assertThat(deserialized.getSouthboundFieldMapping()).hasSize(2);
        assertThat(deserialized.getMqttUserProperties()).containsEntry("key1", "val1");
    }

    @Test
    void roundTrip_minimalRow() throws IOException {
        final DeviceTagRow row = DeviceTagRow.builder().nodeId("ns=0;i=1").build();

        final byte[] json = serializer.serialize(List.of(row));
        final List<DeviceTagRow> result = serializer.deserialize(json);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNodeId()).isEqualTo("ns=0;i=1");
        assertThat(result.getFirst().getTagName()).isNull();
        assertThat(result.getFirst().getNorthboundTopic()).isNull();
    }

    @Test
    void roundTrip_emptyList() throws IOException {
        final byte[] json = serializer.serialize(List.of());
        final List<DeviceTagRow> result = serializer.deserialize(json);
        assertThat(result).isEmpty();
    }

    @Test
    void roundTrip_nullSections_handledCorrectly() throws IOException {
        // Row with only node info, no tag/northbound/southbound
        final DeviceTagRow row = DeviceTagRow.builder()
                .nodePath("/Objects/Data")
                .nodeId("ns=0;i=85")
                .dataType("Int32")
                .build();

        final byte[] json = serializer.serialize(List.of(row));
        final String jsonStr = new String(json, StandardCharsets.UTF_8);

        // Verify northbound and southbound sections are absent
        assertThat(jsonStr).doesNotContain("\"northbound\"");
        assertThat(jsonStr).doesNotContain("\"southbound\"");

        final List<DeviceTagRow> result = serializer.deserialize(json);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNorthboundTopic()).isNull();
        assertThat(result.getFirst().getSouthboundTopic()).isNull();
    }

    @Test
    void roundTrip_wildcardPreservation() throws IOException {
        final DeviceTagRow row = DeviceTagRow.builder()
                .nodeId("ns=0;i=1")
                .tagName("*")
                .tagNameDefault("auto-tag")
                .northboundTopic("*")
                .northboundTopicDefault("auto/topic")
                .southboundTopic("*")
                .southboundTopicDefault("auto/write/topic")
                .build();

        final byte[] json = serializer.serialize(List.of(row));
        final List<DeviceTagRow> result = serializer.deserialize(json);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTagName()).isEqualTo("*");
        assertThat(result.getFirst().getNorthboundTopic()).isEqualTo("*");
        assertThat(result.getFirst().getSouthboundTopic()).isEqualTo("*");
    }

    @Test
    void deserialize_unknownFieldsIgnored() throws IOException {
        final String json = """
                {"rows":[{"node":{"node_id":"ns=0;i=1","unknown_field":"ignored"},"tag":{}}]}
                """;
        final List<DeviceTagRow> result = serializer.deserialize(json.getBytes(StandardCharsets.UTF_8));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNodeId()).isEqualTo("ns=0;i=1");
    }

    @Test
    void deserialize_emptyRows() throws IOException {
        final String json = """
                {"rows":[]}
                """;
        final List<DeviceTagRow> result = serializer.deserialize(json.getBytes(StandardCharsets.UTF_8));
        assertThat(result).isEmpty();
    }

    @Test
    void deserialize_nullRows() throws IOException {
        final String json = """
                {"rows":null}
                """;
        final List<DeviceTagRow> result = serializer.deserialize(json.getBytes(StandardCharsets.UTF_8));
        assertThat(result).isEmpty();
    }

    @Test
    void deserialize_malformedJson() {
        assertThatThrownBy(() -> serializer.deserialize("not json".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IOException.class);
    }

    @Test
    void serialize_fieldMappingAsArray() throws IOException {
        final DeviceTagRow row = DeviceTagRow.builder()
                .nodeId("ns=0;i=1")
                .tagName("tag")
                .southboundTopic("topic")
                .southboundFieldMapping(List.of(new FieldMappingInstruction("src", "dst")))
                .build();

        final byte[] json = serializer.serialize(List.of(row));
        final String jsonStr = new String(json, StandardCharsets.UTF_8);

        assertThat(jsonStr).contains("\"field_mapping\"");
        assertThat(jsonStr).contains("\"source\"");
        assertThat(jsonStr).contains("\"destination\"");
    }

    @Test
    void serialize_userPropertiesAsObject() throws IOException {
        final DeviceTagRow row = DeviceTagRow.builder()
                .nodeId("ns=0;i=1")
                .tagName("tag")
                .northboundTopic("topic")
                .mqttUserProperties(Map.of("myKey", "myValue"))
                .build();

        final byte[] json = serializer.serialize(List.of(row));
        final String jsonStr = new String(json, StandardCharsets.UTF_8);
        assertThat(jsonStr).contains("\"mqtt_user_properties\"");
        assertThat(jsonStr).contains("\"myKey\"");
        assertThat(jsonStr).contains("\"myValue\"");
    }

    // --- Streaming serialize ---

    @Test
    void streamingSerialize_producesIdenticalOutput() throws IOException {
        final List<DeviceTagRow> rows = List.of(
                DeviceTagRow.builder()
                        .nodePath("/Objects/Data/Int32")
                        .namespaceUri("urn:test")
                        .namespaceIndex(2)
                        .nodeId("ns=2;i=100")
                        .dataType("Int32")
                        .tagName("my-tag")
                        .northboundTopic("adapter/data/int32")
                        .maxQos(1)
                        .build(),
                DeviceTagRow.builder().nodeId("ns=0;i=1").build());

        final byte[] fromBytes = serializer.serialize(rows);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(rows, baos);

        assertThat(baos.toByteArray()).isEqualTo(fromBytes);
    }

    @Test
    void streamingSerialize_emptyList() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(List.of(), baos);
        final List<DeviceTagRow> result = serializer.deserialize(baos.toByteArray());
        assertThat(result).isEmpty();
    }

    @Test
    void streamingSerialize_roundTrip() throws IOException {
        final DeviceTagRow row = DeviceTagRow.builder()
                .nodePath("/Objects/Data/Int32")
                .nodeId("ns=2;i=100")
                .dataType("Int32")
                .tagName("my-tag")
                .tagNameDefault("adapter-int32")
                .northboundTopic("adapter/data/int32")
                .southboundTopic("adapter/write/data/int32")
                .southboundFieldMapping(List.of(new FieldMappingInstruction("val", "val")))
                .mqttUserProperties(Map.of("key", "value"))
                .build();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(List.of(row), baos);
        final List<DeviceTagRow> result = serializer.deserialize(baos.toByteArray());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNodeId()).isEqualTo("ns=2;i=100");
        assertThat(result.getFirst().getTagName()).isEqualTo("my-tag");
        assertThat(result.getFirst().getSouthboundFieldMapping()).hasSize(1);
        assertThat(result.getFirst().getMqttUserProperties()).containsEntry("key", "value");
    }

    // --- Large payload ---

    @Test
    void roundTrip_largePayload_5000Rows() throws IOException {
        final List<DeviceTagRow> rows = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            rows.add(DeviceTagRow.builder()
                    .nodePath("/Objects/Data/Node" + i)
                    .namespaceUri("urn:test:ns")
                    .namespaceIndex(2)
                    .nodeId("ns=2;i=" + (1000 + i))
                    .dataType("Int32")
                    .tagName("tag-" + i)
                    .northboundTopic("adapter/data/node" + i)
                    .maxQos(1)
                    .build());
        }

        final byte[] json = serializer.serialize(rows);
        final List<DeviceTagRow> result = serializer.deserialize(json);
        assertThat(result).hasSize(5000);
    }

    // --- Concurrency ---

    @Test
    void concurrentSerialization_threadSafe() throws Exception {
        final int threadCount = 10;
        final int rowsPerThread = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);
        final AtomicInteger errors = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadIdx = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    final List<DeviceTagRow> rows = new ArrayList<>();
                    for (int i = 0; i < rowsPerThread; i++) {
                        rows.add(DeviceTagRow.builder()
                                .nodeId("ns=2;i=" + (threadIdx * 1000 + i))
                                .tagName("t" + threadIdx + "-tag-" + i)
                                .build());
                    }
                    final byte[] json = serializer.serialize(rows);
                    final List<DeviceTagRow> result = serializer.deserialize(json);
                    if (result.size() != rowsPerThread) {
                        errors.incrementAndGet();
                    }
                } catch (final Exception e) {
                    errors.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(errors.get()).isZero();
    }

    @Test
    void serialize_producesCompactJson_noPrettyPrint() throws IOException {
        // Compact output halves wire size on large browses; consumers that want indented JSON
        // can pipe through `jq`.
        final DeviceTagRow row = DeviceTagRow.builder()
                .nodeId("ns=2;i=100")
                .tagName("t1")
                .nodePath("/A")
                .build();
        final byte[] bytes = serializer.serialize(List.of(row));
        final String json = new String(bytes, StandardCharsets.UTF_8);

        // A pretty-printed single-row document would contain newlines between object keys.
        // Compact output contains no newlines at all (Jackson's default separator is a comma).
        assertThat(json).doesNotContain("\n");
        assertThat(json).doesNotContain("\r");
        // Round-trip must still work — the shape hasn't changed, only the whitespace.
        assertThat(serializer.deserialize(bytes)).hasSize(1);
    }
}
