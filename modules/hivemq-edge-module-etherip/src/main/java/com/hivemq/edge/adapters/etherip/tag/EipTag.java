package com.hivemq.edge.adapters.etherip.tag;

import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class EipTag implements Tag<EipAddress> {

    private final @NotNull String tagName;
    private final @NotNull EipAddress eipAddress;

    public EipTag(final @NotNull String tagName, final @NotNull EipAddress eipAddress) {
        this.tagName = tagName;
        this.eipAddress = eipAddress;
    }


    @Override
    public @NotNull EipAddress getTagAddress() {
        return eipAddress;
    }

    @Override
    public @NotNull String getTagName() {
        return "";
    }
}
