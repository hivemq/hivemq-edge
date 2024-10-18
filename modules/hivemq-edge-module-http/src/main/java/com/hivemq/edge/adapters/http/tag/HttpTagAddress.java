package com.hivemq.edge.adapters.http.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpTagAddress {

    @JsonProperty(value = "url", required = true)
    @ModuleConfigField(title = "URL",
                       description = "The url of the HTTP request you would like to make",
                       format = ModuleConfigField.FieldType.URI,
                       required = true)
    private final @NotNull String url;

    @JsonCreator
    public HttpTagAddress(@JsonProperty("url") final @NotNull String url) {
        this.url = url;
    }

    public @NotNull String getUrl() {
        return url;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HttpTagAddress that = (HttpTagAddress) o;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
