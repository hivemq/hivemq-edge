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
import org.jetbrains.annotations.Nullable;

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

    /**
     * Builds the complete connection config map for the compiled output by merging the source fields with
     * adapter-type-specific defaults.
     *
     * <p>The compiled config is fully explicit — all optional fields are expanded to their documented defaults so that
     * neither the applier nor Edge itself needs to apply any defaults.
     *
     * <p>The default implementation returns the source map unchanged. Type descriptors that know their adapter's full
     * schema override this to produce a complete map.
     *
     * @param source the {@code connection:} map from the adapter manifest (validated, non-null)
     * @param adapterId the adapter's id — used to populate the {@code id} field that Edge requires internally
     */
    default @NotNull Map<String, Object> buildConnectionConfig(
            @NotNull Map<String, Object> source, @Nullable String adapterId) {
        return source;
    }
}
