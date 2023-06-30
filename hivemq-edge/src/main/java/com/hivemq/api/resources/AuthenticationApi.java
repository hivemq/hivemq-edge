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
import com.hivemq.api.model.auth.ApiBearerToken;
import com.hivemq.api.model.auth.UsernamePasswordCredentials;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Simon L Johnson
 */

@Path(AuthenticationApi.PATH)
@Tag(name = "Authentication Endpoint",
     description = "Services to obtain and validate security tokens with the HiveMQ Edge API.")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AuthenticationApi {

    String PATH = "/api/v1/auth";

    @POST
    @Path("/authenticate")
    @Operation(summary = "Authorize the presented user to obtain a secure token for use on the API.",
               operationId = "authenticate",
               description = "Authorize the presented user to obtain a secure token for use on the API.",
               tags = {"Authentication"},
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Username & Password Credentials to Authenticate as.",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = ApiBearerToken.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example Authentication configuration.",
                                                                              name = "example-authentication",
                                                                              summary = "Example authentication",
                                                                              value = ApiBodyExamples.EXAMPLE_AUTHENTICATION_JSON)
                                    })),
                       @ApiResponse(responseCode = "401",
                                    description = "The requested credentials could not be authenticated.",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = ApiErrorMessage.class),
                                                       examples = @ExampleObject(description = "Unable to authenticate credentials",
                                                                                 name = "authentication-invalid",
                                                                                 summary = "The requested credentials could not be authenticated",
                                                                                 value = ApiBodyExamples.EXAMPLE_AUTHENTICATION_ERROR_JSON)))})
    Response authenticate(final @NotNull UsernamePasswordCredentials credentials);


    @POST
    @Path("/validate-token")
    @Operation(summary = "Authorize the presented user to obtain a secure token for use on the API.",
               operationId = "validate-token",
               description = "Authorize the presented user to obtain a secure token for use on the API.",
               tags = {"Authentication"},
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "The token was valid"),
                       @ApiResponse(responseCode = "401",
                                    description = "The token was invalid")})
    Response validate(final @NotNull ApiBearerToken token);

    @POST
    @Path("/refresh-token")
    @Operation(summary = "Obtain a fresh JWT for the previously authenticated user.",
               operationId = "refresh-token",
               description = "Authorize the presented user to obtain a secure token for use on the API.",
               tags = {"Authentication"},
               responses = {
                       @ApiResponse(responseCode = "200",
                                    description = "Obtain a new JWT from a previously authentication token.",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = ApiBearerToken.class),
                                                       examples = {
                                                               @ExampleObject(description = "Example Authentication configuration.",
                                                                              name = "example-authentication",
                                                                              summary = "Example authentication",
                                                                              value = ApiBodyExamples.EXAMPLE_AUTHENTICATION_JSON)
                                                       })),
                       @ApiResponse(responseCode = "401",
                                    description = "The requested credentials could not be authenticated.",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                                       schema = @Schema(implementation = ApiErrorMessage.class),
                                                       examples = @ExampleObject(description = "Unable to authenticate credentials",
                                                                                 name = "authentication-invalid",
                                                                                 summary = "The requested credentials could not be authenticated",
                                                                                 value = ApiBodyExamples.EXAMPLE_AUTHENTICATION_ERROR_JSON)))})
    Response reissueToken();

}
