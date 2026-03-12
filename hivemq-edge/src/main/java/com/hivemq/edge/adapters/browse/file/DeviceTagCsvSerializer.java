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

import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import com.hivemq.edge.adapters.browse.model.FieldMappingInstruction;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Serializes and deserializes {@link DeviceTagRow} lists to/from CSV format using Apache Commons CSV.
 * <p>
 * The CSV uses RFC 4180 format with a header row. Columns follow the 21-field design with
 * compact encodings for field mappings ({@code source->dest;source2->dest2}) and
 * MQTT user properties ({@code key=value;key=value}).
 */
@Singleton
public class DeviceTagCsvSerializer {

    @Inject
    public DeviceTagCsvSerializer() {}

    // Column names in canonical order
    static final String COL_NODE_PATH = "node_path";
    static final String COL_NAMESPACE_URI = "namespace_uri";
    static final String COL_NAMESPACE_INDEX = "namespace_index";
    static final String COL_NODE_ID = "node_id";
    static final String COL_DATA_TYPE = "data_type";
    static final String COL_ACCESS_LEVEL = "access_level";
    static final String COL_NODE_DESCRIPTION = "node_description";
    static final String COL_TAG_NAME = "tag_name";
    static final String COL_TAG_NAME_DEFAULT = "tag_name_default";
    static final String COL_TAG_DESCRIPTION = "tag_description";
    static final String COL_NORTHBOUND_TOPIC = "northbound_topic";
    static final String COL_NORTHBOUND_TOPIC_DEFAULT = "northbound_topic_default";
    static final String COL_SOUTHBOUND_TOPIC = "southbound_topic";
    static final String COL_SOUTHBOUND_TOPIC_DEFAULT = "southbound_topic_default";
    static final String COL_SOUTHBOUND_FIELD_MAPPING = "southbound_field_mapping";
    static final String COL_MAX_QOS = "max_qos";
    static final String COL_MESSAGE_EXPIRY_INTERVAL = "message_expiry_interval";
    static final String COL_INCLUDE_TIMESTAMP = "include_timestamp";
    static final String COL_INCLUDE_TAG_NAMES = "include_tag_names";
    static final String COL_INCLUDE_METADATA = "include_metadata";
    static final String COL_MQTT_USER_PROPERTIES = "mqtt_user_properties";

    private static final String[] HEADER = {
        COL_NODE_PATH,
        COL_NAMESPACE_URI,
        COL_NAMESPACE_INDEX,
        COL_NODE_ID,
        COL_DATA_TYPE,
        COL_ACCESS_LEVEL,
        COL_NODE_DESCRIPTION,
        COL_TAG_NAME,
        COL_TAG_NAME_DEFAULT,
        COL_TAG_DESCRIPTION,
        COL_NORTHBOUND_TOPIC,
        COL_NORTHBOUND_TOPIC_DEFAULT,
        COL_SOUTHBOUND_TOPIC,
        COL_SOUTHBOUND_TOPIC_DEFAULT,
        COL_SOUTHBOUND_FIELD_MAPPING,
        COL_MAX_QOS,
        COL_MESSAGE_EXPIRY_INTERVAL,
        COL_INCLUDE_TIMESTAMP,
        COL_INCLUDE_TAG_NAMES,
        COL_INCLUDE_METADATA,
        COL_MQTT_USER_PROPERTIES
    };

    private static final CSVFormat CSV_FORMAT = CSVFormat.RFC4180
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .build();

