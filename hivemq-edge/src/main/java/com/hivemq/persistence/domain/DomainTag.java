package com.hivemq.persistence.domain;

import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.api.model.tags.TagAddress;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;

@Immutable
public class DomainTag {

    private final @NotNull TagAddress tagAddress;
    private final @NotNull String tag;

    public DomainTag(
            final @NotNull TagAddress tagAddress, final @NotNull String tag) {
        this.tagAddress = tagAddress;
        this.tag = tag;
    }

    public static DomainTag fromDomainTagEntity(final @NotNull DomainTagModel domainTag) {
        return new DomainTag(domainTag.getTagAddress(), domainTag.getTag());
    }

    public @NotNull String getTag() {
        return tag;
    }

    public @NotNull TagAddress getTagAddress() {
        return tagAddress;
    }
}
