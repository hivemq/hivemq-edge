package com.hivemq.persistence.domain;

import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

public interface DomainTagPersistence {


    @NotNull
    DomainTagAddResult addDomainTag(@NotNull String adapterId, @NotNull DomainTag domainTag);

    @NotNull
    DomainTagUpdateResult updateDomainTag(
            @NotNull String adapterId,
            @NotNull String tagId,
            @NotNull DomainTagModel domainTag);


    @NotNull
    DomainTagDeleteResult deleteDomainTag(@NotNull String adapterId, @NotNull String tagId);



    @NotNull
    List<DomainTag> getDomainTags();


    @NotNull
    List<DomainTag> getTagsForAdapter(@NotNull String adapterId);
}
