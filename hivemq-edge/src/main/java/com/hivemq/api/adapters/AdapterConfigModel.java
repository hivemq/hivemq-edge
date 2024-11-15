package com.hivemq.api.adapters;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.adapters.Adapter;
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

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AdapterConfigModel(
            @JsonProperty("config") final @NotNull Adapter adapter,
            @JsonProperty("tags") final @NotNull List<DomainTagModel> domainTagModels) {
        this.adapter = adapter;
        this.domainTagModels = domainTagModels;
    }

    public @NotNull Adapter getAdapter() {
        return adapter;
    }

    public @NotNull List<DomainTagModel> getDomainTagModels() {
        return domainTagModels;
    }

    @Override
    public String toString() {
        return "FullAdapter{" + "adapter" +
                "=" + adapter + ", domainTagModels=" + domainTagModels + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AdapterConfigModel that = (AdapterConfigModel) o;
        return Objects.equals(adapter, that.adapter) && Objects.equals(domainTagModels, that.domainTagModels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adapter, domainTagModels);
    }
}
