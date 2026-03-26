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
package com.hivemq.edge.adapters.browse.rest;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.discovery.BrowseException;
import com.hivemq.adapter.sdk.api.discovery.BrowsedNode;
import com.hivemq.adapter.sdk.api.discovery.BulkTagBrowser;
import com.hivemq.api.AbstractApi;
import com.hivemq.edge.adapters.browse.file.DeviceTagCsvSerializer;
import com.hivemq.edge.adapters.browse.file.DeviceTagJsonSerializer;
import com.hivemq.edge.adapters.browse.file.DeviceTagYamlSerializer;
import com.hivemq.edge.adapters.browse.importer.DeviceTagImporter;
import com.hivemq.edge.adapters.browse.importer.DeviceTagImporterException;
import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import com.hivemq.edge.adapters.browse.model.ImportMode;
import com.hivemq.edge.adapters.browse.model.ImportResult;
import com.hivemq.edge.adapters.browse.validate.ValidationError;
import com.hivemq.edge.api.DeviceTagBrowsingApi;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class DeviceTagBrowsingResourceImpl extends AbstractApi implements DeviceTagBrowsingApi {

    private static final @NotNull String MEDIA_TYPE_CSV = "text/csv";
    private static final @NotNull String MEDIA_TYPE_YAML = "application/yaml";

    private final @NotNull ProtocolAdapterManager protocolAdapterManager;
    private final @NotNull DeviceTagCsvSerializer csvSerializer;
    private final @NotNull DeviceTagJsonSerializer jsonSerializer;
    private final @NotNull DeviceTagYamlSerializer yamlSerializer;
    private final @NotNull DeviceTagImporter importer;
    private final @NotNull ObjectMapper objectMapper;

    @Inject
    public DeviceTagBrowsingResourceImpl(
            final @NotNull ProtocolAdapterManager protocolAdapterManager,
            final @NotNull DeviceTagCsvSerializer csvSerializer,
            final @NotNull DeviceTagJsonSerializer jsonSerializer,
            final @NotNull DeviceTagYamlSerializer yamlSerializer,
            final @NotNull DeviceTagImporter importer,
            final @NotNull ObjectMapper objectMapper) {
        this.protocolAdapterManager = protocolAdapterManager;
        this.csvSerializer = csvSerializer;
        this.jsonSerializer = jsonSerializer;
        this.yamlSerializer = yamlSerializer;
        this.importer = importer;
        this.objectMapper = objectMapper;
    }

    private @NotNull String resolveFormat() {
        final String accept = headers != null ? headers.getHeaderString("Accept") : null;
        if (accept == null || accept.isEmpty() || accept.contains("*/*")) {
            return MEDIA_TYPE_CSV;
        }
        final String type = accept.toLowerCase(Locale.ROOT);
        return type.contains("json") ? APPLICATION_JSON : type.contains("yaml") ? MEDIA_TYPE_YAML : MEDIA_TYPE_CSV;
    }

    @Override
    public @NotNull Response browseDeviceTags(
            final @NotNull String adapterId, final @Nullable String rootId, final @Nullable Integer maxDepth) {
        // Lookup adapter
        final Optional<ProtocolAdapterWrapper> wrapperOpt =
                protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId);
        if (wrapperOpt.isEmpty()) {
            return errorResponse(Response.Status.NOT_FOUND, "Adapter '" + adapterId + "' not found");
        }
        final ProtocolAdapter adapter = wrapperOpt.get().getAdapter();
        if (!(adapter instanceof final BulkTagBrowser browser)) {
            return errorResponse(
                    Response.Status.CONFLICT, "Adapter '" + adapterId + "' does not support bulk tag browsing");
        }

        // Browse
        final int depth = maxDepth != null ? maxDepth : 0;
        final List<BrowsedNode> nodes;
        try {
            nodes = browser.browse(rootId, depth);
        } catch (final BrowseException e) {
            logger.error("Browse failed for adapter '{}'", adapterId, e);
            if (e.getMessage() != null && e.getMessage().contains("timed out")) {
                return errorResponse(Response.Status.GATEWAY_TIMEOUT, e.getMessage());
            }
            return errorResponse(Response.Status.CONFLICT, e.getMessage() != null ? e.getMessage() : "Browse failed");
        }

        // Lazy mapping: DeviceTagRow objects are created one-at-a-time during serialization,
        // avoiding a second fully-materialized list alongside the BrowsedNode list.
        final Iterable<DeviceTagRow> rows =
                () -> nodes.stream().map(DeviceTagRow::fromBrowsedNode).iterator();

        // Determine output format and stream directly to the HTTP response
        final String format = resolveFormat();
        final String extension;
        final String mediaType;
        final StreamingOutput stream;
        switch (format) {
            case APPLICATION_JSON -> {
                stream = output -> jsonSerializer.serialize(rows, output);
                extension = "json";
                mediaType = APPLICATION_JSON;
            }
            case MEDIA_TYPE_YAML -> {
                stream = output -> yamlSerializer.serialize(rows, output);
                extension = "yaml";
                mediaType = MEDIA_TYPE_YAML;
            }
            default -> {
                stream = output -> csvSerializer.serialize(rows, output);
                extension = "csv";
                mediaType = MEDIA_TYPE_CSV;
            }
        }
        return Response.ok(stream, mediaType)
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + adapterId + "-device-tags." + extension + "\"")
                .build();
    }

    @Override
    public @NotNull Response importDeviceTags(
            final @NotNull String adapterId,
            final @NotNull File body,
            final @Nullable String mode,
            final @Nullable Boolean validateNodes) {
        // Lookup adapter
        final Optional<ProtocolAdapterWrapper> wrapperOpt =
                protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId);
        if (wrapperOpt.isEmpty()) {
            return errorResponse(Response.Status.NOT_FOUND, "Adapter '" + adapterId + "' not found");
        }

        // Parse mode
        final String modeStr = mode != null ? mode : "MERGE_SAFE";
        final ImportMode importMode;
        try {
            importMode = ImportMode.valueOf(modeStr);
        } catch (final IllegalArgumentException e) {
            return errorResponse(
                    Response.Status.BAD_REQUEST,
                    "Invalid import mode '" + modeStr
                            + "'. Valid values: CREATE, DELETE, OVERWRITE, MERGE_SAFE, MERGE_OVERWRITE");
        }

        // Read body bytes from the File provided by JAX-RS
        final byte[] bodyBytes;
        try {
            bodyBytes = Files.readAllBytes(body.toPath());
        } catch (final IOException e) {
            logger.warn("Failed to read import file for adapter '{}'", adapterId, e);
            return errorResponse(Response.Status.BAD_REQUEST, "Failed to read file: " + e.getMessage());
        }

        // Deserialize body based on Content-Type
        final String contentType = headers != null ? headers.getHeaderString("Content-Type") : "";
        final List<DeviceTagRow> rows;
        try {
            final String ct = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";
            if (ct.contains("json")) {
                rows = jsonSerializer.deserialize(bodyBytes);
            } else if (ct.contains("yaml")) {
                rows = yamlSerializer.deserialize(bodyBytes);
            } else if (ct.contains("csv") || ct.contains("text")) {
                rows = csvSerializer.deserialize(bodyBytes);
            } else {
                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                        .entity(errorBody(
                                "Unsupported Content-Type",
                                "Supported types: text/csv, application/json, application/yaml"))
                        .type(APPLICATION_JSON)
                        .build();
            }
        } catch (final Exception e) {
            logger.warn("Failed to parse import file for adapter '{}'", adapterId, e);
            return errorResponse(Response.Status.BAD_REQUEST, "Failed to parse file: " + e.getMessage());
        }

        // Perform import
        try {
            final ImportResult result = importer.doImport(rows, importMode, adapterId);
            return Response.ok(objectMapper.writeValueAsString(result), APPLICATION_JSON)
                    .build();
        } catch (final DeviceTagImporterException e) {
            return validationErrorResponse(e.getErrors());
        } catch (final Exception e) {
            logger.error("Import failed for adapter '{}'", adapterId, e);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Import failed: " + e.getMessage());
        }
    }

    private @NotNull Response errorResponse(final @NotNull Response.Status status, final @NotNull String detail) {
        return Response.status(status)
                .entity(errorBody(status.getReasonPhrase(), detail))
                .type(APPLICATION_JSON)
                .build();
    }

    private @NotNull Response validationErrorResponse(final @NotNull List<ValidationError> errors) {
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Validation Failed");
        body.put("detail", errors.size() + " validation error(s) found");
        body.put("errors", errors);
        try {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(objectMapper.writeValueAsString(body))
                    .type(APPLICATION_JSON)
                    .build();
        } catch (final Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"title\":\"Validation Failed\"}")
                    .type(APPLICATION_JSON)
                    .build();
        }
    }

    private @NotNull String errorBody(final @NotNull String title, final @NotNull String detail) {
        try {
            return objectMapper.writeValueAsString(Map.of("title", title, "detail", detail));
        } catch (final Exception e) {
            return "{\"title\":\"" + title + "\",\"detail\":\"" + detail + "\"}";
        }
    }
}
