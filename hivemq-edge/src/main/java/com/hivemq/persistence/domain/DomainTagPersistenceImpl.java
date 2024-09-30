package com.hivemq.persistence.domain;

import com.google.common.collect.ImmutableList;
import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
public class DomainTagPersistenceImpl implements DomainTagPersistence {

    private final @NotNull ConcurrentHashMap<String, Set<DomainTag>> adapterToDomainTag = new ConcurrentHashMap<>();

    @Override
    public @NotNull DomainTagAddResult addDomainTag(
            final @NotNull String adapterId, final @NotNull DomainTag domainTag) {


        final DomainTagAddResult[] result = new DomainTagAddResult[1];
        adapterToDomainTag.compute(adapterId, (key, currentValue) -> {
            if (currentValue == null) {
                final Set<DomainTag> domainTags = new HashSet<>();
                domainTags.add(domainTag);
                result[0] = DomainTagAddResult.success();
                return domainTags;
            } else {
                if (currentValue.add(domainTag)) {
                    result[0] = DomainTagAddResult.success();
                } else {
                    result[0] = DomainTagAddResult.failed(DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS,
                            "An identical DomainTag exists already for adapter '" + adapterId + "'");
                }
                return currentValue;
            }
        });
        return result[0];
    }

    @Override
    public @NotNull DomainTagUpdateResult updateDomainTag(
            @NotNull final String adapterId, @NotNull final String tagId, @NotNull final DomainTagModel domainTag) {
        final Set<DomainTag> domainTags = adapterToDomainTag.get(adapterId);
        if (domainTags == null || domainTags.isEmpty()) {
            return DomainTagUpdateResult.failed(DomainTagUpdateResult.DomainTagUpdateStatus.NOT_FOUND,
                    "No adapter with id '{}' was found with DomainTags");
        }

        // TODO tagId needs to cleared up


        return null;
    }

    @Override
    public @NotNull DomainTagDeleteResult deleteDomainTag(
            @NotNull final String adapterId, @NotNull final String tagId) {
        final Set<DomainTag> domainTags = adapterToDomainTag.get(adapterId);
        if (domainTags == null || domainTags.isEmpty()) {
            return DomainTagDeleteResult.failed(DomainTagDeleteResult.DomainTagDeleteStatus.NOT_FOUND,
                    "No adapter with id '{}' was found with DomainTags");
        }

        // TODO tagId needs to cleared up


        return null;
    }

    @Override
    public @NotNull List<DomainTag> getDomainTags() {
        return adapterToDomainTag.values().stream().flatMap(Set::stream).collect(Collectors.toList());
    }

    @Override
    public @NotNull List<DomainTag> getTagsForAdapter(@NotNull final String adapterId) {
        return ImmutableList.copyOf(adapterToDomainTag.get(adapterId));
    }
}
