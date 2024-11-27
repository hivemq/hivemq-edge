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
package com.hivemq.api.adapters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.adapters.Adapter;
import com.hivemq.api.model.fieldmapping.FieldMappingsModel;
import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class AdapterConfigModel {

    @JsonProperty("config")
    @Schema(name = "config",
            description = "The adapter configuration")
    private final @NotNull Adapter adapter;

    @JsonProperty("tags")
    @Schema(name = "tags",
            description = "The tags defined for this adapter")
    private final @NotNull List<DomainTagModel> domainTagModels;

    @JsonProperty("fieldMappings")
    @Schema(name = "fieldMappings", description = "The fieldMappings for this adapter")
    private final @NotNull List<FieldMappingsModel> fieldMappings;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AdapterConfigModel(
            @JsonProperty("config") final @NotNull Adapter adapter,
            @JsonProperty("tags") final @NotNull List<DomainTagModel> domainTagModels,
            @JsonProperty("fieldMappings") final @NotNull List<FieldMappingsModel> fieldMappings) {
        this.adapter = adapter;
        this.domainTagModels = domainTagModels;
        this.fieldMappings = fieldMappings;
    }

    public @NotNull Adapter getAdapter() {
        return adapter;
    }

    public @NotNull List<DomainTagModel> getDomainTagModels() {
        return domainTagModels;
    }

    public @NotNull List<FieldMappingsModel> getFieldMappings() {
        return fieldMappings;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final AdapterConfigModel that = (AdapterConfigModel) o;
        return adapter.equals(that.adapter) &&
                domainTagModels.equals(that.domainTagModels) &&
                fieldMappings.equals(that.fieldMappings);
    }

    @Override
    public int hashCode() {
        int result = adapter.hashCode();
        result = 31 * result + domainTagModels.hashCode();
        result = 31 * result + fieldMappings.hashCode();
        return result;
    }

    @Override
    public @NotNull String toString() {
        return "AdapterConfigModel{" +
                "adapter=" +
                adapter +
                ", domainTagModels=" +
                domainTagModels +
                ", fieldMappings=" +
                fieldMappings +
                '}';
    }
}
