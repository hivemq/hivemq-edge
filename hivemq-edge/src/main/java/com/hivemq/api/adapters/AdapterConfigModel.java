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
import com.hivemq.api.model.mappings.frommapping.FromEdgeMappingModel;
import com.hivemq.api.model.mappings.tomapping.ToEdgeMappingModel;
import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

public class AdapterConfigModel {

    @JsonProperty("config")
    @Schema(name = "config",
            description = "The adapter configuration")
    private final @NotNull Adapter adapter;

    @JsonProperty("tags")
    @Schema(name = "tags",
            description = "The tags defined for this adapter")
    private final @NotNull List<DomainTagModel> domainTagModels;

    @JsonProperty("toEdgeMappings")
    @Schema(name = "toEdgeMappings",
            description = "The toEdge mappings for thid adapter")
    private final @NotNull List<ToEdgeMappingModel> toEdgeMappingModels;

    @JsonProperty("fromEdgeMappings")
    @Schema(name = "fromEdgeMappings",
            description = "The fromEdge mappings for thid adapter")
    private final @NotNull List<FromEdgeMappingModel> fromEdgeMappingModels;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AdapterConfigModel(
            @JsonProperty("config") final @NotNull Adapter adapter,
            @JsonProperty("tags") final @NotNull List<DomainTagModel> domainTagModels,
            @JsonProperty("toEdgeMappings") final @NotNull List<ToEdgeMappingModel> toEdgeMappingModels,
            @JsonProperty("fromEdgeMappings") final @NotNull List<FromEdgeMappingModel> fromEdgeMappingModels
    ) {
        this.adapter = adapter;
        this.domainTagModels = domainTagModels;
        this.toEdgeMappingModels = Objects.requireNonNullElse(toEdgeMappingModels, List.of());
        this.fromEdgeMappingModels = Objects.requireNonNullElse(fromEdgeMappingModels, List.of());
    }

    public @NotNull Adapter getAdapter() {
        return adapter;
    }

    public @NotNull List<DomainTagModel> getDomainTagModels() {
        return domainTagModels;
    }

    public @NotNull List<ToEdgeMappingModel> getToEdgeMappingModels() {
        return toEdgeMappingModels;
    }

    public @NotNull List<FromEdgeMappingModel> getFromEdgeMappingModels() {
        return fromEdgeMappingModels;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AdapterConfigModel that = (AdapterConfigModel) o;
        return Objects.equals(adapter, that.adapter) &&
                Objects.equals(domainTagModels, that.domainTagModels) &&
                Objects.equals(toEdgeMappingModels, that.toEdgeMappingModels) &&
                Objects.equals(fromEdgeMappingModels, that.fromEdgeMappingModels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adapter, domainTagModels, toEdgeMappingModels, fromEdgeMappingModels);
    }

    @Override
    public String toString() {
        return "AdapterConfigModel{" +
                "adapter=" +
                adapter +
                ", domainTagModels=" +
                domainTagModels +
                ", toEdgeMappingModels=" +
                toEdgeMappingModels +
                ", fromEdgeMappingModels=" +
                fromEdgeMappingModels +
                '}';
    }
}
