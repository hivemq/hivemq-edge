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
package com.hivemq.edge.adapters.browse.model;

import com.hivemq.edge.adapters.browse.BrowsedNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Full row model for a device tag file. Contains all 21 fields from the Houston design:
 * 7 informational fields (from browse) and 14 editable fields (user fills in or defaults).
 * Immutable once built via {@link Builder}.
 */
public final class DeviceTagRow {

    // --- Informational fields (populated by browse) ---
    private final @Nullable String nodePath;
    private final @Nullable String namespaceUri;
    private final int namespaceIndex;
    private final @Nullable String nodeId;
    private final @Nullable String dataType;
    private final @Nullable String accessLevel;
    private final @Nullable String nodeDescription;

    // --- Editable fields (user fills in) ---
    private final @Nullable String tagName;
    private final @Nullable String tagNameDefault;
    private final @Nullable String tagDescription;
    private final @Nullable String northboundTopic;
    private final @Nullable String northboundTopicDefault;
    private final @Nullable String southboundTopic;
    private final @Nullable String southboundTopicDefault;
    private final @Nullable List<FieldMappingInstruction> southboundFieldMapping;
    private final @Nullable Integer maxQos;
    private final @Nullable Long messageExpiryInterval;
    private final @Nullable Boolean includeTimestamp;
    private final @Nullable Boolean includeTagNames;
    private final @Nullable Boolean includeMetadata;
    private final @Nullable Map<String, String> mqttUserProperties;

    private DeviceTagRow(final @NotNull Builder builder) {
        this.nodePath = builder.nodePath;
        this.namespaceUri = builder.namespaceUri;
        this.namespaceIndex = builder.namespaceIndex;
        this.nodeId = builder.nodeId;
        this.dataType = builder.dataType;
        this.accessLevel = builder.accessLevel;
        this.nodeDescription = builder.nodeDescription;
        this.tagName = builder.tagName;
        this.tagNameDefault = builder.tagNameDefault;
        this.tagDescription = builder.tagDescription;
        this.northboundTopic = builder.northboundTopic;
        this.northboundTopicDefault = builder.northboundTopicDefault;
        this.southboundTopic = builder.southboundTopic;
        this.southboundTopicDefault = builder.southboundTopicDefault;
        this.southboundFieldMapping = builder.southboundFieldMapping;
        this.maxQos = builder.maxQos;
        this.messageExpiryInterval = builder.messageExpiryInterval;
        this.includeTimestamp = builder.includeTimestamp;
        this.includeTagNames = builder.includeTagNames;
        this.includeMetadata = builder.includeMetadata;
        this.mqttUserProperties = builder.mqttUserProperties;
    }

    /**
     * Creates a DeviceTagRow from a BrowsedNode, copying informational fields and setting
     * editable fields to their defaults (null or generated values).
     */
    public static @NotNull DeviceTagRow fromBrowsedNode(final @NotNull BrowsedNode node) {
        return new Builder()
                .nodePath(node.nodePath())
                .namespaceUri(node.namespaceUri())
                .namespaceIndex(node.namespaceIndex())
                .nodeId(node.nodeId())
                .dataType(node.dataType())
                .accessLevel(node.accessLevel())
                .nodeDescription(node.nodeDescription())
                .tagNameDefault(node.tagNameDefault())
                .tagDescription(node.tagDescription())
                .northboundTopicDefault(node.northboundTopicDefault())
                .southboundTopicDefault(node.southboundTopicDefault())
                .build();
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    // --- Getters ---

    public @Nullable String getNodePath() {
        return nodePath;
    }

    public @Nullable String getNamespaceUri() {
        return namespaceUri;
    }

    public int getNamespaceIndex() {
        return namespaceIndex;
    }

    public @Nullable String getNodeId() {
        return nodeId;
    }

    public @Nullable String getDataType() {
        return dataType;
    }

    public @Nullable String getAccessLevel() {
        return accessLevel;
    }

    public @Nullable String getNodeDescription() {
        return nodeDescription;
    }

    public @Nullable String getTagName() {
        return tagName;
    }

    public @Nullable String getTagNameDefault() {
        return tagNameDefault;
    }

    public @Nullable String getTagDescription() {
        return tagDescription;
    }

    public @Nullable String getNorthboundTopic() {
        return northboundTopic;
    }

    public @Nullable String getNorthboundTopicDefault() {
        return northboundTopicDefault;
    }

    public @Nullable String getSouthboundTopic() {
        return southboundTopic;
    }

    public @Nullable String getSouthboundTopicDefault() {
        return southboundTopicDefault;
    }

    public @Nullable List<FieldMappingInstruction> getSouthboundFieldMapping() {
        return southboundFieldMapping;
    }

    public @Nullable Integer getMaxQos() {
        return maxQos;
    }

    public @Nullable Long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }

