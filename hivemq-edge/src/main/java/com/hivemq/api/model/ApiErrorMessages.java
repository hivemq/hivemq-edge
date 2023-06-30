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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ApiErrorMessages {

    @JsonProperty("errors")
    private final @NotNull List<@NotNull ApiErrorMessage> errors = new ArrayList<>();

    public ApiErrorMessages(final @NotNull ApiErrorMessage... errors) {
        if(errors != null && errors.length > 0){
            this.errors.addAll(Lists.newArrayList(errors));
        }
    }

    @JsonCreator
    public ApiErrorMessages(@JsonProperty("errors") final List<@NotNull ApiErrorMessage> errors) {
        if(errors != null && errors.size() > 0){
            this.errors.addAll(errors);
        }
    }

    public @NotNull List<@NotNull ApiErrorMessage> getErrors() {
        return errors;
    }

    @JsonIgnore
    public ApiErrorMessages addError(final @NotNull ApiErrorMessage error){
        this.errors.add(error);
        return this;
    }
}
