package com.hivemq.edge.adapters.etherip.config.tag;

import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class EipTag implements Tag<EipTagDefinition> {


    private final @NotNull String tagName;
    private final @NotNull String description;
    private final @NotNull EipTagDefinition eipTagDefinition;

    public EipTag(final @NotNull String tagName, final @NotNull String description, final @NotNull EipTagDefinition eipTagDefinition) {
        this.tagName = tagName;
        this.description = description;
        this.eipTagDefinition = eipTagDefinition;
    }


    @Override
    public @NotNull EipTagDefinition getTagDefinition() {
        return eipTagDefinition;
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    public @NotNull String getDescription() {
        return description;
    }

    @Override
    public boolean equals(@NotNull final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EipTag eipTag = (EipTag) o;
        return tagName.equals(eipTag.tagName) && eipTagDefinition.equals(eipTag.eipTagDefinition);
    }

    @Override
    public int hashCode() {
        int result = tagName.hashCode();
        result = 31 * result + eipTagDefinition.hashCode();
        return result;
    }
}
