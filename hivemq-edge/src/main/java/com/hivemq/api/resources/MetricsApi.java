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
import com.hivemq.api.model.metrics.DataPoint;
import com.hivemq.api.model.metrics.MetricList;
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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Simon L Johnson
 */

@Path(MetricsApi.PATH)
@Tag(name = "Metrics Endpoint",
     description = "Gain insight and system metrics.")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public interface MetricsApi {

    String PATH = "/api/v1/metrics";

    @GET
    @Path("/")
    @Operation(summary = "Obtain a list of available metrics",
               operationId = "getMetrics",
               description = "Obtain the latest sample for the metric requested.",
               tags = {"Metrics"},
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = MetricList.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example response with metrics listed.",
                                                                              name = "metrics-list-sample",
                                                                              summary = "List Metrics",
                                                                              value = ApiBodyExamples.EXAMPLE_METRIC_LIST_JSON)
                                    }))})
    Response getMetrics();


    @GET
    @Path("/{metricName}/latest")
    @Operation(summary = "Obtain the latest sample for the metric requested",
               operationId = "getSample",
               description = "Obtain the latest sample for the metric requested.",
               tags = {"Metrics"},
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Success",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = DataPoint.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example response with metrics listed.",
                                                                              name = "metric-sample",
                                                                              summary = "Metric Sample",
                                                                              value = ApiBodyExamples.EXAMPLE_DATAPOINT_JSON)
                                                       }))})
    Response getSample(final @NotNull @Parameter(name = "metricName",
                                                 description = "The metric to search for.",
                                                 required = true,
                                                 in = ParameterIn.PATH)
                       @PathParam("metricName") String prefix);

}
