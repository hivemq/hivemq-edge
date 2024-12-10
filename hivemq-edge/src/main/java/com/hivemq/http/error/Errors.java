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
import java.util.Objects;

public abstract class Errors<T extends Error> {

    @JsonProperty("type")
    private final @NotNull String type;

    @JsonProperty("title")
    private final @NotNull String title;

    @JsonProperty("detail")
    private final @Nullable String detail;

    @JsonProperty("status")
    private final int status;

    @JsonProperty("errors")
    private final @NotNull List<T> errors;

    @JsonCreator
    public Errors(
            @JsonProperty("type") final @NotNull String type,
            @JsonProperty("title") final @NotNull String title,
            @JsonProperty("detail") final @Nullable String detail,
            @JsonProperty("status") final int status,
            @JsonProperty("errors") final @NotNull List<T> errors) {
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.status = status;
        this.errors = errors;
    }

    public @NotNull List<T> getErrors() {
        return errors;
    }

    public @NotNull URI getType() {
        return URI.create("http://nowhere/" + type);
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Errors errors1 = (Errors) o;
        return status == errors1.status &&
                Objects.equals(type, errors1.type) &&
                Objects.equals(title, errors1.title) &&
                Objects.equals(detail, errors1.detail) &&
                Objects.equals(errors, errors1.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, title, detail, status, errors);
    }

    @Override
    public String toString() {
        return "Errors{" +
                "type=" +
                type +
                ", title='" +
                title +
                '\'' +
                ", detail='" +
                detail +
                '\'' +
                ", status=" +
                status +
                ", errors=" +
                errors +
                '}';
    }
}
