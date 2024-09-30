package com.hivemq.api.resources;


import com.hivemq.api.model.ApiBodyExamples;
import com.hivemq.api.model.TagResourceExamples;
import com.hivemq.api.model.status.StatusTransitionResult;
import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.api.model.tags.DomainTagModelList;
import com.hivemq.api.model.tags.TagSchemaList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path(DomainTagApi.PATH)
@Tag(name = "Tag Endpoint", description = "Services to manage domain tags.")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public interface DomainTagApi {

    String PATH = "/api/v1/management";

    @GET
    @Path("/protocol-adapters/adapters/{adapterId}/tags")
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
    @Path("/protocol-adapters/adapters/adapters/{adapterId}")
    @Operation(summary = "Add a new domain tag to the specified adapter",
               operationId = "add-adapter-domainTags",
               description = "Add a new domain tag to the specified adapter.",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Success")})
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
    @Path("/protocol-adapters/adapters/adapters/{adapterId}/tags/{tagId}")
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
    @Path("/protocol-adapters/adapters/adapters/{adapterId}/tags/{tagId}")
    @Operation(summary = "Update the domain tag of an adapter.",
               description = "Update the domain tag of an adapter.",
               operationId = "update-adapter-domainTags",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = StatusTransitionResult.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example response with PENDING status.",
                                                                              name = "transition-status-result",
                                                                              summary = "Adapter Connection Transition Result",
                                                                              value = ApiBodyExamples.EXAMPLE_STATUS_TRANSITION_RESULT)}))})
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


    @GET
    @Path("/domain/tags")
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



    // TODO not part of this ticket as it does not include schemas
    /**

    @GET
    @Path("/domain/tags/schema")
    @Operation(summary = "Get the data schema associated with the specified tags",
               operationId = "get-tag-schemas",
               description = "Get the data schema associated with the specified tags",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = TagSchemaList.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example for domain tags in opc ua",
                                                                              name = "opc ua domain tags example",
                                                                              summary = "Example for domain tags for opc ua ",
                                                                              value = TagResourceExamples.EXAMPLE_OPC_UA)}))})
    @Produces(APPLICATION_JSON)
    @NotNull
    Response getTagSchema();


    **/

}
