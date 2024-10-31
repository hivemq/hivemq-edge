package com.hivemq.edge.adapters.opcua.config.tag;

import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class OpcuaTag implements Tag<OpcuaTagDefinition> {

    private final @NotNull String tagName;
    private final @NotNull OpcuaTagDefinition opcuaTagDefinition;

    public OpcuaTag(final @NotNull String tagName, final @NotNull OpcuaTagDefinition opcuaTagDefinition) {
        this.tagName = tagName;
        this.opcuaTagDefinition = opcuaTagDefinition;
    }

    @Override
    public @NotNull OpcuaTagDefinition getDefinition() {
        return opcuaTagDefinition;
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

        final OpcuaTag httpTag = (OpcuaTag) o;
        return tagName.equals(httpTag.tagName) && opcuaTagDefinition.equals(httpTag.opcuaTagDefinition);
    }

    @Override
    public int hashCode() {
        int result = tagName.hashCode();
        result = 31 * result + opcuaTagDefinition.hashCode();
        return result;
    }
}
