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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.util.List;
import java.util.stream.Collectors;

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
    private @Nullable JsonSchema schema;

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

    public synchronized @NotNull JsonSchema generateSchema() {
        if (schema == null) {
            final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            schema = factory.getSchema(generateSchemaNode());
            schema.initializeValidators();
        }
        return schema;
    }

    public @NotNull List<ProtocolAdapterValidationFailure> validateObject(final @NotNull Object o) {
        Preconditions.checkNotNull(o);
        final JsonNode node;
        if (o instanceof JsonNode) {
            node = (JsonNode) o;
        } else {
            node = objectMapper.valueToTree(o);
        }
        return generateSchema().validate(node)
                .stream()
                .map(ProtocolAdapterSchemaManager::convertMessage)
                .collect(Collectors.toList());
    }


    static ProtocolAdapterValidationFailure convertMessage(final @NotNull ValidationMessage validationMessage) {
        return new ProtocolAdapterValidationFailureImpl(validationMessage.getMessage(),
                validationMessage.getEvaluationPath().toString(),
                validationMessage.getClass());
    }


}
