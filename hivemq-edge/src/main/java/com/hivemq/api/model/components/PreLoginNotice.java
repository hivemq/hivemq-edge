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
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNullElse;

public class PreLoginNotice {

    @JsonProperty("enabled")
    @Schema(description = "Whether the pre login notice should be shown prior to login in")
    private final @NotNull Boolean enabled;

    @JsonProperty("title")
    @Schema(description = "The title of the notice")
    private final @NotNull String title;

    @JsonProperty("message")
    @Schema(description = "The message of the notice")
    private final @NotNull String message;

    @JsonProperty("consent")
    @Schema(description = "The message of the notice")
    private final @Nullable String consent;

    public PreLoginNotice() {
        this(false, null, null, null);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PreLoginNotice(
            final @Nullable Boolean enabled,
            final @Nullable String title,
            final @Nullable String message,
            final @Nullable String consent) {
        this.enabled = requireNonNullElse(enabled, false);
        this.title = requireNonNullElse(title, "");
        this.message = requireNonNullElse(message, "");
        this.consent = consent;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @NotNull String getTitle() {
        return title;
    }

    public @NotNull String getMessage() {
        return message;
    }

    public @Nullable String getConsent() {
        return consent;
    }
}
