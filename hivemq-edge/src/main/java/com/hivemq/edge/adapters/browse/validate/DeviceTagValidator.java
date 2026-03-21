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
package com.hivemq.edge.adapters.browse.validate;

import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.DUPLICATE_NODE;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.DUPLICATE_TAG_NAME;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.EDGE_TAG_CONFLICT;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.INVALID_EXPIRY;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.INVALID_FIELD_MAPPING;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.INVALID_NODE_ID;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.INVALID_QOS;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.INVALID_TAG_NAME;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.INVALID_TOPIC;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.INVALID_USER_PROPERTIES;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.MAPPING_WITHOUT_TAG;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.TAG_CONFLICT;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.TAG_IN_USE_BY_COMBINER;

import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import com.hivemq.edge.adapters.browse.model.FieldMappingInstruction;
import com.hivemq.edge.adapters.browse.model.ImportMode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Validates a list of {@link DeviceTagRow} entries before import.
 * Collects all errors (no short-circuiting). Row numbers are 1-indexed.
 */
@Singleton
public class DeviceTagValidator {

    private static final @NotNull Pattern TAG_NAME_PATTERN = Pattern.compile("\\S+");
    private static final int MAX_TAG_NAME_LENGTH = 256;

    private final @NotNull ProtocolAdapterExtractor adapterExtractor;
    private final @NotNull DataCombiningExtractor combiningExtractor;

    @Inject
    public DeviceTagValidator(
            final @NotNull ProtocolAdapterExtractor adapterExtractor,
            final @NotNull DataCombiningExtractor combiningExtractor) {
        this.adapterExtractor = adapterExtractor;
        this.combiningExtractor = combiningExtractor;
    }

    private static void validateNorthboundTopic(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        final String topic = row.getNorthboundTopic();
        if (topic == null) {
            return;
        }
        if (topic.isEmpty() || topic.contains("\0")) {
            errors.add(new ValidationError(
                    rowNum, "northbound_topic", topic, INVALID_TOPIC, "Northbound topic contains invalid characters"));
        }
        if (topic.contains("+") || topic.contains("#")) {
            errors.add(new ValidationError(
                    rowNum,
                    "northbound_topic",
                    topic,
                    INVALID_TOPIC,
                    "Northbound topic must not contain MQTT wildcards (+ or #)"));
        }
    }

    // --- File-level validations ---

    private static void validateSouthboundTopic(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        final String topic = row.getSouthboundTopic();
        if (topic == null) {
            return;
        }
        if (topic.isEmpty() || topic.contains("\0")) {
            errors.add(new ValidationError(
                    rowNum,
                    "southbound_topic",
                    topic,
                    INVALID_TOPIC,
                    "Southbound topic filter contains invalid characters"));
        }
    }

    // --- Row-level validations ---

    private static void validateQos(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        final Integer qos = row.getMaxQos();
        if (qos == null) {
            return;
        }
        if (qos < 0 || qos > 2) {
            errors.add(
                    new ValidationError(rowNum, "max_qos", String.valueOf(qos), INVALID_QOS, "QoS must be 0, 1, or 2"));
        }
    }

    private static void validateExpiryInterval(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        final Long expiry = row.getMessageExpiryInterval();
        if (expiry == null) {
            return;
        }
        if (expiry <= 0) {
            errors.add(new ValidationError(
                    rowNum,
                    "message_expiry_interval",
                    String.valueOf(expiry),
                    INVALID_EXPIRY,
                    "Message expiry interval must be greater than 0"));
        }
    }

    private static void validateFieldMapping(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        final List<FieldMappingInstruction> mappings = row.getSouthboundFieldMapping();
        if (mappings == null) {
            return;
        }
        for (final FieldMappingInstruction fm : mappings) {
            if (fm.source() == null || fm.source().isEmpty()) {
                errors.add(new ValidationError(
                        rowNum,
                        "southbound_field_mapping",
                        null,
                        INVALID_FIELD_MAPPING,
                        "Field mapping source must not be empty"));
            }
            if (fm.destination() == null || fm.destination().isEmpty()) {
                errors.add(new ValidationError(
                        rowNum,
                        "southbound_field_mapping",
                        null,
                        INVALID_FIELD_MAPPING,
                        "Field mapping destination must not be empty"));
            }
        }
    }

