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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.discovery.BrowseException;
import com.hivemq.adapter.sdk.api.discovery.BrowsedNode;
import com.hivemq.adapter.sdk.api.discovery.BulkTagBrowser;
import com.hivemq.edge.adapters.browse.file.DeviceTagCsvSerializer;
import com.hivemq.edge.adapters.browse.file.DeviceTagJsonSerializer;
import com.hivemq.edge.adapters.browse.file.DeviceTagYamlSerializer;
import com.hivemq.edge.adapters.browse.importer.DeviceTagImporter;
import com.hivemq.edge.adapters.browse.importer.DeviceTagImporterException;
import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import com.hivemq.edge.adapters.browse.model.ImportMode;
import com.hivemq.edge.adapters.browse.model.ImportResult;
import com.hivemq.edge.adapters.browse.validate.ValidationError;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JAX-RS resource providing bulk device tag browsing and import endpoints.
 * These are hand-crafted (not OpenAPI-generated) endpoints under the existing adapter API path.
 */
@Singleton
@Path("/api/v1/management/protocol-adapters/adapters/{adapterId}/device-tags")
public class DeviceTagBrowsingResource {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DeviceTagBrowsingResource.class);

    private static final String MEDIA_TYPE_CSV = "text/csv";
    private static final String MEDIA_TYPE_YAML = "application/yaml";

    private final @NotNull ProtocolAdapterManager protocolAdapterManager;
    private final @NotNull DeviceTagCsvSerializer csvSerializer;
    private final @NotNull DeviceTagJsonSerializer jsonSerializer;
    private final @NotNull DeviceTagYamlSerializer yamlSerializer;
    private final @NotNull DeviceTagImporter importer;
    private final @NotNull ObjectMapper objectMapper;

    @Inject
    public DeviceTagBrowsingResource(
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

    private static @NotNull String resolveFormat(final String accept, final @NotNull String defaultFormat) {
        if (accept == null || accept.isEmpty() || accept.contains("*/*")) {
            return defaultFormat;
        }
        if (accept.contains("json")) {
            return MediaType.APPLICATION_JSON;
        }
        if (accept.contains("yaml")) {
            return MEDIA_TYPE_YAML;
        }
        if (accept.contains("csv") || accept.contains("text")) {
            return MEDIA_TYPE_CSV;
        }
        return defaultFormat;
    }

    /**
     * Browse the device address space and return discovered nodes as a file.
     *
     * @param adapterId  the adapter ID
     * @param rootNodeId optional root node ID (default: ObjectsFolder i=85)
     * @param maxDepth   optional max depth (default: 0 = unlimited)
     * @param accept     Accept header for content negotiation
     * @return 200 with file body, 404/409/504 on error
     */
    @POST
    @Path("/browse")
    @Produces({MEDIA_TYPE_CSV, MediaType.APPLICATION_JSON, MEDIA_TYPE_YAML})
    public @NotNull Response browse(
            @PathParam("adapterId") final @NotNull String adapterId,
            @QueryParam("rootNodeId") final String rootNodeId,
            @QueryParam("maxDepth") @DefaultValue("0") final int maxDepth,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept) {

        // Lookup adapter
        final Optional<ProtocolAdapterWrapper> wrapperOpt =
                protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId);
        if (wrapperOpt.isEmpty()) {
            return errorResponse(Response.Status.NOT_FOUND, "Adapter '" + adapterId + "' not found");
        }

        final ProtocolAdapter adapter = wrapperOpt.get().getAdapter();
        if (!(adapter instanceof BulkTagBrowser browser)) {
            return errorResponse(Response.Status.CONFLICT,
                    "Adapter '" + adapterId + "' does not support bulk tag browsing");
        }

        // Browse
        final List<BrowsedNode> nodes;
        try {
            nodes = browser.browse(rootNodeId, maxDepth);
        } catch (final BrowseException e) {
            log.error("Browse failed for adapter '{}'", adapterId, e);
            if (e.getMessage() != null && e.getMessage().contains("timed out")) {
                return errorResponse(Response.Status.GATEWAY_TIMEOUT, e.getMessage());
            }
            return errorResponse(Response.Status.CONFLICT, e.getMessage());
        }

        // Map to DeviceTagRow
        final List<DeviceTagRow> rows = nodes.stream().map(DeviceTagRow::fromBrowsedNode).toList();

        // Determine output format
        final String format = resolveFormat(accept, MEDIA_TYPE_CSV);
        final String extension;
        final String mediaType;
        final byte[] data;

        try {
            switch (format) {
                case MediaType.APPLICATION_JSON -> {
                    data = jsonSerializer.serialize(rows);
                    extension = "json";
                    mediaType = MediaType.APPLICATION_JSON;
                }
                case MEDIA_TYPE_YAML -> {
                    data = yamlSerializer.serialize(rows);
                    extension = "yaml";
                    mediaType = MEDIA_TYPE_YAML;
                }
                default -> {
                    data = csvSerializer.serialize(rows);
                    extension = "csv";
                    mediaType = MEDIA_TYPE_CSV;
                }
            }
        } catch (final IOException e) {
            log.error("Failed to serialize browse results for adapter '{}'", adapterId, e);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Serialization failed: " + e.getMessage());
        }

        return Response.ok(data, mediaType)
                .header("Content-Disposition",
                        "attachment; filename=\"" + adapterId + "-device-tags." + extension + "\"")
                .build();
    }

    // --- Helpers ---

    /**
     * Import device tags and mappings from a file.
     *
     * @param adapterId     the adapter ID
     * @param mode          import conflict-resolution mode (default: MERGE_SAFE)
     * @param validateNodes whether to validate node existence (default: false)
     * @param contentType   Content-Type header
     * @param body          the file content
     * @return 200 with ImportResult, 400 with errors, 404/409/415 on error
     */
    @POST
    @Path("/import")
    @Consumes({MEDIA_TYPE_CSV, MediaType.APPLICATION_JSON, MEDIA_TYPE_YAML})
    @Produces(MediaType.APPLICATION_JSON)
    public @NotNull Response importTags(
            @PathParam("adapterId") final @NotNull String adapterId,
            @QueryParam("mode") @DefaultValue("MERGE_SAFE") final String mode,
            @QueryParam("validateNodes") @DefaultValue("false") final boolean validateNodes,
            @HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            final byte[] body) {

        // Lookup adapter
        final Optional<ProtocolAdapterWrapper> wrapperOpt =
                protocolAdapterManager.getProtocolAdapterWrapperByAdapterId(adapterId);
        if (wrapperOpt.isEmpty()) {
            return errorResponse(Response.Status.NOT_FOUND, "Adapter '" + adapterId + "' not found");
        }

        // Parse mode
        final ImportMode importMode;
        try {
            importMode = ImportMode.valueOf(mode);
        } catch (final IllegalArgumentException e) {
            return errorResponse(Response.Status.BAD_REQUEST,
                    "Invalid import mode '" +
                            mode +
                            "'. Valid values: CREATE, DELETE, OVERWRITE, MERGE_SAFE, MERGE_OVERWRITE");
        }

        // Deserialize body based on Content-Type
        final List<DeviceTagRow> rows;
        try {
            final String ct = contentType != null ? contentType.toLowerCase() : "";
            if (ct.contains("json")) {
                rows = jsonSerializer.deserialize(body);
            } else if (ct.contains("yaml")) {
                rows = yamlSerializer.deserialize(body);
            } else if (ct.contains("csv") || ct.contains("text")) {
                rows = csvSerializer.deserialize(body);
            } else {
                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                        .entity(errorBody("Unsupported Content-Type",
                                "Supported types: text/csv, application/json, application/yaml"))
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
        } catch (final Exception e) {
            log.warn("Failed to parse import file for adapter '{}'", adapterId, e);
            return errorResponse(Response.Status.BAD_REQUEST, "Failed to parse file: " + e.getMessage());
        }

        // Perform import
        try {
            final ImportResult result = importer.doImport(rows, importMode, adapterId);
            return Response.ok(objectMapper.writeValueAsString(result), MediaType.APPLICATION_JSON).build();
        } catch (final DeviceTagImporterException e) {
            return validationErrorResponse(e.getErrors());
        } catch (final Exception e) {
            log.error("Import failed for adapter '{}'", adapterId, e);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Import failed: " + e.getMessage());
        }
    }

    private @NotNull Response errorResponse(final @NotNull Response.Status status, final @NotNull String detail) {
        return Response.status(status)
                .entity(errorBody(status.getReasonPhrase(), detail))
                .type(MediaType.APPLICATION_JSON)
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
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (final Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"title\":\"Validation Failed\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    private @NotNull String errorBody(final @NotNull String title, final @NotNull String detail) {
        try {
            final Map<String, String> body = Map.of("title", title, "detail", detail);
            return objectMapper.writeValueAsString(body);
        } catch (final Exception e) {
            return "{\"title\":\"" + title + "\",\"detail\":\"" + detail + "\"}";
        }
    }
}
