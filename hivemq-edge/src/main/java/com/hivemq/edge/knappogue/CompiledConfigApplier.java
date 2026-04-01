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
package com.hivemq.edge.knappogue;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.MqttUserPropertyEntity;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.edge.compiler.lib.model.CompiledAdapterConfig;
import com.hivemq.edge.compiler.lib.model.CompiledConfig;
import com.hivemq.edge.compiler.lib.model.CompiledNorthboundMapping;
import com.hivemq.edge.compiler.lib.model.CompiledTag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates a {@link CompiledConfig} (from the edge-compiler) into Edge's internal entity model and applies it via
 * the hot-reload path.
 *
 * <p>Only the adapter list is replaced. All other Edge config (listeners, MQTT settings, bridges, UNS, etc.) is left
 * intact — partial-replace semantics.
 */
public class CompiledConfigApplier {

    private static final @NotNull Logger log = LoggerFactory.getLogger(CompiledConfigApplier.class);

    private final @NotNull ConfigFileReaderWriter configFileReaderWriter;

    public CompiledConfigApplier(final @NotNull ConfigFileReaderWriter configFileReaderWriter) {
        this.configFileReaderWriter = configFileReaderWriter;
    }

    /**
     * Applies the compiled config to the running Edge instance.
     *
     * @return true if the config was applied successfully
     */
    public boolean apply(final @NotNull CompiledConfig compiledConfig) {
        if (!CompiledConfig.NOTICE.equals(compiledConfig.notice())) {
            log.error(
                    "Compiled config has unexpected _notice field: '{}'. Refusing to apply.", compiledConfig.notice());
            return false;
        }

        final List<ProtocolAdapterEntity> translatedAdapters = compiledConfig.protocolAdapters().stream()
                .map(this::translateAdapter)
                .toList();

        if (!compiledConfig.dataCombiners().isEmpty()) {
            log.warn(
                    "Compiled config contains {} data combiner(s) — data combiner translation is not yet implemented and will be skipped.",
                    compiledConfig.dataCombiners().size());
        }

        final HiveMQConfigEntity currentEntity = configFileReaderWriter.getCurrentConfigEntity();
        currentEntity.getProtocolAdapterConfig().clear();
        currentEntity.getProtocolAdapterConfig().addAll(translatedAdapters);

        final boolean success = configFileReaderWriter.applyCompiledConfig(currentEntity);
        if (success) {
            log.info(
                    "Applied compiled config: {} adapter(s) from edge version '{}'.",
                    translatedAdapters.size(),
                    compiledConfig.edgeVersion());
            configFileReaderWriter.writeConfigWithSync();
        } else {
            log.error("Failed to apply compiled config — see above for details.");
        }
        return success;
    }

    private @NotNull ProtocolAdapterEntity translateAdapter(final @NotNull CompiledAdapterConfig src) {
        final List<TagEntity> tags = src.tags().stream()
                .map(tag -> translateTag(tag, src.protocolId()))
                .toList();
        final List<NorthboundMappingEntity> northbound = src.northboundMappings().stream()
                .map(this::translateNorthboundMapping)
                .toList();

        return new ProtocolAdapterEntity(
                src.adapterId(),
                src.protocolId(),
                1, // configVersion default — compiler omits this field
                expandAdapterConfig(src.config(), src.protocolId()),
                northbound,
                List.of(), // southboundMappings — empty for POC
                tags);
    }

    private @NotNull TagEntity translateTag(final @NotNull CompiledTag src, final @NotNull String protocolId) {
        return new TagEntity(src.name(), src.description(), expandDefinition(src.definition(), protocolId));
    }

    /**
     * Expands a compiled adapter config into the runtime format expected by the adapter.
     *
     * <p>OPC-UA: synthesizes the {@code uri} field from {@code protocol} (default {@code opc.tcp}), {@code host}, and
     * {@code port}, then removes those three source-only fields so the runtime config matches the XML format exactly.
     */
    private @NotNull Map<String, Object> expandAdapterConfig(
            final @NotNull Map<String, Object> config, final @NotNull String protocolId) {
        return switch (protocolId.toLowerCase(Locale.ROOT)) {
            case "opcua" -> expandOpcUaConfig(config);
            default -> config;
        };
    }

