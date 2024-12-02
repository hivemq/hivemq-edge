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
import com.hivemq.api.model.samples.PayloadSampleList;
import org.jetbrains.annotations.NotNull;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path(SamplingApi.PATH)
@Tag(name = "Payload Sampling", description = "Manage samples of payloads.")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public interface SamplingApi {

    String PATH = "/api/v1/management/sampling";

    @GET
    @Path("/topic/{topic}")
    @Operation(summary = "Obtain a list of samples that their gathered for the given topic.",
               operationId = "getSamplesForTopic",
               description = "Obtain a list of samples that their gathered for the given topic.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = PayloadSampleList.class)))})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull
    Response getSamplesForTopic(
            @NotNull @Parameter(name = "topic",
                                description = "The topic.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("topic") String topic);

    @GET
    @Path("/schema/{topic}")
    @Operation(summary = "Obtain a JsonSchema based in the stored samples for a given topic.",
               operationId = "getSchemaForTopic",
               description = "Obtain a JsonSchema based in the stored samples for a given topic.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = JsonNode.class)))})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull
    Response getSchemaForTopic(
            @NotNull @Parameter(name = "topic",
                                description = "The topic.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("topic") String topic);


    @POST
    @Path("/topic/{topic}")
    @Operation(summary = "Start sampling for the given topic.",
               operationId = "startSamplingForTopic",
               description = "Start sampling for the given topic.",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Success")})
    @NotNull
    Response startSamplingForTopic(
            @NotNull @Parameter(name = "topic",
                                description = "The topic.",
                                required = true,
                                in = ParameterIn.PATH) @PathParam("topic") String topic);
}
