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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A tiny in-process HTTP server backed by the JDK {@link HttpServer}, for driving the {@link HttpProtocolAdapter}
 * against scripted endpoints in unit tests without any third-party dependency. Handlers run on a cached thread pool so
 * several requests can be in flight concurrently — which lets a test prove the adapter fans out its polls in parallel
 * without blocking the dispatch thread.
 */
public final class LocalHttpServer implements AutoCloseable {

    private final @NotNull HttpServer server;
    private final @NotNull ExecutorService executor = Executors.newCachedThreadPool();
    private final @NotNull AtomicReference<RecordedRequest> lastRequest = new AtomicReference<>();
    private final @NotNull AtomicInteger receivedCount = new AtomicInteger();

    LocalHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.start();
    }

    /**
     * @param path a request path.
     * @return the absolute URL for the given path on this server.
     */
    @NotNull
    String url(final @NotNull String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    /**
     * Register an endpoint that records the request and responds immediately.
     */
    void respond(
            final @NotNull String path,
            final int status,
            final @Nullable String contentType,
            final @NotNull String body) {
        register(path, null, status, contentType, body);
    }

    /**
     * Register an endpoint that records the request, awaits the latch, and then responds — so a test can prove the
     * dispatch thread returns before the response arrives.
     */
    void respondWhenReleased(
            final @NotNull String path,
            final @NotNull CountDownLatch release,
            final int status,
            final @Nullable String contentType,
            final @NotNull String body) {
        register(path, release, status, contentType, body);
    }

    /**
     * @return the most recently received request, or {@code null} if none was received.
     */
    @Nullable
    RecordedRequest lastRequest() {
        return lastRequest.get();
    }

    /**
     * @return the total number of requests received across all endpoints.
     */
    int receivedCount() {
        return receivedCount.get();
    }

    private void register(
            final @NotNull String path,
            final @Nullable CountDownLatch release,
            final int status,
            final @Nullable String contentType,
            final @NotNull String body) {
        server.createContext(path, exchange -> {
            record(exchange);
            if (release != null) {
                try {
                    release.await();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            respondTo(exchange, status, contentType, body);
        });
    }

    private void record(final @NotNull HttpExchange exchange) throws IOException {
        final byte[] requestBody = exchange.getRequestBody().readAllBytes();
        lastRequest.set(new RecordedRequest(
                exchange.getRequestMethod(),
                Map.copyOf(exchange.getRequestHeaders()),
                new String(requestBody, StandardCharsets.UTF_8)));
        receivedCount.incrementAndGet();
    }

    private static void respondTo(
            final @NotNull HttpExchange exchange,
            final int status,
            final @Nullable String contentType,
            final @NotNull String body)
            throws IOException {
        if (contentType != null) {
            exchange.getResponseHeaders().set("Content-Type", contentType);
        }
        final byte[] out = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, out.length == 0 ? -1 : out.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(out);
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    /**
     * @param method  the HTTP method of the recorded request.
     * @param headers the request headers.
     * @param body    the request body as a UTF-8 string.
     */
    public record RecordedRequest(
            @NotNull String method,
            @NotNull Map<String, List<String>> headers,
            @NotNull String body) {

        /**
         * @param name a header name (matched case-insensitively, since the server normalizes header key casing).
         * @return the first value of the named request header, or {@code null} if absent.
         */
        @Nullable
        String firstHeader(final @NotNull String name) {
            return headers.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .flatMap(entry -> entry.getValue().stream())
                    .findFirst()
                    .orElse(null);
        }
    }
}