    private static void validateUserProperties(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        final Map<String, String> props = row.getMqttUserProperties();
        if (props == null) {
            return;
        }
        for (final Map.Entry<String, String> entry : props.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty()) {
                errors.add(new ValidationError(
                        rowNum,
                        "mqtt_user_properties",
                        null,
                        INVALID_USER_PROPERTIES,
                        "User property key must not be empty"));
            }
        }
    }

    private static void validateMappingRequiresTag(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        if (!row.hasTag()) {
            if (row.getNorthboundTopic() != null && !row.getNorthboundTopic().isEmpty()) {
                errors.add(new ValidationError(
                        rowNum,
                        "northbound_topic",
                        row.getNorthboundTopic(),
                        MAPPING_WITHOUT_TAG,
                        "Northbound mapping requires a tag name"));
            }
            if (row.getSouthboundTopic() != null && !row.getSouthboundTopic().isEmpty()) {
                errors.add(new ValidationError(
                        rowNum,
                        "southbound_topic",
                        row.getSouthboundTopic(),
                        MAPPING_WITHOUT_TAG,
                        "Southbound mapping requires a tag name"));
            }
        }
    }

    private static void validateNodeId(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        if (!row.hasTag()) {
            return;
        }
        final String nodeId = row.getNodeId();
        if (nodeId == null || nodeId.isEmpty()) {
            errors.add(new ValidationError(
                    rowNum, "node_id", null, INVALID_NODE_ID, "Node ID is required when creating a tag"));
        }
    }

    private static boolean tagDefinitionsMatch(final @NotNull DeviceTagRow row, final @NotNull TagEntity existing) {
        final Map<String, Object> existingDef = existing.getDefinition();
        final String existingNode =
                existingDef.get("node") != null ? existingDef.get("node").toString() : null;
        return Objects.equals(row.getNodeId(), existingNode);
    }

    private static void validateTagName(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        final String tagName = row.getTagName();
        if (tagName == null || tagName.isEmpty()) {
            return;
        }
        if (tagName.length() > MAX_TAG_NAME_LENGTH) {
            errors.add(new ValidationError(
                    rowNum,
                    "tag_name",
                    tagName,
                    INVALID_TAG_NAME,
                    "Tag name exceeds maximum length of " + MAX_TAG_NAME_LENGTH + " characters"));
        } else if (!TAG_NAME_PATTERN.matcher(tagName).matches()) {
            errors.add(new ValidationError(
                    rowNum, "tag_name", tagName, INVALID_TAG_NAME, "Tag name must not contain whitespace"));
        }
    }

    /**
     * Validate rows for import. Rows should have wildcards already resolved by the caller.
     *
     * @param rows      the rows to validate
     * @param mode      the import mode
     * @param adapterId the target adapter ID
     * @return list of validation errors (empty if valid)
     */
    public @NotNull List<ValidationError> validate(
            final @NotNull List<DeviceTagRow> rows, final @NotNull ImportMode mode, final @NotNull String adapterId) {
        final List<ValidationError> errors = new ArrayList<>();

        // Load current adapter state
        final Optional<ProtocolAdapterEntity> adapterOpt = adapterExtractor.getAdapterByAdapterId(adapterId);
        final ProtocolAdapterEntity adapter = adapterOpt.orElse(null);

        // File-level validations
        validateFileLevelDuplicates(rows, errors);

        // Row-level validations
        for (int i = 0; i < rows.size(); i++) {
            final int rowNum = i + 1;
            final DeviceTagRow row = rows.get(i);

            // Mapping-without-tag check applies to ALL rows (including those without tags)
            validateMappingRequiresTag(row, rowNum, errors);

            if (!row.hasTag()) {
                continue; // Remaining validations only apply to rows with tagName
            }

            validateTagName(row, rowNum, errors);
            validateNorthboundTopic(row, rowNum, errors);
            validateSouthboundTopic(row, rowNum, errors);
            validateQos(row, rowNum, errors);
            validateExpiryInterval(row, rowNum, errors);
            validateFieldMapping(row, rowNum, errors);
            validateUserProperties(row, rowNum, errors);
            validateNodeId(row, rowNum, errors);
        }

        // Cross-reference validations
        if (adapter != null) {
            validateCrossReferences(rows, mode, adapter, adapterId, errors);
        }

        return errors;
    }

    private void validateFileLevelDuplicates(
            final @NotNull List<DeviceTagRow> rows, final @NotNull List<ValidationError> errors) {
        // Check duplicate node IDs
        final Set<String> seenNodeIds = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            final DeviceTagRow row = rows.get(i);
            if (row.getNodeId() != null && !row.getNodeId().isEmpty()) {
                if (!seenNodeIds.add(row.getNodeId())) {
                    errors.add(new ValidationError(
                            i + 1,
                            "node_id",
                            row.getNodeId(),
                            DUPLICATE_NODE,
                            "Duplicate node ID '" + row.getNodeId() + "' in file"));
                }
            }
        }

        // Check duplicate tag names
        final Set<String> seenTagNames = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            final DeviceTagRow row = rows.get(i);
            if (row.getTagName() != null && !row.getTagName().isEmpty()) {
                if (!seenTagNames.add(row.getTagName())) {
                    errors.add(new ValidationError(
                            i + 1,
                            "tag_name",
                            row.getTagName(),
                            DUPLICATE_TAG_NAME,
                            "Duplicate tag name '" + row.getTagName() + "' in file"));
                }
            }
        }
    }

    private void validateCrossReferences(
            final @NotNull List<DeviceTagRow> rows,
            final @NotNull ImportMode mode,
            final @NotNull ProtocolAdapterEntity adapter,
            final @NotNull String adapterId,
            final @NotNull List<ValidationError> errors) {
        final Set<String> fileTagNames = rows.stream()
                .filter(DeviceTagRow::hasTag)
                .map(DeviceTagRow::getTagName)
                .collect(Collectors.toSet());
        final Map<String, TagEntity> edgeTagsByName =
                adapter.getTags().stream().collect(Collectors.toMap(TagEntity::getName, t -> t));

        // Check if tags to be deleted are in use by combiners
        if (mode == ImportMode.DELETE || mode == ImportMode.OVERWRITE) {
            final Set<String> tagsToDelete = new HashSet<>(edgeTagsByName.keySet());
            if (mode == ImportMode.DELETE) {
                // DELETE mode: all edge-only tags are deleted
                tagsToDelete.removeAll(fileTagNames);
            }
            // OVERWRITE: tags not in file are deleted
            if (mode == ImportMode.OVERWRITE) {
                tagsToDelete.removeAll(fileTagNames);
            }

            final Set<String> combinerTags = collectCombinerTags();

            for (final String tagToDelete : tagsToDelete) {
                if (combinerTags.contains(tagToDelete)) {
                    errors.add(new ValidationError(
                            null,
                            "tag_name",
                            tagToDelete,
                            TAG_IN_USE_BY_COMBINER,
                            "Tag '" + tagToDelete + "' is in use by a combiner and cannot be deleted"));
                }
            }
        }

        // Check for tag name conflicts with other adapters
        for (final DeviceTagRow row : rows) {
            if (!row.hasTag()) {
                continue;
            }
            final String tagName = row.getTagName();

            // Check against all other adapters' tags
            for (final ProtocolAdapterEntity otherAdapter : adapterExtractor.getAllConfigs()) {
                if (otherAdapter.getAdapterId().equals(adapterId)) {
                    continue;
                }
                for (final TagEntity otherTag : otherAdapter.getTags()) {
                    if (otherTag.getName().equals(tagName)) {
                        errors.add(new ValidationError(
                                null,
                                "tag_name",
                                tagName,
                                EDGE_TAG_CONFLICT,
                                "Tag name '" + tagName
                                        + "' is already used by adapter '"
                                        + otherAdapter.getAdapterId()
                                        + "'"));
                    }
                }
            }
        }

        // Mode-specific conflict checks
        if (mode == ImportMode.CREATE) {
            // Edge-only tags are an error in CREATE mode
            final Set<String> edgeOnlyTags = new HashSet<>(edgeTagsByName.keySet());
            edgeOnlyTags.removeAll(fileTagNames);
            for (final String edgeOnly : edgeOnlyTags) {
                errors.add(
                        new ValidationError(
                                null,
                                "tag_name",
                                edgeOnly,
                                TAG_CONFLICT,
                                "Tag '" + edgeOnly
                                        + "' exists on adapter but not in file. CREATE requires an empty adapter or use OVERWRITE mode."));
            }
            // Both-exist with different definition is an error
            for (final DeviceTagRow row : rows) {
                if (!row.hasTag()) {
                    continue;
                }
                final TagEntity existing = edgeTagsByName.get(row.getTagName());
                if (existing != null && !tagDefinitionsMatch(row, existing)) {
                    errors.add(new ValidationError(
                            null,
                            "tag_name",
                            row.getTagName(),
                            TAG_CONFLICT,
                            "Tag '" + row.getTagName() + "' exists with different definition. Use OVERWRITE mode."));
                }
            }
            // Both-exist with identical definition is a noop — no error
        } else if (mode == ImportMode.DELETE) {
            // File-only tags are an error in DELETE mode
            final Set<String> fileOnlyTags = new HashSet<>(fileTagNames);
            fileOnlyTags.removeAll(edgeTagsByName.keySet());
            for (final String fileOnly : fileOnlyTags) {
                errors.add(new ValidationError(
                        null,
                        "tag_name",
                        fileOnly,
                        TAG_CONFLICT,
                        "Tag '" + fileOnly
                                + "' is in the file but does not exist on the adapter. DELETE cannot create tags."));
            }
            // Both-exist with different definition is an error
            for (final DeviceTagRow row : rows) {
                if (!row.hasTag()) {
                    continue;
                }
                final TagEntity existing = edgeTagsByName.get(row.getTagName());
                if (existing != null && !tagDefinitionsMatch(row, existing)) {
                    errors.add(new ValidationError(
                            null,
                            "tag_name",
                            row.getTagName(),
                            TAG_CONFLICT,
                            "Tag '" + row.getTagName()
                                    + "' exists with different definition. DELETE cannot modify tags."));
                }
            }
        } else if (mode == ImportMode.MERGE_SAFE) {
            // MERGE_SAFE: fail if same tag exists with different properties
            for (final DeviceTagRow row : rows) {
                if (!row.hasTag()) {
                    continue;
                }
                final TagEntity existing = edgeTagsByName.get(row.getTagName());
                if (existing != null && !tagDefinitionsMatch(row, existing)) {
                    errors.add(new ValidationError(
                            null,
                            "tag_name",
                            row.getTagName(),
                            TAG_CONFLICT,
                            "Tag '" + row.getTagName()
                                    + "' exists with different definition. Use MERGE_OVERWRITE mode."));
                }
            }
        }
    }

    private @NotNull Set<String> collectCombinerTags() {
        final Set<String> tags = new HashSet<>();
        for (final DataCombiner combiner : combiningExtractor.getAllCombiners()) {
            for (final DataCombining combining : combiner.dataCombinings()) {
                if (combining.sources() != null && combining.sources().tags() != null) {
                    tags.addAll(combining.sources().tags());
                }
            }
        }
        return tags;
    }
}
