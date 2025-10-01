/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.api.errors.pulse;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ProblemDetails;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class ManagedAssetNotFoundError extends ProblemDetails {
    public ManagedAssetNotFoundError(final @NotNull UUID id) {
        this(id.toString());
    }

    public ManagedAssetNotFoundError(final @NotNull String id) {
        super("ManagedAssetNotFoundError",
                "Managed Asset Not Found",
                "The managed asset '" + id + "' is not found.",
                HttpStatus.NOT_FOUND_404,
                List.of());
    }
}
