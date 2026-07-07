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
package com.hivemq.edge.adapters.http.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpProtocolAdapterTest {

    private static final @NotNull Duration TIMEOUT = Duration.ofSeconds(10);

    private LocalHttpServer server;
    private ManualDispatcher dispatcher;
    private RecordingProtocolAdapterOutput output;

    @BeforeEach
    void setUp() throws IOException {
        server = new LocalHttpServer();
        dispatcher = new ManualDispatcher();
        output = new RecordingProtocolAdapterOutput();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void lifecycleCommandsAcknowledgeInOrder() {
        final HttpProtocolAdapter adapter = newAdapter(Map.of());

        adapter.start();
        adapter.connect();
        adapter.disconnect();
        adapter.stop();
        dispatcher.drainAll();

        assertThat(output.events).containsExactly("started", "connected", "disconnected", "stopped");
    }

    @Test
    void getReturningJsonEmitsAJsonDataPoint() {
        server.respond("/json", 200, "application/json", "{\"temperature\":21}");
        final HttpProtocolAdapter adapter = connected(Map.of());

        poll(adapter, get("/json"));
        awaitResults(1);

        assertThat(output.nodeErrors).isEmpty();
        assertThat(output.dataPoints).hasSize(1);
        assertThat(output.dataPoints.get(0).value().getTagValue()).isInstanceOf(JsonNode.class);
        assertThat(output.dataPoints.get(0).value().treatTagValueAsJson()).isTrue();
    }

    @Test
    void getReturningPlainTextEmitsABase64DataUriString() {
        server.respond("/text", 200, "text/plain", "hello");
        final HttpProtocolAdapter adapter = connected(Map.of());

        poll(adapter, get("/text"));
        awaitResults(1);

        assertThat(output.nodeErrors).isEmpty();
        assertThat(output.dataPoints).hasSize(1);
        assertThat(output.dataPoints.get(0).value().treatTagValueAsJson()).isFalse();
        assertThat(output.dataPoints.get(0).value().getTagValue().toString()).startsWith("data:text/plain;base64,");
    }

    @Test
    void assertResponseIsJsonParsesAPlainBodyAsJson() {
        server.respond("/plain-json", 200, "text/plain", "{\"value\":7}");
        final HttpProtocolAdapter adapter = connected(Map.of("assertResponseIsJson", true));

        poll(adapter, get("/plain-json"));
        awaitResults(1);

        assertThat(output.dataPoints).hasSize(1);
        assertThat(output.dataPoints.get(0).value().getTagValue()).isInstanceOf(JsonNode.class);
    }

    @Test
    void notFoundWithPublishSuccessOnlyReportsANodeErrorAndNoDataPoint() {
        server.respond("/missing", 404, "text/plain", "not found");
        final HttpProtocolAdapter adapter = connected(Map.of("httpPublishSuccessStatusCodeOnly", true));

        poll(adapter, get("/missing"));
        awaitResults(1);

        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).reason()).contains("404");
    }

    @Test
    void notFoundWithPublishingAllowedEmitsTheResponseBody() {
        server.respond("/missing", 404, "text/plain", "not found");
        final HttpProtocolAdapter adapter = connected(Map.of("httpPublishSuccessStatusCodeOnly", false));

        poll(adapter, get("/missing"));
        awaitResults(1);

        assertThat(output.nodeErrors).isEmpty();
        assertThat(output.dataPoints).hasSize(1);
        assertThat(output.dataPoints.get(0).value().getTagValue().toString()).startsWith("data:text/plain;base64,");
    }

    @Test
    void aHungEndpointFailsOnThePerRequestTimeout() {
        final CountDownLatch neverReleased = new CountDownLatch(1);
        server.respondWhenReleased("/slow", neverReleased, 200, "application/json", "{}");
        final HttpProtocolAdapter adapter = connected(Map.of());
        // The minimum per-request timeout is one second; the request cannot complete before then.
        final HttpNode node =
                new HttpNode(server.url("/slow"), HttpMethod.GET, 1, HttpContentType.JSON, null, List.of());

        poll(adapter, node);
        try {
            awaitResults(1);
            assertThat(output.dataPoints).isEmpty();
            assertThat(output.nodeErrors).hasSize(1);
        } finally {
            neverReleased.countDown();
        }
    }

    @Test
    void postSendsTheConfiguredBodyContentTypeCustomHeadersAndUserAgent() {
        server.respond("/command", 200, "application/json", "{\"ok\":true}");
        final HttpProtocolAdapter adapter = connected(Map.of());
        final HttpNode node = new HttpNode(
                server.url("/command"),
                HttpMethod.POST,
                5,
                HttpContentType.JSON,
                "{\"command\":\"open\"}",
                List.of(new HttpHeader("X-Token", "secret")));

        poll(adapter, node);
        awaitResults(1);

        final LocalHttpServer.RecordedRequest request = server.lastRequest();
        assertThat(request).isNotNull();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.body()).isEqualTo("{\"command\":\"open\"}");
        assertThat(request.firstHeader("Content-Type")).isEqualTo("application/json");
        assertThat(request.firstHeader("X-Token")).isEqualTo("secret");
        assertThat(request.firstHeader("User-Agent")).startsWith("HiveMQ-Edge;");
    }

    @Test
    void pollingBeforeConnectReportsANodeError() {
        final HttpProtocolAdapter adapter = newAdapter(Map.of());

        poll(adapter, get("/json"));

        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).reason()).contains("not initialized");
    }

    @Test
    void allowUntrustedCertificatesStillPollsAPlainEndpoint() {
        server.respond("/json", 200, "application/json", "{\"n\":1}");
        final HttpProtocolAdapter adapter = connected(Map.of("allowUntrustedCertificates", true));

        poll(adapter, get("/json"));
        awaitResults(1);

        assertThat(output.dataPoints).hasSize(1);
    }

    @Test
    void aBatchOfPollsFansOutConcurrentlyWithoutBlockingTheDispatchThread() {
        final CountDownLatch release = new CountDownLatch(1);
        server.respondWhenReleased("/a", release, 200, "application/json", "{\"n\":1}");
        server.respondWhenReleased("/b", release, 200, "application/json", "{\"n\":2}");
        server.respondWhenReleased("/c", release, 200, "application/json", "{\"n\":3}");
        final HttpProtocolAdapter adapter = connected(Map.of());

        adapter.pollBatch(List.of(get("/a"), get("/b"), get("/c")));
        dispatcher.drainAll();

        // The dispatch thread returned after firing all three requests; the responses are still blocked, so all three
        // requests are in flight concurrently and nothing has been reported yet.
        await().atMost(TIMEOUT).until(() -> server.receivedCount() == 3);
        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).isEmpty();

        release.countDown();
        awaitResults(3);
        assertThat(output.dataPoints).hasSize(3);
    }

    private @NotNull HttpProtocolAdapter newAdapter(final @NotNull Map<String, Object> config) {
        return new HttpProtocolAdapter(
                HttpAdapterTestFixtures.input(
                        "http-1", dispatcher, new HttpAdapterTestFixtures.TestDataPointFactory(), config),
                output);
    }

    private @NotNull HttpProtocolAdapter connected(final @NotNull Map<String, Object> config) {
        final HttpProtocolAdapter adapter = newAdapter(config);
        adapter.connect();
        dispatcher.drainAll();
        return adapter;
    }

    private void poll(final @NotNull HttpProtocolAdapter adapter, final @NotNull HttpNode node) {
        adapter.pollBatch(List.<Node>of(node));
        dispatcher.drainAll();
    }

    private @NotNull HttpNode get(final @NotNull String path) {
        return new HttpNode(server.url(path), HttpMethod.GET, 5, HttpContentType.JSON, null, List.of());
    }

    private void awaitResults(final int count) {
        await().atMost(TIMEOUT).until(() -> output.dataPoints.size() + output.nodeErrors.size() >= count);
    }
}
