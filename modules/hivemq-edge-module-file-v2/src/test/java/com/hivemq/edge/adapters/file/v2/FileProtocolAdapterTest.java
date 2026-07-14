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
package com.hivemq.edge.adapters.file.v2;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileProtocolAdapterTest {

    private final ManualDispatcher dispatcher = new ManualDispatcher();
    private final RecordingProtocolAdapterOutput output = new RecordingProtocolAdapterOutput();
    private FileProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FileProtocolAdapter(
                FileAdapterTestFixtures.input("file-1", dispatcher, new FileAdapterTestFixtures.TestDataPointFactory()),
                output);
    }

    @Test
    void lifecycleCommandsAcknowledgeInOrder() {
        adapter.start();
        adapter.connect();
        adapter.disconnect();
        adapter.stop();
        dispatcher.drainAll();

        assertThat(output.events).containsExactly("started", "connected", "disconnected", "stopped");
    }

    @Test
    void pollingAJsonFileEmitsAJsonDataPoint(@TempDir final Path directory) throws IOException {
        final Path file = directory.resolve("reading.json");
        Files.writeString(file, "{\"temperature\":21}");
        final FileNode node = new FileNode(file.toString(), FileContentType.TEXT_JSON);

        poll(node);

        assertThat(output.nodeErrors).isEmpty();
        assertThat(output.dataPoints).hasSize(1);
        assertThat(output.dataPoints.get(0).value().getTagValue()).isInstanceOf(JsonNode.class);
        assertThat(output.dataPoints.get(0).value().treatTagValueAsJson()).isFalse();
    }

    @Test
    void pollingAPlainTextFileEmitsAStringDataPoint(@TempDir final Path directory) throws IOException {
        final Path file = directory.resolve("reading.txt");
        Files.writeString(file, "the quick brown fox");
        final FileNode node = new FileNode(file.toString(), FileContentType.TEXT_PLAIN);

        poll(node);

        assertThat(output.nodeErrors).isEmpty();
        assertThat(output.dataPoints).hasSize(1);
        assertThat(output.dataPoints.get(0).value().getTagValue()).isEqualTo("the quick brown fox");
        assertThat(output.dataPoints.get(0).value().treatTagValueAsJson()).isFalse();
    }

    @Test
    void pollingABinaryFileEmitsBase64EncodedBytes(@TempDir final Path directory) throws IOException {
        final Path file = directory.resolve("reading.bin");
        final byte[] raw = {0, 1, 2, 3, (byte) 0xFF};
        Files.write(file, raw);
        final FileNode node = new FileNode(file.toString(), FileContentType.BINARY);

        poll(node);

        assertThat(output.nodeErrors).isEmpty();
        assertThat(output.dataPoints).hasSize(1);
        assertThat(output.dataPoints.get(0).value().getTagValue()).isInstanceOf(byte[].class);
        assertThat((byte[]) output.dataPoints.get(0).value().getTagValue())
                .isEqualTo(Base64.getEncoder().encode(raw));
    }

    @Test
    void pollingAMissingFileReportsANodeError(@TempDir final Path directory) {
        final FileNode node =
                new FileNode(directory.resolve("does-not-exist.txt").toString(), FileContentType.TEXT_PLAIN);

        poll(node);

        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).spontaneous()).isFalse();
    }

    @Test
    void pollingAnInvalidPathReportsANodeError() {
        final FileNode node = new FileNode("\0", FileContentType.TEXT_PLAIN);

        poll(node);

        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).reason()).contains("file path is invalid");
    }

    @Test
    void pollingAFileOverTheSizeCapReportsANodeErrorAndNoDataPoint(@TempDir final Path directory) throws IOException {
        final Path file = directory.resolve("big.txt");
        Files.write(file, new byte[64_001]);
        final FileNode node = new FileNode(file.toString(), FileContentType.TEXT_PLAIN);

        poll(node);

        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).reason()).contains("exceeds the limit");
    }

    @Test
    void malformedJsonFileReportsANodeError(@TempDir final Path directory) throws IOException {
        final Path file = directory.resolve("broken.json");
        Files.writeString(file, "{not-json");
        final FileNode node = new FileNode(file.toString(), FileContentType.TEXT_JSON);

        poll(node);

        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).hasSize(1);
    }

    @Test
    void aBatchPollEmitsAResultPerNodeInOrder(@TempDir final Path directory) throws IOException {
        final Path good = directory.resolve("good.txt");
        Files.writeString(good, "value");
        final FileNode goodNode = new FileNode(good.toString(), FileContentType.TEXT_PLAIN);
        final FileNode missingNode =
                new FileNode(directory.resolve("missing.txt").toString(), FileContentType.TEXT_PLAIN);

        adapter.pollBatch(List.of(goodNode, missingNode));
        dispatcher.drainAll();

        assertThat(output.dataPoints).hasSize(1);
        assertThat(output.dataPoints.get(0).node()).isSameAs(goodNode);
        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).node()).isSameAs(missingNode);
        assertThat(output.events).containsExactly("dataPoint", "nodeError");
    }

    @Test
    void csvContentIsReadAsUtf8Text(@TempDir final Path directory) throws IOException {
        final Path file = directory.resolve("reading.csv");
        Files.writeString(file, "a,b,c", StandardCharsets.UTF_8);
        final FileNode node = new FileNode(file.toString(), FileContentType.TEXT_CSV);

        poll(node);

        assertThat(output.dataPoints).hasSize(1);
        assertThat(output.dataPoints.get(0).value().getTagValue()).isEqualTo("a,b,c");
    }

    @Test
    void xmlContentIsReadAsUtf8Text(@TempDir final Path directory) throws IOException {
        final Path file = directory.resolve("reading.xml");
        Files.writeString(file, "<a>1</a>", StandardCharsets.UTF_8);
        final FileNode node = new FileNode(file.toString(), FileContentType.TEXT_XML);

        poll(node);

        assertThat(output.dataPoints).hasSize(1);
        assertThat(output.dataPoints.get(0).value().getTagValue()).isEqualTo("<a>1</a>");
    }

    private void poll(final @org.jetbrains.annotations.NotNull Node node) {
        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();
    }
}
