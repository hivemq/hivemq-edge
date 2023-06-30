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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Mapper that handles StrongTyped ApiErrors and will respond with either the
 * standard API Error Bean OR the Supplied Response included in the Exception.
 *
 * @author Simon L Johnson
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionMapper.class);

    @Override
    public Response toResponse(@NotNull final ApiException exception) {
        logger.warn("Api Error Handled Api Exception Mapper {}", exception.getMessage(), exception.getCause());
        String message = exception.getMessage();
        ApiErrorMessage apiError = new ApiErrorMessage();
        apiError.setTitle(message);
        apiError.setFieldName(exception.getFieldName());
        return Response.status(exception.getHttpStatusCode())
                .entity(apiError)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
