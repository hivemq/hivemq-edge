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
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public class ProblemDetails {

    @JsonProperty(value = "type")
    private final @Nullable String type;

    @JsonProperty(value = "title", required = true)
    private final @NotNull String title;

    @JsonProperty("detail")
    private final @Nullable String detail;

    @JsonProperty("status")
    private final int status;

    @JsonProperty("code")
    @Schema(description = "Correlation id")
    private final @Nullable String code;

    @JsonProperty("errors")
    private final @NotNull List<Error> errors;

    @JsonCreator
    public ProblemDetails(
            @JsonProperty(value = "type") final @Nullable String type,
            @JsonProperty(value = "title", required = true) final @NotNull String title,
            @JsonProperty("detail") final @Nullable String detail,
            @JsonProperty("status") final int status,
            @JsonProperty("errors") final @NotNull List<Error> errors) {
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.status = status;
        this.errors = errors;
        this.code = null;
    }

    public @NotNull List<Error> getErrors() {
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

    public @Nullable String getCode() {
        return code;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ProblemDetails that = (ProblemDetails) o;
        return status == that.status &&
                Objects.equals(type, that.type) &&
                Objects.equals(title, that.title) &&
                Objects.equals(detail, that.detail) &&
                Objects.equals(code, that.code) &&
                Objects.equals(errors, that.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, title, detail, status, code, errors);
    }

    @Override
    public String toString() {
        return "ProblemDetails{" +
                "type='" +
                type +
                '\'' +
                ", title='" +
                title +
                '\'' +
                ", detail='" +
                detail +
                '\'' +
                ", status=" +
                status +
                ", code='" +
                code +
                '\'' +
                ", errors=" +
                errors +
                '}';
    }
}
