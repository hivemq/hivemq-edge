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
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adapter type descriptor for OPC-UA.
 *
 * <p>DEVICE-TAG {@code id} maps to {@code definition.node} — matching {@code OpcuaTagDefinition.node}.
 *
 * <p>The connection block must declare {@code host} and {@code port}. An optional {@code protocol} field sets the
 * URI scheme (default: {@code opc.tcp}). The full {@code uri} is synthesized by the applier from these fields —
 * declaring {@code uri} directly in the connection block is an error.
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

    /**
     * Expands the OPC-UA connection config with all documented defaults so the compiled JSON is fully explicit.
     *
     * <p>The {@code id} field is populated from the adapter id. {@code protocol} defaults to {@code opc.tcp}. All
     * {@code connectionOptions}, {@code opcuaToMqtt}, {@code security}, and {@code tls} sub-fields are expanded.
     * Unknown source fields are intentionally dropped — the compiler owns the complete schema.
     *
     * <p>{@code host} and {@code port} are preserved as-is; the applier synthesizes {@code uri} from them and strips
     * these source-only fields before applying the config to Edge.
     */
    @Override
    public @NotNull Map<String, Object> buildConnectionConfig(
            final @NotNull Map<String, Object> source, final @Nullable String adapterId) {
        final Map<String, Object> result = new LinkedHashMap<>();

        result.put("id", adapterId != null ? adapterId : "");
        result.put("host", source.get("host"));
        result.put("port", source.get("port"));
        result.put("protocol", source.getOrDefault("protocol", "opc.tcp"));
        result.put("overrideUri", source.getOrDefault("overrideUri", false));
        result.put("applicationUri", source.getOrDefault("applicationUri", ""));

        final Map<String, Object> srcToMqtt = nested(source, "opcuaToMqtt");
        final Map<String, Object> toMqtt = new LinkedHashMap<>();
        toMqtt.put("serverQueueSize", srcToMqtt.getOrDefault("serverQueueSize", 1));
        toMqtt.put("publishingInterval", srcToMqtt.getOrDefault("publishingInterval", 1000));
        result.put("opcuaToMqtt", toMqtt);

        final Map<String, Object> srcSec = nested(source, "security");
        final Map<String, Object> security = new LinkedHashMap<>();
        security.put("policy", srcSec.getOrDefault("policy", "NONE"));
        security.put("messageSecurityMode", srcSec.getOrDefault("messageSecurityMode", "NONE"));
        result.put("security", security);

        final Map<String, Object> srcTls = nested(source, "tls");
        final Map<String, Object> tls = new LinkedHashMap<>();
        tls.put("enabled", srcTls.getOrDefault("enabled", false));
        tls.put("tlsChecks", srcTls.getOrDefault("tlsChecks", "STANDARD"));
        result.put("tls", tls);

        final Map<String, Object> srcOpts = nested(source, "connectionOptions");
        final Map<String, Object> connOpts = new LinkedHashMap<>();
        connOpts.put("reconnectOnServiceFault", srcOpts.getOrDefault("reconnectOnServiceFault", true));
        connOpts.put("requestTimeoutMs", srcOpts.getOrDefault("requestTimeoutMs", 30000));
        connOpts.put("keepAliveIntervalMs", srcOpts.getOrDefault("keepAliveIntervalMs", 10000));
        connOpts.put("connectionTimeoutMs", srcOpts.getOrDefault("connectionTimeoutMs", 30000));
        connOpts.put("healthCheckIntervalMs", srcOpts.getOrDefault("healthCheckIntervalMs", 30000));
        connOpts.put(
                "retryIntervalMs",
                srcOpts.getOrDefault("retryIntervalMs", "1000,2000,4000,8000,16000,32000,64000,128000,256000,300000"));
        connOpts.put("keepAliveFailuresAllowed", srcOpts.getOrDefault("keepAliveFailuresAllowed", 3));
        connOpts.put("sessionTimeoutMs", srcOpts.getOrDefault("sessionTimeoutMs", 120000));
        connOpts.put("autoReconnect", srcOpts.getOrDefault("autoReconnect", true));
        result.put("connectionOptions", connOpts);

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
        if (connection.containsKey("uri")) {
            errors.add(Diagnostic.error(
                    "ADAPTER_INVALID_CONNECTION_FIELD",
                    "OPC-UA adapter '"
                            + manifest.id
                            + "' must not declare 'uri' — the URI is synthesized from 'host' and 'port'",
                    manifest.path,
                    Map.of("adapterId", manifest.id != null ? manifest.id : "", "field", "uri")));
        }
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Map<String, Object> nested(
            final @NotNull Map<String, Object> parent, final @NotNull String key) {
        final Object v = parent.get(key);
        return v instanceof Map<?, ?> ? (Map<String, Object>) v : Map.of();
    }
}
