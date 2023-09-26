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
package com.hivemq.api.resources;

import com.hivemq.api.model.ApiBodyExamples;
import com.hivemq.api.model.bridge.Bridge;
import com.hivemq.api.model.bridge.BridgeList;
import com.hivemq.api.model.status.Status;
import com.hivemq.api.model.status.StatusList;
import com.hivemq.api.model.status.StatusTransitionCommand;
import com.hivemq.api.model.status.StatusTransitionResult;
import com.hivemq.codec.transcoder.TranscodingResult;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Simon L Johnson
 */

@Path(BridgeApi.PATH)
@Tag(name = "Bridges",
     description = "Explore and interact with the Bridges configured on your Gateway.")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public interface BridgeApi {

    String PATH = "/api/v1/management/bridges";

    @GET
    @Path("")
    @Operation(summary = "List all bridges in the system",
               operationId = "getBridges",
               description = "Get all bridges configured in the system.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = BridgeList.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example response with several bridges.",
                                                                              name = "bridge-list-result",
                                                                              summary = "Bridge List result",
                                                                              value = ApiBodyExamples.EXAMPLE_BRIDGE_LIST_JSON)
                                                       }))})
    Response listBridges();

    @POST
    @Path("")
    @Operation(summary = "Add a new Bridge",
               operationId = "addBridge",
               description = "Add bridge configured in the system.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success")})
    Response addBridge(
            @Parameter(name = "bridge",
                       description = "The new bridge.",
                       required = true,
                       in = ParameterIn.DEFAULT)
            final @NotNull Bridge bridge);

    @DELETE
    @Path("/{bridgeId: ([a-zA-Z_0-9\\-])*}")
    @Operation(summary = "Remove a Bridge",
               operationId = "removeBridge",
               description = "Remove bridge configured in the system.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success")})
    Response deleteBridge(
            @Parameter(name = "bridgeId",
                       description = "The id of the bridge to delete.",
                       required = true,
                       in = ParameterIn.PATH)
            @PathParam("bridgeId") @NotNull String bridgeId);

    @PUT
    @Path("/{bridgeId: ([a-zA-Z_0-9\\-])*}")
    @Operation(summary = "Update a Bridge",
               operationId = "updateBridge",
               description = "Update bridge configured in the system.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success")})
    Response updateBridge(
            @Parameter(name = "bridge",
                       description = "The bridge to update.",
                       required = true,
                       in = ParameterIn.PATH)
            @PathParam("bridgeId") String bridgeId, final @NotNull Bridge bridge);



    @GET
    @Path("/{bridgeId: ([a-zA-Z_0-9\\-])*}/connection-status")
    @Operation(summary = "Get the up to date status of a bridge",
               description = "Get the up to date status of a bridge.",
               operationId = "get-bridge-status",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = Status.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example response with CONNECTED status.",
                                                                              name = "bridge-connection-status-result",
                                                                              summary = "Bridge Connection Status Result",
                                                                              value = ApiBodyExamples.EXAMPLE_CONNECTION_STATUS_JSON)
                                                       }))})
    Response getStatus(@Parameter(name = "bridgeId",
                                            description = "The name of the bridge to query.",
                                            required = true,
                                            in = ParameterIn.PATH)
                                 @PathParam("bridgeId") @NotNull final String bridgeId) ;

    @PUT
    @Path("/{bridgeId: ([a-zA-Z_0-9\\-])*}/status")
    @Operation(summary = "Transition the runtime status of a bridge",
               description = "Transition the connection status of a bridge.",
               operationId = "transition-bridge-status",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = StatusTransitionResult.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example response with PENDING status.",
                                                                              name = "transition-status-result",
                                                                              summary = "Bridge Connection Transition Result",
                                                                              value = ApiBodyExamples.EXAMPLE_STATUS_TRANSITION_RESULT)
                                                       }))})
    Response changeStatus(
            @Parameter(name = "bridgeId",
                       description = "The id of the bridge whose runtime-status will change.",
                       required = true,
                       in = ParameterIn.PATH)
            final @PathParam("bridgeId") String bridgeId,
            @Parameter(name = "command",
                       description = "The command to transition the bridge runtime status.",
                       required = true,
                       in = ParameterIn.DEFAULT)
            final @NotNull StatusTransitionCommand command);


    @GET
    @Path("/{bridgeId: ([a-zA-Z_0-9\\-])*}")
    @Operation(summary = "Get a bridge by ID",
               description = "Get a bridge by ID.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = Bridge.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example Bridge.",
                                                                              name = "bridge-get-result",
                                                                              summary = "Get Bridge Result",
                                                                              value = ApiBodyExamples.EXAMPLE_BRIDGE_JSON)
                                                       }))})
    Response getBridgeByName(@Parameter(name = "bridgeId",
                                             description = "The id of the bridge to query.",
                                             required = true,
                                             in = ParameterIn.PATH)
                                 @PathParam("bridgeId") @NotNull final String bridgeId) ;

    @GET
    @Path("/status")
    @Operation(summary = "Get the status of all the bridges in the system.",
               description = "Obtain the details.",
               operationId = "get-bridges-status",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "The Connection Details Verification Result.",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = StatusList.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example connection status list.",
                                                                              name = "example-connection-status",
                                                                              summary = "Example connection status",
                                                                              value = ApiBodyExamples.EXAMPLE_CONNECTION_STATUS_LIST_JSON)
                                                       }))})
    Response status();

}
