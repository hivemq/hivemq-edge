package com.hivemq.api.resources;


import com.hivemq.api.model.TagResourceExamples;
import com.hivemq.api.model.adapters.Adapter;
import com.hivemq.api.model.tags.DomainTagList;
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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(TagApi.PATH)
@Tag(name = "Tag Endpoint", description = "Services to manage domain tags.")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface TagApi {

    String PATH = "/api/v1/management/protocol-adapters/adapters";

    @GET
    @Path("/{adapterId}/tags")
    @Operation(summary = "Get the domain tags for the device connected through this adapter.",
               operationId = "get-adapter-domainTags",
               description = "Get the domain tags for the device connected through this adapter.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = DomainTagList.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example for domain tags in opc ua",
                                                                              name = "opc ua domain tags example",
                                                                              summary = "Example for domain tags for opc ua ",
                                                                              value = TagResourceExamples.EXAMPLE_OPC_UA)}))})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull
    Response getDomainTags(
            @NotNull @Parameter(name = "adapterId",
                                description = "The adapter id.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("adapterId") String adapterType);





    @POST
    @Path("/adapters/{adapterId}")
    @Operation(summary = "Add a new domain tag to the specified adapter",
               operationId = "add-adapter-domainTags",
               description = "Add a new domain tag to the specified adapter.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success")})
    Response addAdapter(
            @NotNull @Parameter(name = "adapterId",
                                description = "The adapter type.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("adapterType") String adapterType,
            @NotNull @Parameter(name = "adapter",
                                description = "The new adapter.",
                                required = true,
                                in = ParameterIn.DEFAULT) Adapter adapter);


}
