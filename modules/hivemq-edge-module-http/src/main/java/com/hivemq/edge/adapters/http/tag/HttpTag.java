package com.hivemq.edge.adapters.http.tag;

import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class HttpTag implements Tag<HttpTagAddress> {

    private final @NotNull String tagName;
    private final @NotNull HttpTagAddress httpTagAddress;

    public HttpTag(final @NotNull String tagName, final @NotNull HttpTagAddress httpTagAddress) {
        this.tagName = tagName;
        this.httpTagAddress = httpTagAddress;
    }


    @Override
    public @NotNull HttpTagAddress getTagAddress() {
        return httpTagAddress;
    }

    @Override
    public @NotNull String getTagName() {
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
        return tagName.equals(httpTag.tagName) && httpTagAddress.equals(httpTag.httpTagAddress);
    }

    @Override
    public int hashCode() {
        int result = tagName.hashCode();
        result = 31 * result + httpTagAddress.hashCode();
        return result;
    }
}
