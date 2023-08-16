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
import com.hivemq.api.model.ApiErrorMessage;
import com.hivemq.api.model.components.ListenerList;
import com.hivemq.api.model.components.NotificationList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Simon L Johnson
 */

@Path(GatewayApi.PATH)
@Tag(name = "Gateway Endpoint",
     description = "Services to interact with the gateway configuration.")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public interface GatewayApi {

    String PATH = "/api/v1/gateway";


    @GET
    @Path("/configuration")
    @Operation(summary = "Obtain HiveMQ Edge Configuration",
               operationId = "get-xml-configuration",
               description = "Obtain gateway configuration.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_XML,
                                                       schema = @Schema(implementation = String.class))),
                       @ApiResponse(responseCode = "405",
                                    description = "Error - function not supported",
                                    content = @Content(mediaType = MediaType.APPLICATION_XML,
                                                       schema = @Schema(implementation = ApiErrorMessage.class),
                                                       examples = {
                                                               @ExampleObject(description = "Export is not allowed from gateway",
                                                                              name = "export-not-allowed",
                                                                              summary = "Example export disabled",
                                                                              value = ApiBodyExamples.EXAMPLE_XML_EXPORT_ERROR_JSON)
                                                       }))})
    @Produces(MediaType.APPLICATION_XML)
    @NotNull Response getXmlConfiguration();

    @GET
    @Path("/listeners")
    @Operation(summary = "Obtain the listeners configured ",
               operationId = "get-listeners",
               description = "Obtain listener.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = ListenerList.class),
                                                       examples = {
                                                               @ExampleObject(description = "A list of listeners configured in the gateway",
                                                                              name = "listener-configuration",
                                                                              summary = "Listener configuration",
                                                                              value = ApiBodyExamples.EXAMPLE_LISTENER_LIST_JSON)
                                                       }))})
    @Produces(MediaType.APPLICATION_JSON)
    @NotNull Response getListeners();

}
