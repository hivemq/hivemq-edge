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
package com.hivemq.api.model.components;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.ItemsResponse;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

/**
 * Bean to transport listener details across the API
 *
 * @author Simon L Johnson
 */
public class ListenerList extends ItemsResponse<Listener> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ListenerList(
            @JsonProperty("items") final @NotNull List<@NotNull Listener> listeners) {
        super(listeners);
    }
}