    private @NotNull Map<String, Object> expandOpcUaConfig(final @NotNull Map<String, Object> config) {
        final Map<String, Object> result = new LinkedHashMap<>(config);
        final Object host = result.remove("host");
        final Object port = result.remove("port");
        final Object protocol = result.remove("protocol");
        if (host instanceof String hostStr && port != null) {
            final String scheme = (protocol instanceof String s && !s.isBlank()) ? s : "opc.tcp";
            result.put("uri", scheme + "://" + hostStr + ":" + port);
        } else {
            log.warn(
                    "OPC-UA adapter config is missing 'host' or 'port' — cannot synthesize URI; adapter may not connect.");
        }
        return result;
    }

    /**
     * Expands a compiled tag definition into the runtime format expected by the adapter.
     *
     * <p>OPC-UA: the compiled {@code {node: "..."}} definition is already in the correct runtime format — pass through.
     * <p>BACnet/IP: the compiled {@code {address: "device::object/type/property"}} string is expanded into the
     * structured fields ({@code deviceInstanceNumber}, {@code objectInstanceNumber}, {@code objectType},
     * {@code propertyType}) that the BACnet runtime adapter expects.
     */
    private @NotNull Map<String, Object> expandDefinition(
            final @NotNull Map<String, Object> compiled, final @NotNull String protocolId) {
        return switch (protocolId.toLowerCase(Locale.ROOT)) {
            case "bacnetip" -> expandBacNetIpDefinition(compiled);
            default -> compiled;
        };
    }

    /**
     * Parses a BACnet address string of the form {@code deviceNumber::objectNumber/type/property} into the structured
     * definition map used by the BACnet runtime adapter.
     *
     * <p>Example: {@code "1234::0/analog-input/present-value"} →
     * {@code {deviceInstanceNumber: 1234, objectInstanceNumber: 0, objectType: "ANALOG_INPUT",
     * propertyType: "PRESENT_VALUE"}}.
     */
    private @NotNull Map<String, Object> expandBacNetIpDefinition(final @NotNull Map<String, Object> compiled) {
        final Object rawAddress = compiled.get("address");
        if (!(rawAddress instanceof String address)) {
            log.warn("BACnet tag definition has no 'address' field — passing definition through as-is.");
            return compiled;
        }
        final int sepIndex = address.indexOf("::");
        if (sepIndex < 0) {
            log.warn("BACnet address '{}' is missing '::' separator — passing through as-is.", address);
            return compiled;
        }
        final String[] remainder = address.substring(sepIndex + 2).split("/", 3);
        if (remainder.length != 3) {
            log.warn(
                    "BACnet address '{}' does not match expected 'device::object/type/property' format — passing through as-is.",
                    address);
            return compiled;
        }
        try {
            return Map.of(
                    "deviceInstanceNumber", Integer.parseInt(address.substring(0, sepIndex)),
                    "objectInstanceNumber", Integer.parseInt(remainder[0]),
                    "objectType", remainder[1].replace("-", "_").toUpperCase(Locale.ROOT),
                    "propertyType", remainder[2].replace("-", "_").toUpperCase(Locale.ROOT));
        } catch (final NumberFormatException e) {
            log.warn(
                    "BACnet address '{}' contains a non-integer device or object number — passing through as-is.",
                    address);
            return compiled;
        }
    }

    private @NotNull NorthboundMappingEntity translateNorthboundMapping(final @NotNull CompiledNorthboundMapping src) {
        final List<MqttUserPropertyEntity> userProperties = src.userProperties().stream()
                .map(p -> new MqttUserPropertyEntity(p.name(), p.value()))
                .toList();
        return new NorthboundMappingEntity(
                src.tagName(),
                src.topic(),
                src.maxQos(),
                null, // messageHandlingOptions — ignored by NorthboundMappingEntity
                src.includeTagNames(),
                src.includeTimestamp(),
                src.includeMetadata(),
                userProperties,
                src.messageExpiryInterval());
    }
}
