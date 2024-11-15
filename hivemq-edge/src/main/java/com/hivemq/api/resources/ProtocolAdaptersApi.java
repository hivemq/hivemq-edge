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

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.api.model.adapters.Adapter;
import com.hivemq.api.model.adapters.AdaptersList;
import com.hivemq.api.model.adapters.ProtocolAdaptersList;
import com.hivemq.api.model.adapters.ValuesTree;
import com.hivemq.api.model.status.Status;
import com.hivemq.api.model.status.StatusList;
import com.hivemq.api.model.status.StatusTransitionCommand;
import com.hivemq.api.model.status.StatusTransitionResult;
import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.api.model.tags.DomainTagModelList;
import com.hivemq.api.model.tags.TagSchema;
import com.hivemq.api.resources.examples.ApiBodyExamples;
import com.hivemq.api.resources.examples.TagResourceExamples;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.http.error.Errors;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path(ProtocolAdaptersApi.PATH)
@Tag(name = "Protocol Adapters", description = "Interact with protocol adapters.")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public interface ProtocolAdaptersApi {

    String PATH = "/api/v1/management/protocol-adapters";

    @GET
    @Path("/types")
    @Operation(summary = "Obtain a list of available protocol adapter types",
               operationId = "getAdapterTypes",
               description = "Obtain a list of available protocol adapter types.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = ProtocolAdaptersList.class)))})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull
    Response getAdapterTypes();

    @GET
    @Path("/tagschemas/{protocolId}")
    @Operation(summary = "Obtain the JSON schema for a tag for a specific protocol adapter.",
               operationId = "getTagSchema",
               description = "Obtain the tag schema for a specific portocol adapter.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = TagSchema.class)))})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull
    Response getTagSchema(
            @NotNull @Parameter(name = "protocolId",
                                description = "The protocol id.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("protocolId") String protocolId);

    @POST
    @Path("/adapters/{adapterType: ([a-zA-Z_0-9\\-])*}")
    @Operation(summary = "Add a new Adapter",
               operationId = "addAdapter",
               description = "Add adapter to the system.",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Success")})
    @NotNull
    Response addAdapter(
            @NotNull @Parameter(name = "adapterType",
                                description = "The adapter type.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("adapterType") String adapterType,
            @NotNull @Parameter(name = "adapter",
                                description = "The new adapter.",
                                required = true,
                                in = ParameterIn.DEFAULT) Adapter adapter);

    @GET
    @Path("/adapters/")
    @Operation(summary = "Obtain a list of configured adapters",
               operationId = "getAdapters",
               description = "Obtain a list of configured adapters.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = AdaptersList.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example adapter list.",
                                                                              name = "adapter-list",
                                                                              value = ApiBodyExamples.EXAMPLE_ADAPTER_LIST)}))})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull
    Response getAdapters();


    @GET
    @Path("/types/{adapterType: ([a-zA-Z_0-9\\-])*}")
    @Operation(summary = "Obtain a list of configured adapters for the specified type",
               operationId = "getAdaptersForType",
               description = "Obtain a list of configured adapters for the specified type.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = AdaptersList.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example filtered adapter list.",
                                                                              name = "filtered-adapters",
                                                                              value = ApiBodyExamples.EXAMPLE_FILTERED_TYPE_ADAPTERS)}))})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull
    Response getAdaptersForType(
            @NotNull @Parameter(name = "adapterType",
                                description = "The adapter type.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("adapterType") String adapterType);


    @GET
    @Path("/adapters/{adapterId: ([a-zA-Z_0-9\\-])*}")
    @Operation(summary = "Obtain the details for a configured adapter for the specified type",
               operationId = "getAdapter",
               description = "Obtain the details for a configured adapter for the specified type\".",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = Adapter.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example adapter.",
                                                                              name = "adapter",
                                                                              value = ApiBodyExamples.EXAMPLE_ADAPTER)}))})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull
    Response getAdapter(
            @NotNull @Parameter(name = "adapterId",
                                description = "The adapter Id.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("adapterId") String adapterId);

    @PUT
    @Path("/adapters/{adapterId: ([a-zA-Z_0-9\\-])*}")
    @Operation(summary = "Update an adapter",
               operationId = "updateAdapter",
               description = "Update adapter configured in the system.",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Success")})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull
    Response updateAdapter(
            @NotNull @Parameter(name = "adapterId",
                                description = "The adapter Id.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("adapterId") String adapterId,
            @NotNull final Adapter adapter);

    @DELETE
    @Path("/adapters/{adapterId: ([a-zA-Z_0-9\\-])*}")
    @Operation(summary = "Delete an adapter",
               operationId = "deleteAdapter",
               description = "Delete adapter configured in the system.",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Success")})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull
    Response deleteAdapter(
            @NotNull @Parameter(name = "adapterId",
                                description = "The adapter Id.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("adapterId") String adapterId);


    @GET
    @Path("/adapters/{adapterId: ([a-zA-Z_0-9\\-])*}/discover")
    @Operation(summary = "Discover a list of available data points",
               operationId = "discoverDataPoints",
               description = "Obtain a list of available values accessible via this protocol adapter.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = ValuesTree.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example discovery request.",
                                                                              name = "discover",
                                                                              value = ApiBodyExamples.EXAMPLE_DISCOVERY)})),
                       @ApiResponse(responseCode = "400", description = "Protocol adapter does not support discovery")})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull
    Response discoverValues(
            @NotNull @Parameter(name = "adapterId",
                                description = "The adapter Id.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("adapterId") String adapterId,
            @Nullable @Parameter(name = "root",
                                 description = "The root to browse.",
                                 in = ParameterIn.QUERY) @QueryParam("root") String rootNode,
            @Nullable @Parameter(name = "depth",
                                 description = "The recursive depth to include. Must be larger than 0.",
                                 in = ParameterIn.QUERY) @QueryParam("depth") Integer depth);

    @GET
    @Path("/adapters/{adapterId: ([a-zA-Z_0-9\\-])*}/status")
    @Operation(summary = "Get the up to date status of an adapter",
               description = "Get the up to date status an adapter.",
               operationId = "get-adapter-status",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = Status.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example connection status.",
                                                                              name = "example-connection-status",
                                                                              summary = "Example connection status",
                                                                              value = ApiBodyExamples.EXAMPLE_CONNECTION_STATUS_JSON)}))})
    @NotNull
    Response getStatus(
            @Parameter(name = "adapterId",
                       description = "The name of the adapter to query.",
                       required = true,
                       in = ParameterIn.PATH) @PathParam("adapterId") @NotNull final String adapterId);

    @PUT
    @Path("/adapters/{adapterId: ([a-zA-Z_0-9\\-])*}/status")
    @Operation(summary = "Transition the runtime status of an adapter",
               description = "Transition the runtime status of an adapter.",
               operationId = "transition-adapter-status",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = StatusTransitionResult.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example response with PENDING status.",
                                                                              name = "transition-status-result",
                                                                              summary = "Adapter Connection Transition Result",
                                                                              value = ApiBodyExamples.EXAMPLE_STATUS_TRANSITION_RESULT)}))})
    @NotNull
    Response changeStatus(
            @Parameter(name = "adapterId",
                       description = "The id of the adapter whose runtime status will change.",
                       required = true,
                       in = ParameterIn.PATH) final @PathParam("adapterId") @NotNull String adapterId,
            @Parameter(name = "command",
                       description = "The command to transition the adapter runtime status.",
                       required = true,
                       in = ParameterIn.DEFAULT) final @NotNull StatusTransitionCommand command);

    @GET
    @Path("/status")
    @Operation(summary = "Get the status of all the adapters in the system.",
               description = "Obtain the details.",
               operationId = "get-adapters-status",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "The Connection Details Verification Result.",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = StatusList.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example connection status list.",
                                                                              name = "example-connection-status",
                                                                              summary = "Example connection status",
                                                                              value = ApiBodyExamples.EXAMPLE_CONNECTION_STATUS_LIST_JSON)}))})
    @NotNull
    Response status();


    @GET
    @Path("/adapters/{adapterId}/tags")
    @Operation(summary = "Get the domain tags for the device connected through this adapter.",
               operationId = "get-adapter-domainTags",
               description = "Get the domain tags for the device connected through this adapter.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = DomainTagModelList.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example for domain tags in opc ua",
                                                                              name = "opc ua domain tags example",
                                                                              summary = "Example for domain tags for opc ua ",
                                                                              value = TagResourceExamples.EXAMPLE_OPC_UA)}))})
    @Produces(APPLICATION_JSON)
    @NotNull
    Response getDomainTagsForAdapter(
            @NotNull @Parameter(name = "adapterId",
                                description = "The adapter id.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("adapterId") String adapterId);


    @POST
    @Path("/adapters/{adapterId}/tags")
    @Operation(summary = "Add a new domain tag to the specified adapter",
               operationId = "add-adapter-domainTags",
               description = "Add a new domain tag to the specified adapter.",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Success"),
                       @ApiResponse(responseCode = "403",
                                    description = "Already Present",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = Errors.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example response in case an tag is already present for this tagId.",
                                                                              name = "already present example",
                                                                              summary = "An example response in case an tag is already present for this tagId.",
                                                                              value = TagResourceExamples.EXAMPLE_ALREADY_PRESENT)}))}

    )
    @NotNull
    Response addAdapterDomainTag(
            @NotNull @Parameter(name = "adapterId",
                                description = "The adapter id.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("adapterId") String adapterId,
            @NotNull @Parameter(name = "domainTag",
                                description = "The domain tag.",
                                required = true,
                                in = ParameterIn.DEFAULT) DomainTagModel domainTag);


    @DELETE
    @Path("/adapters/{adapterId}/tags/{tagId}")
    @Operation(summary = "Delete an domain tag",
               operationId = "delete-adapter-domainTags",
               description = "Delete the specified domain tag on the given adapter.",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Success")})
    @Produces(APPLICATION_JSON)
    @NotNull
    Response deleteDomainTag(
            @NotNull @Parameter(name = "adapterId",
                                description = "The adapter Id.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("adapterId") String adapterId,
            @NotNull @Parameter(name = "tagId",
                                description = "The domain tag Id.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("tagId") String tagId);


    @PUT
    @Path("/adapters/{adapterId}/tags/{tagId}")
    @Operation(summary = "Update the domain tag of an adapter.",
               description = "Update the domain tag of an adapter.",
               operationId = "update-adapter-domainTag",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Success"),
                       @ApiResponse(responseCode = "403",
                                    description = "Not Found",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = Errors.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example response in case no tag is present for this tagId.",
                                                                              name = "already present example",
                                                                              summary = "An example response in case no tag is present for this tagId.",
                                                                              value = TagResourceExamples.EXAMPLE_NOT_PRESENT)}))})
    @NotNull
    Response updateDomainTag(
            @Parameter(name = "adapterId",
                       description = "The id of the adapter whose domain tag will be updated.",
                       required = true,
                       in = ParameterIn.PATH) final @PathParam("adapterId") @NotNull String adapterId,
            @NotNull @Parameter(name = "tagId",
                                description = "The id of the domain tag that will be changed.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("tagId") String tagId,
            final @NotNull DomainTagModel domainTag);


    @PUT
    @Path("/adapters/{adapterId}/tags/")
    @Operation(summary = "Update the domain tag of an adapter.",
               description = "Update all domain tags of an adapter.",
               operationId = "update-adapter-domainTags",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Success"),
                       @ApiResponse(responseCode = "403",
                                    description = "Not Found",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = Errors.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example response in case no tag is present for this tagId.",
                                                                              name = "already present example",
                                                                              summary = "An example response in case no tag is present for this tagId.",
                                                                              value = TagResourceExamples.EXAMPLE_NOT_PRESENT)}))})
    @NotNull
    Response updateDomainTags(
            @Parameter(name = "adapterId",
                       description = "The id of the adapter whose domain tags will be updated.",
                       required = true,
                       in = ParameterIn.PATH) final @PathParam("adapterId") @NotNull String adapterId,
            final @NotNull DomainTagModelList domainTagList);

    @GET
    @Path("/tags")
    @Operation(summary = "Get the list of all domain tags created in this Edge instance",
               operationId = "get-domain-tags",
               description = "Get the list of all domain tags created in this Edge instance",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = DomainTagModelList.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example for domain tags in opc ua",
                                                                              name = "opc ua domain tags example",
                                                                              summary = "Example for domain tags for opc ua ",
                                                                              value = TagResourceExamples.EXAMPLE_OPC_UA)}))})
    @Produces(APPLICATION_JSON)
    @NotNull
    Response getDomainTags();

    @GET
    @Path("/tags/{tagName}")
    @Operation(summary = "Get the domain tag with the given name in this Edge instance",
               operationId = "get-domain-tag",
               description = "Get a domain tag created in this Edge instance",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = DomainTagModel.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example for domain tags in opc ua",
                                                                              name = "opc ua domain tags example",
                                                                              summary = "Example for domain tags for opc ua ",
                                                                              value = TagResourceExamples.EXAMPLE_OPC_UA)}))})
    @Produces(APPLICATION_JSON)
    @NotNull
    Response getDomainTag(@NotNull @Parameter(name = "tagName",
                                              description = "The tag name (base64 encoded).",
                                              required = true,
                                              in = ParameterIn.PATH) @PathParam("tagName") String tagName);

    @GET
    @Path("/writing-schema/{adapterId}/{tagName}")
    @Operation(summary = "Get a json schema that explains the json schema that is used to write to a PLC for the given tag name.",
               operationId = "get-writing-schema",
               description = "Get a json schema that explains the json schema that is used to write to a PLC for the given tag name.\"",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = JsonNode.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example for domain tags in opc ua",
                                                                              name = "opc ua domain tags example",
                                                                              summary = "Example for domain tags for opc ua ",
                                                                              // TODO
                                                                              value = TagResourceExamples.EXAMPLE_OPC_UA)}))})
    @Produces(APPLICATION_JSON)
    @NotNull
    Response getWritingSchema(
            @NotNull @Parameter(name = "adapterId",
                                description = "The id of the adapter for which the Json Schema for writing to a PLC gets created.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("adapterId") String adapterId,
            @NotNull @Parameter(name = "tagName",
                                description = "The tag name (base64 encoded) for which the Json Schema for writing to a PLC gets created.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("tagName") String tagName);

}
