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
package com.hivemq.edge.compiler.source.resolution;

import com.hivemq.edge.compiler.source.model.SourceDeviceTag;
import com.hivemq.edge.compiler.source.model.SourceFile;
import com.hivemq.edge.compiler.source.model.SourceNorthboundMapping;
import com.hivemq.edge.compiler.source.model.SourceTag;
import com.hivemq.edge.compiler.source.validation.Diagnostic;
import com.hivemq.edge.compiler.source.validation.Diagnostic.DiagnosticRange;
import com.hivemq.edge.compiler.source.validation.DiagnosticCollector;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves all references within one adapter directory scope.
 *
 * <p>After resolution:
 * <ul>
 *   <li>Every TAG has a resolved DEVICE-TAG (inline or looked up by id) — or none if standalone
 *   <li>Every NB mapping references a TAG name that exists in the adapter's tag pool
 * </ul>
 */
public class AdapterScopeResolver {

    public @NotNull ResolvedAdapter resolve(
            final @NotNull SourceFile manifest,
            final @NotNull List<SourceFile> allFilesInDirectory,
            final @NotNull DiagnosticCollector errors) {

        final String adapterId = manifest.id != null ? manifest.id : "<unknown>";
        final Path manifestPath = manifest.path;

        // Build DEVICE-TAG pool from all deviceTags: sections in all files in this directory
        final Map<String, SourceDeviceTag> deviceTagPool = new HashMap<>();
        for (final SourceFile file : allFilesInDirectory) {
            for (final SourceDeviceTag dt : file.deviceTags) {
                if (dt.id == null || dt.id.isBlank()) {
                    errors.add(Diagnostic.error(
                            "DEVICE_TAG_MISSING_ID",
                            "DEVICE-TAG in adapter '" + adapterId + "' has no 'id' field",
                            file.path,
                            DiagnosticRange.ofNullable(dt.line, dt.character)));
                    continue;
                }
                if (deviceTagPool.containsKey(dt.id)) {
                    errors.add(Diagnostic.error(
                            "DUPLICATE_DEVICE_TAG_ID",
                            "DEVICE-TAG id '" + dt.id + "' is defined more than once in adapter '" + adapterId + "'",
                            file.path,
                            DiagnosticRange.ofNullable(dt.line, dt.character),
                            Map.of("adapterId", adapterId, "deviceTagId", dt.id)));
                } else {
                    deviceTagPool.put(dt.id, dt);
                }
            }
        }

        // Build TAG pool — also handles inline DEVICE-TAGs and by-id references
        final Map<String, ResolvedTag> tagPool = new HashMap<>();

        for (final SourceFile file : allFilesInDirectory) {
            for (final SourceTag sourceTag : file.tags) {
                resolveTag(sourceTag, adapterId, file.path, deviceTagPool, tagPool, errors);
            }
        }

        // Resolve NB mappings — inline TAGs are extracted first, then by-name refs are resolved
        final List<ResolvedNorthboundMapping> resolvedMappings = new ArrayList<>();
        for (final SourceFile file : allFilesInDirectory) {
            for (final SourceNorthboundMapping mapping : file.northbound) {
                resolveNorthboundMapping(
                        mapping, adapterId, file.path, deviceTagPool, tagPool, resolvedMappings, errors);
            }
        }

        return new ResolvedAdapter(manifest, tagPool, resolvedMappings);
    }

