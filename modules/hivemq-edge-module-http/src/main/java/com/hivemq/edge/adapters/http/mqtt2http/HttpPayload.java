package com.hivemq.edge.adapters.http.mqtt2http;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import org.jetbrains.annotations.NotNull;

public class HttpPayload implements WritingPayload {

    @JsonProperty("value")
    private final @NotNull JsonNode value;

    public HttpPayload(
            final @NotNull @JsonProperty("value") JsonNode value) {
        this.value = value;
    }

    public @NotNull JsonNode getValue() {
        return value;
    }
}
