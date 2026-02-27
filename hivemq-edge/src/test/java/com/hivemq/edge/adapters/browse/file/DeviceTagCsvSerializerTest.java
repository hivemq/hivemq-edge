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

import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import com.hivemq.edge.adapters.browse.model.FieldMappingInstruction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceTagCsvSerializerTest {

    private final DeviceTagCsvSerializer serializer = new DeviceTagCsvSerializer();

    // --- Round-trip tests ---

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
                .southboundFieldMapping(List.of(new FieldMappingInstruction("value", "value"),
                        new FieldMappingInstruction("status", "quality")))
                .maxQos(1)
                .messageExpiryInterval(3600L)
                .includeTimestamp(true)
                .includeTagNames(false)
                .includeMetadata(true)
                .mqttUserProperties(Map.of("key1", "val1", "key2", "val2"))
                .build();

        final byte[] csv = serializer.serialize(List.of(row));
        final List<DeviceTagRow> result = serializer.deserialize(csv);

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
        assertThat(deserialized.getMqttUserProperties()).hasSize(2);
    }

    @Test
    void roundTrip_minimalRow() throws IOException {
        final DeviceTagRow row = DeviceTagRow.builder().nodeId("ns=0;i=1").build();

        final byte[] csv = serializer.serialize(List.of(row));
        final List<DeviceTagRow> result = serializer.deserialize(csv);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNodeId()).isEqualTo("ns=0;i=1");
        assertThat(result.getFirst().getTagName()).isNull();
        assertThat(result.getFirst().getSouthboundFieldMapping()).isNull();
        assertThat(result.getFirst().getMqttUserProperties()).isNull();
    }

    @Test
    void roundTrip_multipleRows() throws IOException {
        final List<DeviceTagRow> rows =
                List.of(DeviceTagRow.builder().nodePath("/B").nodeId("ns=0;i=2").tagName("tag-b").build(),
                        DeviceTagRow.builder().nodePath("/A").nodeId("ns=0;i=1").tagName("tag-a").build(),
                        DeviceTagRow.builder().nodePath("/C").nodeId("ns=0;i=3").tagName("tag-c").build());

        final byte[] csv = serializer.serialize(rows);
        final List<DeviceTagRow> result = serializer.deserialize(csv);

        assertThat(result).hasSize(3);
        // Serialization sorts by nodePath ascending
        assertThat(result.getFirst().getNodePath()).isEqualTo("/A");
        assertThat(result.get(1).getNodePath()).isEqualTo("/B");
        assertThat(result.get(2).getNodePath()).isEqualTo("/C");
    }

    @Test
    void roundTrip_emptyList() throws IOException {
        final byte[] csv = serializer.serialize(List.of());
        final List<DeviceTagRow> result = serializer.deserialize(csv);
        assertThat(result).isEmpty();
    }

    // --- Compact encoding tests ---

    @Test
    void encodeFieldMapping_singleMapping() {
        final String encoded =
                DeviceTagCsvSerializer.encodeFieldMapping(List.of(new FieldMappingInstruction("src", "dst")));
        assertThat(encoded).isEqualTo("src->dst");
    }

    @Test
    void encodeFieldMapping_multipleMappings() {
        final String encoded = DeviceTagCsvSerializer.encodeFieldMapping(List.of(new FieldMappingInstruction("a", "b"),
                new FieldMappingInstruction("c", "d")));
        assertThat(encoded).isEqualTo("a->b;c->d");
    }

    @Test
    void encodeFieldMapping_null() {
        assertThat(DeviceTagCsvSerializer.encodeFieldMapping(null)).isNull();
    }

    @Test
    void encodeFieldMapping_emptyList() {
        assertThat(DeviceTagCsvSerializer.encodeFieldMapping(List.of())).isNull();
    }

    @Test
    void decodeFieldMapping_singleMapping() {
        final List<FieldMappingInstruction> result = DeviceTagCsvSerializer.decodeFieldMapping("src->dst");
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().source()).isEqualTo("src");
        assertThat(result.getFirst().destination()).isEqualTo("dst");
    }

    @Test
    void decodeFieldMapping_multipleMappings() {
        final List<FieldMappingInstruction> result = DeviceTagCsvSerializer.decodeFieldMapping("a->b;c->d");
        assertThat(result).hasSize(2);
    }

    @Test
    void decodeFieldMapping_nullOrEmpty() {
        assertThat(DeviceTagCsvSerializer.decodeFieldMapping(null)).isNull();
        assertThat(DeviceTagCsvSerializer.decodeFieldMapping("")).isNull();
    }

    @Test
    void decodeFieldMapping_malformed() {
        assertThatThrownBy(() -> DeviceTagCsvSerializer.decodeFieldMapping("noarrow")).isInstanceOf(
                IllegalArgumentException.class).hasMessageContaining("Invalid field mapping format");
    }

    @Test
    void encodeUserProperties_simple() {
        final String encoded = DeviceTagCsvSerializer.encodeUserProperties(Map.of("key", "value"));
        assertThat(encoded).isEqualTo("key=value");
    }

    @Test
    void decodeUserProperties_simple() {
        final Map<String, String> result = DeviceTagCsvSerializer.decodeUserProperties("key=value");
        assertThat(result).containsEntry("key", "value");
    }

    @Test
    void decodeUserProperties_multiple() {
        final Map<String, String> result = DeviceTagCsvSerializer.decodeUserProperties("a=1;b=2");
        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("a", "1");
        assertThat(result).containsEntry("b", "2");
    }

    @Test
    void decodeUserProperties_malformed() {
        assertThatThrownBy(() -> DeviceTagCsvSerializer.decodeUserProperties("noequals")).isInstanceOf(
                IllegalArgumentException.class).hasMessageContaining("Invalid user property format");
    }

    // --- Boolean parsing ---

    @Test
    void deserialize_booleanParsing() throws IOException {
        final String csv = """
                node_path,node_id,tag_name,include_timestamp,include_tag_names,include_metadata\r
                /path,ns=0;i=1,tag1,true,false,TRUE\r
                /path2,ns=0;i=2,tag2,FALSE,True,false\r
                """;
        final List<DeviceTagRow> result = serializer.deserialize(csv.getBytes(StandardCharsets.UTF_8));
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getIncludeTimestamp()).isTrue();
        assertThat(result.getFirst().getIncludeTagNames()).isFalse();
        assertThat(result.getFirst().getIncludeMetadata()).isTrue();
        assertThat(result.get(1).getIncludeTimestamp()).isFalse();
        assertThat(result.get(1).getIncludeTagNames()).isTrue();
        assertThat(result.get(1).getIncludeMetadata()).isFalse();
    }

    // --- Missing/extra columns ---

    @Test
    void deserialize_missingOptionalColumns() throws IOException {
        final String csv = "node_id,tag_name\r\nns=0;i=1,my-tag\r\n";
        final List<DeviceTagRow> result = serializer.deserialize(csv.getBytes(StandardCharsets.UTF_8));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNodeId()).isEqualTo("ns=0;i=1");
        assertThat(result.getFirst().getTagName()).isEqualTo("my-tag");
        assertThat(result.getFirst().getNodePath()).isNull();
        assertThat(result.getFirst().getMaxQos()).isNull();
    }

    @Test
    void deserialize_unknownColumnsIgnored() throws IOException {
        final String csv = "node_id,tag_name,unknown_column\r\nns=0;i=1,my-tag,ignored\r\n";
        final List<DeviceTagRow> result = serializer.deserialize(csv.getBytes(StandardCharsets.UTF_8));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNodeId()).isEqualTo("ns=0;i=1");
    }

    @Test
    void deserialize_emptyCells_treatedAsNull() throws IOException {
        final String csv = "node_id,tag_name,max_qos\r\nns=0;i=1,,\r\n";
        final List<DeviceTagRow> result = serializer.deserialize(csv.getBytes(StandardCharsets.UTF_8));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTagName()).isNull();
        assertThat(result.getFirst().getMaxQos()).isNull();
    }

    // --- Wildcard preservation ---

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

        final byte[] csv = serializer.serialize(List.of(row));
        final List<DeviceTagRow> result = serializer.deserialize(csv);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTagName()).isEqualTo("*");
        assertThat(result.getFirst().getTagNameDefault()).isEqualTo("auto-tag");
        assertThat(result.getFirst().getNorthboundTopic()).isEqualTo("*");
        assertThat(result.getFirst().getSouthboundTopic()).isEqualTo("*");
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
                    .accessLevel("READ_WRITE")
                    .tagName("tag-" + i)
                    .tagNameDefault("default-tag-" + i)
                    .northboundTopic("adapter/data/node" + i)
                    .northboundTopicDefault("adapter/data/node" + i)
                    .southboundTopic("adapter/write/data/node" + i)
                    .southboundTopicDefault("adapter/write/data/node" + i)
                    .southboundFieldMapping(List.of(new FieldMappingInstruction("value", "value")))
                    .maxQos(1)
                    .includeTimestamp(true)
                    .build());
        }

        final byte[] csv = serializer.serialize(rows);
        final List<DeviceTagRow> result = serializer.deserialize(csv);

        assertThat(result).hasSize(5000);
        // Verify sorted order
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i - 1).getNodePath()).isLessThanOrEqualTo(result.get(i).getNodePath());
        }
    }

    // --- Concurrency ---

    @Test
    void concurrentSerialization_threadSafe() throws Exception {
        final int threadCount = 10;
        final int rowsPerThread = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);
        final AtomicInteger errors = new AtomicInteger(0);
        try (final ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int t = 0; t < threadCount; t++) {
                final int threadIdx = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        final List<DeviceTagRow> rows = new ArrayList<>();
                        for (int i = 0; i < rowsPerThread; i++) {
                            rows.add(DeviceTagRow.builder()
                                    .nodePath("/Thread" + threadIdx + "/Node" + i)
                                    .nodeId("ns=2;i=" + (threadIdx * 1000 + i))
                                    .tagName("t" + threadIdx + "-tag-" + i)
                                    .build());
                        }
                        final byte[] csv = serializer.serialize(rows);
                        final List<DeviceTagRow> result = serializer.deserialize(csv);
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
        }
        assertThat(errors.get()).isZero();
    }

    // --- CSV format specifics ---

    @Test
    void serialize_outputIsUtf8() throws IOException {
        final DeviceTagRow row = DeviceTagRow.builder()
                .nodePath("/Objects/Data/Gerät")
                .nodeId("ns=2;i=1")
                .tagDescription("Métriques de température")
                .build();

        final byte[] csv = serializer.serialize(List.of(row));
        final String csvStr = new String(csv, StandardCharsets.UTF_8);
        assertThat(csvStr).contains("Gerät");
        assertThat(csvStr).contains("Métriques");
    }

    @Test
    void serialize_containsHeader() throws IOException {
        final byte[] csv = serializer.serialize(List.of(DeviceTagRow.builder().nodeId("ns=0;i=1").build()));
        final String csvStr = new String(csv, StandardCharsets.UTF_8);
        assertThat(csvStr).startsWith("node_path,namespace_uri,namespace_index,node_id");
    }
}
