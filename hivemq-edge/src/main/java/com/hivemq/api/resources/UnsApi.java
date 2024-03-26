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
import com.hivemq.api.model.uns.NamespaceProfileBean;
import com.hivemq.api.model.uns.NamespaceProfilesList;
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
    @Path("/profiles")
    @Operation(summary = "Obtain UNS profiles",
               operationId = "get-profiles",
               description = "Obtain available profiles of UNS.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = NamespaceProfilesList.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example UNS profiles config.",
                                                                              name = "default-profiles",
                                                                              summary = "Example configuration",
                                                                              value = ApiBodyExamples.EXAMPLE_UNS_PROFILES_JSON)
                                                       }))})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull Response getProfiles();


    @POST
    @Path("/profile")
    @Operation(summary = "Set active UNS profile",
               operationId = "set-active-profile",
               description = "Set active UNS profile.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success")})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull Response setActiveProfile(@Parameter(name = "namespace",
                                            description = "The updated namespace configuration.",
                                            required = true,
                                            in = ParameterIn.DEFAULT)
                                     final @NotNull NamespaceProfileBean bean) ;
}
