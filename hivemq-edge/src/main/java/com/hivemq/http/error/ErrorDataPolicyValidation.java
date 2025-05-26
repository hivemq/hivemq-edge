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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ErrorDataPolicyValidation extends Error {
    private static final String REQUIRED_FIELD_MISSING = "Required field missing";
    @JsonProperty("path")
    @Schema(description = "The path of the issue")
    protected @Nullable String path;
    @JsonProperty("rule")
    @Schema(description = "The rule of the issue")
    protected @Nullable ErrorDataPolicyValidationRule rule;
    @JsonProperty("title")
    @Schema(description = "The title of the issue")
    protected @Nullable String title;

    public ErrorDataPolicyValidation(final @NotNull String detail, final @Nullable String parameter) {
        super(detail, parameter);
    }

    public ErrorDataPolicyValidation(final @NotNull String detail) {
        super(detail);
    }

    public static @NotNull ErrorDataPolicyValidation missingField(final @NotNull String field) {
        return new ErrorDataPolicyValidation(String.format("Required field '%s' is missing", field), field).setTitle(
                REQUIRED_FIELD_MISSING).setRule(ErrorDataPolicyValidationRule.MissingField);
    }

    public @Nullable String getPath() {
        return path;
    }

    public @NotNull ErrorDataPolicyValidation setPath(@Nullable final String path) {
        this.path = path;
        return this;
    }

    public @Nullable ErrorDataPolicyValidationRule getRule() {
        return rule;
    }

    public @NotNull ErrorDataPolicyValidation setRule(@Nullable final ErrorDataPolicyValidationRule rule) {
        this.rule = rule;
        return this;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public @NotNull ErrorDataPolicyValidation setTitle(@Nullable final String title) {
        this.title = title;
        return this;
    }

    public enum ErrorDataPolicyValidationRule {
        MissingField("data-policy-validation-missing-field"),
        ;

        private final @NotNull String displayName;

        ErrorDataPolicyValidationRule(@NotNull final String displayName) {
            this.displayName = displayName;
        }

        @JsonValue
        public @NotNull String getDisplayName() {
            return displayName;
        }
    }
}
