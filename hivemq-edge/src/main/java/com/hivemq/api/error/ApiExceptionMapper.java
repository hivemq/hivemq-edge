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
package com.hivemq.api.error;

import com.hivemq.api.model.ApiErrorMessage;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapper that handles StrongTyped ApiErrors and will respond with either the
 * standard API Error Bean OR the Supplied Response included in the Exception.
 *
 * @author Simon L Johnson
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {
    public static final @NotNull String APPLICATION_PROBLEM_JSON_CHARSET_UTF_8 =
            "application/problem+json;charset=utf-8";
    public static final @NotNull MediaType APPLICATION_PROBLEM_JSON_TYPE =
            new MediaType("application", "problem+json", "utf-8");
    private static final @NotNull Logger logger = LoggerFactory.getLogger(ApiExceptionMapper.class);

    @Override
    public @NotNull Response toResponse(final @NotNull ApiException exception) {
        logger.warn("Api Error Handled Api Exception Mapper {}", exception.getMessage(), exception.getCause());
        final String message = exception.getMessage();
        final ApiErrorMessage apiError = new ApiErrorMessage();
        apiError.setTitle(message);
        apiError.setFieldName(exception.getFieldName());
        return Response.status(exception.getHttpStatusCode())
                .entity(apiError)
                .type(APPLICATION_PROBLEM_JSON_TYPE)
                .build();
    }
}
