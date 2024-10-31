package com.hivemq.edge.adapters.http.tag;

import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class HttpTag implements Tag<HttpTagDefinition> {

    private final @NotNull String tagName;
    private final @NotNull HttpTagDefinition httpTagDefinition;

    public HttpTag(final @NotNull String tagName, final @NotNull HttpTagDefinition httpTagDefinition) {
        this.tagName = tagName;
        this.httpTagDefinition = httpTagDefinition;
    }


    @Override
    public @NotNull HttpTagDefinition getDefinition() {
        return httpTagDefinition;
    }

    @Override
    public @NotNull String getName() {
        return tagName;
    }

    @Override
    public boolean equals(@NotNull final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HttpTag httpTag = (HttpTag) o;
        return tagName.equals(httpTag.tagName) && httpTagDefinition.equals(httpTag.httpTagDefinition);
    }

    @Override
    public int hashCode() {
        int result = tagName.hashCode();
        result = 31 * result + httpTagDefinition.hashCode();
        return result;
    }
}
