/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.persistence.domain;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class DomainTagPersistenceImpl implements DomainTagPersistence {

    private final @NotNull HashMap<String, Set<DomainTag>> adapterToDomainTag = new HashMap<>();
    private final @NotNull ArrayList<String> alreadyUsedTags = new ArrayList<>();

    @Inject
    public DomainTagPersistenceImpl() {
    }

    @Override
    public synchronized @NotNull DomainTagAddResult addDomainTag(
            final @NotNull String adapterId, final @NotNull DomainTag domainTag) {
        if (alreadyUsedTags.contains(domainTag.getTag())) {
            return DomainTagAddResult.failed(DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS,
                    "An identical DomainTag exists already for adapter '" + adapterId + "'");
        }

        adapterToDomainTag.compute(adapterId, (key, currentValue) -> {
            if (currentValue == null) {
                final Set<DomainTag> domainTags = new HashSet<>();
                domainTags.add(domainTag);
                alreadyUsedTags.add(domainTag.getTag());
                return domainTags;
            } else {
                currentValue.add(domainTag);
                alreadyUsedTags.add(domainTag.getTag());
                return currentValue;
            }
        });
        return DomainTagAddResult.success();
    }

    @Override
    public synchronized @NotNull DomainTagUpdateResult updateDomainTag(
            @NotNull final String adapterId, @NotNull final String tagId, @NotNull final DomainTag domainTag) {
        final Set<DomainTag> domainTags = adapterToDomainTag.get(adapterId);
        if (domainTags == null || domainTags.isEmpty()) {
            return DomainTagUpdateResult.failed(DomainTagUpdateResult.DomainTagUpdateStatus.NOT_FOUND,
                    "No adapter with id '{}' was found.");
        }

        final boolean removed = domainTags.removeIf(domainTag1 -> domainTag1.getTag().equals(tagId));
        if (!removed) {
            return DomainTagUpdateResult.failed(DomainTagUpdateResult.DomainTagUpdateStatus.NOT_FOUND,
                    "No tag with id '" + tagId + "' was found for adapter '" + adapterId + "'.");
        } else {
            domainTags.add(domainTag);
            return DomainTagUpdateResult.success();
        }
    }

    @Override
    public @NotNull DomainTagUpdateResult updateDomainTags(
            @NotNull final String adapterId, @NotNull final Set<DomainTag> domainTags) {

        // first step check that the tagIds are not used by any other adapter but only on this adapter:
        final Set<DomainTag> existingDomainTags = adapterToDomainTag.get(adapterId);

        if (existingDomainTags == null) {
            return DomainTagUpdateResult.failed(DomainTagUpdateResult.DomainTagUpdateStatus.NOT_FOUND,
                    "No adapter with id '{}' was found.");
        }

        final Set<String> alreadyExistingIdsForTheAdapter =
                existingDomainTags.stream().map(DomainTag::getTag).collect(Collectors.toSet());

        for (final DomainTag domainTag : domainTags) {
            if (alreadyExistingIdsForTheAdapter.contains(domainTag.getTag())) {
                // adapter already has the tag and updating is ok
                continue;
            }

            if (alreadyUsedTags.contains(domainTag.getTag())) {
                // this is a problem: Another adapter has a tag with the same name. This is not allowed and we must stop here.
                return DomainTagUpdateResult.failed(DomainTagUpdateResult.DomainTagUpdateStatus.ALREADY_USED_BY_ANOTHER_ADAPTER,
                        domainTag.getTag());
            }
        }

        // we need to remove all tag names that are not used anymore and add tag names that are used now and were not before
        alreadyUsedTags.removeAll(alreadyExistingIdsForTheAdapter);
        for (final DomainTag domainTag : domainTags) {
            alreadyUsedTags.add(domainTag.getTag());
        }

        adapterToDomainTag.put(adapterId, domainTags);

        return DomainTagUpdateResult.success();
    }

    @Override
    public synchronized @NotNull DomainTagDeleteResult deleteDomainTag(
            @NotNull final String adapterId, @NotNull final String tagId) {
        final Set<DomainTag> domainTags = adapterToDomainTag.get(adapterId);
        if (domainTags == null || domainTags.isEmpty()) {
            return DomainTagDeleteResult.failed(DomainTagDeleteResult.DomainTagDeleteStatus.NOT_FOUND,
                    "No adapter with id '{}' was found with DomainTags");
        }

        final boolean removed = domainTags.removeIf(domainTag1 -> domainTag1.getTag().equals(tagId));
        if (!removed) {
            return DomainTagDeleteResult.failed(DomainTagDeleteResult.DomainTagDeleteStatus.NOT_FOUND,
                    "No tag with id '{}' was found.");
        } else {
            alreadyUsedTags.remove(tagId);
            return DomainTagDeleteResult.success();
        }
    }

    @Override
    public synchronized @NotNull List<DomainTag> getDomainTags() {
        return adapterToDomainTag.values().stream().flatMap(Set::stream).collect(Collectors.toList());
    }

    @Override
    public synchronized @NotNull List<DomainTag> getTagsForAdapter(@NotNull final String adapterId) {
        final Set<DomainTag> domainTags = adapterToDomainTag.get(adapterId);
        if (domainTags != null) {
            return ImmutableList.copyOf(domainTags);
        } else {
            return List.of();
        }
    }
}
