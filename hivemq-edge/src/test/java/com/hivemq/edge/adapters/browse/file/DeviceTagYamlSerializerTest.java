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

class DeviceTagYamlSerializerTest {

    private final DeviceTagYamlSerializer serializer = new DeviceTagYamlSerializer();

    @Test
    void roundTrip_allFields() throws IOException {
        final DeviceTagRow row = DeviceTagRow.builder()
                .nodePath("/Objects/Data/Double")
                .namespaceUri("urn:test")
                .namespaceIndex(2)
                .nodeId("ns=2;i=200")
                .dataType("Double")
                .accessLevel("READ")
                .nodeDescription("A double node")
                .tagName("dbl-tag")
                .tagNameDefault("adapter-double")
                .tagDescription("Double description")
                .northboundTopic("adapter/data/double")
                .northboundTopicDefault("adapter/data/double")
                .southboundTopic("adapter/write/data/double")
                .southboundTopicDefault("adapter/write/data/double")
                .southboundFieldMapping(List.of(new FieldMappingInstruction("val", "val")))
                .maxQos(2)
                .messageExpiryInterval(7200L)
                .includeTimestamp(false)
                .includeTagNames(true)
                .includeMetadata(false)
                .mqttUserProperties(Map.of("env", "prod"))
                .build();

        final byte[] yaml = serializer.serialize(List.of(row));
        final List<DeviceTagRow> result = serializer.deserialize(yaml);

        assertThat(result).hasSize(1);
        final DeviceTagRow d = result.getFirst();
        assertThat(d.getNodePath()).isEqualTo("/Objects/Data/Double");
        assertThat(d.getNodeId()).isEqualTo("ns=2;i=200");
        assertThat(d.getDataType()).isEqualTo("Double");
        assertThat(d.getTagName()).isEqualTo("dbl-tag");
        assertThat(d.getNorthboundTopic()).isEqualTo("adapter/data/double");
        assertThat(d.getSouthboundTopic()).isEqualTo("adapter/write/data/double");
        assertThat(d.getMaxQos()).isEqualTo(2);
        assertThat(d.getMessageExpiryInterval()).isEqualTo(7200L);
        assertThat(d.getIncludeTimestamp()).isFalse();
        assertThat(d.getIncludeTagNames()).isTrue();
        assertThat(d.getSouthboundFieldMapping()).hasSize(1);
        assertThat(d.getMqttUserProperties()).containsEntry("env", "prod");
    }

    @Test
    void roundTrip_minimalRow() throws IOException {
        final DeviceTagRow row = DeviceTagRow.builder().nodeId("ns=0;i=1").build();

        final byte[] yaml = serializer.serialize(List.of(row));
        final List<DeviceTagRow> result = serializer.deserialize(yaml);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNodeId()).isEqualTo("ns=0;i=1");
        assertThat(result.getFirst().getTagName()).isNull();
    }

    @Test
    void roundTrip_emptyList() throws IOException {
        final byte[] yaml = serializer.serialize(List.of());
        final List<DeviceTagRow> result = serializer.deserialize(yaml);
        assertThat(result).isEmpty();
    }

    @Test
    void roundTrip_wildcardPreservation() throws IOException {
        final DeviceTagRow row =
                DeviceTagRow.builder().nodeId("ns=0;i=1").tagName("*").tagNameDefault("auto-tag").build();

        final byte[] yaml = serializer.serialize(List.of(row));
        final List<DeviceTagRow> result = serializer.deserialize(yaml);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTagName()).isEqualTo("*");
        assertThat(result.getFirst().getTagNameDefault()).isEqualTo("auto-tag");
    }

    @Test
    void deserialize_malformedYaml() {
        final byte[] malformed = "not: valid: yaml: [".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> serializer.deserialize(malformed)).isInstanceOf(IOException.class);
    }

    @Test
    void serialize_noDocumentStartMarker() throws IOException {
        final DeviceTagRow row = DeviceTagRow.builder().nodeId("ns=0;i=1").build();

        final byte[] yaml = serializer.serialize(List.of(row));
        final String yamlStr = new String(yaml, StandardCharsets.UTF_8);
        assertThat(yamlStr).doesNotStartWith("---");
    }

    // --- Large payload ---

    @Test
    void roundTrip_largePayload_5000Rows() throws IOException {
        final List<DeviceTagRow> rows = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            rows.add(DeviceTagRow.builder()
                    .nodePath("/Objects/Data/Node" + i)
                    .nodeId("ns=2;i=" + (1000 + i))
                    .tagName("tag-" + i)
                    .build());
        }

        final byte[] yaml = serializer.serialize(rows);
        final List<DeviceTagRow> result = serializer.deserialize(yaml);
        assertThat(result).hasSize(5000);
    }

    // --- Concurrency ---

    @Test
    void concurrentSerialization_threadSafe() throws Exception {
        final int threadCount = 10;
        final int rowsPerThread = 50;
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
                    final byte[] yaml = serializer.serialize(rows);
                    final List<DeviceTagRow> result = serializer.deserialize(yaml);
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

    // --- Cross-format compatibility ---

    @Test
    void jsonAndYaml_produceEquivalentResults() throws IOException {
        final DeviceTagJsonSerializer jsonSerializer = new DeviceTagJsonSerializer();

        final DeviceTagRow row = DeviceTagRow.builder()
                .nodePath("/Objects/Data/Int32")
                .namespaceUri("urn:test")
                .namespaceIndex(2)
                .nodeId("ns=2;i=100")
                .dataType("Int32")
                .tagName("my-tag")
                .northboundTopic("topic/a")
                .maxQos(1)
                .build();

        final byte[] json = jsonSerializer.serialize(List.of(row));
        final byte[] yaml = serializer.serialize(List.of(row));

        final List<DeviceTagRow> fromJson = jsonSerializer.deserialize(json);
        final List<DeviceTagRow> fromYaml = serializer.deserialize(yaml);

        assertThat(fromJson).hasSize(1);
        assertThat(fromYaml).hasSize(1);
        assertThat(fromJson.getFirst()).isEqualTo(fromYaml.getFirst());
    }
}
