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

import com.hivemq.adapter.sdk.api.schema.ObjectSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * The factory for the v2 File adapter type. It is discovered by the module loader through the
 * {@code META-INF/services} declaration and instantiated with its no-argument constructor. It exposes the type
 * identity, constructs a {@link FileProtocolAdapter} per configured instance, and advertises the reused v1
 * {@link Schema}s the framework validates the instance configuration and projects the node definition against.
 */
public final class FileProtocolAdapterFactory implements ProtocolAdapterFactory {

    // The File adapter has no adapter-level settings, so the configuration is an empty object that accepts nothing
    // beyond an empty configuration section.
    private static final @NotNull Schema ADAPTER_CONFIG_SCHEMA =
            new ObjectSchema(Map.of(), List.of(), false, "File adapter configuration", null, false, true, false);

    private static final @NotNull Schema NODE_DEFINITION_SCHEMA = buildNodeDefinitionSchema();

    @Override
    public @NotNull ProtocolAdapterInformation information() {
        return FileProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull FileProtocolAdapter createAdapter(
            final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
        return new FileProtocolAdapter(input, output);
    }

    @Override
    public @NotNull Schema adapterConfigSchema() {
        return ADAPTER_CONFIG_SCHEMA;
    }

    @Override
    public @NotNull Schema nodeDefinitionSchema() {
        return NODE_DEFINITION_SCHEMA;
    }

    private static @NotNull Schema buildNodeDefinitionSchema() {
        final Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put(
                "filePath",
                new ScalarSchema(
                        ScalarType.STRING,
                        null,
                        null,
                        "The file path",
                        "The absolute path to the file that should be read.",
                        false,
                        true,
                        false));
        // The reused v1 Schema has no enum constraint, so the content type projects as a plain string; the accepted
        // values are the names of the FileContentType constants.
        properties.put(
                "contentType",
                new ScalarSchema(
                        ScalarType.STRING,
                        null,
                        null,
                        "Content Type",
                        "The type of the content within the file (one of BINARY, TEXT_PLAIN, TEXT_JSON, TEXT_XML,"
                                + " TEXT_CSV).",
                        false,
                        true,
                        false));
        return new ObjectSchema(
                properties,
                List.of("filePath", "contentType"),
                false,
                "File node definition",
                null,
                false,
                true,
                false);
    }
}
