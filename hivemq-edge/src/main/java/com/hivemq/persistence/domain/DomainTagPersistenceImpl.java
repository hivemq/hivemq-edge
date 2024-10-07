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
        final DomainTagAddResult[] result = new DomainTagAddResult[1];

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



    //TODO as tagId is now edge-wide unique, the logic needs to be checked
    @Override
    public synchronized @NotNull DomainTagUpdateResult updateDomainTag(
            @NotNull final String adapterId, @NotNull final String tagId, @NotNull final DomainTag domainTag) {
        final Set<DomainTag> domainTags = adapterToDomainTag.get(adapterId);
        if (domainTags == null || domainTags.isEmpty()) {
            return DomainTagUpdateResult.failed(DomainTagUpdateResult.DomainTagUpdateStatus.NOT_FOUND,
                    "No adapter with id '{}' was found with DomainTags");
        }

        final boolean removed = domainTags.removeIf(domainTag1 -> domainTag1.getTag().equals(tagId));
        if (!removed) {
            return DomainTagUpdateResult.failed(DomainTagUpdateResult.DomainTagUpdateStatus.NOT_FOUND,
                    "No tag with id '{}' was found.");
        } else {
            domainTags.add(domainTag);
            return DomainTagUpdateResult.success();
        }
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
