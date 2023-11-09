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
import com.hivemq.api.model.events.EventList;
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(EventApi.PATH)
@Tag(name = "Events", description = "Interact with the system event sub-system.")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public interface EventApi {

    String PATH = "/api/v1/management/events";

    @GET
    @Path("")
    @Operation(summary = "List most recent events in the system",
               operationId = "getEvents",
               description = "Get all bridges configured in the system.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = EventList.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example response with several events.",
                                                                              name = "event-list-result",
                                                                              summary = "Event List result",
                                                                              value = ApiBodyExamples.EXAMPLE_EVENT_LIST)
                                                       }))})
    Response listEvents(   @Parameter(name = "limit",
                                    description = "Obtain all events since the specified epoch.",
                                    in = ParameterIn.QUERY) @QueryParam("limit") @DefaultValue("100") Integer limit,
                            @Parameter(name = "since",
                                  description = "Obtain all events since the specified epoch.",
                                  in = ParameterIn.QUERY) @QueryParam("since") Long timestamp);

}
