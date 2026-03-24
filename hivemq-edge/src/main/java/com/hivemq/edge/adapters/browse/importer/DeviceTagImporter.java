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

import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.ADAPTER_NOT_FOUND;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.UPDATE_FAILED;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.WILDCARD_NO_DEFAULT;
import static java.util.Objects.requireNonNull;

import com.hivemq.configuration.entity.adapter.MqttUserPropertyEntity;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.SouthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.InstructionEntity;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import com.hivemq.edge.adapters.browse.model.ImportMode;
import com.hivemq.edge.adapters.browse.model.ImportResult;
import com.hivemq.edge.adapters.browse.model.TagAction;
import com.hivemq.edge.adapters.browse.validate.DeviceTagValidator;
import com.hivemq.edge.adapters.browse.validate.ValidationError;
import com.hivemq.mqtt.message.QoS;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DeviceTagImporter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DeviceTagImporter.class);

    private final @NotNull DeviceTagValidator validator;
    private final @NotNull ProtocolAdapterExtractor adapterExtractor;

    @Inject
    public DeviceTagImporter(
            final @NotNull DeviceTagValidator validator, final @NotNull ProtocolAdapterExtractor adapterExtractor) {
        this.validator = validator;
        this.adapterExtractor = adapterExtractor;
    }

    private static @Nullable String resolveWildcard(final @Nullable String value, final @Nullable String defaultValue) {
        return "*".equals(value) ? defaultValue : value;
    }

    private static @NotNull TagEntity toTagEntity(final @NotNull DeviceTagRow row) {
        return new TagEntity(
                requireNonNull(row.getTagName()), row.getTagDescription(), Map.of("node", row.getNodeId()));
    }

    private static @NotNull NorthboundMappingEntity toNorthboundMappingEntity(final @NotNull DeviceTagRow row) {
        final int qos = row.getMaxQos() != null ? row.getMaxQos() : QoS.AT_LEAST_ONCE.getQosNumber();

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
                requireNonNull(row.getTagName()),
                requireNonNull(row.getNorthboundTopic()),
                qos,
                null, // messageHandlingOptions — always MQTTMessagePerTag
                includeTagNames,
                includeTimestamp,
                userProperties,
                expiry);
    }

    private static @NotNull SouthboundMappingEntity toSouthboundMappingEntity(final @NotNull DeviceTagRow row) {
        final List<InstructionEntity> instructions;
        if (row.getSouthboundFieldMapping() != null
                && !row.getSouthboundFieldMapping().isEmpty()) {
            instructions = row.getSouthboundFieldMapping().stream()
                    .map(fm -> new InstructionEntity(fm.source(), fm.destination(), null))
                    .toList();
        } else {
            // Default: value -> value
            instructions = List.of(new InstructionEntity("value", "value", null));
        }
        return new SouthboundMappingEntity(
                requireNonNull(row.getTagName()),
                requireNonNull(row.getSouthboundTopic()),
                new FieldMappingEntity(instructions),
                ""); // fromNorthSchema — empty; populated after tag creation by the adapter
    }

    private static boolean tagDefinitionsMatch(final @NotNull DeviceTagRow row, final @NotNull TagEntity existing) {
        return Objects.equals(row.getTagName(), existing.getName())
                && Objects.equals(nullToEmpty(row.getTagDescription()), nullToEmpty(existing.getDescription()));
    }

    private static @NotNull String nullToEmpty(final @Nullable String s) {
        return s == null ? "" : s;
    }

    private static boolean mappingsMatch(
            final @NotNull DeviceTagRow row,
            final @Nullable NorthboundMappingEntity existingNb,
            final @Nullable SouthboundMappingEntity existingSb) {
        if (row.hasNorthboundMapping()) {
            if (existingNb == null) {
                return false;
            }
            if (!Objects.equals(row.getNorthboundTopic(), existingNb.getTopic())) {
                return false;
            }
            if (row.getMaxQos() != null && row.getMaxQos() != existingNb.getMaxQoS()) {
                return false;
            }
            if (row.getIncludeTagNames() != null && !row.getIncludeTagNames().equals(existingNb.isIncludeTagNames())) {
                return false;
            }
            if (row.getIncludeTimestamp() != null
                    && !row.getIncludeTimestamp().equals(existingNb.isIncludeTimestamp())) {
                return false;
            }
            if (row.getMessageExpiryInterval() != null
                    && row.getMessageExpiryInterval() != existingNb.getMessageExpiryInterval()) {
                return false;
            }
            if (!userPropertiesMatch(row.getMqttUserProperties(), existingNb.getUserProperties())) {
                return false;
            }
        }
        if (row.hasSouthboundMapping()) {
            if (existingSb == null) {
                return false;
            }
            return Objects.equals(row.getSouthboundTopic(), existingSb.getTopicFilter());
        }
        return true;
    }

    private static boolean userPropertiesMatch(
            final @Nullable Map<String, String> rowProps, final @NotNull List<MqttUserPropertyEntity> existingProps) {
        if (rowProps == null) {
            return true;
        }
        if (rowProps.size() != existingProps.size()) {
            return false;
        }
        for (final MqttUserPropertyEntity prop : existingProps) {
            if (!Objects.equals(rowProps.get(prop.getName()), prop.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static @NotNull List<ValidationError> collectWildcardErrors(final @NotNull List<DeviceTagRow> rows) {
        final List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            final DeviceTagRow row = rows.get(i);
            final int rowNum = i + 1;
            if ("*".equals(row.getTagName())
                    && (row.getTagNameDefault() == null
                            || row.getTagNameDefault().isEmpty())) {
                errors.add(new ValidationError(
                        rowNum,
                        "tag_name",
                        "*",
                        WILDCARD_NO_DEFAULT,
                        "Wildcard '*' used for tag_name but no default value available"));
            }
            if ("*".equals(row.getNorthboundTopic())
                    && (row.getNorthboundTopicDefault() == null
                            || row.getNorthboundTopicDefault().isEmpty())) {
                errors.add(new ValidationError(
                        rowNum,
                        "northbound_topic",
                        "*",
                        WILDCARD_NO_DEFAULT,
                        "Wildcard '*' used for northbound_topic but no default value available"));
            }
            if ("*".equals(row.getSouthboundTopic())
                    && (row.getSouthboundTopicDefault() == null
                            || row.getSouthboundTopicDefault().isEmpty())) {
                errors.add(new ValidationError(
                        rowNum,
                        "southbound_topic",
                        "*",
                        WILDCARD_NO_DEFAULT,
                        "Wildcard '*' used for southbound_topic but no default value available"));
            }
        }
        return errors;
    }

    public @NotNull ImportResult doImport(
            final @NotNull List<DeviceTagRow> rows, final @NotNull ImportMode mode, final @NotNull String adapterId)
            throws DeviceTagImporterException {

        // Step 1: Resolve wildcards
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

        // Step 2: Validate
        final List<ValidationError> errors = validator.validate(resolved, mode, adapterId);

        // Collect wildcard resolution errors
        final List<ValidationError> wildcardErrors = collectWildcardErrors(rows);
        final List<ValidationError> allErrors = new ArrayList<>(errors);
        allErrors.addAll(wildcardErrors);

        // Step 3: If errors, fail
        if (!allErrors.isEmpty()) {
            throw new DeviceTagImporterException(allErrors);
        }

        // Step 4: Load current state
        final ProtocolAdapterEntity adapter = adapterExtractor
                .getAdapterByAdapterId(adapterId)
                .orElseThrow(() -> new DeviceTagImporterException(List.of(new ValidationError(
                        null, null, adapterId, ADAPTER_NOT_FOUND, "Adapter '" + adapterId + "' not found"))));

        // Build maps of current edge state (by tagName for mapping lookups)
        final Map<String, NorthboundMappingEntity> edgeNorthboundByTag = adapter.getNorthboundMappings().stream()
                .collect(Collectors.toMap(NorthboundMappingEntity::getTagName, m -> m, (a, b) -> a));
        final Map<String, SouthboundMappingEntity> edgeSouthboundByTag = adapter.getSouthboundMappings().stream()
                .collect(Collectors.toMap(SouthboundMappingEntity::getTagName, m -> m, (a, b) -> a));

        // Build nodeId-keyed maps for classification
        final Map<String, TagEntity> edgeTagsByNodeId = new LinkedHashMap<>();
        for (final TagEntity tag : adapter.getTags()) {
            final Object node = tag.getDefinition().get("node");
            if (node != null) {
                edgeTagsByNodeId.put(node.toString(), tag);
            }
        }

        final Map<String, DeviceTagRow> fileRowsByNodeId = new LinkedHashMap<>();
        for (final DeviceTagRow row : resolved) {
            if (row.hasTag() && row.getNodeId() != null) {
                fileRowsByNodeId.put(row.getNodeId(), row);
            }
        }

        // Step 5: Classify by nodeId
        final Set<String> edgeOnlyNodeIds = new HashSet<>(edgeTagsByNodeId.keySet());
        edgeOnlyNodeIds.removeAll(fileRowsByNodeId.keySet());

        final Set<String> fileOnlyNodeIds = new HashSet<>(fileRowsByNodeId.keySet());
        fileOnlyNodeIds.removeAll(edgeTagsByNodeId.keySet());

        final Set<String> inBothNodeIds = new HashSet<>(fileRowsByNodeId.keySet());
        inBothNodeIds.retainAll(edgeTagsByNodeId.keySet());

        // Step 6: Determine actions per mode
        final List<TagAction> tagActions = new ArrayList<>();
        final List<TagEntity> finalTags = new ArrayList<>();
        final List<NorthboundMappingEntity> finalNorthbound = new ArrayList<>();
        final List<SouthboundMappingEntity> finalSouthbound = new ArrayList<>();

        int tagsCreated = 0, tagsUpdated = 0, tagsDeleted = 0;
        int nbCreated = 0, nbDeleted = 0, sbCreated = 0, sbDeleted = 0;

        // Process edge-only nodes
        for (final String nodeId : edgeOnlyNodeIds) {
            final TagEntity edgeTag = requireNonNull(edgeTagsByNodeId.get(nodeId));
            final String tagName = edgeTag.getName();
            switch (mode) {
                case DELETE, OVERWRITE -> {
                    tagActions.add(new TagAction(tagName, TagAction.Action.DELETED));
                    tagsDeleted++;
                    if (edgeNorthboundByTag.containsKey(tagName)) {
                        nbDeleted++;
                    }
                    if (edgeSouthboundByTag.containsKey(tagName)) {
                        sbDeleted++;
                    }
                }
                case MERGE_SAFE, MERGE_OVERWRITE -> {
                    // KEEP — add to final lists
                    finalTags.add(edgeTag);
                    if (edgeNorthboundByTag.containsKey(tagName)) {
                        finalNorthbound.add(edgeNorthboundByTag.get(tagName));
                    }
                    if (edgeSouthboundByTag.containsKey(tagName)) {
                        finalSouthbound.add(edgeSouthboundByTag.get(tagName));
                    }
                }
                case CREATE -> {
                    // CREATE mode: edge-only nodes are rejected by validation.
                    // This branch should never execute if validation passed.
                }
            }
        }

        // Process file-only nodes (DELETE mode: file-only should have been caught by validation)
        for (final String nodeId : fileOnlyNodeIds) {
            if (mode == ImportMode.DELETE) {
                // DELETE mode does not create tags — file-only nodes are rejected by validation
                continue;
            }
            final DeviceTagRow row = requireNonNull(fileRowsByNodeId.get(nodeId));
            final String tagName = requireNonNull(row.getTagName());
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

        // Process in-both nodes (same nodeId on edge and in file)
        for (final String nodeId : inBothNodeIds) {
            final DeviceTagRow row = requireNonNull(fileRowsByNodeId.get(nodeId));
            final TagEntity existing = requireNonNull(edgeTagsByNodeId.get(nodeId));
            final String oldTagName = existing.getName();

            final boolean identical = tagDefinitionsMatch(row, existing)
                    && mappingsMatch(row, edgeNorthboundByTag.get(oldTagName), edgeSouthboundByTag.get(oldTagName));

            if (identical) {
                // No-op — keep existing
                finalTags.add(existing);
                if (edgeNorthboundByTag.containsKey(oldTagName)) {
                    finalNorthbound.add(edgeNorthboundByTag.get(oldTagName));
                }
                if (edgeSouthboundByTag.containsKey(oldTagName)) {
                    finalSouthbound.add(edgeSouthboundByTag.get(oldTagName));
                }
            } else {
                // Differ (description change, rename, or mapping change)
                switch (mode) {
                    case OVERWRITE, MERGE_OVERWRITE -> {
                        finalTags.add(toTagEntity(row));
                        tagActions.add(new TagAction(requireNonNull(row.getTagName()), TagAction.Action.UPDATED));
                        tagsUpdated++;

                        // Replace mappings (delete old by oldTagName, create new)
                        if (edgeNorthboundByTag.containsKey(oldTagName)) {
                            nbDeleted++;
                        }
                        if (edgeSouthboundByTag.containsKey(oldTagName)) {
                            sbDeleted++;
                        }
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
                        if (edgeNorthboundByTag.containsKey(oldTagName)) {
                            finalNorthbound.add(edgeNorthboundByTag.get(oldTagName));
                        }
                        if (edgeSouthboundByTag.containsKey(oldTagName)) {
                            finalSouthbound.add(edgeSouthboundByTag.get(oldTagName));
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
            throw new DeviceTagImporterException(List.of(new ValidationError(
                    null,
                    null,
                    null,
                    UPDATE_FAILED,
                    "Failed to update adapter configuration. Possible tag name conflict.")));
        }

        log.info(
                "Import completed for adapter '{}': {} tags created, {} updated, {} deleted",
                adapterId,
                tagsCreated,
                tagsUpdated,
                tagsDeleted);

        return new ImportResult(
                tagsCreated, tagsUpdated, tagsDeleted, nbCreated, nbDeleted, sbCreated, sbDeleted, tagActions);
    }
}
