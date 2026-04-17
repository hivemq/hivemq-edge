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
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// region DeviceTagImporter is the class to import tags from a file to an adapter (instance variables and constructor)
// =====================================================================================================================
@Singleton
public class DeviceTagImporter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DeviceTagImporter.class);

    private final @NotNull ProtocolAdapterExtractor adapterExtractor;
    private final @NotNull DataCombiningExtractor combiningExtractor;

    @Inject
    public DeviceTagImporter(
            final @NotNull ProtocolAdapterExtractor adapterExtractor,
            final @NotNull DataCombiningExtractor combiningExtractor) {
        this.adapterExtractor = adapterExtractor;
        this.combiningExtractor = combiningExtractor;
    }

    // endregion

    // region doImport - executes the import of tags and their mappings from a file to an adapter
    // =================================================================================================================
    // It reads the information from the file and adapter, computes the goal state, and updates the configuration
    // Generally the file describes the desired goal state (mode = CREATE, DELETE, OVERWRITE),
    // but it is also possible to merge file and adapter state (mode = OVERWRITE).
    //
    // One very important aspect is that everything is keyed/indexed by the nodeId (and not by the tagName)
    // so a row in the file is interpreted as "for this nodeId, there should be a tag with the following tagName"
    // and the information from the adapter is interpreted as "for this nodeId, there exists a tag with that tagName"
    // and the computation merges those two lists based by their nodeIds "for this nodeId, this is the goal state"
    //
    // In addition Edge enforces that for every nodeId there can be at most one tag,
    // because some adapters have problems with multiple subscriptions,
    // and northbound mappings are the better way to create fan-out.

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

        final List<ValidationError> errors = new ArrayList<>();
        final ImportStat stats = new ImportStat();

        // fetch the information from the file and the edge adapter (keyed by their nodeIds)
        final Map<String, TagWithMappings<TagEntity>> twmsFile =
                getTagsWithMappingsByNodeIdFromFile(rows, browser, errors);

        final ProtocolAdapterEntity adapter = getProtocolAdapterEntity(adapterId);
        final Map<String, TagWithMappings<TagEntity>> twmsEdge = getTagsWithMappingsByNodeIdFromEdge(adapter, errors);

        // compute the final list of tags and mappings, depending on the mode
        final List<TagWithMappings<TagEntity>> twmsFinal =
                computeFinalTagsWithMappingsList(twmsFile, twmsEdge, mode, stats, errors);

        // most validations are done en-passant, these can only be done in the end based on the final list
        checkNoTagIsAssignedToTwoMultipleNodes(twmsFinal, errors);
        checkNoDanglingReferencesFromCombinerMappings(adapterId, twmsFinal, errors);

        // At this point it should be considered to browse the adapter again and verify that all nodes exist

        // we make NO change if there is ANY problem at all (so far we have only collected, planned, and validated)
        if (!errors.isEmpty()) {
            throw new DeviceTagImporterException(errors);
        }

        // if there was no validation error, now is the time to execute the plan
        final ProtocolAdapterEntity fac = finalAdapterConfiguration(adapter, twmsFinal);
        final boolean success = adapterExtractor.updateAdapter(fac);
        if (!success) {
            throw new DeviceTagImporterException(List.of(new ValidationError(
                    null, null, null, UPDATE_FAILED, "Failed to update adapter configuration. Unknown Error.")));
        }

        // log and return the statistics
        logStatistics(adapterId, stats);
        return ImportResultRec(stats);
    }

    // endregion

    // region Tag and Node abstractions - to make the import protocol independent
    // =================================================================================================================
    // These methods provide an abstraction for the import, so that the main code could work for all protocols.
    //
    // It uses the following concepts:
    // - Tag: the entity in Edge that allows to read from a device, consisting of the following fields:
    //     - tagName: the "identifier" of the tag
    //     - description
    //     - and the protocol specific part, which is an abstract concept with protocol specific implementations
    // - Node: the protocol specific part of the tag, consisting of the following logical aspects:
    //     - nodeId: the "identifier" for the node
    //     - type: the datatype of the node, is mapped to a schema for the tag, not yet implemented
    //     - information: additional information, which is not relevant for the function, but useful for the user
    // - NodeId: a concrete representation of the nodeId as an opaque string (which is compared and used as an index)
    //
    // The Tag and Protocol adapter call the protocol specific part of the tag the "tag definition",
    // which is unfortunate and incorrect, e.g. the tagName is clearly part of the definition of a tag,
    // so we instead use the term "node" and "nodeId", which helps to distinguish this entity from the tag entity.
    //
    // The code below implements these concepts for the OPC-UA adapter.
    // There is minor problem in the current implementation the nodeId should actually contain the namespaceUri
    // and not the namespaceIndex, since the former is stable while the latter might change
    //
    // Eventually these concepts and abstractions belong in the Protocol Adapter SDK,
    // and the concrete implementation in the adapter code.
    // But we will only do this when we have completely nailed the concepts and abstractions
    // (we don't have that many shots at making incompatible changes to the Protocol Adapter SDK).

    private static @NotNull Map<String, Object> node(final @NotNull String nodeId) {
        return Map.of("node", nodeId);
    }

    private static @NotNull TagEntity tag(
            final @NotNull String tagName, final @NotNull String description, final @NotNull Map<String, Object> node) {
        return new TagEntity(tagName, description, node);
    }

    private static @NotNull TagEntity tag(
            final @NotNull String tagName, final @NotNull String description, final @NotNull String nodeId) {
        return tag(tagName, description, node(nodeId));
    }

    private static @NotNull String id(final @NotNull Map<String, Object> node) {
        requireNonNull(node.get("node"), "node identifier must not be null");
        return (String) node.get("node");
    }

    private static @NotNull Map<String, Object> node(final @NotNull TagEntity tag) {
        requireNonNull(tag.getDefinition(), "tag entity must not be null");
        return tag.getDefinition();
    }

    private static @NotNull String nodeId(final @NotNull TagEntity tag) {
        return id(node(tag));
    }

    // endregion

    // region TagWithMappings - structure to keep info about a tag and its associated north- and southbound mappings
    // =================================================================================================================
    // The structure to keep information about a tag and its associated north- and southbound mappings.
    // A TagWithMappings from the adapter represents an existing tag and all associated mappings,
    // a TagWithMappings from the file represent an INTENT - an intended goal state for the tag and its mappings.

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

    // endregion

    // region Read tags and mappings from the file, returned as a map of nodeId -> TagWithMappings
    // =================================================================================================================
    // One step is to read the map of tags with their mappings from the file.
    // It uses the methods from the following section to extract and validate individual fields.
    // Most validations are field level (field must not be empty, not too long, etc.),
    // some are row level (if there is a northbound mapping, then the tagName must be defined),
    // and one is cross-row level (if two rows have the same node-Id, then they must also have the same tagName).
    // Note that lines without a tagName are silently skipped - this might be surprising for users.

    private @NonNull Map<String, TagWithMappings<TagEntity>> getTagsWithMappingsByNodeIdFromFile(
            final @NonNull List<DeviceTagRow> rows,
            final @Nullable BulkTagBrowser browser,
            final @NotNull List<ValidationError> errors) {
        final Map<String, TagWithMappings<TagEntity>> tagWithMappingsByNodeIdFromFile = new HashMap<>();
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
            // check that we don't create two tags for one node;
            // it is a deliberate decision to forbid multiple tags for one node,
            // i.e. require a 1:1 correspondence between nodes and tag;
            // for some protocols (devices) this would actually not be a problem to allow multiple tags for one node,
            // but some devices fail if the adapter tries to subscribe twice to the same node;
            // this could be circumvented by subscribing only once and then multiplexing to the multiple tags,
            // but this is pointless, the proper place for multiplexing are the multiple northbound mappings for one tag
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
        return tagWithMappingsByNodeIdFromFile;
    }

    // endregion

    // region Methods to extract and validate (field and row-level) tags, mappings and their fields from a file row
    // =================================================================================================================
    // Code to extract and validate tags, mappings, and their fields from a row from the file.

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
        return tag(tagName, description, nodeId);
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
                if (prop.getValue() == null) {
                    errors.add(new ValidationError(
                            rowNum,
                            "mqtt_user_properties",
                            null,
                            INVALID_USER_PROPERTIES,
                            "User property value must not be empty"));
                    continue;
                } else if (prop.getValue().length() > MAX_USER_PROPERTY_LENGTH) {
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

    // endregion

    // region Read tags and mappings from the Edge Adapter, returned as a map of nodeId -> TagWithMappings
    // =================================================================================================================
    // Extract the information about existing tags and their mappings from the adapter.
    // This is pretty straight forward.

    private ProtocolAdapterEntity getProtocolAdapterEntity(@NonNull String adapterId)
            throws DeviceTagImporterException {
        final ProtocolAdapterEntity adapter = adapterExtractor
                .getAdapterByAdapterId(adapterId)
                .orElseThrow(() -> new DeviceTagImporterException(List.of(new ValidationError(
                        null, null, adapterId, ADAPTER_NOT_FOUND, "Adapter '" + adapterId + "' not found"))));
        return adapter;
    }

    @SuppressWarnings("unused") // errors reserved for future use (dangling-reference detection)
    private static @NonNull Map<String, TagWithMappings<TagEntity>> getTagsWithMappingsByNodeIdFromEdge(
            final @NotNull ProtocolAdapterEntity adapter, final @NotNull List<ValidationError> errors) {
        final Map<String, TagWithMappings<TagEntity>> tagWithMappingsByNodeIdFromEdge = new HashMap<>();
        final Map<String, String> nodeIdByTagNameFromEdge = new HashMap<>();

        for (final TagEntity tag : adapter.getTags()) {
            tagWithMappingsByNodeIdFromEdge.put(nodeId(tag), new TagWithMappings<>(tag));
            nodeIdByTagNameFromEdge.put(tag.getName(), nodeId(tag));
        }

        for (final NorthboundMappingEntity northboundMappingEntity : adapter.getNorthboundMappings()) {
            final String nodeId = nodeIdByTagNameFromEdge.get(northboundMappingEntity.getTagName());
            final var twm = tagWithMappingsByNodeIdFromEdge.get(nodeId);
            if (twm == null) {
                // That is actually bad: the existing configuration had dangling tag reference
                // maybe we should catch this as an error too (and let the user delete mapping first in the frontend)
                continue;
            }
            twm.northboundMappings.add(northboundMappingEntity);
        }

        for (final SouthboundMappingEntity southboundMappingEntity : adapter.getSouthboundMappings()) {
            final String nodeId = nodeIdByTagNameFromEdge.get(southboundMappingEntity.getTagName());
            final var twm = tagWithMappingsByNodeIdFromEdge.get(nodeId);
            if (twm == null) {
                // That is actually bad: the existing configuration had dangling tag reference
                // maybe we should catch this as an error too (and let the user delete mapping first in the frontend)
                continue;
            }
            twm.southboundMappings.add(southboundMappingEntity);
        }

        return tagWithMappingsByNodeIdFromEdge;
    }

    // endregion

    // region Computing the final tag list, depending on the import mode, merging file and edge adapter information
    // =================================================================================================================
    // Compute which tags and mappings should exist after the import.
    // In three modes (CREATE, DELETE, OVERWRITE) the list of tags is exactly what is in the file,
    // and the mode only asserts how to get there (only with creations, deletions, or arbitrary CrUD operations).
    // In the other two modes (MERGE_SAFE, MERGE_OVERWRITE) the list of tags is basically the union of file and edge.
    // The code is rather simple, the method is only long because of the error messages and stats collection.

    private @NotNull List<TagWithMappings<TagEntity>> computeFinalTagsWithMappingsList(
            final @NonNull Map<String, TagWithMappings<TagEntity>> tagWithMappingsByNodeIdFromFile,
            final @NonNull Map<String, TagWithMappings<TagEntity>> tagWithMappingsByNodeIdFromEdge,
            final @NotNull ImportMode mode,
            final @NotNull ImportStat stats,
            final @NotNull List<ValidationError> errors) {
        final List<TagWithMappings<TagEntity>> tagWithMappingsFinal = new ArrayList<>();

        // loop over all the nodeIds, both from the file and the adapter
        final Set<String> allNodeIds = new HashSet<>(tagWithMappingsByNodeIdFromFile.keySet());
        allNodeIds.addAll(tagWithMappingsByNodeIdFromEdge.keySet());
        for (final String nodeId : allNodeIds) {

            // get the file and edge tag with their mappings, and the information where they exist
            final var twmFile = tagWithMappingsByNodeIdFromFile.get(nodeId);
            final String tagNameFile = twmFile != null ? twmFile.tag.getName() : null;
            final var twmEdge = tagWithMappingsByNodeIdFromEdge.get(nodeId);
            final String tagNameEdge = twmEdge != null ? twmEdge.tag.getName() : null;
            final boolean onlyFile = twmFile != null && twmEdge == null;
            final boolean onlyEdge = twmFile == null && twmEdge != null;
            final boolean bothSame = twmFile != null && twmEdge != null && twmFile.match(twmEdge);
            final boolean bothDiff = twmFile != null && twmEdge != null && !twmFile.match(twmEdge);

            // Keep the existing tag
            if (bothSame || (onlyEdge && (mode == ImportMode.MERGE_SAFE || mode == ImportMode.MERGE_OVERWRITE))) {
                requireNonNull(twmEdge);
                tagWithMappingsFinal.add(twmEdge);
                // no statistics for tags that we keep
            }

            // Create a new tag as specified in the file
            else if (onlyFile && mode != ImportMode.DELETE) {
                requireNonNull(twmFile);
                requireNonNull(tagNameFile);
                tagWithMappingsFinal.add(twmFile);
                stats.tagActions.add(new TagAction(tagNameFile, TagAction.Action.CREATED));
                stats.tagsCreated++;
                stats.nbCreated += twmFile.northboundMappings.size();
                stats.sbCreated += twmFile.southboundMappings.size();
            }

            // Update an existing tag with the one specified in the file
            else if (bothDiff && (mode == ImportMode.OVERWRITE || mode == ImportMode.MERGE_OVERWRITE)) {
                requireNonNull(twmFile);
                requireNonNull(tagNameFile);
                requireNonNull(twmEdge);
                tagWithMappingsFinal.add(twmFile);
                stats.tagActions.add(new TagAction(tagNameFile, TagAction.Action.UPDATED));
                stats.tagsUpdated++;
                stats.nbDeleted += twmEdge.northboundMappings.size();
                stats.sbDeleted += twmEdge.southboundMappings.size();
                stats.nbCreated += twmFile.northboundMappings.size();
                stats.sbCreated += twmFile.southboundMappings.size();
            }

            // Delete an existing tag, because it doesn't exist in the file
            else if (onlyEdge && (mode == ImportMode.DELETE || mode == ImportMode.OVERWRITE)) {
                requireNonNull(twmEdge);
                requireNonNull(tagNameEdge);
                stats.tagActions.add(new TagAction(tagNameEdge, TagAction.Action.DELETED));
                stats.tagsDeleted++;
                stats.nbDeleted += twmEdge.northboundMappings.size();
                stats.sbDeleted += twmEdge.southboundMappings.size();
            }

            // Everything else is an error
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

        return tagWithMappingsFinal;
    }

    // endregion

    // region Post-compute validations (no tag references multiple nodes, no combiner has a dangling reference)
    // =================================================================================================================
    // Two more validations that are only possible once the final list of tags and mappings has been computed:
    // The first checks that there are no two nodes, where the tags that will be created have the same tag name,
    // this is of course impossible (one tag cannot reference two different nodes).
    // The second checks that there are not combiner mappings that reference a tag that will not exist after the import
    // note that there are other code paths (REST API) that might lead to dangling references.

    private static void checkNoTagIsAssignedToTwoMultipleNodes(
            final @NotNull List<TagWithMappings<TagEntity>> twmsFinal, final @NotNull List<ValidationError> errors) {

        twmsFinal.stream()
                .collect(Collectors.groupingBy(
                        twm -> twm.tag.getName(), Collectors.mapping(twm -> nodeId(twm.tag), Collectors.toSet())))
                .entrySet()
                .stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(e -> errors.add(new ValidationError(
                        null,
                        "tag_name",
                        e.getKey(),
                        DUPLICATE_NODE,
                        "Tag '" + e.getKey()
                                + "' (nodes: "
                                + e.getValue()
                                + ") will be assigned to multiple nodes. "
                                + "Ensure uniqueness of the tag names.")));
    }

    private void checkNoDanglingReferencesFromCombinerMappings(
            final @NonNull String adapterId,
            final @NonNull List<TagWithMappings<TagEntity>> twmsFinal,
            final @NonNull List<ValidationError> errors) {

        final Set<String> deletedCombinerTags = new HashSet<>(collectCombinerTagNames(adapterId));
        for (final TagWithMappings<TagEntity> twm : twmsFinal) {
            deletedCombinerTags.remove(twm.tag.getName());
        }

        for (final String tagName : deletedCombinerTags) {
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

    private @NotNull Set<String> collectCombinerTagNames(final @NotNull String adapterId) {
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

    // endregion

    // region Build the final adapter configuration
    // =================================================================================================================
    // Transform the list of tags with mappings into the configuration for the adapter (in the format that it expects).

    private static @NonNull ProtocolAdapterEntity finalAdapterConfiguration(
            final @NotNull ProtocolAdapterEntity adapter, final @NotNull List<TagWithMappings<TagEntity>> twmsFinal) {
        return new ProtocolAdapterEntity(
                adapter.getAdapterId(),
                adapter.getProtocolId(),
                adapter.getConfigVersion(),
                adapter.getConfig(),
                twmsFinal.stream()
                        .flatMap(twm -> twm.northboundMappings.stream())
                        .collect(Collectors.toList()),
                twmsFinal.stream()
                        .flatMap(twm -> twm.southboundMappings.stream())
                        .collect(Collectors.toList()),
                twmsFinal.stream().map(twm -> twm.tag).collect(Collectors.toList()));
    }

    // endregion

    // region Statistics and logging
    // =================================================================================================================
    // Minor helper functions for logging and statistics.

    private static class ImportStat {
        final List<TagAction> tagActions;
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

    private static void logStatistics(final @NotNull String adapterId, final @NotNull ImportStat stats) {
        // log statistics
        log.info(
                "Import completed for adapter '{}': {} tags created, {} updated, {} deleted",
                adapterId,
                stats.tagsCreated,
                stats.tagsUpdated,
                stats.tagsDeleted);
    }

    private static @NonNull ImportResult ImportResultRec(final @NotNull ImportStat stats) {
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
    // endregion
}
