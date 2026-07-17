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
package com.hivemq.edge.adapters.databases.v2;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import org.junit.jupiter.api.Test;

class DatabasesSchemaTest {

    private final DatabasesProtocolAdapterFactory factory = new DatabasesProtocolAdapterFactory();

    @Test
    void adapterConfigSchemaProjectsTheConnectionParameters() {
        final ObjectNode json = SchemaJsonRepresentation.INSTANCE.toJsonSchema(factory.adapterConfigSchema());

        assertThat(json.get("type").asText()).isEqualTo("object");
        assertThat(json.get("properties").get("type").get("type").asText()).isEqualTo("string");
        assertThat(json.get("properties").get("server").get("type").asText()).isEqualTo("string");
        assertThat(json.get("properties").get("database").get("type").asText()).isEqualTo("string");
        assertThat(json.get("properties").get("username").get("type").asText()).isEqualTo("string");
        assertThat(json.get("properties").get("password").get("type").asText()).isEqualTo("string");
        assertThat(json.get("properties").get("encrypt").get("type").asText()).isEqualTo("boolean");
        assertThat(json.get("properties").get("trustCertificate").get("type").asText())
                .isEqualTo("boolean");
        assertThat(json.get("additionalProperties").asBoolean()).isFalse();
    }

    @Test
    void thePortProjectsAsABoundedInteger() {
        final ObjectNode json = SchemaJsonRepresentation.INSTANCE.toJsonSchema(factory.adapterConfigSchema());

        final ObjectNode port = (ObjectNode) json.get("properties").get("port");
        assertThat(port.get("type").asText()).isEqualTo("integer");
        assertThat(port.get("minimum").asLong()).isEqualTo(1);
        assertThat(port.get("maximum").asLong()).isEqualTo(65536);
    }

    @Test
    void theConnectionTimeoutProjectsWithItsCeiling() {
        final ObjectNode json = SchemaJsonRepresentation.INSTANCE.toJsonSchema(factory.adapterConfigSchema());

        final ObjectNode timeout = (ObjectNode) json.get("properties").get("connectionTimeoutSeconds");
        assertThat(timeout.get("type").asText()).isEqualTo("integer");
        assertThat(timeout.get("maximum").asLong()).isEqualTo(181);
    }

    @Test
    void theConnectionParametersAreRequiredButTheOptionsAreNot() {
        final ObjectNode json = SchemaJsonRepresentation.INSTANCE.toJsonSchema(factory.adapterConfigSchema());

        assertThat(json.get("required"))
                .extracting(node -> node.asText())
                .containsExactly("type", "server", "port", "database", "username", "password");
    }

    @Test
    void nodeDefinitionSchemaProjectsTheQueryAndTheSplitFlag() {
        final ObjectNode json = SchemaJsonRepresentation.INSTANCE.toJsonSchema(factory.nodeDefinitionSchema());

        assertThat(json.get("type").asText()).isEqualTo("object");
        assertThat(json.get("properties").get("query").get("type").asText()).isEqualTo("string");
        assertThat(json.get("properties")
                        .get("spiltLinesInIndividualMessages")
                        .get("type")
                        .asText())
                .isEqualTo("boolean");
        assertThat(json.get("required")).extracting(node -> node.asText()).containsExactly("query");
        assertThat(json.get("additionalProperties").asBoolean()).isFalse();
    }
}
