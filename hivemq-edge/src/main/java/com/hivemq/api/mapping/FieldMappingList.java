package com.hivemq.api.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.ItemsResponse;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

public class FieldMappingList extends ItemsResponse<FieldMappings> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public FieldMappingList(
            @JsonProperty("items") final @NotNull List<FieldMappings> items) {
        super(items);
    }
}
