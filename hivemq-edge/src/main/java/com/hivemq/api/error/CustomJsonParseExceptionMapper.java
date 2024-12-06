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

import com.fasterxml.jackson.core.JsonParseException;
import com.hivemq.http.error.Error;
import org.jetbrains.annotations.NotNull;
import com.hivemq.util.ErrorResponseUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.List;

import static com.hivemq.http.HttpConstants.ERROR_TYPE_UNABLE_TO_PARS_JSON;

public class CustomJsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    @Override
    public @NotNull Response toResponse(final JsonParseException exception) {
        final String originalMessage = exception.getOriginalMessage();
        if (originalMessage != null) {
            return ErrorResponseUtil.validationErrors(ERROR_TYPE_UNABLE_TO_PARS_JSON, List.of(new Error(originalMessage, null, null, null)));
        } else {
            return ErrorResponseUtil.invalidInput("Unable to parse JSON body, please check the input format.");
        }
    }
}
