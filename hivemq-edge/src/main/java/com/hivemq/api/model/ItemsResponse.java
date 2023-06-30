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
package com.hivemq.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public abstract class ItemsResponse<T> {

    @JsonProperty("items")
    @Schema(description = "List of result items that are returned by this endpoint")
    private final @NotNull List<@NotNull T> items;

    protected ItemsResponse(final @NotNull List<@NotNull T> items) {
        this.items = items;
    }

    public @NotNull List<@NotNull T> getItems() {
        return items;
    }
}
