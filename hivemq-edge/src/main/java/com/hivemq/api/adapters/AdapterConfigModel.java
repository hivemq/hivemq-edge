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
import com.hivemq.api.model.mappings.northbound.NorthboundMappingModel;
import com.hivemq.api.model.mappings.southbound.SouthboundMappingModel;
import com.hivemq.api.model.tags.DomainTagModel;
import org.jetbrains.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@Schema(name = "AdapterConfig")
public class AdapterConfigModel {

    @JsonProperty("config")
    @Schema(name = "config",
            description = "The adapter configuration")
    private final @NotNull Adapter adapter;

    @JsonProperty("tags")
    @Schema(name = "tags",
            description = "The tags defined for this adapter")
    private final @NotNull List<DomainTagModel> domainTagModels;

    @JsonProperty("southboundMappings")
    @Schema(name = "southboundMappings",
            description = "The southbound mappings for this adapter")
    private final @NotNull List<SouthboundMappingModel> southboundMappingModels;

    @JsonProperty("northboundMappings")
    @Schema(name = "northboundMappings",
            description = "The northbound mappings for this adapter")
    private final @NotNull List<NorthboundMappingModel> northboundMappingModels;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AdapterConfigModel(
            @JsonProperty("config") final @NotNull Adapter adapter,
            @JsonProperty("tags") final @NotNull List<DomainTagModel> domainTagModels,
            @JsonProperty("southboundMappings") final @NotNull List<SouthboundMappingModel> southboundMappingModels,
            @JsonProperty("northboundMappings") final @NotNull List<NorthboundMappingModel> northboundMappingModels
    ) {
        this.adapter = adapter;
        this.domainTagModels = domainTagModels;
        this.southboundMappingModels = Objects.requireNonNullElse(southboundMappingModels, List.of());
        this.northboundMappingModels = Objects.requireNonNullElse(northboundMappingModels, List.of());
    }

    public @NotNull Adapter getAdapter() {
        return adapter;
    }

    public @NotNull List<DomainTagModel> getDomainTagModels() {
        return domainTagModels;
    }

    public @NotNull List<SouthboundMappingModel> getSouthboundMappingModels() {
        return southboundMappingModels;
    }

    public @NotNull List<NorthboundMappingModel> getNorthboundMappingModels() {
        return northboundMappingModels;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AdapterConfigModel that = (AdapterConfigModel) o;
        return Objects.equals(adapter, that.adapter) &&
                Objects.equals(domainTagModels, that.domainTagModels) &&
                Objects.equals(southboundMappingModels, that.southboundMappingModels) &&
                Objects.equals(northboundMappingModels, that.northboundMappingModels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adapter, domainTagModels, southboundMappingModels, northboundMappingModels);
    }

    @Override
    public String toString() {
        return "AdapterConfigModel{" +
                "adapter=" +
                adapter +
                ", domainTagModels=" +
                domainTagModels +
                ", southboundMappingModels=" + southboundMappingModels +
                ", northboundMappingModels=" + northboundMappingModels +
                '}';
    }
}
