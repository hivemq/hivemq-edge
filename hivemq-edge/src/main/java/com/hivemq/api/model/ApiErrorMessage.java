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
package com.hivemq.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bean to pass error messages back through the API in a meaningful way.
 *
 * @author Simon L Johnson
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorMessage {

    @JsonProperty("title")
    @Schema(description = "The title of this error")
    private String title;

    @JsonProperty("detail")
    @Schema(description = "Detailed contextual description of this error")
    private String detail;

    @JsonProperty("fieldName")
    @Schema(description = "Application Error Code associate with this field")
    private String fieldName;

    public ApiErrorMessage() {
    }

    public ApiErrorMessage(final @JsonProperty("fieldName") @NotNull String fieldName,
                           final @JsonProperty("title") @NotNull String title,
                           final @JsonProperty("detail") @NotNull String detail) {
        this.fieldName = fieldName;
        this.title = title;
        this.detail = detail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(@NotNull final String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(@NotNull final String detail) {
        this.detail = detail;
    }

    public void setFieldName(@NotNull final String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public static ApiErrorMessage from(@NotNull final String message){
        Preconditions.checkNotNull(message);
        return new ApiErrorMessage(null, message, null);
    }

}
