package com.hivemq.api.model.uns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.ItemsResponse;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

public class NamespaceProfilesList extends ItemsResponse<NamespaceProfileBean> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public NamespaceProfilesList(
            @JsonProperty("items") final @NotNull List<@NotNull NamespaceProfileBean> items) {
        super(items);
    }
}