    public @Nullable Boolean getIncludeTimestamp() {
        return includeTimestamp;
    }

    public @Nullable Boolean getIncludeTagNames() {
        return includeTagNames;
    }

    public @Nullable Boolean getIncludeMetadata() {
        return includeMetadata;
    }

    public @Nullable Map<String, String> getMqttUserProperties() {
        return mqttUserProperties;
    }

    /**
     * Returns true if this row should result in tag creation (tagName is non-null and non-empty).
     */
    public boolean hasTag() {
        return tagName != null && !tagName.isEmpty();
    }

    /**
     * Returns true if this row should create a northbound mapping.
     */
    public boolean hasNorthboundMapping() {
        return hasTag() && northboundTopic != null && !northboundTopic.isEmpty();
    }

    /**
     * Returns true if this row should create a southbound mapping.
     */
    public boolean hasSouthboundMapping() {
        return hasTag() && southboundTopic != null && !southboundTopic.isEmpty();
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof final DeviceTagRow that)) return false;
        return namespaceIndex == that.namespaceIndex
                && Objects.equals(nodePath, that.nodePath)
                && Objects.equals(namespaceUri, that.namespaceUri)
                && Objects.equals(nodeId, that.nodeId)
                && Objects.equals(dataType, that.dataType)
                && Objects.equals(accessLevel, that.accessLevel)
                && Objects.equals(nodeDescription, that.nodeDescription)
                && Objects.equals(tagName, that.tagName)
                && Objects.equals(tagNameDefault, that.tagNameDefault)
                && Objects.equals(tagDescription, that.tagDescription)
                && Objects.equals(northboundTopic, that.northboundTopic)
                && Objects.equals(northboundTopicDefault, that.northboundTopicDefault)
                && Objects.equals(southboundTopic, that.southboundTopic)
                && Objects.equals(southboundTopicDefault, that.southboundTopicDefault)
                && Objects.equals(southboundFieldMapping, that.southboundFieldMapping)
                && Objects.equals(maxQos, that.maxQos)
                && Objects.equals(messageExpiryInterval, that.messageExpiryInterval)
                && Objects.equals(includeTimestamp, that.includeTimestamp)
                && Objects.equals(includeTagNames, that.includeTagNames)
                && Objects.equals(includeMetadata, that.includeMetadata)
                && Objects.equals(mqttUserProperties, that.mqttUserProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                nodePath,
                namespaceUri,
                namespaceIndex,
                nodeId,
                dataType,
                accessLevel,
                nodeDescription,
                tagName,
                tagNameDefault,
                tagDescription,
                northboundTopic,
                northboundTopicDefault,
                southboundTopic,
                southboundTopicDefault,
                southboundFieldMapping,
                maxQos,
                messageExpiryInterval,
                includeTimestamp,
                includeTagNames,
                includeMetadata,
                mqttUserProperties);
    }

    @Override
    public @NotNull String toString() {
        return "DeviceTagRow{" + "nodePath='"
                + nodePath + '\'' + ", nodeId='"
                + nodeId + '\'' + ", tagName='"
                + tagName + '\'' + ", tagNameDefault='"
                + tagNameDefault + '\'' + '}';
    }

    public static final class Builder {
        private @Nullable String nodePath;
        private @Nullable String namespaceUri;
        private int namespaceIndex;
        private @Nullable String nodeId;
        private @Nullable String dataType;
        private @Nullable String accessLevel;
        private @Nullable String nodeDescription;
        private @Nullable String tagName;
        private @Nullable String tagNameDefault;
        private @Nullable String tagDescription;
        private @Nullable String northboundTopic;
        private @Nullable String northboundTopicDefault;
        private @Nullable String southboundTopic;
        private @Nullable String southboundTopicDefault;
        private @Nullable List<FieldMappingInstruction> southboundFieldMapping;
        private @Nullable Integer maxQos;
        private @Nullable Long messageExpiryInterval;
        private @Nullable Boolean includeTimestamp;
        private @Nullable Boolean includeTagNames;
        private @Nullable Boolean includeMetadata;
        private @Nullable Map<String, String> mqttUserProperties;

        private Builder() {}

        public @NotNull Builder nodePath(final @Nullable String nodePath) {
            this.nodePath = nodePath;
            return this;
        }

        public @NotNull Builder namespaceUri(final @Nullable String namespaceUri) {
            this.namespaceUri = namespaceUri;
            return this;
        }

        public @NotNull Builder namespaceIndex(final int namespaceIndex) {
            this.namespaceIndex = namespaceIndex;
            return this;
        }

        public @NotNull Builder nodeId(final @Nullable String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public @NotNull Builder dataType(final @Nullable String dataType) {
            this.dataType = dataType;
            return this;
        }

        public @NotNull Builder accessLevel(final @Nullable String accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        public @NotNull Builder nodeDescription(final @Nullable String nodeDescription) {
            this.nodeDescription = nodeDescription;
            return this;
        }

        public @NotNull Builder tagName(final @Nullable String tagName) {
            this.tagName = tagName;
            return this;
        }

        public @NotNull Builder tagNameDefault(final @Nullable String tagNameDefault) {
            this.tagNameDefault = tagNameDefault;
            return this;
        }

        public @NotNull Builder tagDescription(final @Nullable String tagDescription) {
            this.tagDescription = tagDescription;
            return this;
        }

        public @NotNull Builder northboundTopic(final @Nullable String northboundTopic) {
            this.northboundTopic = northboundTopic;
            return this;
        }

        public @NotNull Builder northboundTopicDefault(final @Nullable String northboundTopicDefault) {
            this.northboundTopicDefault = northboundTopicDefault;
            return this;
        }

        public @NotNull Builder southboundTopic(final @Nullable String southboundTopic) {
            this.southboundTopic = southboundTopic;
            return this;
        }

        public @NotNull Builder southboundTopicDefault(final @Nullable String southboundTopicDefault) {
            this.southboundTopicDefault = southboundTopicDefault;
            return this;
        }

        public @NotNull Builder southboundFieldMapping(
                final @Nullable List<FieldMappingInstruction> southboundFieldMapping) {
            this.southboundFieldMapping = southboundFieldMapping;
            return this;
        }

        public @NotNull Builder maxQos(final @Nullable Integer maxQos) {
            this.maxQos = maxQos;
            return this;
        }

        public @NotNull Builder messageExpiryInterval(final @Nullable Long messageExpiryInterval) {
            this.messageExpiryInterval = messageExpiryInterval;
            return this;
        }

        public @NotNull Builder includeTimestamp(final @Nullable Boolean includeTimestamp) {
            this.includeTimestamp = includeTimestamp;
            return this;
        }

        public @NotNull Builder includeTagNames(final @Nullable Boolean includeTagNames) {
            this.includeTagNames = includeTagNames;
            return this;
        }

        public @NotNull Builder includeMetadata(final @Nullable Boolean includeMetadata) {
            this.includeMetadata = includeMetadata;
            return this;
        }

        public @NotNull Builder mqttUserProperties(final @Nullable Map<String, String> mqttUserProperties) {
            this.mqttUserProperties = mqttUserProperties;
            return this;
        }

        public @NotNull DeviceTagRow build() {
            return new DeviceTagRow(this);
        }
    }
}
