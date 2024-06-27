package com.hivemq.edge.adapters.file.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.config.UserProperty;
import com.hivemq.edge.adapters.file.config.ContentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class FilePayload {

    @JsonProperty("timestamp")
    private final @Nullable Long timestamp;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final @Nullable List<UserProperty> userProperties;

    @JsonProperty("value")
    private final @NotNull Object value;

    @JsonProperty("tagName")
    private final @Nullable String tagName;

    @JsonProperty("contentType")
    private final @Nullable ContentType contentType;

    public FilePayload(
            final @Nullable Long timestamp,
            final @Nullable List<UserProperty> userProperties,
            final @NotNull Object value,
            final @Nullable String tagName,
            final @Nullable ContentType contentType) {
        this.timestamp = timestamp;
        this.userProperties = userProperties;
        this.value = value;
        this.tagName = tagName;
        this.contentType = contentType;
    }
}
