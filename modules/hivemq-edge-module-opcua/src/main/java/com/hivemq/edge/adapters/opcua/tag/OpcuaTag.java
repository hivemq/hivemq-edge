package com.hivemq.edge.adapters.opcua.tag;

import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class OpcuaTag implements Tag<OpcuaTagAddress> {

    private final @NotNull String tagName;
    private final @NotNull OpcuaTagAddress opcuaTagAddress;

    public OpcuaTag(final @NotNull String tagName, final @NotNull OpcuaTagAddress opcuaTagAddress) {
        this.tagName = tagName;
        this.opcuaTagAddress = opcuaTagAddress;
    }

    @Override
    public @NotNull OpcuaTagAddress getTagAddress() {
        return opcuaTagAddress;
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

        final OpcuaTag httpTag = (OpcuaTag) o;
        return tagName.equals(httpTag.tagName) && opcuaTagAddress.equals(httpTag.opcuaTagAddress);
    }

    @Override
    public int hashCode() {
        int result = tagName.hashCode();
        result = 31 * result + opcuaTagAddress.hashCode();
        return result;
    }
}
