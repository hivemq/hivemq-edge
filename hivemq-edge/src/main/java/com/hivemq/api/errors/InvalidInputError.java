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
package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InvalidInputError extends ProblemDetails {
    public InvalidInputError(
            final @Nullable String error) {
        super(
                "InvalidInputError",
                "Invalid input",
                "JSON failed validation.",
                HttpStatus.BAD_REQUEST_400,
                List.of(new Error("Unparseable JSON: " + error, null)));
    }
}
