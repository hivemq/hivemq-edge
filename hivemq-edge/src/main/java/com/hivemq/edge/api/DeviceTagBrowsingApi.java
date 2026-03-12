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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
@Api(value = "the Device Tag Browsing API")
public interface DeviceTagBrowsingApi {

    @NotNull
    String MEDIA_TYPE_CSV = "text/csv";

    @NotNull
    String MEDIA_TYPE_YAML = "application/yaml";

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
    @RolesAllowed({"admin"})
    @ApiOperation(
            value = "Browse device tags",
            notes = "Browse the device address space and return discovered nodes as a downloadable file.",
            tags = {"Protocol Adapters"})
    @ApiResponses(
            value = {
                @ApiResponse(code = 200, message = "Success"),
                @ApiResponse(code = 404, message = "Adapter not found"),
                @ApiResponse(code = 409, message = "Adapter does not support bulk tag browsing or browse failed"),
                @ApiResponse(code = 504, message = "Browse timed out")
            })
    @NotNull
    Response browse(
            @PathParam("adapterId") @ApiParam("The adapter ID.") @NotNull String adapterId,
            @QueryParam("rootNodeId") @ApiParam("Optional root node ID.") @Nullable String rootNodeId,
            @QueryParam("maxDepth") @DefaultValue("0") @ApiParam("Max browse depth (0 = unlimited).") int maxDepth,
            @HeaderParam(HttpHeaders.ACCEPT) @NotNull String accept);

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
    @RolesAllowed({"admin"})
    @ApiOperation(
            value = "Import device tags",
            notes = "Import device tags and mappings from a file.",
            tags = {"Protocol Adapters"})
    @ApiResponses(
            value = {
                @ApiResponse(code = 200, message = "Success"),
                @ApiResponse(code = 400, message = "Invalid file or validation errors"),
                @ApiResponse(code = 404, message = "Adapter not found"),
                @ApiResponse(code = 415, message = "Unsupported media type")
            })
    @NotNull
    Response importTags(
            @PathParam("adapterId") @ApiParam("The adapter ID.") @NotNull String adapterId,
            @QueryParam("mode") @DefaultValue("MERGE_SAFE") @ApiParam("Import conflict-resolution mode.") @NotNull
                    String mode,
            @QueryParam("validateNodes") @DefaultValue("false") @ApiParam("Validate node existence.")
                    boolean validateNodes,
            @HeaderParam(HttpHeaders.CONTENT_TYPE) @NotNull String contentType,
            byte @NotNull [] body);
}
