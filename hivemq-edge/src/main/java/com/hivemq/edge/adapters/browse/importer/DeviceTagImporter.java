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
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.UPDATE_FAILED;
import static com.hivemq.edge.adapters.browse.validate.ValidationError.Code.WILDCARD_NO_DEFAULT;
import static java.util.Objects.requireNonNull;

import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.configuration.entity.adapter.MqttUserPropertyEntity;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.SouthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.InstructionEntity;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.adapters.browse.BrowseException;
import com.hivemq.edge.adapters.browse.BulkTagBrowser;
import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import com.hivemq.edge.adapters.browse.model.FieldMappingInstruction;
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
    private final @NotNull DataCombiningExtractor combiningExtractor;

    @Inject
    public DeviceTagImporter(
            final @NotNull DeviceTagValidator validator,
            final @NotNull ProtocolAdapterExtractor adapterExtractor,
            final @NotNull DataCombiningExtractor combiningExtractor) {
        this.validator = validator;
        this.adapterExtractor = adapterExtractor;
        this.combiningExtractor = combiningExtractor;
    }

    // ------------------------------------------------------------------------------------------------------------------

    private static final int MAX_DESCRIPTION_LENGTH = 1024;
    private static final int MAX_TOPIC_LENGTH = 65535;
    private static final int MAX_USER_PROPERTY_LENGTH = 256;
    private static final long MAX_MESSAGE_EXPIRY_INTERVAL = 4_294_967_295L; // MQTT uint32 max

    private @Nullable String tagNameValidated(
            final @NotNull DeviceTagRow row, final int rowNum, final @NotNull List<ValidationError> errors) {
        String tagName = row.getTagName();
        if (tagName == null || tagName.isEmpty()) {
            // rows without a tag_name are silently skipped, this is NOT an error, it is like they don't exist at all
            // the only check is whether the row has north- or southbound mapping defined, that is an error
            if (row.getNorthboundTopic() != null && !row.getNorthboundTopic().isEmpty()) {
                errors.add(new ValidationError(
                        rowNum,
                        "northbound_topic",
                        row.getNorthboundTopic(),
                        MAPPING_WITHOUT_TAG,
                        "Northbound mapping topic is defined, but row has empty tag_name."
                                + "Delete the mapping topic or define a tag_name to create a tag."));
            }
            if (row.getSouthboundTopic() != null && !row.getSouthboundTopic().isEmpty()) {
                errors.add(new ValidationError(
                        rowNum,
                        "southbound_topic",
                        row.getSouthboundTopic(),
                        MAPPING_WITHOUT_TAG,
                        "Southbound mapping topic is defined, but row has empty tag_name."
                                + "Delete the mapping topic or define a tag_name to create a tag."));
            }
            return null;
        }
        if (tagName.equals("*")) {
            tagName = row.getTagNameDefault();
            // Rules for tagNames are deliberately the same as the rules for topics, see Edge-Lore/Product/TagNames
            if (tagName == null || tagName.isEmpty()) {
                errors.add(new ValidationError(
                        rowNum,
                        "tag_name",
                        "*",
                        WILDCARD_NO_DEFAULT,
                        "Tag name uses the '*' default character but tag_name_default is empty."
                                + "Define either tag_name_default or use a different tag_name."));
                return null;
            }
            if (tagName.length() > MAX_TOPIC_LENGTH) {
                errors.add(new ValidationError(
                        rowNum,
                        "tag_name",
                        tagName.substring(50) + "...",
                        INVALID_TAG_NAME,
                        "Tag name is longer than 65535 character." + "Shorten the tag_name."));
                return null;
            }
            if (tagName.contains("#") || tagName.contains("+") || tagName.contains("\0")) {
                errors.add(new ValidationError(
                        rowNum,
                        "tag_name",
                        tagName,
                        INVALID_TAG_NAME,
                        "Tag name contains invalid characters '#', '+', or '\0'."
                                + "Remove invalid characters from tag_name."));
                return null;
            }
        }
        return tagName;
    }

    private @Nullable String nodeIdValidated(
            final @NotNull DeviceTagRow row,
            final int rowNum,
            final @NotNull List<ValidationError> errors,
            final @Nullable BulkTagBrowser browser) {
        String nodeId = row.getNodeId();
        if (nodeId == null || nodeId.isEmpty()) {
            errors.add(new ValidationError(
                    rowNum,
                    "node_id",
                    "",
                    INVALID_NODE_ID,
                    "NodeId is empty. " + "Define NodeId to define the node for the tag."));
            return null;
        }
        if (browser != null
                && row.getNamespaceUri() != null
                && !row.getNamespaceUri().isEmpty()) {
            try {
                nodeId = browser.resolveNodeId(nodeId, row.getNamespaceUri());
            } catch (final BrowseException e) {
                log.warn(
                        "Failed to resolve namespace URI '{}' for nodeId '{}': {}",
                        row.getNamespaceUri(),
                        nodeId,
                        e.getMessage());
            }
        }
        return nodeId;
    }

    private @Nullable TagEntity tagValidated(
            final @NotNull DeviceTagRow row,
            final int rowNum,
            final @NotNull List<ValidationError> errors,
            final @NotNull String tagName,
            final @NotNull String nodeId) {
        final String description = Objects.requireNonNullElse(row.getTagDescription(), "");
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            errors.add(new ValidationError(
                    rowNum,
                    "tag_description",
                    description.substring(0, 50) + "...",
                    INVALID_TAG_NAME,
                    "Tag description exceeds maximum length of " + MAX_DESCRIPTION_LENGTH
                            + ". "
                            + "Shorten the description."));
            return null;
        }
        return new TagEntity(tagName, description, Map.of("node", nodeId));
    }

    private @Nullable NorthboundMappingEntity northboundMappingValidated(
            final @NotNull DeviceTagRow row,
            final int rowNum,
            final @NotNull List<ValidationError> errors,
            final @NotNull String tagName) {

        String topic = row.getNorthboundTopic();
        if (topic == null || topic.trim().isEmpty()) {
            return null;
        }
        if (topic.equals("*")) {
            topic = row.getNorthboundTopicDefault();
            // Rules for topics, see Edge-Lore/Product/TagNames
            if (topic == null || topic.isEmpty()) {
                errors.add(new ValidationError(
                        rowNum,
                        "northbound_topic",
                        "*",
                        WILDCARD_NO_DEFAULT,
                        "Northbound topic uses the '*' default character but northbound_topic_default is empty."
                                + "Define either northbound_topic_default or use a different northbound_topic."));
                return null;
            }
            if (topic.length() > MAX_TOPIC_LENGTH) {
                errors.add(new ValidationError(
                        rowNum,
                        "northbound_topic",
                        topic.substring(50) + "...",
                        INVALID_TOPIC,
                        "Northbound topic is longer than 65535 character." + "Shorten the northbound_topic."));
                return null;
            }
            if (topic.contains("#") || topic.contains("+") || topic.contains("\0")) {
                errors.add(new ValidationError(
                        rowNum,
                        "northbound_topic",
                        topic,
                        INVALID_TOPIC,
                        "Northbound topic contains invalid characters '#', '+', or '\0'."
                                + "Remove invalid characters from northbound_topic."));
                return null;
            }
        }

        Integer maxQos = row.getMaxQos();
        if (maxQos == null) {
            maxQos = QoS.AT_LEAST_ONCE.getQosNumber();
        }
        if (maxQos < 0 || maxQos > 2) {
            errors.add(new ValidationError(
                    rowNum,
                    "max_qos",
                    String.valueOf(maxQos),
                    INVALID_QOS,
                    "Max QoS is not a valid MQTT Quality of Service number. " + "Set QoS to 0, 1, or 2"));
        }

        final boolean includeTagNames = Objects.requireNonNullElse(row.getIncludeTagNames(), false);
        final boolean includeTimestamp = Objects.requireNonNullElse(row.getIncludeTimestamp(), true);
        final boolean includeMetadata = Objects.requireNonNullElse(row.getIncludeMetadata(), false);

        final List<MqttUserPropertyEntity> userProperties = new ArrayList<>();
        if (row.getMqttUserProperties() != null) {
            for (final var prop : row.getMqttUserProperties().entrySet()) {
                if (prop.getKey() == null || prop.getKey().isEmpty()) {
                    errors.add(new ValidationError(
                            rowNum,
                            "mqtt_user_properties",
                            null,
                            INVALID_USER_PROPERTIES,
                            "User property key must not be empty"));
                    continue;
                } else if (prop.getKey().length() > MAX_USER_PROPERTY_LENGTH) {
                    errors.add(new ValidationError(
                            rowNum,
                            "mqtt_user_properties",
                            prop.getKey().substring(0, 50) + "...",
                            INVALID_USER_PROPERTIES,
                            "User property key exceeds maximum length of " + MAX_USER_PROPERTY_LENGTH));
                    continue;
                }
                if (prop.getValue() != null && prop.getValue().length() > MAX_USER_PROPERTY_LENGTH) {
                    errors.add(new ValidationError(
                            rowNum,
                            "mqtt_user_properties",
                            prop.getKey(),
                            INVALID_USER_PROPERTIES,
                            "User property value exceeds maximum length of " + MAX_USER_PROPERTY_LENGTH));
                    continue;
                }
                userProperties.add(new MqttUserPropertyEntity(prop.getKey(), prop.getValue()));
            }
        }

        final Long expiry = row.getMessageExpiryInterval(); // null is allowed in constructor below
        if (expiry != null && (expiry <= 0 || expiry > MAX_MESSAGE_EXPIRY_INTERVAL)) {
            errors.add(new ValidationError(
                    rowNum,
                    "message_expiry_interval",
                    String.valueOf(expiry),
                    INVALID_EXPIRY,
                    "Message expiry interval must be between 1 and " + MAX_MESSAGE_EXPIRY_INTERVAL + " seconds"));
            return null;
        }

        return new NorthboundMappingEntity(
                tagName,
                topic,
                maxQos,
                null, // messageHandlingOptions — always MQTTMessagePerTag
                includeTagNames,
                includeTimestamp,
                includeMetadata,
                userProperties,
                expiry);
    }

    private @Nullable SouthboundMappingEntity southboundMappingValidated(
            final @NotNull DeviceTagRow row,
            final int rowNum,
            final @NotNull List<ValidationError> errors,
            final @NotNull String tagName) {

        String topic = row.getSouthboundTopic();
        if (topic == null || topic.trim().isEmpty()) {
            return null;
        }
        if (topic.equals("*")) {
            topic = row.getSouthboundTopicDefault();
            // Rules for topics, see Edge-Lore/Product/TagNames
            if (topic == null || topic.isEmpty()) {
                errors.add(new ValidationError(
                        rowNum,
                        "southbound_topic",
                        "*",
                        WILDCARD_NO_DEFAULT,
                        "Southbound topic uses the '*' default character but southbound_topic_default is empty."
                                + "Define either southbound_topic_default or use a different southbound_topic."));
                return null;
            }
            if (topic.length() > MAX_TOPIC_LENGTH) {
                errors.add(new ValidationError(
                        rowNum,
                        "southbound_topic",
                        topic.substring(50) + "...",
                        INVALID_TOPIC,
                        "Southbound topic is longer than 65535 character." + "Shorten the southbound_topic."));
                return null;
            }
            if (topic.contains("#") || topic.contains("+") || topic.contains("\0")) {
                errors.add(new ValidationError(
                        rowNum,
                        "southbound_topic",
                        topic,
                        INVALID_TOPIC,
                        "Southbound topic contains invalid characters '#', '+', or '\0'."
                                + "Remove invalid characters from southbound_topic."));
                return null;
            }
        }

        final List<InstructionEntity> instructions = new ArrayList<>();
        if (row.getSouthboundFieldMapping() != null) {
            for (final FieldMappingInstruction fm : row.getSouthboundFieldMapping()) {
                if (fm.source() == null || fm.source().isEmpty()) {
                    errors.add(new ValidationError(
                            rowNum,
                            "southbound_field_mapping",
                            null,
                            INVALID_FIELD_MAPPING,
                            "Field mapping source is empty. Define a field mapping source."));
                    return null;
                }
                if (fm.destination() == null || fm.destination().isEmpty()) {
                    errors.add(new ValidationError(
                            rowNum,
                            "southbound_field_mapping",
                            null,
                            INVALID_FIELD_MAPPING,
                            "Field mapping destination is empty. Define a field mapping destination."));
                    return null;
                }
                instructions.add(new InstructionEntity(fm.source(), fm.destination(), null));
            }
        } else {
            // is there really a default for the southbound field mapping?
            instructions.add(new InstructionEntity("value", "value", null));
        }

        return new SouthboundMappingEntity(
                tagName,
                topic,
                new FieldMappingEntity(instructions),
                ""); // fromNorthSchema — empty; populated after tag creation by the adapter
    }

    // ------------------------------------------------------------------------------------------------------------------

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

        public boolean matchTag(@NonNull T that) {
            return Objects.equals(this.tag.getName(), that.getName())
                    && Objects.equals(this.tag.getDescription(), that.getDescription());
        }

        public boolean match(@NonNull TagWithMappings<T> that) {
            return matchTag(that.tag)
                    && Objects.equals(this.northboundMappings, that.northboundMappings)
                    && Objects.equals(this.southboundMappings, that.southboundMappings);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------

    private @NotNull Set<String> collectCombinerTags(final @NotNull String adapterId) {
        final Set<String> tagNames = new HashSet<>();
        for (final DataCombiner combiner : combiningExtractor.getAllCombiners()) {
            for (final DataCombining mapping : combiner.dataCombinings()) {
                final DataIdentifierReference p = mapping.sources().primaryReference();
                if (p != null
                        && p.type() == DataIdentifierReference.Type.TAG
                        && p.scope() != null
                        && p.scope().equals(adapterId)
                        && p.id() != null) {
                    tagNames.add(p.id());
                }
                for (final var inst : mapping.instructions()) {
                    final DataIdentifierReference s = inst.dataIdentifierReference();
                    if (s != null
                            && s.type() == DataIdentifierReference.Type.TAG
                            && s.scope() != null
                            && s.scope().equals(adapterId)
                            && s.id() != null) {
                        tagNames.add(s.id());
                    }
                }
            }
        }
        return tagNames;
    }

    // ------------------------------------------------------------------------------------------------------------------

    private static class ImportStat {
        List<TagAction> tagActions;
        int tagsCreated;
        int tagsUpdated;
        int tagsDeleted;
        int nbCreated;
        int nbDeleted;
        int sbCreated;
        int sbDeleted;

        ImportStat() {
            tagActions = new ArrayList<>();
            tagsCreated = tagsUpdated = tagsDeleted = 0;
            nbCreated = nbDeleted = 0;
            sbCreated = sbDeleted = 0;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------

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

    private @NotNull ImportResult doImportLocked(
            final @NotNull List<DeviceTagRow> rows,
            final @NotNull ImportMode mode,
            final @NotNull String adapterId,
            final @Nullable BulkTagBrowser browser)
            throws DeviceTagImporterException {

        // connect to the adapter
        final ProtocolAdapterEntity adapter = adapterExtractor
                .getAdapterByAdapterId(adapterId)
                .orElseThrow(() -> new DeviceTagImporterException(List.of(new ValidationError(
                        null, null, adapterId, ADAPTER_NOT_FOUND, "Adapter '" + adapterId + "' not found"))));

        // One List to rule them all, One List to find them, One List to bring them all and in the darkness bind them.
        List<ValidationError> errors = new ArrayList<>();

        // collect all the tags with their mappings from the File
        Map<String, TagWithMappings<TagEntity>> tagWithMappingsByNodeIdFromFile = new HashMap<>();
        for (int rowNum = 1; rowNum <= rows.size(); rowNum++) {
            final DeviceTagRow row = rows.get(rowNum - 1);

            final String tagName = tagNameValidated(row, rowNum, errors);
            if (tagName == null) {
                continue;
            }

            final String nodeId = nodeIdValidated(row, rowNum, errors, browser);
            if (nodeId == null) {
                continue;
            }

            final TagEntity tag = tagValidated(row, rowNum, errors, tagName, nodeId);
            if (tag == null) {
                continue;
            }
            final var twm = tagWithMappingsByNodeIdFromFile.computeIfAbsent(nodeId, z -> new TagWithMappings<>(tag));
            if (!twm.matchTag(tag)) {
                errors.add(new ValidationError(
                        rowNum,
                        "node_id",
                        nodeId,
                        DUPLICATE_TAG_NAME,
                        "Duplicate tag names for node '" + nodeId
                                + "'. "
                                + "Ensure that every node has a unique tag_name."));
            }

            final NorthboundMappingEntity nbm = northboundMappingValidated(row, rowNum, errors, tagName);
            if (nbm != null) {
                twm.northboundMappings.add(nbm);
            }

            final SouthboundMappingEntity sbm = southboundMappingValidated(row, rowNum, errors, tagName);
            if (sbm != null) {
                twm.southboundMappings.add(sbm);
            }
        }

        // collect all tags with their mappings from the Edge
        final Map<String, TagWithMappings<TagEntity>> tagWithMappingsByNodeIdFromEdge = new HashMap<>();
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
            twm.northboundMappings.add(northboundMappingEntity);
        }
        for (final SouthboundMappingEntity southboundMappingEntity : adapter.getSouthboundMappings()) {
            String nodeId = nodeIdByTagNameFromEdge.get(southboundMappingEntity.getTagName());
            final var twm = tagWithMappingsByNodeIdFromEdge.get(nodeId);
            if (twm == null) {
                // That is actually bad: the existing configuration had dangling tag reference
                continue;
            }
            twm.southboundMappings.add(southboundMappingEntity);
        }

        // figure out which tags and mappings will exist after the import
        final List<TagEntity> finalTags = new ArrayList<>();
        final List<NorthboundMappingEntity> finalNorthbound = new ArrayList<>();
        final List<SouthboundMappingEntity> finalSouthbound = new ArrayList<>();
        final ImportStat stats = new ImportStat();

        // collect the (keyset) of all the tags that will be created, updated, or kept
        // represented as map of their nodeIds, to detect tags that will be duplicated
        final Map<String, Set<String>> nodeIdsByTagName = new HashMap<>();

        // loop over all the nodeIds, both from the file and the adapter
        final Set<String> allNodeIds = new HashSet<>(tagWithMappingsByNodeIdFromFile.keySet());
        allNodeIds.addAll(tagWithMappingsByNodeIdFromEdge.keySet());
        for (String nodeId : allNodeIds) {

            // get the file and edge tag with their mappings, and the information where they exist
            final var twmFile = tagWithMappingsByNodeIdFromFile.get(nodeId);
            final String tagNameFile = twmFile != null ? twmFile.tag.getName() : null;
            final var twmEdge = tagWithMappingsByNodeIdFromEdge.get(nodeId);
            final String tagNameEdge = twmEdge != null ? twmEdge.tag.getName() : null;
            final boolean onlyFile = twmFile != null && twmEdge == null;
            final boolean onlyEdge = twmFile == null && twmEdge != null;
            final boolean bothSame = twmFile != null && twmEdge != null && twmFile.match(twmEdge);
            final boolean bothDiff = twmFile != null && twmEdge != null && !twmFile.match(twmEdge);

            // collect the tags that will exist after the import, with their nodeIds
            if (bothSame || (onlyFile && mode != ImportMode.DELETE)) {
                nodeIdsByTagName
                        .computeIfAbsent(tagNameFile, k -> new HashSet<>())
                        .add(nodeId);
            } else if (onlyEdge && (mode == ImportMode.MERGE_SAFE || mode == ImportMode.MERGE_OVERWRITE)) {
                nodeIdsByTagName
                        .computeIfAbsent(tagNameEdge, k -> new HashSet<>())
                        .add(nodeId);
            }

            // KEEP, 5+2 cases, from Edge
            if (bothSame || (onlyEdge && (mode == ImportMode.MERGE_SAFE || mode == ImportMode.MERGE_OVERWRITE))) {
                requireNonNull(twmEdge);
                finalTags.add(twmEdge.tag);
                finalNorthbound.addAll(twmEdge.northboundMappings);
                finalSouthbound.addAll(twmEdge.southboundMappings);
                // no statistics for tags that we keep
            }

            // CREATE, 4 cases, from File
            else if (onlyFile && mode != ImportMode.DELETE) {
                requireNonNull(twmFile);
                requireNonNull(tagNameFile);
                finalTags.add(twmFile.tag);
                finalNorthbound.addAll(twmFile.northboundMappings);
                finalSouthbound.addAll(twmFile.southboundMappings);
                stats.tagActions.add(new TagAction(tagNameFile, TagAction.Action.CREATED));
                stats.tagsCreated++;
                stats.nbCreated += twmFile.northboundMappings.size();
                stats.sbCreated += twmFile.southboundMappings.size();
            }

            // UPDATE, 2 cases, from File
            else if (bothDiff && (mode == ImportMode.OVERWRITE || mode == ImportMode.MERGE_OVERWRITE)) {
                requireNonNull(twmFile);
                requireNonNull(tagNameFile);
                requireNonNull(twmEdge);
                finalTags.add(twmFile.tag);
                finalNorthbound.addAll(twmFile.northboundMappings);
                finalSouthbound.addAll(twmFile.southboundMappings);
                stats.tagActions.add(new TagAction(tagNameFile, TagAction.Action.UPDATED));
                stats.tagsUpdated++;
                stats.nbDeleted += twmEdge.northboundMappings.size();
                stats.sbDeleted += twmEdge.southboundMappings.size();
                stats.nbCreated += twmFile.northboundMappings.size();
                stats.sbCreated += twmFile.southboundMappings.size();
            }

            // DELETE, 2 cases, from Edge
            else if (onlyEdge && (mode == ImportMode.DELETE || mode == ImportMode.OVERWRITE)) {
                requireNonNull(twmEdge);
                requireNonNull(tagNameEdge);
                stats.tagActions.add(new TagAction(tagNameEdge, TagAction.Action.DELETED));
                stats.tagsDeleted++;
                stats.nbDeleted += twmEdge.northboundMappings.size();
                stats.sbDeleted += twmEdge.southboundMappings.size();
            }

            // ERROR, 1 + 3 + 1 cases
            else if (onlyFile && mode == ImportMode.DELETE) {
                errors.add(new ValidationError(
                        null,
                        "tag_name",
                        tagNameFile,
                        TAG_CONFLICT,
                        "Tag '" + tagNameFile
                                + "' (node: "
                                + nodeId
                                + ") exists in the file but not on the adapter and would be created. "
                                + "Use CREATE, OVERWRITE, or MERGE mode to create."));
            } else if (bothDiff && mode != ImportMode.OVERWRITE && mode != ImportMode.MERGE_OVERWRITE) {
                errors.add(new ValidationError(
                        null,
                        "tag_name",
                        tagNameFile,
                        TAG_CONFLICT,
                        "Tag '" + tagNameFile
                                + "' (node: "
                                + nodeId
                                + ") exists on the adapter and the file with different definitions and would be updated. "
                                + "Use OVERWRITE or MERGE_OVERWRITE mode to update."));
            } else if (onlyEdge && mode == ImportMode.CREATE) {
                errors.add(new ValidationError(
                        null,
                        "tag_name",
                        tagNameEdge,
                        TAG_CONFLICT,
                        "Tag '" + tagNameEdge
                                + "' (node "
                                + nodeId
                                + ") exists on adapter but not in file and would be deleted. "
                                + "Use DELETE or OVERWRITE to delete - or MERGE to keep."));
            } else {
                errors.add(new ValidationError(
                        null, "", "", UPDATE_FAILED, "Failed to update adapter configuration. Unknown Error 1."));
            }
        }

        // raise an error if we are creating, updating, or keeping (duplicate) tags for multiple nodeIds
        for (String tagName : nodeIdsByTagName.keySet()) {
            if (nodeIdsByTagName.get(tagName).size() > 1) {
                errors.add(new ValidationError(
                        null,
                        "tag_name",
                        tagName,
                        DUPLICATE_NODE,
                        "Tag '" + tagName
                                + "' (nodes: "
                                + nodeIdsByTagName.get(tagName)
                                + ") will be assigned to multiple nodes. "
                                + "Ensure uniqueness of the tag names."));
            }
        }

        // raise an error if we are deleting a tag that is used in a combiner
        final Set<String> duplicateTagNames = new HashSet<>(collectCombinerTags(adapterId));
        duplicateTagNames.removeAll(nodeIdsByTagName.keySet());
        for (String tagName : duplicateTagNames) {
            errors.add(new ValidationError(
                    null,
                    "tag_name",
                    tagName,
                    TAG_IN_USE_BY_COMBINER,
                    "Tag '" + tagName
                            + "' is used in a combiner, but not created, updated, or kept. "
                            + "Ensure that there are no dangling references."));
        }

        // only now do we know whether the import is valid
        if (!errors.isEmpty()) {
            throw new DeviceTagImporterException(errors);
        }

        // create the adapter with its new configuration
        final ProtocolAdapterEntity updatedAdapter = new ProtocolAdapterEntity(
                adapter.getAdapterId(),
                adapter.getProtocolId(),
                adapter.getConfigVersion(),
                adapter.getConfig(),
                finalNorthbound,
                finalSouthbound,
                finalTags);

        // and now is the time to actually DO something at all (up to this point we have only been planning)
        final boolean success = adapterExtractor.updateAdapter(updatedAdapter);
        if (!success) {
            throw new DeviceTagImporterException(List.of(new ValidationError(
                    null, null, null, UPDATE_FAILED, "Failed to update adapter configuration. Unknown Error.")));
        }

        // log statistics
        log.info(
                "Import completed for adapter '{}': {} tags created, {} updated, {} deleted",
                adapterId,
                stats.tagsCreated,
                stats.tagsUpdated,
                stats.tagsDeleted);

        // and return the statistics
        return new ImportResult(
                stats.tagsCreated,
                stats.tagsUpdated,
                stats.tagsDeleted,
                stats.nbCreated,
                stats.nbDeleted,
                stats.sbCreated,
                stats.sbDeleted,
                stats.tagActions);
    }
}
