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
import com.hivemq.api.resources.impl.HealthCheckResourceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Simon L Johnson
 */
@Path("/api/v1/health")
@Produces(MediaType.APPLICATION_JSON)
public interface HealthCheckApi {

    @GET
    @Path("/liveness")
    @Operation(summary = "Endpoint to determine whether the gateway is considered UP",
               description = "Endpoint to determine whether the gateway is considered UP.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = HealthCheckResourceImpl.HealthStatus.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example success health response.",
                                                                              name = "success-health",
                                                                              value = ApiBodyExamples.EXAMPLE_HEALTH_LIVENESS)
                                                       }))})
    Response liveness();

    @GET
    @Path("/readiness")
    @Operation(summary = "Endpoint to determine whether the gateway is considered ready",
               description = "Endpoint to determine whether the gateway is considered ready.",
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = HealthCheckResourceImpl.HealthStatus.class),
                                                       examples = {
                                                               @ExampleObject(description = "An example success health response.",
                                                                              name = "success-health",
                                                                              value = ApiBodyExamples.EXAMPLE_HEALTH_READINESS)
                                                       }))})
    Response readiness();

}
