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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bean to transport Link details across the API
 * @author Simon L Johnson
 */
public class Notification {

    public enum LEVEL {
        NOTICE, WARNING, ERROR
    }

    @JsonProperty("level")
    @Schema(description = "The notification level")
    private final @NotNull LEVEL level;

    @JsonProperty("title")
    @Schema(description = "The notification title")
    private final @NotNull String title;

    @JsonProperty("description")
    @Schema(description = "The notification description", nullable = true)
    private final @Nullable String description;

    @JsonProperty("link")
    @Schema(description = "An associated link", nullable = true)
    private final @Nullable Link link;


    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Notification(
            @NotNull @JsonProperty("level") final LEVEL level,
            @NotNull @JsonProperty("title") final String title,
            @NotNull @JsonProperty("description") final String description,
            @Nullable @JsonProperty("link") final Link link) {
        this.level = level;
        this.title = title;
        this.description = description;
        this.link = link;
    }

    public LEVEL getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Link getLink() {
        return link;
    }
}
