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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import org.junit.jupiter.api.Test;

class HttpSchemaTest {

    private final HttpProtocolAdapterFactory factory = new HttpProtocolAdapterFactory();

    @Test
    void adapterConfigSchemaProjectsTheFourResponseHandlingFields() {
        final ObjectNode json = SchemaJsonRepresentation.INSTANCE.toJsonSchema(factory.adapterConfigSchema());

        assertThat(json.get("type").asText()).isEqualTo("object");
        final ObjectNode properties = (ObjectNode) json.get("properties");
        assertThat(properties.get("httpConnectTimeoutSeconds").get("type").asText())
                .isEqualTo("integer");
        assertThat(properties.get("httpConnectTimeoutSeconds").get("minimum").asInt())
                .isEqualTo(1);
        assertThat(properties.get("httpConnectTimeoutSeconds").get("maximum").asInt())
                .isEqualTo(60);
        assertThat(properties.get("allowUntrustedCertificates").get("type").asText())
                .isEqualTo("boolean");
        assertThat(properties.get("assertResponseIsJson").get("type").asText()).isEqualTo("boolean");
        assertThat(properties
                        .get("httpPublishSuccessStatusCodeOnly")
                        .get("type")
                        .asText())
                .isEqualTo("boolean");
        assertThat(json.get("additionalProperties").asBoolean()).isFalse();
    }

    @Test
    void nodeDefinitionSchemaProjectsTheSixFieldsWithUrlRequiredAndHeadersAsAnArray() {
        final ObjectNode json = SchemaJsonRepresentation.INSTANCE.toJsonSchema(factory.nodeDefinitionSchema());

        assertThat(json.get("type").asText()).isEqualTo("object");
        final ObjectNode properties = (ObjectNode) json.get("properties");
        assertThat(properties.get("url").get("type").asText()).isEqualTo("string");
        assertThat(properties.get("httpRequestMethod").get("type").asText()).isEqualTo("string");
        assertThat(properties.get("httpRequestTimeoutSeconds").get("type").asText())
                .isEqualTo("integer");
        assertThat(properties.get("httpRequestBodyContentType").get("type").asText())
                .isEqualTo("string");
        assertThat(properties.has("httpRequestBody")).isTrue();
        assertThat(json.get("required")).extracting(node -> node.asText()).containsExactly("url");

        final ObjectNode headers = (ObjectNode) properties.get("httpHeaders");
        assertThat(headers.get("type").asText()).isEqualTo("array");
        final ObjectNode headerItem = (ObjectNode) headers.get("items");
        assertThat(headerItem.get("type").asText()).isEqualTo("object");
        assertThat(headerItem.get("properties").has("name")).isTrue();
        assertThat(headerItem.get("properties").has("value")).isTrue();
    }
}
