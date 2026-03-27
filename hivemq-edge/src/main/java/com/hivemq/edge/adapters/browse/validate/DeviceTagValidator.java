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
import static java.util.Objects.requireNonNull;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
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
        } else if (topic.contains("+") || topic.contains("#")) {
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
        final String rowDesc = row.getTagDescription() != null ? row.getTagDescription() : "";
        final String existingDesc = existing.getDescription() != null ? existing.getDescription() : "";
        return Objects.equals(row.getTagName(), existing.getName()) && Objects.equals(rowDesc, existingDesc);
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
            validateCrossReferences(rows, mode, adapter, errors);
        }

        return errors;
    }

    private void validateFileLevelDuplicates(
            final @NotNull List<DeviceTagRow> rows, final @NotNull List<ValidationError> errors) {
        // Duplicate nodeId: allowed only if all rows with the same nodeId share the same tagName
        final Map<String, DeviceTagRow> firstByNodeId = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            final DeviceTagRow row = rows.get(i);
            if (row.getNodeId() == null || row.getNodeId().isEmpty()) {
                continue;
            }
            final DeviceTagRow first = firstByNodeId.putIfAbsent(row.getNodeId(), row);
            if (first != null && !Objects.equals(first.getTagName(), row.getTagName())) {
                errors.add(new ValidationError(
                        i + 1,
                        "node_id",
                        row.getNodeId(),
                        DUPLICATE_NODE,
                        "Duplicate node ID '" + row.getNodeId() + "' with different tag name in file"));
            }
        }

        // Duplicate tagName: allowed if nodeId and description are identical (multi-mapping rows).
        // Rows that share a tagName but differ in nodeId or description are rejected.
        final Map<String, DeviceTagRow> firstByTagName = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            final DeviceTagRow row = rows.get(i);
            if (row.getTagName() == null || row.getTagName().isEmpty()) {
                continue;
            }
            final DeviceTagRow first = firstByTagName.putIfAbsent(row.getTagName(), row);
            if (first != null) {
                if (!Objects.equals(first.getNodeId(), row.getNodeId())
                        || !Objects.equals(first.getTagDescription(), row.getTagDescription())) {
                    errors.add(new ValidationError(
                            i + 1,
                            "tag_name",
                            row.getTagName(),
                            DUPLICATE_TAG_NAME,
                            "Duplicate tag name '" + row.getTagName() + "' with different definition in file"));
                }
            }
        }
    }

    private void validateCrossReferences(
            final @NotNull List<DeviceTagRow> rows,
            final @NotNull ImportMode mode,
            final @NotNull ProtocolAdapterEntity adapter,
            final @NotNull List<ValidationError> errors) {

        // Build nodeId-keyed maps for classification
        final Map<String, DeviceTagRow> fileRowsByNodeId = new LinkedHashMap<>();
        for (final DeviceTagRow row : rows) {
            if (row.hasTag() && row.getNodeId() != null && !row.getNodeId().isEmpty()) {
                fileRowsByNodeId.put(row.getNodeId(), row);
            }
        }

        final Map<String, TagEntity> edgeTagsByNodeId = new LinkedHashMap<>();
        for (final TagEntity tag : adapter.getTags()) {
            final Object node = tag.getDefinition().get("node");
            if (node != null) {
                edgeTagsByNodeId.put(node.toString(), tag);
            }
        }

        // Classify by nodeId
        final Set<String> edgeOnlyNodeIds = new HashSet<>(edgeTagsByNodeId.keySet());
        edgeOnlyNodeIds.removeAll(fileRowsByNodeId.keySet());

        final Set<String> fileOnlyNodeIds = new HashSet<>(fileRowsByNodeId.keySet());
        fileOnlyNodeIds.removeAll(edgeTagsByNodeId.keySet());

        final Set<String> inBothNodeIds = new HashSet<>(fileRowsByNodeId.keySet());
        inBothNodeIds.retainAll(edgeTagsByNodeId.keySet());

        // Combiner check: tags to be deleted must not be in use by combiners
        if (mode == ImportMode.DELETE || mode == ImportMode.OVERWRITE) {
            final Set<String> combinerTags = collectCombinerTags();
            for (final String nodeId : edgeOnlyNodeIds) {
                final String tagName =
                        requireNonNull(edgeTagsByNodeId.get(nodeId)).getName();
                if (combinerTags.contains(tagName)) {
                    errors.add(new ValidationError(
                            null,
                            "tag_name",
                            tagName,
                            TAG_IN_USE_BY_COMBINER,
                            "Tag '" + tagName + "' is in use by a combiner and cannot be deleted"));
                }
            }
        }

        // Mode-specific conflict checks (by nodeId)
        if (mode == ImportMode.CREATE) {
            // Edge-only nodeIds are an error in CREATE mode
            for (final String nodeId : edgeOnlyNodeIds) {
                final TagEntity edgeTag = requireNonNull(edgeTagsByNodeId.get(nodeId));
                errors.add(
                        new ValidationError(
                                null,
                                "tag_name",
                                edgeTag.getName(),
                                TAG_CONFLICT,
                                "Tag '"
                                        + edgeTag.getName()
                                        + "' (node "
                                        + nodeId
                                        + ") exists on adapter but not in file. CREATE requires an empty adapter or use OVERWRITE mode."));
            }
            // inBoth with different definition is an error
            for (final String nodeId : inBothNodeIds) {
                final DeviceTagRow row = requireNonNull(fileRowsByNodeId.get(nodeId));
                final TagEntity existing = requireNonNull(edgeTagsByNodeId.get(nodeId));
                if (!tagDefinitionsMatch(row, existing)) {
                    errors.add(new ValidationError(
                            null,
                            "tag_name",
                            row.getTagName(),
                            TAG_CONFLICT,
                            "Tag for node '" + nodeId + "' exists with different definition. Use OVERWRITE mode."));
                }
            }
        } else if (mode == ImportMode.DELETE) {
            // File-only nodeIds are an error in DELETE mode
            for (final String nodeId : fileOnlyNodeIds) {
                final DeviceTagRow row = requireNonNull(fileRowsByNodeId.get(nodeId));
                errors.add(new ValidationError(
                        null,
                        "tag_name",
                        row.getTagName(),
                        TAG_CONFLICT,
                        "Tag '"
                                + row.getTagName()
                                + "' (node "
                                + nodeId
                                + ") is in the file but does not exist on the adapter. DELETE cannot create tags."));
            }
            // inBoth with different definition is an error
            for (final String nodeId : inBothNodeIds) {
                final DeviceTagRow row = requireNonNull(fileRowsByNodeId.get(nodeId));
                final TagEntity existing = requireNonNull(edgeTagsByNodeId.get(nodeId));
                if (!tagDefinitionsMatch(row, existing)) {
                    errors.add(new ValidationError(
                            null,
                            "tag_name",
                            row.getTagName(),
                            TAG_CONFLICT,
                            "Tag for node '"
                                    + nodeId
                                    + "' exists with different definition. DELETE cannot modify tags."));
                }
            }
        } else if (mode == ImportMode.MERGE_SAFE) {
            // inBoth with different definition (tagName or description change)
            for (final String nodeId : inBothNodeIds) {
                final DeviceTagRow row = requireNonNull(fileRowsByNodeId.get(nodeId));
                final TagEntity existing = requireNonNull(edgeTagsByNodeId.get(nodeId));
                if (!tagDefinitionsMatch(row, existing)) {
                    errors.add(new ValidationError(
                            null,
                            "tag_name",
                            row.getTagName(),
                            TAG_CONFLICT,
                            "Tag for node '"
                                    + nodeId
                                    + "' exists with different definition. Use MERGE_OVERWRITE mode."));
                }
            }
        }

        // TagName collision: file tags must not reuse a tagName that belongs to a surviving edge-only tag
        if (mode == ImportMode.MERGE_SAFE || mode == ImportMode.MERGE_OVERWRITE) {
            final Set<String> survivingEdgeTagNames = new HashSet<>();
            for (final String nodeId : edgeOnlyNodeIds) {
                survivingEdgeTagNames.add(
                        requireNonNull(edgeTagsByNodeId.get(nodeId)).getName());
            }

            for (final String nodeId : fileOnlyNodeIds) {
                final DeviceTagRow row = requireNonNull(fileRowsByNodeId.get(nodeId));
                if (survivingEdgeTagNames.contains(row.getTagName())) {
                    errors.add(
                            new ValidationError(
                                    null,
                                    "tag_name",
                                    row.getTagName(),
                                    TAG_CONFLICT,
                                    "Tag name '"
                                            + row.getTagName()
                                            + "' collides with an existing tag on a different device node. Use OVERWRITE mode or choose a different tag name."));
                }
            }

            for (final String nodeId : inBothNodeIds) {
                final DeviceTagRow row = requireNonNull(fileRowsByNodeId.get(nodeId));
                final TagEntity existing = requireNonNull(edgeTagsByNodeId.get(nodeId));
                if (!requireNonNull(row.getTagName()).equals(existing.getName())
                        && survivingEdgeTagNames.contains(row.getTagName())) {
                    errors.add(
                            new ValidationError(
                                    null,
                                    "tag_name",
                                    row.getTagName(),
                                    TAG_CONFLICT,
                                    "Renamed tag '"
                                            + row.getTagName()
                                            + "' collides with an existing tag on a different device node. Choose a different tag name."));
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
