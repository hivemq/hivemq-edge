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
package com.hivemq.api.model.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import org.jetbrains.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon L Johnson
 */
public class PayloadImpl implements Payload {

    @JsonProperty("contentType")
    @Schema(description = "The content type of the payload that the event contains",
            required = true)
    private @NotNull ContentType contentType;

    @JsonProperty("content")
    @Schema(description = "The content of the payload encoded as a string")
    private @NotNull String content;

    public PayloadImpl(@JsonProperty("contentType") final @NotNull ContentType contentType,
                       @JsonProperty("content") final @NotNull String content) {
        this.contentType = contentType;
        this.content = content;
    }

    @Override
    public @NotNull ContentType getContentType() {
        return contentType;
    }

    @Override
    public @NotNull String getContent() {
        return content;
    }

    public static Payload from(final @NotNull Payload.ContentType contentType, final @NotNull String data) {
        Preconditions.checkNotNull(contentType);
        Preconditions.checkNotNull(data);
        return new PayloadImpl(contentType, data);
    }

    public static Payload fromObject(final @NotNull ObjectMapper mapper, final @NotNull Object data) {
        try {
            Preconditions.checkNotNull(mapper);
            Preconditions.checkNotNull(data);
            return new PayloadImpl(PayloadImpl.ContentType.JSON, mapper.writeValueAsString(data));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

