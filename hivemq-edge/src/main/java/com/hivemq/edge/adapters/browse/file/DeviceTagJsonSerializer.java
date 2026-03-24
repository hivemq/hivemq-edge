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
package com.hivemq.edge.adapters.browse.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import com.hivemq.edge.adapters.browse.model.FieldMappingInstruction;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/**
 * Serializes and deserializes {@link DeviceTagRow} lists to/from JSON format using Jackson.
 * Uses a nested structure: {@code {rows: [{node: {...}, tag: {...}, northbound: {...}, southbound: {...}}]}}.
 */
@Singleton
public class DeviceTagJsonSerializer {

    private final @NotNull ObjectMapper mapper;

    @Inject
    public DeviceTagJsonSerializer() {
        this(createDefaultMapper());
    }

    DeviceTagJsonSerializer(final @NotNull ObjectMapper mapper) {
        this.mapper = mapper;
    }

    static @NotNull ObjectMapper createDefaultMapper() {
        return new ObjectMapper()
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    static @NotNull FileDto toFileDto(final @NotNull List<DeviceTagRow> rows) {
        return new FileDto(rows.stream().map(DeviceTagJsonSerializer::toRowDto).toList());
    }

    static @NotNull List<DeviceTagRow> fromFileDto(final @NotNull FileDto dto) {
        if (dto.rows == null) {
            return List.of();
        }
        return dto.rows.stream().map(DeviceTagJsonSerializer::fromRowDto).toList();
    }

    static @NotNull RowDto toRowDto(final @NotNull DeviceTagRow row) {
        final NodeDto node = new NodeDto();
        node.nodePath = row.getNodePath();
        node.namespaceUri = row.getNamespaceUri();
        node.namespaceIndex = row.getNamespaceIndex();
        node.nodeId = row.getNodeId();
        node.dataType = row.getDataType();
        node.accessLevel = row.getAccessLevel();
        node.nodeDescription = row.getNodeDescription();

        final TagDto tag = new TagDto();
        tag.tagName = row.getTagName();
        tag.tagNameDefault = row.getTagNameDefault();
        tag.tagDescription = row.getTagDescription();

        NorthboundDto northbound = null;
        if (row.getNorthboundTopic() != null
                || row.getNorthboundTopicDefault() != null
                || row.getMaxQos() != null
                || row.getMessageExpiryInterval() != null
                || row.getIncludeTimestamp() != null
                || row.getIncludeTagNames() != null
                || row.getIncludeMetadata() != null
                || row.getMqttUserProperties() != null) {
            northbound = new NorthboundDto();
            northbound.topic = row.getNorthboundTopic();
            northbound.topicDefault = row.getNorthboundTopicDefault();
            northbound.maxQos = row.getMaxQos();
            northbound.messageExpiryInterval = row.getMessageExpiryInterval();
            northbound.includeTimestamp = row.getIncludeTimestamp();
            northbound.includeTagNames = row.getIncludeTagNames();
            northbound.includeMetadata = row.getIncludeMetadata();
            northbound.mqttUserProperties = row.getMqttUserProperties();
        }

        SouthboundDto southbound = null;
        if (row.getSouthboundTopic() != null
                || row.getSouthboundTopicDefault() != null
                || row.getSouthboundFieldMapping() != null) {
            southbound = new SouthboundDto();
            southbound.topic = row.getSouthboundTopic();
            southbound.topicDefault = row.getSouthboundTopicDefault();
            if (row.getSouthboundFieldMapping() != null) {
                southbound.fieldMapping = row.getSouthboundFieldMapping().stream()
                        .map(fm -> {
                            final FieldMappingDto fmDto = new FieldMappingDto();
                            fmDto.source = fm.source();
                            fmDto.destination = fm.destination();
                            return fmDto;
                        })
                        .toList();
            }
        }

        final RowDto rowDto = new RowDto();
        rowDto.node = node;
        rowDto.tag = tag;
        rowDto.northbound = northbound;
        rowDto.southbound = southbound;
        return rowDto;
    }

    private static @NotNull DeviceTagRow fromRowDto(final @NotNull RowDto rowDto) {
        final DeviceTagRow.Builder builder = getBuilder(rowDto);
        if (rowDto.northbound != null) {
            builder.northboundTopic(rowDto.northbound.topic);
            builder.northboundTopicDefault(rowDto.northbound.topicDefault);
            builder.maxQos(rowDto.northbound.maxQos);
            builder.messageExpiryInterval(rowDto.northbound.messageExpiryInterval);
            builder.includeTimestamp(rowDto.northbound.includeTimestamp);
            builder.includeTagNames(rowDto.northbound.includeTagNames);
            builder.includeMetadata(rowDto.northbound.includeMetadata);
            builder.mqttUserProperties(rowDto.northbound.mqttUserProperties);
        }
        if (rowDto.southbound != null) {
            builder.southboundTopic(rowDto.southbound.topic);
            builder.southboundTopicDefault(rowDto.southbound.topicDefault);
            if (rowDto.southbound.fieldMapping != null) {
                builder.southboundFieldMapping(rowDto.southbound.fieldMapping.stream()
                        .map(fm -> new FieldMappingInstruction(fm.source, fm.destination))
                        .toList());
            }
        }
        return builder.build();
    }

    private static DeviceTagRow.@NonNull Builder getBuilder(final @NonNull RowDto rowDto) {
        final DeviceTagRow.Builder builder = DeviceTagRow.builder();
        if (rowDto.node != null) {
            builder.nodePath(rowDto.node.nodePath);
            builder.namespaceUri(rowDto.node.namespaceUri);
            builder.namespaceIndex(rowDto.node.namespaceIndex);
            builder.nodeId(rowDto.node.nodeId);
            builder.dataType(rowDto.node.dataType);
            builder.accessLevel(rowDto.node.accessLevel);
            builder.nodeDescription(rowDto.node.nodeDescription);
        }
        if (rowDto.tag != null) {
            builder.tagName(rowDto.tag.tagName);
            builder.tagNameDefault(rowDto.tag.tagNameDefault);
            builder.tagDescription(rowDto.tag.tagDescription);
        }
        return builder;
    }

    public byte @NotNull [] serialize(final @NotNull List<DeviceTagRow> rows) throws IOException {
        return mapper.writeValueAsBytes(toFileDto(rows));
    }

    public void serialize(final @NotNull Iterable<DeviceTagRow> rows, final @NotNull OutputStream out)
            throws IOException {
        try (final JsonGenerator gen = mapper.createGenerator(out)) {
            gen.writeStartObject();
            gen.writeFieldName("rows");
            gen.writeStartArray();
            for (final DeviceTagRow row : rows) {
                mapper.writeValue(gen, toRowDto(row));
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    public @NotNull List<DeviceTagRow> deserialize(final byte @NotNull [] data) throws IOException {
        return fromFileDto(mapper.readValue(data, FileDto.class));
    }

    static class FileDto {
        @JsonProperty("rows")
        @Nullable
        List<RowDto> rows;

        FileDto() {
            // required by jackson
        }

        FileDto(final @Nullable List<RowDto> rows) {
            this.rows = rows;
        }
    }

    static class RowDto {
        @JsonProperty("node")
        @Nullable
        NodeDto node;

        @JsonProperty("tag")
        @Nullable
        TagDto tag;

        @JsonProperty("northbound")
        @Nullable
        NorthboundDto northbound;

        @JsonProperty("southbound")
        @Nullable
        SouthboundDto southbound;
    }

    static class NodeDto {
        @JsonProperty("node_path")
        @Nullable
        String nodePath;

        @JsonProperty("namespace_uri")
        @Nullable
        String namespaceUri;

        @JsonProperty("namespace_index")
        int namespaceIndex;

        @JsonProperty("node_id")
        @Nullable
        String nodeId;

        @JsonProperty("data_type")
        @Nullable
        String dataType;

        @JsonProperty("access_level")
        @Nullable
        String accessLevel;

        @JsonProperty("node_description")
        @Nullable
        String nodeDescription;
    }

    static class TagDto {
        @JsonProperty("tag_name")
        @Nullable
        String tagName;

        @JsonProperty("tag_name_default")
        @Nullable
        String tagNameDefault;

        @JsonProperty("tag_description")
        @Nullable
        String tagDescription;
    }

    static class NorthboundDto {
        @JsonProperty("topic")
        @Nullable
        String topic;

        @JsonProperty("topic_default")
        @Nullable
        String topicDefault;

        @JsonProperty("max_qos")
        @Nullable
        Integer maxQos;

        @JsonProperty("message_expiry_interval")
        @Nullable
        Long messageExpiryInterval;

        @JsonProperty("include_timestamp")
        @Nullable
        Boolean includeTimestamp;

        @JsonProperty("include_tag_names")
        @Nullable
        Boolean includeTagNames;

        @JsonProperty("include_metadata")
        @Nullable
        Boolean includeMetadata;

        @JsonProperty("mqtt_user_properties")
        @Nullable
        Map<String, String> mqttUserProperties;
    }

    static class SouthboundDto {
        @JsonProperty("topic")
        @Nullable
        String topic;

        @JsonProperty("topic_default")
        @Nullable
        String topicDefault;

        @JsonProperty("field_mapping")
        @Nullable
        List<FieldMappingDto> fieldMapping;
    }

    static class FieldMappingDto {
        @JsonProperty("source")
        @NotNull
        String source = "";

        @JsonProperty("destination")
        @NotNull
        String destination = "";
    }
}
