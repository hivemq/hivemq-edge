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
package com.hivemq.edge.impl.remote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.model.HiveMQEdgeRemoteConfiguration;
import com.hivemq.edge.model.HiveMQEdgeRemoteEvent;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.RandomPortGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiveMQEdgeHttpServiceImplTest {

    private static final @NotNull String EDGE_VERSION = "2024.1-test";
    private static final int TIMEOUT_MILLIS = 5000;
    private static final int RETRY_MILLIS = 100;

    private List<JsonNode> receivedEvents;
    private HttpServer httpServer;
    private int port;
    private ObjectMapper objectMapper;
    private HiveMQEdgeHttpServiceImpl service;
    private CountDownLatch eventLatch;

    @BeforeEach
    void setUp() throws IOException {
        port = RandomPortGenerator.get();
        objectMapper = new ObjectMapper();
        receivedEvents = Collections.synchronizedList(new ArrayList<>());
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.stop();
        }
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void testServiceDiscoveryAndConfigurationFetch() throws Exception {
        final String configJson = createConfigurationJson();
        final String servicesJson = createServicesJson();

        httpServer.createContext("/services", new JsonResponseHandler(servicesJson));
        httpServer.createContext("/config", new JsonResponseHandler(configJson));
        httpServer.createContext("/usage", new UsageEventHandler());
        httpServer.start();

        service = new HiveMQEdgeHttpServiceImpl(EDGE_VERSION,
                objectMapper,
                "http://localhost:" + port + "/services",
                TIMEOUT_MILLIS,
                TIMEOUT_MILLIS,
                RETRY_MILLIS,
                true);

        assertTrue(waitForCondition(() -> service.isOnline(), 10000), "Service should come online");

        assertTrue(waitForCondition(() -> service.getRemoteConfiguration().isPresent(), 5000),
                "Remote configuration should be present");

        final Optional<HiveMQEdgeRemoteConfiguration> config = service.getRemoteConfiguration();
        assertTrue(config.isPresent(), "Remote configuration should be present");
        assertNotNull(config.get().getCloudLink(), "Cloud link should not be null");
    }

    @Test
    void testUsageEventsAreSentToEndpoint() throws Exception {
        eventLatch = new CountDownLatch(3);
        final String configJson = createConfigurationJson();
        final String servicesJson = createServicesJson();

        httpServer.createContext("/services", new JsonResponseHandler(servicesJson));
        httpServer.createContext("/config", new JsonResponseHandler(configJson));
        httpServer.createContext("/usage", new UsageEventHandler());
        httpServer.start();

        service = new HiveMQEdgeHttpServiceImpl(EDGE_VERSION,
                objectMapper,
                "http://localhost:" + port + "/services",
                TIMEOUT_MILLIS,
                TIMEOUT_MILLIS,
                RETRY_MILLIS,
                true);

        assertTrue(waitForCondition(() -> service.isOnline(), 10000), "Service should come online");

        service.fireEvent(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED), false);
        service.fireEvent(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.BRIDGE_STARTED), false);

        assertTrue(eventLatch.await(10, TimeUnit.SECONDS), "Should receive all events within timeout");

        assertTrue(receivedEvents.size() >= 2, "Should have received at least 2 usage events");

        final boolean hasAdapterEvent =
                receivedEvents.stream().anyMatch(e -> "ADAPTER_STARTED".equals(e.get("eventType").asText()));
        final boolean hasBridgeEvent =
                receivedEvents.stream().anyMatch(e -> "BRIDGE_STARTED".equals(e.get("eventType").asText()));

        assertTrue(hasAdapterEvent, "Should have received ADAPTER_STARTED event");
        assertTrue(hasBridgeEvent, "Should have received BRIDGE_STARTED event");

        for (final JsonNode event : receivedEvents) {
            assertEquals(EDGE_VERSION, event.get("edgeVersion").asText(), "Edge version should be set on events");
        }
    }

    @Test
    void testServiceGoesOfflineWhenServerStops() throws Exception {
        final String configJson = createConfigurationJson();
        final String servicesJson = createServicesJson();

        httpServer.createContext("/services", new JsonResponseHandler(servicesJson));
        httpServer.createContext("/config", new JsonResponseHandler(configJson));
        httpServer.createContext("/usage", new UsageEventHandler());
        httpServer.start();

        service = new HiveMQEdgeHttpServiceImpl(EDGE_VERSION,
                objectMapper,
                "http://localhost:" + port + "/services",
                TIMEOUT_MILLIS,
                TIMEOUT_MILLIS,
                RETRY_MILLIS,
                true);

        assertTrue(waitForCondition(() -> service.isOnline(), 10000), "Service should come online");
        assertTrue(service.isOnline(), "Service should be online");

        service.stop();

        assertFalse(service.isOnline(), "Service should be offline after stop");
    }

    @Test
    void testEventsQueuedWhenOfflineIfFlagSet() throws Exception {
        eventLatch = new CountDownLatch(2);
        final String configJson = createConfigurationJson();
        final String servicesJson = createServicesJson();

        httpServer.createContext("/services", new JsonResponseHandler(servicesJson));
        httpServer.createContext("/config", new JsonResponseHandler(configJson));
        httpServer.createContext("/usage", new UsageEventHandler());
        httpServer.start();

        service = new HiveMQEdgeHttpServiceImpl(EDGE_VERSION,
                objectMapper,
                "http://localhost:" + port + "/services",
                TIMEOUT_MILLIS,
                TIMEOUT_MILLIS,
                RETRY_MILLIS,
                true);

        service.fireEvent(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED), true);

        assertTrue(eventLatch.await(10, TimeUnit.SECONDS), "Queued event should be sent once online");

        final boolean hasAdapterEvent =
                receivedEvents.stream().anyMatch(e -> "ADAPTER_STARTED".equals(e.get("eventType").asText()));
        assertTrue(hasAdapterEvent, "Queued ADAPTER_STARTED event should have been sent");
    }

    @Test
    void testStopCleansUpResources() throws Exception {
        final String configJson = createConfigurationJson();
        final String servicesJson = createServicesJson();

        httpServer.createContext("/services", new JsonResponseHandler(servicesJson));
        httpServer.createContext("/config", new JsonResponseHandler(configJson));
        httpServer.createContext("/usage", new UsageEventHandler());
        httpServer.start();

        service = new HiveMQEdgeHttpServiceImpl(EDGE_VERSION,
                objectMapper,
                "http://localhost:" + port + "/services",
                TIMEOUT_MILLIS,
                TIMEOUT_MILLIS,
                RETRY_MILLIS,
                true);

        assertTrue(waitForCondition(() -> service.isOnline(), 10000), "Service should come online");

        service.stop();

        assertFalse(service.isOnline(), "Service should be offline after stop");
        assertTrue(service.getRemoteConfiguration().isEmpty(), "Configuration should be cleared after stop");
    }

    @Test
    void testStopCleansUpResources_concurrentStopDuringConfigFetch() throws Exception {
        final int iterations = 50;
        final AtomicInteger failures = new AtomicInteger(0);
        final AtomicBoolean testFailed = new AtomicBoolean(false);

        for (int i = 0; i < iterations && !testFailed.get(); i++) {
            final CountDownLatch configRequestReceived = new CountDownLatch(1);
            final CountDownLatch allowConfigResponse = new CountDownLatch(1);

            final HttpServer localServer = HttpServer.create(new InetSocketAddress(0), 0);
            final int localPort = localServer.getAddress().getPort();

            final String servicesJson = String.format("""
                    {
                        "usageEndpoint": "http://localhost:%d/usage",
                        "configEndpoint": "http://localhost:%d/config"
                    }
                    """, localPort, localPort);
            final String configJson = createConfigurationJson();

            localServer.createContext("/services", new JsonResponseHandler(servicesJson));
            localServer.createContext("/config", exchange -> {
                configRequestReceived.countDown();
                try {
                    allowConfigResponse.await(5, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                final byte[] responseBytes = configJson.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (final OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            });
            localServer.createContext("/usage", exchange -> exchange.sendResponseHeaders(200, -1));
            localServer.start();

            final HiveMQEdgeHttpServiceImpl localService = new HiveMQEdgeHttpServiceImpl(EDGE_VERSION,
                    objectMapper,
                    "http://localhost:" + localPort + "/services",
                    TIMEOUT_MILLIS,
                    TIMEOUT_MILLIS,
                    RETRY_MILLIS,
                    false);

            try {
                final boolean requestReceived = configRequestReceived.await(10, TimeUnit.SECONDS);
                if (!requestReceived) {
                    continue;
                }

                final ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        Thread.sleep(10);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    localService.stop();
                });

                allowConfigResponse.countDown();

                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);

                Thread.sleep(50);

                if (localService.getRemoteConfiguration().isPresent()) {
                    failures.incrementAndGet();
                    if (failures.get() > 5) {
                        testFailed.set(true);
                    }
                }
            } finally {
                localService.stop();
                localServer.stop(0);
            }
        }

        assertEquals(0,
                failures.get(),
                "Configuration should be cleared after stop in all iterations, but failed " +
                        failures.get() +
                        " times");
    }

    @Test
    void testMultipleEventTypes() throws Exception {
        eventLatch = new CountDownLatch(5);
        final String configJson = createConfigurationJson();
        final String servicesJson = createServicesJson();

        httpServer.createContext("/services", new JsonResponseHandler(servicesJson));
        httpServer.createContext("/config", new JsonResponseHandler(configJson));
        httpServer.createContext("/usage", new UsageEventHandler());
        httpServer.start();

        service = new HiveMQEdgeHttpServiceImpl(EDGE_VERSION,
                objectMapper,
                "http://localhost:" + port + "/services",
                TIMEOUT_MILLIS,
                TIMEOUT_MILLIS,
                RETRY_MILLIS,
                true);

        assertTrue(waitForCondition(() -> service.isOnline(), 10000), "Service should come online");

        service.fireEvent(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED), false);
        service.fireEvent(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_ERROR), false);
        service.fireEvent(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.BRIDGE_STARTED), false);
        service.fireEvent(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.BRIDGE_ERROR), false);

        assertTrue(eventLatch.await(10, TimeUnit.SECONDS), "Should receive all events");

        assertTrue(receivedEvents.size() >= 4, "Should have received at least 4 custom events");
    }

    @Test
    void testEventContainsSessionAndInstallationTokens() throws Exception {
        eventLatch = new CountDownLatch(2);
        final String configJson = createConfigurationJson();
        final String servicesJson = createServicesJson();

        httpServer.createContext("/services", new JsonResponseHandler(servicesJson));
        httpServer.createContext("/config", new JsonResponseHandler(configJson));
        httpServer.createContext("/usage", new UsageEventHandler());
        httpServer.start();

        service = new HiveMQEdgeHttpServiceImpl(EDGE_VERSION,
                objectMapper,
                "http://localhost:" + port + "/services",
                TIMEOUT_MILLIS,
                TIMEOUT_MILLIS,
                RETRY_MILLIS,
                true);

        assertTrue(waitForCondition(() -> service.isOnline(), 10000), "Service should come online");

        service.fireEvent(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED), false);

        assertTrue(eventLatch.await(10, TimeUnit.SECONDS), "Should receive event");

        final JsonNode adapterEvent = receivedEvents.stream()
                .filter(e -> "ADAPTER_STARTED".equals(e.get("eventType").asText()))
                .findFirst()
                .orElse(null);

        assertNotNull(adapterEvent, "Should have received ADAPTER_STARTED event");
        assertNotNull(adapterEvent.get("sessionToken"), "Event should have sessionToken");
        assertNotNull(adapterEvent.get("installationToken"), "Event should have installationToken");
        assertNotNull(adapterEvent.get("created"), "Event should have created timestamp");
    }

    private @NotNull String createServicesJson() {
        return String.format("""
                {
                    "usageEndpoint": "http://localhost:%d/usage",
                    "configEndpoint": "http://localhost:%d/config"
                }
                """, port, port);
    }

    private @NotNull String createConfigurationJson() {
        return """
                {
                    "ctas": [],
                    "resources": [],
                    "extensions": [],
                    "modules": [],
                    "properties": {},
                    "cloudLink": {"displayText": "Cloud", "url": "https://cloud.hivemq.com", "external": true},
                    "gitHubLink": {"displayText": "GitHub", "url": "https://github.com/hivemq", "external": true},
                    "documentationLink": {"displayText": "Docs", "url": "https://docs.hivemq.com", "external": true}
                }
                """;
    }

    private boolean waitForCondition(final @NotNull BooleanSupplier condition, final long timeoutMillis)
            throws InterruptedException {
        final long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(50);
        }
        return condition.getAsBoolean();
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    private class JsonResponseHandler implements HttpHandler {
        private final @NotNull String jsonResponse;

        JsonResponseHandler(final @NotNull String jsonResponse) {
            this.jsonResponse = jsonResponse;
        }

        @Override
        public void handle(final @NotNull HttpExchange exchange) throws IOException {
            final String method = exchange.getRequestMethod();
            if ("HEAD".equals(method)) {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            final byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (final OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    private class UsageEventHandler implements HttpHandler {
        @Override
        public void handle(final @NotNull HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            try (final InputStream is = exchange.getRequestBody()) {
                final String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                final JsonNode event = objectMapper.readTree(body);
                receivedEvents.add(event);
                if (eventLatch != null) {
                    eventLatch.countDown();
                }
            }
            exchange.sendResponseHeaders(200, -1);
        }
    }
}
