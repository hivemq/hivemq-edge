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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adapter type descriptor for BACnet/IP.
 *
 * <p>DEVICE-TAG {@code id} format: {@code deviceNumber::objectNumber/type/property} — e.g.
 * {@code 1001::0/analog-input/present-value}. The full id string is stored as-is in the definition map under key
 * {@code "address"}.
 */
public class BacNetIpTypeDescriptor implements AdapterTypeDescriptor {

    @Override
    public @NotNull String sourceType() {
        return "bacnetip";
    }

    @Override
    public @NotNull String protocolId() {
        return "bacnetip";
    }

    @Override
    public @NotNull Map<String, Object> buildDefinition(final @NotNull SourceDeviceTag deviceTag) {
        final Map<String, Object> definition = new HashMap<>();
        definition.put("address", deviceTag.id != null ? deviceTag.id : "");
        // Pass through any extra fields declared in the DEVICE-TAG
        definition.putAll(deviceTag.extra);
        return definition;
    }

    /**
     * Expands the BACnet/IP connection config with all documented defaults so the compiled JSON is fully explicit.
     *
     * <p>The {@code id} field is populated from the adapter id. {@code deviceId} defaults to {@code 0} (Edge assigns a
     * random device ID). {@code subnetBroadcastAddress} defaults to {@code 255.255.255.255}. {@code
     * discoveryIntervalMillis} defaults to {@code 5000}. All {@code bacnetipToMqtt} sub-fields are expanded.
     */
    @Override
    public @NotNull Map<String, Object> buildConnectionConfig(
            final @NotNull Map<String, Object> source, final @Nullable String adapterId) {
        final Map<String, Object> result = new LinkedHashMap<>();

        result.put("id", adapterId != null ? adapterId : "");
        result.put("host", source.get("host"));
        result.put("port", source.get("port"));
        result.put("deviceId", source.getOrDefault("deviceId", 0));
        result.put("subnetBroadcastAddress", source.getOrDefault("subnetBroadcastAddress", "255.255.255.255"));
        result.put("discoveryIntervalMillis", source.getOrDefault("discoveryIntervalMillis", 5000));

        final Map<String, Object> srcToMqtt = nested(source, "bacnetipToMqtt");
        final Map<String, Object> toMqtt = new LinkedHashMap<>();
        toMqtt.put("pollingIntervalMillis", srcToMqtt.getOrDefault("pollingIntervalMillis", 1000));
        toMqtt.put("maxPollingErrorsBeforeRemoval", srcToMqtt.getOrDefault("maxPollingErrorsBeforeRemoval", 10));
        toMqtt.put("publishChangedDataOnly", srcToMqtt.getOrDefault("publishChangedDataOnly", true));
        result.put("bacnetipToMqtt", toMqtt);

        return result;
    }

    @Override
    public void validateConnectionConfig(
            final @NotNull Map<String, Object> connection,
            final @NotNull SourceFile manifest,
            final @NotNull DiagnosticCollector errors) {
        if (!connection.containsKey("host")) {
            errors.add(Diagnostic.error(
                    "ADAPTER_MISSING_CONNECTION_FIELD",
                    "BACnet/IP adapter '" + manifest.id + "' is missing required connection field 'host'",
                    manifest.path,
                    Map.of("adapterId", manifest.id != null ? manifest.id : "", "field", "host")));
        }
        if (!connection.containsKey("port")) {
            errors.add(Diagnostic.error(
                    "ADAPTER_MISSING_CONNECTION_FIELD",
                    "BACnet/IP adapter '" + manifest.id + "' is missing required connection field 'port'",
                    manifest.path,
                    Map.of("adapterId", manifest.id != null ? manifest.id : "", "field", "port")));
        }
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Map<String, Object> nested(
            final @NotNull Map<String, Object> parent, final @NotNull String key) {
        final Object v = parent.get(key);
        return v instanceof Map<?, ?> ? (Map<String, Object>) v : Map.of();
    }
}
