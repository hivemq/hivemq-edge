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
package com.hivemq.protocols;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.api.json.CustomConfigSchemaGenerator;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterValidationFailure;
import com.hivemq.edge.modules.api.adapters.model.ProtocolAdapterValidationFailureImpl;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encapsulate the management of the schema, and ensure we internally managed API instances to decouple from
 * schema parser
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterSchemaManager {
    private final @NotNull Class<? extends ProtocolSpecificAdapterConfig> configBean;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull CustomConfigSchemaGenerator customConfigSchemaGenerator;
    private @Nullable JsonNode schemaNode;
    private @Nullable Schema schema;

    public ProtocolAdapterSchemaManager(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull Class<? extends ProtocolSpecificAdapterConfig> configBean) {
        this.objectMapper = objectMapper;
        this.configBean = configBean;
        this.customConfigSchemaGenerator = new CustomConfigSchemaGenerator();
    }

    public synchronized @NotNull JsonNode generateSchemaNode() {
        if (schemaNode == null) {
            schemaNode = customConfigSchemaGenerator.generateJsonSchema(configBean);
        }
        return schemaNode;
    }

    public synchronized @NotNull Schema generateSchema() {
        if (schema == null) {
            // the validator parses with Jackson 3, so the Jackson 2 tree is handed over as JSON text
            schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                    .getSchema(generateSchemaNode().toString(), InputFormat.JSON);
            schema.initializeValidators();
        }
        return schema;
    }

    public @NotNull List<ProtocolAdapterValidationFailure> validateObject(final @NotNull Object o) {
        Preconditions.checkNotNull(o);
        final JsonNode node;
        if (o instanceof JsonNode jsonNode) {
            node = jsonNode;
        } else {
            node = objectMapper.valueToTree(o);
        }
        return generateSchema().validate(node.toString(), InputFormat.JSON).stream()
                .map(ProtocolAdapterSchemaManager::convertMessage)
                .collect(Collectors.toList());
    }

    static ProtocolAdapterValidationFailure convertMessage(final @NotNull Error error) {
        return new ProtocolAdapterValidationFailureImpl(
                error.getMessage(), error.getEvaluationPath().toString(), error.getClass());
    }
}
