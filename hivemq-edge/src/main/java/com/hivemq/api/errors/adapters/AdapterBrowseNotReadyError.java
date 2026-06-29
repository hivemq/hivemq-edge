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
package com.hivemq.api.errors.adapters;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ProblemDetails;
import java.util.List;

/**
 * The adapter is connected but its device address-space metadata is still being loaded, so a browse would be
 * non-deterministic. Returned as {@code 503 Service Unavailable} with a {@code Retry-After} hint so the caller
 * retries once the adapter is browse-ready (EDG-577).
 */
public class AdapterBrowseNotReadyError extends ProblemDetails {
    public AdapterBrowseNotReadyError(final String adapterId) {
        super(
                "AdapterBrowseNotReady",
                "Adapter not ready for browsing",
                "Adapter '" + adapterId
                        + "' is connected but still loading its device metadata; retry after the indicated delay.",
                HttpStatus.SERVICE_UNAVAILABLE_503,
                List.of());
    }
}
