package com.hivemq.api.model.capabilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.ItemsResponse;
import com.hivemq.api.model.client.Client;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class CapabilityList extends ItemsResponse<Capability> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CapabilityList(
            @JsonProperty("items") final @NotNull List<@NotNull Capability> items) {
        super(items);
    }

}
