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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.hivemq.api.errors.InvalidInputError;
import com.hivemq.api.errors.ValidationError;
import com.hivemq.http.error.Error;
import com.hivemq.util.ErrorResponseUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Priority;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.List;

@Priority(1)
public class CustomJsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {

    @Override
    public @NotNull Response toResponse(final @NotNull JsonMappingException exception) {
        final String originalMessage = exception.getOriginalMessage();
        if (originalMessage != null) {
            return ErrorResponseUtil.errorResponse(new ValidationError(List.of(new Error(originalMessage, null))));
        } else {
            return ErrorResponseUtil.errorResponse(new InvalidInputError("Unable to parse JSON body, please check the input format."));
        }
    }
}
