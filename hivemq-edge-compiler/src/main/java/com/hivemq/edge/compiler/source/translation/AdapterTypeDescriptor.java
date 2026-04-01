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
package com.hivemq.edge.compiler.source.translation;

import com.hivemq.edge.compiler.source.model.SourceDeviceTag;
import com.hivemq.edge.compiler.source.model.SourceFile;
import com.hivemq.edge.compiler.source.validation.DiagnosticCollector;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Per-adapter-type knowledge needed by the compiler: how to map YAML source fields to the compiled output model.
 */
public interface AdapterTypeDescriptor {

    /** The type discriminator string as declared in the adapter manifest (e.g. {@code "opcua"}). */
    @NotNull
    String sourceType();

    /** The protocol id string used internally by Edge (e.g. {@code "opcua"}). */
    @NotNull
    String protocolId();

    /**
     * Builds the {@code definition} map for a TAG from its DEVICE-TAG.
     *
     * <p>The definition map is the protocol-specific opaque map stored in {@code TagEntity.definition} inside Edge.
     */
    @NotNull
    Map<String, Object> buildDefinition(@NotNull SourceDeviceTag deviceTag);

    /**
     * Validates the adapter's connection config and adds any errors to the collector.
     *
     * @param connection the {@code connection:} map from the adapter manifest
     * @param manifest the full manifest file (for error location)
     * @param errors diagnostics collector
     */
    void validateConnectionConfig(
            @NotNull Map<String, Object> connection, @NotNull SourceFile manifest, @NotNull DiagnosticCollector errors);
}
