/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.file.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.edge.adapters.file.config.ContentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class FilePayload {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("timestamp")
    private final @Nullable Long timestamp;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final @Nullable List<MqttUserProperty> userProperties;

    @JsonProperty("value")
    private final @NotNull Object value;

    @JsonProperty("contentType")
    private final @NotNull String contentType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("tagName")
    private final @Nullable String tagName;

    public FilePayload(
            final @NotNull List<MqttUserProperty> userProperties,
            final @NotNull Object value,
            final @NotNull ContentType contentType,
            final @Nullable String tagName,
            final @Nullable Long timestamp) {
        this.timestamp = timestamp;
        this.userProperties = userProperties;
        this.value = value;
        this.tagName = tagName;
        this.contentType = contentType.getMimeTypeRepresentation();
    }
}
