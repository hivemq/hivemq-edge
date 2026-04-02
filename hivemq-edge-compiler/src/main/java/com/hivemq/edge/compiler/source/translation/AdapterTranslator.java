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

import com.hivemq.edge.compiler.lib.model.CompiledAdapterConfig;
import com.hivemq.edge.compiler.lib.model.CompiledCombinerMapping;
import com.hivemq.edge.compiler.lib.model.CompiledCombinerOutput;
import com.hivemq.edge.compiler.lib.model.CompiledCombinerTrigger;
import com.hivemq.edge.compiler.lib.model.CompiledConfig;
import com.hivemq.edge.compiler.lib.model.CompiledDataCombiner;
import com.hivemq.edge.compiler.lib.model.CompiledInstruction;
import com.hivemq.edge.compiler.lib.model.CompiledInstructionDestination;
import com.hivemq.edge.compiler.lib.model.CompiledInstructionSource;
import com.hivemq.edge.compiler.lib.model.CompiledNorthboundMapping;
import com.hivemq.edge.compiler.lib.model.CompiledTag;
import com.hivemq.edge.compiler.source.model.SourceDataCombiner;
import com.hivemq.edge.compiler.source.model.SourceDeviceTag;
import com.hivemq.edge.compiler.source.model.SourceNorthboundMapping;
import com.hivemq.edge.compiler.source.resolution.GlobalResolver.ResolvedProject;
import com.hivemq.edge.compiler.source.resolution.ResolvedAdapter;
import com.hivemq.edge.compiler.source.resolution.ResolvedNorthboundMapping;
import com.hivemq.edge.compiler.source.resolution.ResolvedTag;
import com.hivemq.edge.compiler.source.validation.Diagnostic;
import com.hivemq.edge.compiler.source.validation.DiagnosticCollector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Translates a fully resolved project into a {@link CompiledConfig}. */
public class AdapterTranslator {

    private static final long DEFAULT_MESSAGE_EXPIRY_INTERVAL = Long.MAX_VALUE;

    private final @NotNull Map<String, AdapterTypeDescriptor> descriptors;

    public AdapterTranslator(final @NotNull List<AdapterTypeDescriptor> descriptors) {
        this.descriptors =
                descriptors.stream().collect(Collectors.toMap(AdapterTypeDescriptor::sourceType, Function.identity()));
    }

    public AdapterTranslator() {
        this(List.of(new OpcUaTypeDescriptor(), new BacNetIpTypeDescriptor()));
    }

    public @NotNull CompiledConfig translate(
            final @NotNull ResolvedProject project,
            final @NotNull String edgeVersion,
            final @NotNull DiagnosticCollector errors) {

        final List<CompiledAdapterConfig> compiledAdapters = new ArrayList<>();
        for (final ResolvedAdapter adapter : project.adapters()) {
            final @Nullable CompiledAdapterConfig compiled = translateAdapter(adapter, errors);
            if (compiled != null) {
                compiledAdapters.add(compiled);
            }
        }

        final List<CompiledDataCombiner> compiledCombiners = new ArrayList<>();
        for (final SourceDataCombiner combiner : project.dataCombiners()) {
            compiledCombiners.add(translateCombiner(combiner, errors));
        }

        return new CompiledConfig(
                CompiledConfig.NOTICE,
                CompiledConfig.SIGNATURE_UNSIGNED,
                CompiledConfig.FORMAT_VERSION,
                edgeVersion,
                compiledAdapters,
                compiledCombiners);
    }

    private @Nullable CompiledAdapterConfig translateAdapter(
            final @NotNull ResolvedAdapter adapter, final @NotNull DiagnosticCollector errors) {

        final String type = adapter.manifest().type;
        if (type == null || type.isBlank()) {
            errors.add(Diagnostic.error(
                    "ADAPTER_MISSING_TYPE",
                    "Adapter '" + adapter.adapterId() + "' has no 'type' field",
                    adapter.manifest().path));
            return null;
        }

        final @Nullable AdapterTypeDescriptor descriptor = descriptors.get(type.toLowerCase());
        if (descriptor == null) {
            errors.add(Diagnostic.error(
                    "ADAPTER_UNKNOWN_TYPE",
                    "Adapter '" + adapter.adapterId() + "' has unknown type '" + type + "'. Supported types: "
                            + descriptors.keySet(),
                    adapter.manifest().path,
                    Map.of("adapterId", adapter.adapterId(), "type", type)));
            return null;
        }

        final Map<String, Object> connection =
                adapter.manifest().connection != null ? adapter.manifest().connection : Map.of();
        descriptor.validateConnectionConfig(connection, adapter.manifest(), errors);

        final List<CompiledTag> compiledTags = adapter.tags().values().stream()
                .map(tag -> translateTag(tag, descriptor))
                .toList();

        final List<CompiledNorthboundMapping> compiledMappings = adapter.northboundMappings().stream()
                .map(m -> translateNorthboundMapping(m))
                .toList();

        final Map<String, Object> builtConnection = descriptor.buildConnectionConfig(connection, adapter.adapterId());
        return new CompiledAdapterConfig(
                adapter.adapterId(),
                descriptor.protocolId(),
                builtConnection,
                compiledTags,
                compiledMappings,
                List.of());
    }

