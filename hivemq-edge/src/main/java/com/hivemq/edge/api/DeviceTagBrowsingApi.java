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
package com.hivemq.edge.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
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
import org.jetbrains.annotations.Nullable;

@Path("/api/v1/management/protocol-adapters/adapters/{adapterId}/device-tags")
@Tag(name = "Protocol Adapters")
public interface DeviceTagBrowsingApi {

    @NotNull
    String MEDIA_TYPE_CSV = "text/csv";

    @NotNull
    String MEDIA_TYPE_YAML = "application/yaml";

    @POST
    @Path("/browse")
    @Produces({MEDIA_TYPE_CSV, MediaType.APPLICATION_JSON, MEDIA_TYPE_YAML})
    @RolesAllowed({"admin"})
    @Operation(
            summary = "Browse device tags",
            description = "Browse the device address space and return discovered nodes as a downloadable file.",
            operationId = "browseDeviceTags",
            responses = {
                @ApiResponse(responseCode = "200", description = "Success"),
                @ApiResponse(responseCode = "404", description = "Adapter not found"),
                @ApiResponse(
                        responseCode = "409",
                        description = "Adapter does not support bulk tag browsing or browse failed"),
                @ApiResponse(responseCode = "504", description = "Browse timed out")
            })
    @NotNull
    Response browse(
            @PathParam("adapterId") @Parameter(description = "The adapter ID.") @NotNull String adapterId,
            @QueryParam("rootNodeId") @Parameter(description = "Optional root node ID.") @Nullable String rootNodeId,
            @QueryParam("maxDepth") @DefaultValue("0") @Parameter(description = "Max browse depth (0 = unlimited).")
                    int maxDepth,
            @HeaderParam(HttpHeaders.ACCEPT) @NotNull String accept);

    @POST
    @Path("/import")
    @Consumes({MEDIA_TYPE_CSV, MediaType.APPLICATION_JSON, MEDIA_TYPE_YAML})
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin"})
    @Operation(
            summary = "Import device tags",
            description = "Import device tags and mappings from a file.",
            operationId = "importDeviceTags",
            requestBody =
                    @RequestBody(
                            description = "The file content (CSV, JSON, or YAML).",
                            required = true,
                            content = {
                                @Content(
                                        mediaType = MEDIA_TYPE_CSV,
                                        schema = @Schema(type = "string", format = "binary")),
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(type = "string", format = "binary")),
                                @Content(
                                        mediaType = MEDIA_TYPE_YAML,
                                        schema = @Schema(type = "string", format = "binary"))
                            }),
            responses = {
                @ApiResponse(responseCode = "200", description = "Success"),
                @ApiResponse(responseCode = "400", description = "Invalid file or validation errors"),
                @ApiResponse(responseCode = "404", description = "Adapter not found"),
                @ApiResponse(responseCode = "415", description = "Unsupported media type")
            })
    @NotNull
    Response importTags(
            @PathParam("adapterId") @Parameter(description = "The adapter ID.") @NotNull String adapterId,
            @QueryParam("mode")
                    @DefaultValue("MERGE_SAFE")
                    @Parameter(
                            description = "Import conflict-resolution mode.",
                            schema =
                                    @Schema(
                                            allowableValues = {
                                                "CREATE",
                                                "DELETE",
                                                "OVERWRITE",
                                                "MERGE_SAFE",
                                                "MERGE_OVERWRITE"
                                            },
                                            defaultValue = "MERGE_SAFE"))
                    @NotNull
                    String mode,
            @QueryParam("validateNodes") @DefaultValue("false") @Parameter(description = "Validate node existence.")
                    boolean validateNodes,
            @HeaderParam(HttpHeaders.CONTENT_TYPE) @NotNull String contentType,
            byte @NotNull [] body);
}
