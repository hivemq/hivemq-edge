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
import com.hivemq.api.model.uns.ISA95ApiBean;
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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(UnsApi.PATH)
@Tag(name = "UNS", description = "Configure Unified Namespace.")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public interface UnsApi {

    String PATH = "/api/v1/management/uns";

    @GET
    @Path("/isa95")
    @Operation(summary = "Obtain isa95 config",
               operationId = "get-isa95",
               description = "Obtain isa95 config.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = ISA95ApiBean.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example ISA 95 config.",
                                                                              name = "default-configuration",
                                                                              summary = "Example configuration",
                                                                              value = ApiBodyExamples.EXAMPLE_ISA_95_JSON)
                                                       }))})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull Response getIsa95();


    @POST
    @Path("/isa95")
    @Operation(summary = "Set isa95 config",
               operationId = "set-isa95",
               description = "Set isa95 config.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success")})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull Response setIsa95(@Parameter(name = "isa95",
                                          description = "The updated isa95 configuration.",
                                          required = true,
                                          in = ParameterIn.DEFAULT)
                                   final @NotNull ISA95ApiBean isa95);

}
