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
import com.hivemq.edge.compiler.source.validation.Diagnostic;
import com.hivemq.edge.compiler.source.validation.DiagnosticCollector;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Adapter type descriptor for OPC-UA.
 *
 * <p>DEVICE-TAG {@code id} maps to {@code definition.node} — matching {@code OpcuaTagDefinition.node}.
 */
public class OpcUaTypeDescriptor implements AdapterTypeDescriptor {

    @Override
    public @NotNull String sourceType() {
        return "opcua";
    }

    @Override
    public @NotNull String protocolId() {
        return "opcua";
    }

    @Override
    public @NotNull Map<String, Object> buildDefinition(final @NotNull SourceDeviceTag deviceTag) {
        return Map.of("node", deviceTag.id != null ? deviceTag.id : "");
    }

    @Override
    public void validateConnectionConfig(
            final @NotNull Map<String, Object> connection,
            final @NotNull SourceFile manifest,
            final @NotNull DiagnosticCollector errors) {
        if (!connection.containsKey("host")) {
            errors.add(Diagnostic.error(
                    "ADAPTER_MISSING_CONNECTION_FIELD",
                    "OPC-UA adapter '" + manifest.id + "' is missing required connection field 'host'",
                    manifest.path,
                    Map.of("adapterId", manifest.id != null ? manifest.id : "", "field", "host")));
        }
        if (!connection.containsKey("port")) {
            errors.add(Diagnostic.error(
                    "ADAPTER_MISSING_CONNECTION_FIELD",
                    "OPC-UA adapter '" + manifest.id + "' is missing required connection field 'port'",
                    manifest.path,
                    Map.of("adapterId", manifest.id != null ? manifest.id : "", "field", "port")));
        }
    }
}
