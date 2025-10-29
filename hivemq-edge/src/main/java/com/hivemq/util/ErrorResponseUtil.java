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
package com.hivemq.util;

import com.hivemq.edge.api.model.ApiProblemDetails;
import com.hivemq.http.HttpConstants;
import com.hivemq.http.error.ProblemDetails;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;

/**
 * @author Christoph Schäbel
 */
public class ErrorResponseUtil {
    public static @NotNull Response errorResponse(final @NotNull ApiProblemDetails error) {
        return Response.status(error.getStatus())
                .entity(error)
                .type(HttpConstants.APPLICATION_PROBLEM_JSON_TYPE)
                .build();
    }

    public static @NotNull Response errorResponse(final @NotNull ProblemDetails errors) {
        return Response.status(errors.getStatus())
                .entity(errors)
                .type(HttpConstants.APPLICATION_PROBLEM_JSON_TYPE)
                .build();
    }
}
