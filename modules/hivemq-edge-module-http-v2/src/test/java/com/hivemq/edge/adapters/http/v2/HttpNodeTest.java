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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import java.util.List;
import org.junit.jupiter.api.Test;

class HttpNodeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesAFullNodeStringViaAPlainObjectMapper() throws Exception {
        final String nodeString = "{\"url\":\"https://example.org/api\",\"httpRequestMethod\":\"POST\","
                + "\"httpRequestTimeoutSeconds\":12,\"httpRequestBodyContentType\":\"JSON\","
                + "\"httpRequestBody\":\"{\\\"a\\\":1}\",\"httpHeaders\":[{\"name\":\"X-Token\",\"value\":\"abc\"}]}";

        final HttpNode node = objectMapper.readValue(nodeString, HttpNode.class);

        assertThat(node.url()).isEqualTo("https://example.org/api");
        assertThat(node.httpRequestMethod()).isEqualTo(HttpMethod.POST);
        assertThat(node.httpRequestTimeoutSeconds()).isEqualTo(12);
        assertThat(node.httpRequestBodyContentType()).isEqualTo(HttpContentType.JSON);
        assertThat(node.httpRequestBody()).isEqualTo("{\"a\":1}");
        assertThat(node.httpHeaders()).containsExactly(new HttpHeader("X-Token", "abc"));
    }

    @Test
    void appliesDefaultsForAbsentOptionalFields() throws Exception {
        final HttpNode node = objectMapper.readValue("{\"url\":\"https://example.org\"}", HttpNode.class);

        assertThat(node.httpRequestMethod()).isEqualTo(HttpMethod.GET);
        assertThat(node.httpRequestBodyContentType()).isEqualTo(HttpContentType.JSON);
        assertThat(node.httpRequestTimeoutSeconds()).isEqualTo(HttpNode.DEFAULT_TIMEOUT_SECONDS);
        assertThat(node.httpRequestBody()).isNull();
        assertThat(node.httpHeaders()).isEmpty();
    }

    @Test
    void clampsAnOversizedTimeoutToTheCeiling() {
        final HttpNode node =
                new HttpNode("https://example.org", HttpMethod.GET, 9999, HttpContentType.JSON, null, List.of());

        assertThat(node.httpRequestTimeoutSeconds()).isEqualTo(HttpNode.MAX_TIMEOUT_SECONDS);
    }

    @Test
    void isUniqueAndTypedAndIdentifiedByItsUrl() {
        final HttpNode node =
                new HttpNode("https://example.org/data", HttpMethod.GET, 5, HttpContentType.JSON, null, List.of());

        assertThat(node.properties()).containsExactlyInAnyOrder(NodeProperty.UNIQUE, NodeProperty.TYPED);
        assertThat(node.nodeId()).isEqualTo("https://example.org/data");
    }

    @Test
    void nodeStringRoundTrips() throws Exception {
        final HttpNode node = new HttpNode(
                "https://example.org/api",
                HttpMethod.PUT,
                30,
                HttpContentType.XML,
                "<a/>",
                List.of(new HttpHeader("Accept", "application/xml")));

        final HttpNode reparsed = objectMapper.readValue(node.nodeString(), HttpNode.class);

        assertThat(reparsed.url()).isEqualTo(node.url());
        assertThat(reparsed.httpRequestMethod()).isEqualTo(HttpMethod.PUT);
        assertThat(reparsed.httpRequestBodyContentType()).isEqualTo(HttpContentType.XML);
        assertThat(reparsed.httpRequestBody()).isEqualTo("<a/>");
        assertThat(reparsed.httpHeaders()).containsExactly(new HttpHeader("Accept", "application/xml"));
    }
}
