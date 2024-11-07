package com.hivemq.api.model.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.ItemsResponse;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

public class FieldMappingListModel extends ItemsResponse<FieldMappingsModel> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public FieldMappingListModel(
            @JsonProperty("items") final @NotNull List<FieldMappingsModel> items) {
        super(items);
    }
}
