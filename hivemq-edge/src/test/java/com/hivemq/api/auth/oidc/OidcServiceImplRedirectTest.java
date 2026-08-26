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

import com.hivemq.api.config.OidcConfiguration;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import util.RandomPortGenerator;

/**
 * Verifies that the resource retriever used for the discovery and JWKS fetches does not follow HTTP
 * redirects. A redirect from a validated endpoint would only widen the set of hosts Edge contacts, so the
 * retriever must stop at the first response rather than chase a {@code 302} to a second server.
 */
class OidcServiceImplRedirectTest {

    private HttpServer redirectingServer;
    private HttpServer secondServer;

    @AfterEach
    void tearDown() {
        if (redirectingServer != null) {
            redirectingServer.stop(0);
        }
        if (secondServer != null) {
            secondServer.stop(0);
        }
    }

    @Test
    void resourceRetriever_doesNotFollowRedirects() throws Exception {
        // The second server records every hit; if a redirect were followed, its counter would move.
        final AtomicInteger secondServerHits = new AtomicInteger();
        final int secondPort = RandomPortGenerator.get();
        secondServer = HttpServer.create(new InetSocketAddress(secondPort), 0);
        secondServer.createContext("/", exchange -> {
            secondServerHits.incrementAndGet();
            final byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        secondServer.start();

        // The first server answers every request with a 302 pointing at the second server.
        final int redirectingPort = RandomPortGenerator.get();
        redirectingServer = HttpServer.create(new InetSocketAddress(redirectingPort), 0);
        redirectingServer.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + secondPort + "/");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        redirectingServer.start();
        final URL firstUrl =
                URI.create("http://127.0.0.1:" + redirectingPort + "/").toURL();

        // Retrieving through the retriever must not chase the redirect: it fails on the 302 instead of
        // fetching the second server's body, and the second server is never contacted.
        assertThatThrownBy(() -> OidcServiceImpl.resourceRetriever(config()).retrieveResource(firstUrl))
                .isInstanceOf(java.io.IOException.class);
        assertThat(secondServerHits).hasValue(0);
    }

    private static OidcConfiguration config() {
        return new OidcConfiguration(
                URI.create("https://idp.example.com"),
                "edge-client",
                null,
                URI.create("https://edge.example.com/callback"),
                "roles",
                List.of(),
                Map.of(),
                Set.of("RS256"));
    }
}