    public byte @NotNull [] serialize(final @NotNull List<DeviceTagRow> rows) throws IOException {
        final List<DeviceTagRow> sorted = rows.stream()
                .sorted(Comparator.comparing(r -> r.getNodePath() != null ? r.getNodePath() : "", String::compareTo))
                .toList();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
                final CSVPrinter printer = new CSVPrinter(
                        writer,
                        CSVFormat.RFC4180
                                .builder()
                                .setHeader(HEADER)
                                .setRecordSeparator("\r\n")
                                .build())) {
            for (final DeviceTagRow row : sorted) {
                printer.printRecord(
                        row.getNodePath(),
                        row.getNamespaceUri(),
                        row.getNamespaceIndex(),
                        row.getNodeId(),
                        row.getDataType(),
                        row.getAccessLevel(),
                        row.getNodeDescription(),
                        row.getTagName(),
                        row.getTagNameDefault(),
                        row.getTagDescription(),
                        row.getNorthboundTopic(),
                        row.getNorthboundTopicDefault(),
                        row.getSouthboundTopic(),
                        row.getSouthboundTopicDefault(),
                        encodeFieldMapping(row.getSouthboundFieldMapping()),
                        row.getMaxQos(),
                        row.getMessageExpiryInterval(),
                        row.getIncludeTimestamp(),
                        row.getIncludeTagNames(),
                        row.getIncludeMetadata(),
                        encodeUserProperties(row.getMqttUserProperties()));
            }
        }
        return baos.toByteArray();
    }

    public @NotNull List<DeviceTagRow> deserialize(final byte @NotNull [] csvData) throws IOException {
        final List<DeviceTagRow> rows = new ArrayList<>();
        try (final InputStreamReader reader =
                        new InputStreamReader(new ByteArrayInputStream(csvData), StandardCharsets.UTF_8);
                final CSVParser parser = CSV_FORMAT.parse(reader)) {

            if (parser.getHeaderMap() == null || parser.getHeaderMap().isEmpty()) {
                throw new IOException("CSV file is missing header row");
            }

            for (final CSVRecord record : parser) {
                final DeviceTagRow.Builder builder = DeviceTagRow.builder();

                builder.nodePath(getOptional(record, COL_NODE_PATH));
                builder.namespaceUri(getOptional(record, COL_NAMESPACE_URI));
                builder.namespaceIndex(getIntOrDefault(record, COL_NAMESPACE_INDEX, 0));
                builder.nodeId(getOptional(record, COL_NODE_ID));
                builder.dataType(getOptional(record, COL_DATA_TYPE));
                builder.accessLevel(getOptional(record, COL_ACCESS_LEVEL));
                builder.nodeDescription(getOptional(record, COL_NODE_DESCRIPTION));
                builder.tagName(getOptional(record, COL_TAG_NAME));
                builder.tagNameDefault(getOptional(record, COL_TAG_NAME_DEFAULT));
                builder.tagDescription(getOptional(record, COL_TAG_DESCRIPTION));
                builder.northboundTopic(getOptional(record, COL_NORTHBOUND_TOPIC));
                builder.northboundTopicDefault(getOptional(record, COL_NORTHBOUND_TOPIC_DEFAULT));
                builder.southboundTopic(getOptional(record, COL_SOUTHBOUND_TOPIC));
                builder.southboundTopicDefault(getOptional(record, COL_SOUTHBOUND_TOPIC_DEFAULT));
                builder.southboundFieldMapping(decodeFieldMapping(getOptional(record, COL_SOUTHBOUND_FIELD_MAPPING)));
                builder.maxQos(getIntegerOrNull(record, COL_MAX_QOS));
                builder.messageExpiryInterval(getLongOrNull(record, COL_MESSAGE_EXPIRY_INTERVAL));
                builder.includeTimestamp(getBooleanOrNull(record, COL_INCLUDE_TIMESTAMP));
                builder.includeTagNames(getBooleanOrNull(record, COL_INCLUDE_TAG_NAMES));
                builder.includeMetadata(getBooleanOrNull(record, COL_INCLUDE_METADATA));
                builder.mqttUserProperties(decodeUserProperties(getOptional(record, COL_MQTT_USER_PROPERTIES)));

                rows.add(builder.build());
            }
        }
        return rows;
    }

    // --- Compact encoding helpers ---

    static @Nullable String encodeFieldMapping(final @Nullable List<FieldMappingInstruction> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mappings.size(); i++) {
            if (i > 0) sb.append(';');
            sb.append(mappings.get(i).source())
                    .append("->")
                    .append(mappings.get(i).destination());
        }
        return sb.toString();
    }

    static @Nullable List<FieldMappingInstruction> decodeFieldMapping(final @Nullable String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        final List<FieldMappingInstruction> result = new ArrayList<>();
        for (final String pair : encoded.split(";")) {
            final String trimmed = pair.trim();
            if (trimmed.isEmpty()) continue;
            final int arrowIdx = trimmed.indexOf("->");
            if (arrowIdx < 0) {
                throw new IllegalArgumentException(
                        "Invalid field mapping format: '" + trimmed + "'. Expected 'source->destination'.");
            }
            result.add(new FieldMappingInstruction(
                    trimmed.substring(0, arrowIdx).trim(),
                    trimmed.substring(arrowIdx + 2).trim()));
        }
        return result.isEmpty() ? null : result;
    }

    static @Nullable String encodeUserProperties(final @Nullable Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final Map.Entry<String, String> entry : properties.entrySet()) {
            if (!first) sb.append(';');
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    static @Nullable Map<String, String> decodeUserProperties(final @Nullable String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        final Map<String, String> result = new LinkedHashMap<>();
        for (final String pair : encoded.split(";")) {
            final String trimmed = pair.trim();
            if (trimmed.isEmpty()) continue;
            final int eqIdx = trimmed.indexOf('=');
            if (eqIdx < 0) {
                throw new IllegalArgumentException(
                        "Invalid user property format: '" + trimmed + "'. Expected 'key=value'.");
            }
            result.put(
                    trimmed.substring(0, eqIdx).trim(),
                    trimmed.substring(eqIdx + 1).trim());
        }
        return result.isEmpty() ? null : result;
    }

    // --- CSV record helpers ---

    private static @Nullable String getOptional(final @NotNull CSVRecord record, final @NotNull String column) {
        if (!record.isMapped(column)) {
            return null;
        }
        final String value = record.get(column);
        return (value == null || value.isEmpty()) ? null : value;
    }

    private static int getIntOrDefault(
            final @NotNull CSVRecord record, final @NotNull String column, final int defaultValue) {
        final String value = getOptional(record, column);
        if (value == null) return defaultValue;
        return Integer.parseInt(value);
    }

    private static @Nullable Integer getIntegerOrNull(final @NotNull CSVRecord record, final @NotNull String column) {
        final String value = getOptional(record, column);
        if (value == null) return null;
        return Integer.parseInt(value);
    }

    private static @Nullable Long getLongOrNull(final @NotNull CSVRecord record, final @NotNull String column) {
        final String value = getOptional(record, column);
        if (value == null) return null;
        return Long.parseLong(value);
    }

    private static @Nullable Boolean getBooleanOrNull(final @NotNull CSVRecord record, final @NotNull String column) {
        final String value = getOptional(record, column);
        if (value == null) return null;
        return Boolean.parseBoolean(value);
    }
}
