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
package com.hivemq.edge.adapters.browse.importer;

import com.hivemq.configuration.entity.adapter.MqttUserPropertyEntity;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.SouthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.InstructionEntity;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import com.hivemq.edge.adapters.browse.model.FieldMappingInstruction;
import com.hivemq.edge.adapters.browse.model.ImportMode;
import com.hivemq.edge.adapters.browse.model.ImportResult;
import com.hivemq.edge.adapters.browse.model.TagAction;
import com.hivemq.edge.adapters.browse.validate.DeviceTagValidator;
import com.hivemq.edge.adapters.browse.validate.ValidationError;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs bulk import of device tags and mappings. Resolves wildcards, validates,
 * computes the diff against current state, and applies mutations atomically.
 */
@Singleton
public class DeviceTagImporter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DeviceTagImporter.class);

    private final @NotNull DeviceTagValidator validator;
    private final @NotNull ProtocolAdapterExtractor adapterExtractor;

    @Inject
    public DeviceTagImporter(
            final @NotNull DeviceTagValidator validator,
            final @NotNull ProtocolAdapterExtractor adapterExtractor) {
        this.validator = validator;
        this.adapterExtractor = adapterExtractor;
    }

    /**
     * Perform bulk import of device tags and mappings.
     *
     * @param rows      the rows to import (wildcards not yet resolved)
     * @param mode      the conflict-resolution mode
     * @param adapterId the target adapter ID
     * @return import result on success
     * @throws ImportException if validation fails or adapter is not found
     */
    public @NotNull ImportResult doImport(
            final @NotNull List<DeviceTagRow> rows,
            final @NotNull ImportMode mode,
            final @NotNull String adapterId) throws ImportException {

        // Step 1: Resolve wildcards
        final List<DeviceTagRow> resolved = resolveWildcards(rows);

        // Step 2: Validate
        final List<ValidationError> errors = validator.validate(resolved, mode, adapterId);

        // Collect wildcard resolution errors
        final List<ValidationError> wildcardErrors = collectWildcardErrors(rows);
        final List<ValidationError> allErrors = new ArrayList<>(errors);
        allErrors.addAll(wildcardErrors);

        // Step 3: If errors, fail
        if (!allErrors.isEmpty()) {
            throw new ImportException(allErrors);
        }

        // Step 4: Load current state
        final ProtocolAdapterEntity adapter = adapterExtractor.getAdapterByAdapterId(adapterId)
                .orElseThrow(() -> new ImportException(List.of(
                        new ValidationError(null, null, adapterId, "ADAPTER_NOT_FOUND",
                                "Adapter '" + adapterId + "' not found"))));

        // Build maps of current edge state
        final Map<String, TagEntity> edgeTagsByName = adapter.getTags().stream()
                .collect(Collectors.toMap(TagEntity::getName, t -> t, (a, b) -> a));
        final Map<String, NorthboundMappingEntity> edgeNorthboundByTag = adapter.getNorthboundMappings().stream()
                .collect(Collectors.toMap(NorthboundMappingEntity::getTagName, m -> m, (a, b) -> a));
        final Map<String, SouthboundMappingEntity> edgeSouthboundByTag = adapter.getSouthboundMappings().stream()
                .collect(Collectors.toMap(SouthboundMappingEntity::getTagName, m -> m, (a, b) -> a));

        // Build map of file rows (only those with tags)
        final Map<String, DeviceTagRow> fileRowsByTag = new LinkedHashMap<>();
        for (final DeviceTagRow row : resolved) {
            if (row.hasTag()) {
                fileRowsByTag.put(row.getTagName(), row);
            }
        }

        // Step 5: Classify tags
        final Set<String> edgeOnlyTags = new HashSet<>(edgeTagsByName.keySet());
        edgeOnlyTags.removeAll(fileRowsByTag.keySet());

        final Set<String> fileOnlyTags = new HashSet<>(fileRowsByTag.keySet());
        fileOnlyTags.removeAll(edgeTagsByName.keySet());

        final Set<String> inBothTags = new HashSet<>(fileRowsByTag.keySet());
        inBothTags.retainAll(edgeTagsByName.keySet());

        // Step 6: Determine actions per mode
        final List<TagAction> tagActions = new ArrayList<>();
        final List<TagEntity> finalTags = new ArrayList<>();
        final List<NorthboundMappingEntity> finalNorthbound = new ArrayList<>();
        final List<SouthboundMappingEntity> finalSouthbound = new ArrayList<>();

        int tagsCreated = 0, tagsUpdated = 0, tagsDeleted = 0;
        int nbCreated = 0, nbDeleted = 0, sbCreated = 0, sbDeleted = 0;

        // Process edge-only tags
        for (final String tagName : edgeOnlyTags) {
            switch (mode) {
                case DELETE, OVERWRITE -> {
                    tagActions.add(new TagAction(tagName, TagAction.Action.DELETED));
                    tagsDeleted++;
                    if (edgeNorthboundByTag.containsKey(tagName)) nbDeleted++;
                    if (edgeSouthboundByTag.containsKey(tagName)) sbDeleted++;
                }
                case MERGE_SAFE, MERGE_OVERWRITE -> {
                    // KEEP — add to final lists
                    finalTags.add(edgeTagsByName.get(tagName));
                    if (edgeNorthboundByTag.containsKey(tagName)) {
                        finalNorthbound.add(edgeNorthboundByTag.get(tagName));
                    }
                    if (edgeSouthboundByTag.containsKey(tagName)) {
                        finalSouthbound.add(edgeSouthboundByTag.get(tagName));
                    }
                }
                case CREATE -> {
                    // CREATE mode: edge-only should have been caught by validation (TAG_CONFLICT)
                    // but keep them for safety
                    finalTags.add(edgeTagsByName.get(tagName));
                    if (edgeNorthboundByTag.containsKey(tagName)) {
                        finalNorthbound.add(edgeNorthboundByTag.get(tagName));
                    }
                    if (edgeSouthboundByTag.containsKey(tagName)) {
                        finalSouthbound.add(edgeSouthboundByTag.get(tagName));
                    }
                }
            }
        }

        // Process file-only tags
        for (final String tagName : fileOnlyTags) {
            final DeviceTagRow row = fileRowsByTag.get(tagName);
            finalTags.add(toTagEntity(row));
            tagActions.add(new TagAction(tagName, TagAction.Action.CREATED));
            tagsCreated++;

            if (row.hasNorthboundMapping()) {
                finalNorthbound.add(toNorthboundMappingEntity(row));
                nbCreated++;
            }
            if (row.hasSouthboundMapping()) {
                finalSouthbound.add(toSouthboundMappingEntity(row));
                sbCreated++;
            }
        }

        // Process in-both tags
        for (final String tagName : inBothTags) {
            final DeviceTagRow row = fileRowsByTag.get(tagName);
            final TagEntity existing = edgeTagsByName.get(tagName);

            final boolean identical = tagDefinitionsMatch(row, existing)
                    && mappingsMatch(row, edgeNorthboundByTag.get(tagName), edgeSouthboundByTag.get(tagName));

            if (identical) {
                // No-op — keep existing
                finalTags.add(existing);
                if (edgeNorthboundByTag.containsKey(tagName)) {
                    finalNorthbound.add(edgeNorthboundByTag.get(tagName));
                }
                if (edgeSouthboundByTag.containsKey(tagName)) {
                    finalSouthbound.add(edgeSouthboundByTag.get(tagName));
                }
            } else {
                // Differ
                switch (mode) {
                    case OVERWRITE, MERGE_OVERWRITE -> {
                        finalTags.add(toTagEntity(row));
                        tagActions.add(new TagAction(tagName, TagAction.Action.UPDATED));
                        tagsUpdated++;

                        // Replace mappings
                        if (edgeNorthboundByTag.containsKey(tagName)) nbDeleted++;
                        if (edgeSouthboundByTag.containsKey(tagName)) sbDeleted++;
                        if (row.hasNorthboundMapping()) {
                            finalNorthbound.add(toNorthboundMappingEntity(row));
                            nbCreated++;
                        }
                        if (row.hasSouthboundMapping()) {
                            finalSouthbound.add(toSouthboundMappingEntity(row));
                            sbCreated++;
                        }
                    }
                    default -> {
                        // CREATE, DELETE, MERGE_SAFE: conflict should have been caught by validation
                        finalTags.add(existing);
                        if (edgeNorthboundByTag.containsKey(tagName)) {
                            finalNorthbound.add(edgeNorthboundByTag.get(tagName));
                        }
                        if (edgeSouthboundByTag.containsKey(tagName)) {
                            finalSouthbound.add(edgeSouthboundByTag.get(tagName));
                        }
                    }
                }
            }
        }

        // Step 7: Apply mutations atomically
        final ProtocolAdapterEntity updatedAdapter = new ProtocolAdapterEntity(
                adapter.getAdapterId(),
                adapter.getProtocolId(),
                adapter.getConfigVersion(),
                adapter.getConfig(),
                finalNorthbound,
                finalSouthbound,
                finalTags);

        final boolean success = adapterExtractor.updateAdapter(updatedAdapter);
        if (!success) {
            throw new ImportException(List.of(new ValidationError(null, null, null,
                    "UPDATE_FAILED",
                    "Failed to update adapter configuration. Possible tag name conflict.")));
        }

        log.info("Import completed for adapter '{}': {} tags created, {} updated, {} deleted",
                adapterId, tagsCreated, tagsUpdated, tagsDeleted);

        return new ImportResult(
                tagsCreated, tagsUpdated, tagsDeleted,
                nbCreated, nbDeleted,
                sbCreated, sbDeleted,
                tagActions);
    }

    // --- Wildcard resolution ---

    private @NotNull List<DeviceTagRow> resolveWildcards(final @NotNull List<DeviceTagRow> rows) {
        final List<DeviceTagRow> resolved = new ArrayList<>();
        for (final DeviceTagRow row : rows) {
            final DeviceTagRow.Builder builder = DeviceTagRow.builder()
                    .nodePath(row.getNodePath())
                    .namespaceUri(row.getNamespaceUri())
                    .namespaceIndex(row.getNamespaceIndex())
                    .nodeId(row.getNodeId())
                    .dataType(row.getDataType())
                    .accessLevel(row.getAccessLevel())
                    .nodeDescription(row.getNodeDescription())
                    .tagNameDefault(row.getTagNameDefault())
                    .tagDescription(row.getTagDescription())
                    .northboundTopicDefault(row.getNorthboundTopicDefault())
                    .southboundTopicDefault(row.getSouthboundTopicDefault())
                    .southboundFieldMapping(row.getSouthboundFieldMapping())
                    .maxQos(row.getMaxQos())
                    .messageExpiryInterval(row.getMessageExpiryInterval())
                    .includeTimestamp(row.getIncludeTimestamp())
                    .includeTagNames(row.getIncludeTagNames())
                    .includeMetadata(row.getIncludeMetadata())
                    .mqttUserProperties(row.getMqttUserProperties());

            // Resolve * wildcards
            builder.tagName(resolveWildcard(row.getTagName(), row.getTagNameDefault()));
            builder.northboundTopic(resolveWildcard(row.getNorthboundTopic(), row.getNorthboundTopicDefault()));
            builder.southboundTopic(resolveWildcard(row.getSouthboundTopic(), row.getSouthboundTopicDefault()));

            resolved.add(builder.build());
        }
        return resolved;
    }

    private static String resolveWildcard(final String value, final String defaultValue) {
        if ("*".equals(value)) {
            return defaultValue;
        }
        return value;
    }

    private @NotNull List<ValidationError> collectWildcardErrors(final @NotNull List<DeviceTagRow> rows) {
        final List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            final DeviceTagRow row = rows.get(i);
            final int rowNum = i + 1;

            if ("*".equals(row.getTagName()) && (row.getTagNameDefault() == null || row.getTagNameDefault().isEmpty())) {
                errors.add(new ValidationError(rowNum, "tag_name", "*",
                        "WILDCARD_NO_DEFAULT",
                        "Wildcard '*' used for tag_name but no default value available"));
            }
            if ("*".equals(row.getNorthboundTopic()) && (row.getNorthboundTopicDefault() == null || row.getNorthboundTopicDefault().isEmpty())) {
                errors.add(new ValidationError(rowNum, "northbound_topic", "*",
                        "WILDCARD_NO_DEFAULT",
                        "Wildcard '*' used for northbound_topic but no default value available"));
            }
            if ("*".equals(row.getSouthboundTopic()) && (row.getSouthboundTopicDefault() == null || row.getSouthboundTopicDefault().isEmpty())) {
                errors.add(new ValidationError(rowNum, "southbound_topic", "*",
                        "WILDCARD_NO_DEFAULT",
                        "Wildcard '*' used for southbound_topic but no default value available"));
            }
        }
        return errors;
    }

    // --- Entity construction ---

    private static @NotNull TagEntity toTagEntity(final @NotNull DeviceTagRow row) {
        return new TagEntity(
                row.getTagName(),
                row.getTagDescription(),
                Map.of("node", row.getNodeId()));
    }

    private static @NotNull NorthboundMappingEntity toNorthboundMappingEntity(final @NotNull DeviceTagRow row) {
        final int qos = row.getMaxQos() != null ? row.getMaxQos() : 1;
        final boolean includeTagNames = row.getIncludeTagNames() != null ? row.getIncludeTagNames() : false;
        final boolean includeTimestamp = row.getIncludeTimestamp() != null ? row.getIncludeTimestamp() : true;
        final Long expiry = row.getMessageExpiryInterval();

        final List<MqttUserPropertyEntity> userProperties;
        if (row.getMqttUserProperties() != null) {
            userProperties = row.getMqttUserProperties().entrySet().stream()
                    .map(e -> new MqttUserPropertyEntity(e.getKey(), e.getValue()))
                    .toList();
        } else {
            userProperties = List.of();
        }

        return new NorthboundMappingEntity(
                row.getTagName(),
                row.getNorthboundTopic(),
                qos,
                null, // messageHandlingOptions — always MQTTMessagePerTag
                includeTagNames,
                includeTimestamp,
                userProperties,
                expiry);
    }

    private static @NotNull SouthboundMappingEntity toSouthboundMappingEntity(final @NotNull DeviceTagRow row) {
        final List<InstructionEntity> instructions;
        if (row.getSouthboundFieldMapping() != null && !row.getSouthboundFieldMapping().isEmpty()) {
            instructions = row.getSouthboundFieldMapping().stream()
                    .map(fm -> new InstructionEntity(fm.source(), fm.destination(), null))
                    .toList();
        } else {
            // Default: value -> value
            instructions = List.of(new InstructionEntity("value", "value", null));
        }

        return new SouthboundMappingEntity(
                row.getTagName(),
                row.getSouthboundTopic(),
                new FieldMappingEntity(instructions),
                ""); // fromNorthSchema — empty; populated after tag creation by the adapter
    }

    // --- Comparison helpers ---

    private static boolean tagDefinitionsMatch(final @NotNull DeviceTagRow row, final @NotNull TagEntity existing) {
        final Map<String, Object> existingDef = existing.getDefinition();
        final String existingNode = existingDef.get("node") != null ? existingDef.get("node").toString() : null;
        return Objects.equals(row.getNodeId(), existingNode)
                && Objects.equals(nullToEmpty(row.getTagDescription()), nullToEmpty(existing.getDescription()));
    }

    private static @NotNull String nullToEmpty(final String s) {
        return s == null ? "" : s;
    }

    private static boolean mappingsMatch(
            final @NotNull DeviceTagRow row,
            final NorthboundMappingEntity existingNb,
            final SouthboundMappingEntity existingSb) {
        // Check northbound
        if (row.hasNorthboundMapping() != (existingNb != null)) return false;
        if (row.hasNorthboundMapping() && existingNb != null) {
            if (!Objects.equals(row.getNorthboundTopic(), existingNb.getTopic())) return false;
            if (row.getMaxQos() != null && row.getMaxQos() != existingNb.getMaxQoS()) return false;
        }

        // Check southbound
        if (row.hasSouthboundMapping() != (existingSb != null)) return false;
        if (row.hasSouthboundMapping() && existingSb != null) {
            if (!Objects.equals(row.getSouthboundTopic(), existingSb.getTopicFilter())) return false;
        }

        return true;
    }

    /**
     * Exception thrown when import validation fails.
     */
    public static class ImportException extends Exception {
        private final @NotNull List<ValidationError> errors;

        public ImportException(final @NotNull List<ValidationError> errors) {
            super("Import validation failed with " + errors.size() + " errors");
            this.errors = errors;
        }

        public @NotNull List<ValidationError> getErrors() {
            return errors;
        }
    }
}
