package com.hivemq.api.model.tags;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.ItemsResponse;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

public class TagSchemaList extends ItemsResponse<TagSchema> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TagSchemaList(
            @JsonProperty("items") final @NotNull List<TagSchema> items) {
        super(items);
    }
}
