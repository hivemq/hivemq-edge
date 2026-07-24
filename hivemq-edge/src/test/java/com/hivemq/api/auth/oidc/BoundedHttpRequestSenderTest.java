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
package com.hivemq.api.auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BoundedHttpRequestSender}: a response within the cap is read, and a response
 * over the cap is rejected while reading rather than buffered whole.
 */
class BoundedHttpRequestSenderTest {

    private static final int LIMIT = 1024;

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void send_readsAResponseWithinTheLimit() throws Exception {
        serve("/ok", "{\"hello\":\"world\"}", 200);

        final HTTPResponse response = new BoundedHttpRequestSender(LIMIT).send(get("/ok"));

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("{\"hello\":\"world\"}");
    }

    @Test
    void send_rejectsAResponseThatExceedsTheLimit() throws Exception {
        // A body larger than the cap must abort the read rather than buffer the whole response.
        serve("/big", "x".repeat(LIMIT * 4), 200);

        assertThatThrownBy(() -> new BoundedHttpRequestSender(LIMIT).send(get("/big")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void send_preservesAnErrorStatusAndItsBody() throws Exception {
        // Error responses may carry a JSON error object; they must be read (within the cap), not dropped.
        serve("/err", "{\"error\":\"invalid_grant\"}", 400);

        final HTTPResponse response = new BoundedHttpRequestSender(LIMIT).send(get("/err"));

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("invalid_grant");
    }

    private @NotNull HTTPRequest get(final @NotNull String path) throws Exception {
        final HTTPRequest request = new HTTPRequest(HTTPRequest.Method.GET, new URL(baseUrl + path));
        request.setConnectTimeout(2000);
        request.setReadTimeout(2000);
        return request;
    }

    private void serve(final @NotNull String path, final @NotNull String body, final int status) {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        server.createContext(path, exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (final OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
    }
}