    private void resolveTag(
            final @NotNull SourceTag sourceTag,
            final @NotNull String adapterId,
            final @Nullable Path filePath,
            final @NotNull Map<String, SourceDeviceTag> deviceTagPool,
            final @NotNull Map<String, ResolvedTag> tagPool,
            final @NotNull DiagnosticCollector errors) {

        if (sourceTag.name == null || sourceTag.name.isBlank()) {
            errors.add(Diagnostic.error(
                    "TAG_MISSING_NAME",
                    "A TAG in adapter '" + adapterId + "' has no 'name' field",
                    filePath,
                    DiagnosticRange.ofNullable(sourceTag.line, sourceTag.character)));
            return;
        }

        if (sourceTag.deviceTag != null && sourceTag.deviceTagId != null) {
            errors.add(Diagnostic.error(
                    "TAG_AMBIGUOUS_DEVICE_TAG",
                    "TAG '" + sourceTag.name + "' in adapter '" + adapterId
                            + "' has both 'deviceTag' and 'deviceTagId' — only one is allowed",
                    filePath,
                    DiagnosticRange.ofNullable(sourceTag.line, sourceTag.character),
                    Map.of("adapterId", adapterId, "tagName", sourceTag.name)));
            return;
        }

        if (tagPool.containsKey(sourceTag.name)) {
            errors.add(Diagnostic.error(
                    "DUPLICATE_TAG_NAME",
                    "TAG '" + sourceTag.name + "' is defined more than once in adapter '" + adapterId + "'",
                    filePath,
                    DiagnosticRange.ofNullable(sourceTag.line, sourceTag.character),
                    Map.of("adapterId", adapterId, "tagName", sourceTag.name)));
            return;
        }

        final SourceDeviceTag resolvedDeviceTag;
        if (sourceTag.deviceTag != null) {
            // Inline DEVICE-TAG — register it in the pool too
            resolvedDeviceTag = sourceTag.deviceTag;
            if (resolvedDeviceTag.id != null && !resolvedDeviceTag.id.isBlank()) {
                deviceTagPool.putIfAbsent(resolvedDeviceTag.id, resolvedDeviceTag);
            }
        } else if (sourceTag.deviceTagId != null) {
            // By-id reference — look up in pool
            resolvedDeviceTag = deviceTagPool.get(sourceTag.deviceTagId);
            if (resolvedDeviceTag == null) {
                errors.add(Diagnostic.error(
                        "UNRESOLVED_DEVICE_TAG_REFERENCE",
                        "TAG '" + sourceTag.name + "' in adapter '" + adapterId + "' references DEVICE-TAG id '"
                                + sourceTag.deviceTagId + "' which does not exist",
                        filePath,
                        DiagnosticRange.ofNullable(sourceTag.line, sourceTag.character),
                        Map.of(
                                "adapterId", adapterId,
                                "tagName", sourceTag.name,
                                "deviceTagId", sourceTag.deviceTagId)));
                return;
            }
        } else {
            // Standalone TAG with no device tag
            resolvedDeviceTag = null;
        }

        tagPool.put(sourceTag.name, new ResolvedTag(sourceTag.name, resolvedDeviceTag));
    }

    private void resolveNorthboundMapping(
            final @NotNull SourceNorthboundMapping mapping,
            final @NotNull String adapterId,
            final @Nullable Path filePath,
            final @NotNull Map<String, SourceDeviceTag> deviceTagPool,
            final @NotNull Map<String, ResolvedTag> tagPool,
            final @NotNull List<ResolvedNorthboundMapping> resolvedMappings,
            final @NotNull DiagnosticCollector errors) {

        if (mapping.tagName != null && mapping.tag != null) {
            errors.add(Diagnostic.error(
                    "MAPPING_AMBIGUOUS_TAG",
                    "A northbound mapping in adapter '" + adapterId
                            + "' has both 'tagName' and 'tag' — only one is allowed",
                    filePath,
                    DiagnosticRange.ofNullable(mapping.line, mapping.character),
                    Map.of("adapterId", adapterId)));
            return;
        }

        if (mapping.topic == null || mapping.topic.isBlank()) {
            errors.add(Diagnostic.error(
                    "MAPPING_MISSING_TOPIC",
                    "A northbound mapping in adapter '" + adapterId + "' has no 'topic' field",
                    filePath,
                    DiagnosticRange.ofNullable(mapping.line, mapping.character),
                    Map.of("adapterId", adapterId)));
            return;
        }

        if (mapping.qos < 0 || mapping.qos > 2) {
            errors.add(Diagnostic.error(
                    "MAPPING_INVALID_QOS",
                    "A northbound mapping in adapter '" + adapterId + "' has invalid qos: " + mapping.qos,
                    filePath,
                    DiagnosticRange.ofNullable(mapping.line, mapping.character),
                    Map.of("adapterId", adapterId, "qos", mapping.qos)));
            return;
        }

        final String resolvedTagName;
        if (mapping.tag != null) {
            // Inline TAG — extract and register, then use its name
            final SourceTag inlineTag = mapping.tag;
            resolveTag(inlineTag, adapterId, filePath, deviceTagPool, tagPool, errors);
            if (inlineTag.name == null || inlineTag.name.isBlank()) {
                return; // error already recorded
            }
            resolvedTagName = inlineTag.name;
        } else if (mapping.tagName != null) {
            // By-name reference — look up in tag pool
            if (!tagPool.containsKey(mapping.tagName)) {
                errors.add(Diagnostic.error(
                        "UNRESOLVED_TAG_REFERENCE",
                        "Northbound mapping in adapter '" + adapterId + "' references tag '" + mapping.tagName
                                + "' which does not exist",
                        filePath,
                        DiagnosticRange.ofNullable(mapping.line, mapping.character),
                        Map.of("adapterId", adapterId, "tagName", mapping.tagName)));
                return;
            }
            resolvedTagName = mapping.tagName;
        } else {
            errors.add(Diagnostic.error(
                    "MAPPING_MISSING_TAG",
                    "A northbound mapping in adapter '" + adapterId
                            + "' has neither 'tagName' nor 'tag' — one is required",
                    filePath,
                    DiagnosticRange.ofNullable(mapping.line, mapping.character),
                    Map.of("adapterId", adapterId)));
            return;
        }

        resolvedMappings.add(new ResolvedNorthboundMapping(resolvedTagName, mapping));
    }
}