    private @NotNull CompiledTag translateTag(
            final @NotNull ResolvedTag tag, final @NotNull AdapterTypeDescriptor descriptor) {
        final @Nullable SourceDeviceTag deviceTag = tag.deviceTag();
        final Map<String, Object> definition = deviceTag != null ? descriptor.buildDefinition(deviceTag) : Map.of();
        final String description = deviceTag != null ? deviceTag.description : null;
        return new CompiledTag(tag.name(), description, definition);
    }

    private @NotNull CompiledNorthboundMapping translateNorthboundMapping(
            final @NotNull ResolvedNorthboundMapping mapping) {
        final SourceNorthboundMapping src = mapping.source();
        return new CompiledNorthboundMapping(
                mapping.tagName(),
                src.topic != null ? src.topic : "",
                src.qos,
                src.includeTagNames != null ? src.includeTagNames : false,
                src.includeTimestamp != null ? src.includeTimestamp : true,
                src.includeMetadata != null ? src.includeMetadata : false,
                List.of(),
                src.messageExpiryInterval != null ? src.messageExpiryInterval : DEFAULT_MESSAGE_EXPIRY_INTERVAL);
    }

    private @NotNull CompiledDataCombiner translateCombiner(
            final @NotNull SourceDataCombiner combiner, final @NotNull DiagnosticCollector errors) {
        final String combinerName = combiner.name != null ? combiner.name : "";
        final String combinerId = combiner.id != null
                ? combiner.id
                : UUID.nameUUIDFromBytes(combinerName.getBytes(StandardCharsets.UTF_8))
                        .toString();

        final List<CompiledCombinerMapping> mappings = new ArrayList<>();
        for (int i = 0; i < combiner.mappings.size(); i++) {
            final var m = combiner.mappings.get(i);
            final String mappingName = m.name != null ? m.name : "mapping-" + i;
            final String mappingId = m.id != null
                    ? m.id
                    : UUID.nameUUIDFromBytes((combinerName + "::" + mappingName).getBytes(StandardCharsets.UTF_8))
                            .toString();

            final CompiledCombinerTrigger trigger = m.trigger != null
                    ? new CompiledCombinerTrigger(m.trigger.tag, m.trigger.topic)
                    : new CompiledCombinerTrigger(null, null);

            final CompiledCombinerOutput output;
            if (m.output != null) {
                if (m.output.qos != null) {
                    errors.add(Diagnostic.warning(
                            "COMBINER_OUTPUT_QOS_IGNORED",
                            "Combiner mapping '" + mappingName + "' specifies output qos: " + m.output.qos
                                    + " — the runtime model has no per-output QoS for data combiners; this value is ignored",
                            combiner.sourcePath,
                            Diagnostic.DiagnosticRange.ofNullable(m.line, m.character),
                            Map.of("combinerName", combinerName, "mappingName", mappingName, "qos", m.output.qos)));
                }
                output = new CompiledCombinerOutput(
                        m.output.topic != null ? m.output.topic : "", m.output.qos != null ? m.output.qos : 1);
            } else {
                output = new CompiledCombinerOutput("", 1);
            }

            final List<CompiledInstruction> instructions = m.instructions.stream()
                    .map(instr -> {
                        final CompiledInstructionSource src = instr.source != null
                                ? new CompiledInstructionSource(
                                        instr.source.tag,
                                        instr.source.topic,
                                        instr.source.field != null ? instr.source.field : "")
                                : new CompiledInstructionSource(null, null, "");
                        final CompiledInstructionDestination dst = instr.destination != null
                                ? new CompiledInstructionDestination(
                                        instr.destination.field != null ? instr.destination.field : "")
                                : new CompiledInstructionDestination("");
                        return new CompiledInstruction(src, dst);
                    })
                    .toList();

            mappings.add(
                    new CompiledCombinerMapping(mappingId, mappingName, m.description, trigger, output, instructions));
        }

        return new CompiledDataCombiner(combinerId, combinerName, combiner.description, mappings);
    }
}
