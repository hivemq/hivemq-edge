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
package com.hivemq.edge.compiler.source.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A physical data point on a device. Identified by a protocol-specific {@code id} string.
 *
 * <pre>{@code
 * id: "ns=2;i=1003"
 * dataType: Float
 * description: Nozzle pressure in bar
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class SourceDeviceTag {

    public @Nullable String id;
    public @Nullable String dataType;
    public @Nullable String description;

    /** All additional protocol-specific fields not explicitly declared above. */
    public @NotNull Map<String, Object> extra = new HashMap<>();

    @JsonAnySetter
    public void setExtra(final @NotNull String key, final @Nullable Object value) {
        extra.put(key, value);
    }
}
