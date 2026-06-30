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
package com.hivemq.protocols.v2.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.AnySchema;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Validates an adapter instance's configuration against the owning type's configuration schema before the adapter is
 * constructed, so an adapter never sees a configuration the framework has not checked against the schema it
 * advertises. The reused configuration {@link Schema} is projected to JSON-Schema through the reused
 * {@link SchemaJsonRepresentation} (the same projection the v2 API serves) and the configuration is validated with
 * the JSON-Schema validator already on the classpath.
 * <p>
 * An unconstrained {@link AnySchema} accepts any configuration, so a type that does not constrain its configuration
 * is validated trivially. A configuration that does not match its type's schema is reported as a
 * {@link ProtocolAdapterConfigException}, which the manager surfaces as a clear {@code ERROR} registry handle.
 */
final class AdapterConfigurationSchemaValidator {

    // The projection emits draft 2019-09; validate against the same dialect.
    private static final @NotNull JsonSchemaFactory SCHEMA_FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);

    private AdapterConfigurationSchemaValidator() {}

    /**
     * @param adapterId            the adapter instance id, for the failure message.
     * @param adapterConfiguration the instance configuration to validate.
     * @param adapterConfigSchema  the owning type's configuration schema.
     * @param objectMapper         the mapper used to render the configuration as JSON for validation.
     * @throws ProtocolAdapterConfigException if the configuration does not match the schema.
     */
    static void validate(
            final @NotNull String adapterId,
            final @NotNull Map<String, Object> adapterConfiguration,
            final @NotNull Schema adapterConfigSchema,
            final @NotNull ObjectMapper objectMapper) {
        if (adapterConfigSchema instanceof AnySchema) {
            // An unconstrained schema accepts any configuration; nothing to validate.
            return;
        }
        final ObjectNode jsonSchemaNode = SchemaJsonRepresentation.INSTANCE.toJsonSchema(adapterConfigSchema);
        final JsonSchema jsonSchema = SCHEMA_FACTORY.getSchema(jsonSchemaNode);
        final JsonNode configurationNode = objectMapper.valueToTree(adapterConfiguration);
        final Set<ValidationMessage> failures = jsonSchema.validate(configurationNode);
        if (!failures.isEmpty()) {
            final String detail = failures.stream()
                    .map(ValidationMessage::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new ProtocolAdapterConfigException(
                    "adapter [" + adapterId + "] configuration does not match its type's schema: " + detail);
        }
    }
}
