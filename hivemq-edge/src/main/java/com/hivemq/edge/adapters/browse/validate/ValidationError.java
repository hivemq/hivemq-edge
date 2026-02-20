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
package com.hivemq.edge.adapters.browse.validate;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single validation error found during bulk import validation.
 *
 * @param row     1-indexed row number (matches Excel), or null for file-level errors
 * @param column  column name (matches CSV header names), or null for row/file-level errors
 * @param value   the offending value, or null for structural errors
 * @param code    machine-readable error code (e.g. {@code INVALID_TAG_NAME})
 * @param message human-readable description of the problem
 */
public record ValidationError(
        @JsonProperty("row") @Nullable Integer row,
        @JsonProperty("column") @Nullable String column,
        @JsonProperty("value") @Nullable String value,
        @JsonProperty("code") @NotNull String code,
        @JsonProperty("message") @NotNull String message) {}
