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

import com.hivemq.api.model.topicFilters.TopicFilterModel;
import com.hivemq.api.model.topicFilters.TopicFilterModelList;
import com.hivemq.api.resources.examples.TopicFiltersResourceExamples;
import org.jetbrains.annotations.NotNull;
import com.hivemq.http.error.ProblemDetails;
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

import static com.hivemq.api.resources.TopicFilterApi.PATH;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


@Path(PATH)
@Tag(name = "Topic Filters", description = "Interact with topic filters.")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public interface TopicFilterApi {

    String PATH = "/api/v1/management/topic-filters";

    @POST
    @Operation(summary = "Add a new topic filter",
               operationId = "add-topicFilters",
               description = "Add a new topic filter.",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Success"),
                       @ApiResponse(responseCode = "500",
                                    description = "Internal Server Error",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = ProblemDetails.class))),
                       @ApiResponse(responseCode = "403",
                                    description = "Already Present",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = ProblemDetails.class)))}

    )
    @NotNull
    Response addTopicFilter(
            @NotNull @Parameter(name = "topicFilter",
                                description = "The topic filter.",
                                required = true,
                                in = ParameterIn.DEFAULT) TopicFilterModel topicFilterModel);


    @GET
    @Operation(summary = "Get the list of all topic filters created in this Edge instance",
               operationId = "get-topicFilters",
               description = "Get the list of all topic filters created in this Edge instance",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = TopicFilterModelList.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example for the topic filter list",
                                                                              name = "An example for the topic filter list",
                                                                              summary = "An example for the topic filter list",
                                                                              value = TopicFiltersResourceExamples.EXAMPLE_TOPIC_FILTER_LIST)}))})
    @Produces(APPLICATION_JSON)
    @NotNull
    Response getTopicFilters();


    @DELETE
    @Path("/{filter}")
    @Operation(summary = "Delete an topic filter",
               operationId = "delete-topicFilter",
               description = "Delete the specified topic filter.",
               responses = {@ApiResponse(responseCode = "200", description = "Success"),

                            @ApiResponse(responseCode = "404",
                                         description = "Topic filter not found",
                                         content = @Content(mediaType = APPLICATION_JSON,
                                                            schema = @Schema(implementation = ProblemDetails.class))),
                            @ApiResponse(responseCode = "403",
                                         description = "Already Present",
                                         content = @Content(mediaType = APPLICATION_JSON,
                                                            schema = @Schema(implementation = ProblemDetails.class)))
               })
    @Produces(APPLICATION_JSON)
    @NotNull
    Response deleteTopicFilter(
            @NotNull @Parameter(name = "filter",
                                description = "The URL-encoded filter of the topic filter that should be deleted.",
                                required = true,
                                schema = @Schema(format = "urlencoded"),
                                in = ParameterIn.PATH) @PathParam("filter") String filter);


    @PUT
    @Path("/{filter}")
    @Operation(summary = "Update a topic filter.",
               description = "Update a topic filter",
               operationId = "update-topicFilter",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Success"),

                       @ApiResponse(responseCode = "400",
                                    description = "Topic filter failed validation",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = ProblemDetails.class))),
                       @ApiResponse(responseCode = "500",
                                    description = "Internal Server Error",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = ProblemDetails.class)))
               })
    @NotNull
    Response updateTopicFilter(
            @NotNull @Parameter(name = "filter",
                                description = "The URL-encoded filter of the topic filter that will be updated.",
                                required = true,
                                schema = @Schema(format = "urlencoded"),
                                in = ParameterIn.PATH) @PathParam("filter") String filter,
            final @NotNull TopicFilterModel topicFilterModel);


    @PUT
    @Operation(summary = "Update all topic filters.",
               description = "Update all topic filters",
               operationId = "update-topicFilters",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Success"),

                       @ApiResponse(responseCode = "500",
                                    description = "Internal Server Error",
                                    content = @Content(mediaType = APPLICATION_JSON,
                                                       schema = @Schema(implementation = ProblemDetails.class)))
               })
    @NotNull
    Response updateTopicFilters(
            final @NotNull TopicFilterModelList topicFilterModelList);

}
