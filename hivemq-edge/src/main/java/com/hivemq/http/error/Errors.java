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
package com.hivemq.http.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;

public class Errors {

    public static final URI GENERIC_BAD_REQUEST = URI.create("/error/generic-bad-request");

    @JsonProperty("type")
    private final @NotNull URI type;

    @JsonProperty("title")
    private final @NotNull String title;

    @JsonProperty("detail")
    private final @Nullable String detail;

    @JsonProperty("status")
    private final int status;

    @JsonProperty("errors")
    private final @NotNull List<@NotNull Error> errors;

    @JsonCreator
    public Errors(
            @JsonProperty("type") final @NotNull URI type,
            @JsonProperty("title") final @NotNull String title,
            @JsonProperty("detail") final @Nullable String detail,
            @JsonProperty("status") final int status,
            @JsonProperty("errors") final @NotNull List<Error> errors) {
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.status = status;
        this.errors = errors;
    }

    public @NotNull List<Error> getErrors() {
        return errors;
    }

    public @NotNull URI getType() {
        return type;
    }

    public @NotNull String getTitle() {
        return title;
    }

    public @Nullable String getDetail() {
        return detail;
    }

    public int getStatus() {
        return status;
    }
}
