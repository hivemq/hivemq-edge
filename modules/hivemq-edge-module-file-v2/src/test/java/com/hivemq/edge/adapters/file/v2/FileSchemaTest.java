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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import org.junit.jupiter.api.Test;

class FileSchemaTest {

    private final FileProtocolAdapterFactory factory = new FileProtocolAdapterFactory();

    @Test
    void nodeDefinitionSchemaProjectsTheTwoRequiredStringFields() {
        final ObjectNode json = SchemaJsonRepresentation.INSTANCE.toJsonSchema(factory.nodeDefinitionSchema());

        assertThat(json.get("type").asText()).isEqualTo("object");
        assertThat(json.get("properties").has("filePath")).isTrue();
        assertThat(json.get("properties").get("filePath").get("type").asText()).isEqualTo("string");
        assertThat(json.get("properties").has("contentType")).isTrue();
        assertThat(json.get("properties").get("contentType").get("type").asText())
                .isEqualTo("string");
        assertThat(json.get("required")).extracting(node -> node.asText()).containsExactly("filePath", "contentType");
    }

    @Test
    void adapterConfigSchemaProjectsAnEmptyClosedObject() {
        final ObjectNode json = SchemaJsonRepresentation.INSTANCE.toJsonSchema(factory.adapterConfigSchema());

        assertThat(json.get("type").asText()).isEqualTo("object");
        assertThat(json.get("properties")).isEmpty();
        assertThat(json.get("additionalProperties").asBoolean()).isFalse();
    }
}
