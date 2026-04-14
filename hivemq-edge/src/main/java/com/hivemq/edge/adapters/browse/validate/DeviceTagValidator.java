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

import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataIdentifierReference;
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
import java.util.HashMap;
import java.util.HashSet;
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
    private static final int MAX_TOPIC_LENGTH = 65535; // MQTT spec limit
    private static final int MAX_DESCRIPTION_LENGTH = 1024;
    private static final int MAX_USER_PROPERTY_LENGTH = 256;
    private static final long MAX_MESSAGE_EXPIRY_INTERVAL = 4_294_967_295L; // MQTT uint32 max

    private final @NotNull ProtocolAdapterExtractor adapterExtractor;
    private final @NotNull DataCombiningExtractor combiningExtractor;

    @Inject
    public DeviceTagValidator(
            final @NotNull ProtocolAdapterExtractor adapterExtractor,
            final @NotNull DataCombiningExtractor combiningExtractor) {
        this.adapterExtractor = adapterExtractor;
        this.combiningExtractor = combiningExtractor;
    }

    // --- Row-level validations ---

    private static void validateMappingRequiresTag(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        if (row.hasTag()) {
            return;
        }
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

    private static void validateNorthboundTopic(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        final String topic = row.getNorthboundTopic();
        if (topic == null) {
            return;
        }
        if (topic.isEmpty() || topic.contains("\0")) {
            errors.add(new ValidationError(
                    rowNum, "northbound_topic", topic, INVALID_TOPIC, "Northbound topic contains invalid characters"));
        } else if (topic.length() > MAX_TOPIC_LENGTH) {
            errors.add(new ValidationError(
                    rowNum,
                    "northbound_topic",
                    topic.substring(0, 50) + "...",
                    INVALID_TOPIC,
                    "Northbound topic exceeds maximum length of " + MAX_TOPIC_LENGTH));
        } else if (topic.contains("+") || topic.contains("#")) {
            errors.add(new ValidationError(
                    rowNum,
                    "northbound_topic",
                    topic,
                    INVALID_TOPIC,
                    "Northbound topic must not contain MQTT wildcards (+ or #)"));
        }
    }

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
        } else if (topic.length() > MAX_TOPIC_LENGTH) {
            errors.add(new ValidationError(
                    rowNum,
                    "southbound_topic",
                    topic.substring(0, 50) + "...",
                    INVALID_TOPIC,
                    "Southbound topic filter exceeds maximum length of " + MAX_TOPIC_LENGTH));
        }
    }

    private static void validateTagDescription(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        final String desc = row.getTagDescription();
        if (desc == null) {
            return;
        }
        if (desc.length() > MAX_DESCRIPTION_LENGTH) {
            errors.add(new ValidationError(
                    rowNum,
                    "tag_description",
                    desc.substring(0, 50) + "...",
                    INVALID_TAG_NAME,
                    "Tag description exceeds maximum length of " + MAX_DESCRIPTION_LENGTH));
        }
    }

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
        if (expiry <= 0 || expiry > MAX_MESSAGE_EXPIRY_INTERVAL) {
            errors.add(new ValidationError(
                    rowNum,
                    "message_expiry_interval",
                    String.valueOf(expiry),
                    INVALID_EXPIRY,
                    "Message expiry interval must be between 1 and " + MAX_MESSAGE_EXPIRY_INTERVAL + " seconds"));
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
            } else if (entry.getKey().length() > MAX_USER_PROPERTY_LENGTH) {
                errors.add(new ValidationError(
                        rowNum,
                        "mqtt_user_properties",
                        entry.getKey().substring(0, 50) + "...",
                        INVALID_USER_PROPERTIES,
                        "User property key exceeds maximum length of " + MAX_USER_PROPERTY_LENGTH));
            }
            if (entry.getValue() != null && entry.getValue().length() > MAX_USER_PROPERTY_LENGTH) {
                errors.add(new ValidationError(
                        rowNum,
                        "mqtt_user_properties",
                        entry.getKey(),
                        INVALID_USER_PROPERTIES,
                        "User property value exceeds maximum length of " + MAX_USER_PROPERTY_LENGTH));
            }
        }
    }

    private static void validateNodeId(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
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
        if (tagName != null && tagName.length() > MAX_TAG_NAME_LENGTH) {
            errors.add(new ValidationError(
                    rowNum,
                    "tag_name",
                    tagName,
                    INVALID_TAG_NAME,
                    "Tag name exceeds maximum length of " + MAX_TAG_NAME_LENGTH + " characters"));
        } else if (tagName != null && !TAG_NAME_PATTERN.matcher(tagName).matches()) {
            errors.add(new ValidationError(
                    rowNum, "tag_name", tagName, INVALID_TAG_NAME, "Tag name must not contain whitespace"));
        }
    }

    /**
     * Validate rows for import. Rows should have wildcards already resolved by the caller.
     *
     * @param fileRows  the rows to validate
     * @param mode      the import mode
     * @param adapterId the target adapter ID
     * @return list of validation errors (empty if valid)
     */
    public @NotNull List<ValidationError> validate(
            final @NotNull List<DeviceTagRow> fileRows,
            final @NotNull ImportMode mode,
            final @NotNull String adapterId) {
        final List<ValidationError> errors = new ArrayList<>();

        // Load current adapter state
        final Optional<ProtocolAdapterEntity> adapterOpt = adapterExtractor.getAdapterByAdapterId(adapterId);
        final ProtocolAdapterEntity adapter = adapterOpt.orElse(null);

        // Row-level validations
        for (int i = 0; i < fileRows.size(); i++) {
            final DeviceTagRow fileRow = fileRows.get(i);
            final int rowNum = i + 1;

            // Mapping-without-tag check applies to ALL rows (including those without tags)
            validateMappingRequiresTag(fileRow, rowNum, errors);

            // Remaining validations only apply to rows with tagName
            if (!fileRow.hasTag()) {
                continue;
            }

            validateTagName(fileRow, rowNum, errors);
            validateTagDescription(fileRow, rowNum, errors);
            validateNorthboundTopic(fileRow, rowNum, errors);
            validateSouthboundTopic(fileRow, rowNum, errors);
            validateQos(fileRow, rowNum, errors);
            validateExpiryInterval(fileRow, rowNum, errors);
            validateFieldMapping(fileRow, rowNum, errors);
            validateUserProperties(fileRow, rowNum, errors);
            validateNodeId(fileRow, rowNum, errors);
        }

        // File-level validations
        validateFileLevelDuplicates(fileRows, errors);

        // Cross-reference validations
        if (adapter != null) {
            validateCrossReferences(fileRows, mode, adapter, errors, adapterId);
        }

        return errors;
    }

    private void validateFileLevelDuplicates(
            final @NotNull List<DeviceTagRow> fileRows, final @NotNull List<ValidationError> errors) {

        // Duplicate nodeId: allowed only if all rows with the same nodeId share the same tagName
        final Map<String, DeviceTagRow> fileRowByNodeId = new HashMap<>();
        for (int i = 0; i < fileRows.size(); i++) {
            final DeviceTagRow fileRow = fileRows.get(i);
            final int rowNum = i + 1;
            if (!fileRow.hasTag()
                    || fileRow.getNodeId() == null
                    || fileRow.getNodeId().isEmpty()) {
                continue;
            }
            final DeviceTagRow fileRow1 = fileRowByNodeId.putIfAbsent(fileRow.getNodeId(), fileRow);
            if (fileRow1 != null && !Objects.equals(fileRow1.getTagName(), fileRow.getTagName())) {
                errors.add(new ValidationError(
                        rowNum,
                        "node_id",
                        fileRow.getNodeId(),
                        DUPLICATE_NODE,
                        "Duplicate node ID '" + fileRow.getNodeId() + "' with different tag name in file"));
            }
        }

        // Duplicate tagBane: allowed only if all rows with the same tagName share the same nodeId and tag definition
        final Map<String, DeviceTagRow> fileRowByTagName = new HashMap<>();
        for (int i = 0; i < fileRows.size(); i++) {
            final DeviceTagRow fileRow = fileRows.get(i);
            final int rowNum = i + 1;
            if (!fileRow.hasTag()
                    || fileRow.getNodeId() == null
                    || fileRow.getNodeId().isEmpty()) {
                continue;
            }
            final DeviceTagRow fileRow1 = fileRowByTagName.putIfAbsent(fileRow.getTagName(), fileRow);
            if (fileRow1 != null
                    && !(Objects.equals(fileRow1.getNodeId(), fileRow.getNodeId())
                            && Objects.equals(fileRow1.getTagDescription(), fileRow.getTagDescription()))) {
                errors.add(new ValidationError(
                        rowNum,
                        "tag_name",
                        fileRow.getTagName(),
                        DUPLICATE_TAG_NAME,
                        "Duplicate tag name '" + fileRow.getTagName() + "' with different definition in file"));
            }
        }
    }

    private void validateCrossReferences(
            final @NotNull List<DeviceTagRow> fileRows,
            final @NotNull ImportMode mode,
            final @NotNull ProtocolAdapterEntity adapter,
            final @NotNull List<ValidationError> errors,
            final @NotNull String adapterId) {

        // collect the fileTags (fileRows) that exist in the file - indexed by their nodeId
        final Map<String, DeviceTagRow> fileRowsByNodeId = new HashMap<>();
        for (final DeviceTagRow fileRow : fileRows) {
            if (fileRow.hasTag()
                    && fileRow.getNodeId() != null
                    && !fileRow.getNodeId().isEmpty()) {
                fileRowsByNodeId.put(fileRow.getNodeId(), fileRow);
            }
        }

        // collect the edgeTags that exist in the adapter - indexed by their nodeId
        final Map<String, TagEntity> edgeTagByNodeId = new HashMap<>();
        for (final TagEntity edgeTag : adapter.getTags()) {
            final Object nodeId = edgeTag.getDefinition().get("node");
            if (nodeId != null) {
                edgeTagByNodeId.put(nodeId.toString(), edgeTag);
            }
        }

        // collect the (keyset) of all the tags that will be created, updated, or kept
        // represented as map of their nodeIds, to detect tags that will be duplicated
        final Map<String, Set<String>> nodeIdsByTagName = new HashMap<>();

        // loop over all the nodeIds, both from the file and the adapter
        for (final String nodeId : union(fileRowsByNodeId.keySet(), edgeTagByNodeId.keySet())) {

            // get the edgeTag and fileRow, and the information where they exist
            final DeviceTagRow fileRow = fileRowsByNodeId.get(nodeId);
            final String fileTagName = fileRow != null ? fileRow.getTagName() : "";
            final TagEntity edgeTag = edgeTagByNodeId.get(nodeId);
            final String edgeTagName = edgeTag != null ? edgeTag.getName() : "";
            final boolean fileOnly = fileRow != null && edgeTag == null;
            final boolean edgeOnly = fileRow == null && edgeTag != null;
            final boolean bothSame = fileRow != null && edgeTag != null && tagDefinitionsMatch(fileRow, edgeTag);
            final boolean bothDiff = fileRow != null && edgeTag != null && !tagDefinitionsMatch(fileRow, edgeTag);

            // collect the tags that will exist after the import, with their nodeIds
            if (bothSame || (fileOnly && mode != ImportMode.DELETE)) {
                nodeIdsByTagName
                        .computeIfAbsent(fileTagName, k -> new HashSet<>())
                        .add(nodeId);
            } else if (edgeOnly && (mode == ImportMode.MERGE_SAFE || mode == ImportMode.MERGE_OVERWRITE)) {
                nodeIdsByTagName
                        .computeIfAbsent(edgeTagName, k -> new HashSet<>())
                        .add(nodeId);
            }

            // raise an error if we are creating, updating, or deleting when we shouldn't
            if (fileOnly && mode == ImportMode.DELETE) {
                errors.add(new ValidationError(
                        null,
                        "tag_name",
                        fileTagName,
                        TAG_CONFLICT,
                        "Tag '" + fileTagName
                                + "' (node: "
                                + nodeId
                                + ") exists in the file but not on the adapter and would be created. "
                                + "Use CREATE, OVERWRITE, or MERGE mode to create."));
            }
            if (bothDiff && mode != ImportMode.OVERWRITE && mode != ImportMode.MERGE_OVERWRITE) {
                errors.add(new ValidationError(
                        null,
                        "tag_name",
                        fileTagName,
                        TAG_CONFLICT,
                        "Tag '" + fileTagName
                                + "' (node: "
                                + nodeId
                                + ") exists on the adapter and the file with different definitions and would be updated. "
                                + "Use OVERWRITE or MERGE_OVERWRITE mode to update."));
            }
            if (edgeOnly && mode == ImportMode.CREATE) {
                errors.add(new ValidationError(
                        null,
                        "tag_name",
                        edgeTagName,
                        TAG_CONFLICT,
                        "Tag '" + edgeTagName
                                + "' (node "
                                + nodeId
                                + ") exists on adapter but not in file and would be deleted. "
                                + "Use DELETE or OVERWRITE to delete - or MERGE to keep."));
            }
        }

        // raise an error if we are creating, updating, or keeping (duplicate) tags for multiple nodeIds
        for (String tagName : nodeIdsByTagName.keySet()) {
            if (nodeIdsByTagName.get(tagName).size() > 1) {
                errors.add(new ValidationError(
                        null,
                        "tag_name",
                        tagName,
                        TAG_CONFLICT,
                        "Tag '" + tagName
                                + "' (nodes: "
                                + nodeIdsByTagName.get(tagName)
                                + ") will be assigned to multiple nodes. "
                                + "Ensure uniqueness of the tag names."));
            }
        }

        // raise an error if we are deleting a tag that is used in a combiner
        for (String tagName : difference(collectCombinerTags(adapterId), nodeIdsByTagName.keySet())) {
            errors.add(new ValidationError(
                    null,
                    "tag_name",
                    tagName,
                    TAG_IN_USE_BY_COMBINER,
                    "Tag '" + tagName
                            + "' is used in a combiner, but not created, updated, or kept. "
                            + "Ensure that there are no dangling references."));
        }
    }

    private @NotNull Set<String> collectCombinerTags(final @NotNull String adapterId) {
        final Set<String> tagNames = new HashSet<>();
        for (final DataCombiner combiner : combiningExtractor.getAllCombiners()) {
            for (final DataCombining mapping : combiner.dataCombinings()) {
                final DataIdentifierReference p = mapping.sources().primaryReference();
                if (p != null
                        && p.type() == DataIdentifierReference.Type.TAG
                        && p.scope() != null
                        && p.scope().equals(adapterId)) {
                    tagNames.add(p.id());
                }
                for (final var inst : mapping.instructions()) {
                    final DataIdentifierReference s = inst.dataIdentifierReference();
                    if (s != null
                            && s.type() == DataIdentifierReference.Type.TAG
                            && s.scope() != null
                            && s.scope().equals(adapterId)) {
                        tagNames.add(s.id());
                    }
                }
            }
        }
        return tagNames;
    }

    private static <T> Set<T> union(final Set<T> a, final Set<T> b) {
        final Set<T> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }

    private static <T> Set<T> difference(final Set<T> a, final Set<T> b) {
        final Set<T> result = new HashSet<>(a);
        result.removeAll(b);
        return result;
    }
}
