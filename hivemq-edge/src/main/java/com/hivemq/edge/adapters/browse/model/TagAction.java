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
package com.hivemq.edge.adapters.browse.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

/**
 * Records the action taken on a single tag during import.
 *
 * @param name   the tag name
 * @param action the action that was performed
 */
public record TagAction(
        @JsonProperty("name") @NotNull String name,
        @JsonProperty("action") @NotNull Action action) {

    public enum Action {
        CREATED,
        UPDATED,
        DELETED
    }
}
