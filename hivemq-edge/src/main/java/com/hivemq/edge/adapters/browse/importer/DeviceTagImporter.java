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
import com.hivemq.edge.adapters.browse.BrowseException;
import com.hivemq.edge.adapters.browse.BulkTagBrowser;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
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

    /**
     * Resolves a node ID against the device's current namespace table using the stable namespace URI.
     * If the adapter supports namespace resolution and the URI is present, the returned node ID will
     * contain the updated namespace index. Otherwise the original nodeId is returned unchanged.
     */
    private static @Nullable String resolveNodeIdIfNeeded(
            final @Nullable BulkTagBrowser browser,
            final @Nullable String nodeId,
            final @Nullable String namespaceUri) {
        if (browser == null || nodeId == null || namespaceUri == null || namespaceUri.isEmpty()) {
            return nodeId;
        }
        try {
            return browser.resolveNodeId(nodeId, namespaceUri);
        } catch (final BrowseException e) {
            log.warn("Failed to resolve namespace URI '{}' for nodeId '{}': {}", namespaceUri, nodeId, e.getMessage());
            return nodeId;
        }
    }

    private static @NotNull TagEntity toTagEntity(final @NotNull DeviceTagRow row) {
        return new TagEntity(
                requireNonNull(row.getTagName()),
                row.getTagDescription(),
                Map.of("node", requireNonNull(row.getNodeId())));
    }

    private static @NotNull NorthboundMappingEntity toNorthboundMappingEntity(final @NotNull DeviceTagRow row) {
        final int qos = row.getMaxQos() != null ? row.getMaxQos() : QoS.AT_LEAST_ONCE.getQosNumber();

        final boolean includeTagNames = row.getIncludeTagNames() != null ? row.getIncludeTagNames() : false;
        final boolean includeTimestamp = row.getIncludeTimestamp() != null ? row.getIncludeTimestamp() : true;
        final boolean includeMetadata = row.getIncludeMetadata() != null ? row.getIncludeMetadata() : false;
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
                includeMetadata,
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

    private static boolean tagMatch(final @NotNull TagEntity tagFile, final @NotNull TagEntity tagEdge) {
        return Objects.equals(tagFile.getName(), tagEdge.getName())
                && Objects.equals(tagFile.getDescription(), tagEdge.getDescription());
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
        return doImport(rows, mode, adapterId, null);
    }

    public @NotNull ImportResult doImport(
            final @NotNull List<DeviceTagRow> rows,
            final @NotNull ImportMode mode,
            final @NotNull String adapterId,
            final @Nullable BulkTagBrowser browser)
            throws DeviceTagImporterException {
        // Synchronize the entire read-compute-write cycle on the same intrinsic lock used by
        // ProtocolAdapterExtractor's synchronized methods to prevent TOCTOU races between
        // concurrent imports (e.g., two OVERWRITE operations reading the same stale state).
        synchronized (adapterExtractor) {
            return doImportLocked(rows, mode, adapterId, browser);
        }
    }

    private static class TagWithMappings<T extends TagEntity> {
        @NotNull
        T tag;

        @NotNull
        Set<NorthboundMappingEntity> northboundMappings;

        @NotNull
        Set<SouthboundMappingEntity> southboundMappings;

        private TagWithMappings(@NotNull T tag) {
            this.tag = tag;
            this.northboundMappings = new HashSet<>();
            this.southboundMappings = new HashSet<>();
        }

        private void addNorthboundMapping(NorthboundMappingEntity northboundMappingEntity) {
            this.northboundMappings.add(northboundMappingEntity);
        }

        private void addSouthboundMapping(SouthboundMappingEntity southboundMappingEntity) {
            this.southboundMappings.add(southboundMappingEntity);
        }

        public boolean match(@NonNull TagWithMappings<T> that) {
            return Objects.equals(this.tag.getName(), that.tag.getName())
                    && Objects.equals(this.tag.getDescription(), that.tag.getDescription())
                    && Objects.equals(this.northboundMappings, that.northboundMappings)
                    && Objects.equals(this.southboundMappings, that.southboundMappings);
        }
    }

    private @NotNull ImportResult doImportLocked(
            final @NotNull List<DeviceTagRow> rows,
            final @NotNull ImportMode mode,
            final @NotNull String adapterId,
            final @Nullable BulkTagBrowser browser)
            throws DeviceTagImporterException {

        // Step 1: Resolve wildcards and namespace URIs
        final List<DeviceTagRow> resolved = new ArrayList<>();
        for (final DeviceTagRow row : rows) {
            // Resolve namespace URI to current index if adapter supports it
            final String resolvedNodeId = resolveNodeIdIfNeeded(browser, row.getNodeId(), row.getNamespaceUri());
            final DeviceTagRow.Builder builder = DeviceTagRow.builder()
                    .nodePath(row.getNodePath())
                    .namespaceUri(row.getNamespaceUri())
                    .namespaceIndex(row.getNamespaceIndex())
                    .nodeId(resolvedNodeId)
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

        // collect all the tags with their mappings from the File
        Map<String, TagWithMappings<TagEntity>> tagWithMappingsByNodeIdFromFile = new HashMap<>();
        for (final DeviceTagRow fileRow : resolved) {
            if (!fileRow.hasTag()
                    || fileRow.getNodeId() == null
                    || fileRow.getNodeId().isEmpty()) {
                continue;
            }
            final String nodeId = fileRow.getNodeId();
            final var tag = toTagEntity(fileRow);
            final var twm = tagWithMappingsByNodeIdFromFile.computeIfAbsent(nodeId, n -> new TagWithMappings<>(tag));
            if (!tagMatch(twm.tag, tag)) {
                throw new DeviceTagImporterException(
                        List.of(
                                new ValidationError(
                                        null,
                                        null,
                                        null,
                                        UPDATE_FAILED,
                                        "Failed to update adapter configuration. Two rows have the same nodeId but different tagNames")));
            }
            if (fileRow.hasNorthboundMapping()) {
                twm.addNorthboundMapping(toNorthboundMappingEntity(fileRow));
            }
            if (fileRow.hasSouthboundMapping()) {
                twm.addSouthboundMapping(toSouthboundMappingEntity(fileRow));
            }
        }

        // collect all tags with their mappings from the Edge
        Map<String, TagWithMappings<TagEntity>> tagWithMappingsByNodeIdFromEdge = new HashMap<>();
        final Map<String, String> nodeIdByTagNameFromEdge = new HashMap<>();
        for (final TagEntity tag : adapter.getTags()) {
            if (tag.getDefinition().get("node") == null) {
                continue;
            }
            final String nodeId = tag.getDefinition().get("node").toString();
            tagWithMappingsByNodeIdFromEdge.put(nodeId, new TagWithMappings<>(tag));
            nodeIdByTagNameFromEdge.put(tag.getName(), nodeId);
        }
        for (final NorthboundMappingEntity northboundMappingEntity : adapter.getNorthboundMappings()) {
            String nodeId = nodeIdByTagNameFromEdge.get(northboundMappingEntity.getTagName());
            final var twm = tagWithMappingsByNodeIdFromEdge.get(nodeId);
            if (twm == null) {
                // That is actually bad: the existing configuration had dangling tag reference
                continue;
            }
            twm.addNorthboundMapping(northboundMappingEntity);
        }
        for (final SouthboundMappingEntity southboundMappingEntity : adapter.getSouthboundMappings()) {
            String nodeId = nodeIdByTagNameFromEdge.get(southboundMappingEntity.getTagName());
            final var twm = tagWithMappingsByNodeIdFromEdge.get(nodeId);
            if (twm == null) {
                // That is actually bad: the existing configuration had dangling tag reference
                continue;
            }
            twm.addSouthboundMapping(southboundMappingEntity);
        }

        // Step 6: Determine actions per mode
        final List<TagAction> tagActions = new ArrayList<>();
        final List<TagEntity> finalTags = new ArrayList<>();
        final List<NorthboundMappingEntity> finalNorthbound = new ArrayList<>();
        final List<SouthboundMappingEntity> finalSouthbound = new ArrayList<>();
        int tagsCreated = 0, tagsUpdated = 0, tagsDeleted = 0;
        int nbCreated = 0, nbDeleted = 0, sbCreated = 0, sbDeleted = 0;

        // loop over all the nodeIds, both from the file and the adapter
        for (String nodeId :
                union(tagWithMappingsByNodeIdFromFile.keySet(), tagWithMappingsByNodeIdFromEdge.keySet())) {

            // get the edgeTag and fileRow, and the information where they exist
            final var twmFile = tagWithMappingsByNodeIdFromFile.get(nodeId);
            final String tagNameFile = twmFile != null ? twmFile.tag.getName() : null;
            final var twmEdge = tagWithMappingsByNodeIdFromEdge.get(nodeId);
            final String tagNameEdge = twmEdge != null ? twmEdge.tag.getName() : null;
            final boolean onlyFile = twmFile != null && twmEdge == null;
            final boolean onlyEdge = twmFile == null && twmEdge != null;
            final boolean bothSame = twmFile != null && twmEdge != null && twmFile.match(twmEdge);
            final boolean bothDiff = twmFile != null && twmEdge != null && !twmFile.match(twmEdge);

            // KEEP, 5+2, from Edge
            if (bothSame || (onlyEdge && (mode == ImportMode.MERGE_SAFE || mode == ImportMode.MERGE_OVERWRITE))) {
                requireNonNull(twmEdge);
                finalTags.add(twmEdge.tag);
                finalNorthbound.addAll(twmEdge.northboundMappings);
                finalSouthbound.addAll(twmEdge.southboundMappings);
                // no statistics for tags that we keep
            }

            // CREATE, 4, from File
            else if (onlyFile && mode != ImportMode.DELETE) {
                requireNonNull(twmFile);
                requireNonNull(tagNameFile);
                finalTags.add(twmFile.tag);
                finalNorthbound.addAll(twmFile.northboundMappings);
                finalSouthbound.addAll(twmFile.southboundMappings);
                tagActions.add(new TagAction(tagNameFile, TagAction.Action.CREATED));
                tagsCreated++;
                nbCreated += twmFile.northboundMappings.size();
                sbCreated += twmFile.southboundMappings.size();
            }

            // UPDATE, 2, from File
            else if (bothDiff && (mode == ImportMode.OVERWRITE || mode == ImportMode.MERGE_OVERWRITE)) {
                requireNonNull(twmFile);
                requireNonNull(tagNameFile);
                requireNonNull(twmEdge);
                finalTags.add(twmFile.tag);
                finalNorthbound.addAll(twmFile.northboundMappings);
                finalSouthbound.addAll(twmFile.southboundMappings);
                tagActions.add(new TagAction(tagNameFile, TagAction.Action.UPDATED));
                tagsUpdated++;
                nbDeleted += twmEdge.northboundMappings.size();
                sbDeleted += twmEdge.southboundMappings.size();
                nbCreated += twmFile.northboundMappings.size();
                sbCreated += twmFile.southboundMappings.size();
            }

            // DELETE, 2, from Edge
            else if (onlyEdge && (mode == ImportMode.DELETE || mode == ImportMode.OVERWRITE)) {
                requireNonNull(twmEdge);
                requireNonNull(tagNameEdge);
                tagActions.add(new TagAction(tagNameEdge, TagAction.Action.DELETED));
                tagsDeleted++;
                nbDeleted += twmEdge.northboundMappings.size();
                sbDeleted += twmEdge.southboundMappings.size();
            }

            // ERROR, 5
            else if ((onlyEdge && mode == ImportMode.CREATE)
                    || (onlyFile && mode == ImportMode.DELETE)
                    || (bothDiff && mode != ImportMode.OVERWRITE && mode != ImportMode.MERGE_OVERWRITE)) {
                throw new DeviceTagImporterException(List.of(new ValidationError(
                        null,
                        null,
                        null,
                        UPDATE_FAILED,
                        "Failed to update adapter configuration. Should have been caught in Validation")));
            } else {
                throw new DeviceTagImporterException(List.of(new ValidationError(
                        null,
                        null,
                        null,
                        UPDATE_FAILED,
                        "Failed to update adapter configuration. This shouldn't happen")));
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

    private static <T> Set<T> union(final Set<T> a, final Set<T> b) {
        final Set<T> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }
}
