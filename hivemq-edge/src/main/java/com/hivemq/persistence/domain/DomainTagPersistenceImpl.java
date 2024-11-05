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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.exceptions.TagNotFoundException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class DomainTagPersistenceImpl implements DomainTagPersistence {

    private static final Logger log = LoggerFactory.getLogger(DomainTagPersistenceImpl.class);

    private final @NotNull HashMap<String, Set<DomainTag>> adapterToDomainTag = new HashMap<>();
    private final @NotNull HashMap<String, DomainTag> tagNameToDomainTag = new HashMap<>();
//    private final @NotNull Map<String, ProtocolAdapterInformation> allAvailableAdapterTypes;
    private final @NotNull ObjectMapper mapper;

    @Inject
    public DomainTagPersistenceImpl() {
        this.mapper = new ObjectMapper();
    }

    @Override
    public synchronized @NotNull DomainTagAddResult addDomainTag(
            final @NotNull DomainTag domainTag) {

//        final ProtocolAdapterConfig protocolAdapterConfig = mapper.convertValue(domainTag.getDefinition(),
//                allAvailableAdapterTypes.get(domainTag.getProtocolId()).configurationClassReading());



        final String adapterId = domainTag.getAdapterId();
        if (tagNameToDomainTag.containsKey(domainTag.getTagName())) {
            return DomainTagAddResult.failed(DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS,
                    "An identical DomainTag exists already for adapter '" + adapterId + "'");
        }

        tagNameToDomainTag.put(domainTag.getTagName(), domainTag);
        adapterToDomainTag.compute(adapterId, (key, currentValue) -> {
            if (currentValue == null) {
                final Set<DomainTag> domainTags = new HashSet<>();
                domainTags.add(domainTag);
                return domainTags;
            } else {
                currentValue.add(domainTag);
                return currentValue;
            }
        });
        return DomainTagAddResult.success();
    }

    @Override
    public synchronized @NotNull DomainTagUpdateResult updateDomainTag(
            @NotNull final String tagId, @NotNull final DomainTag domainTag) {
        final String adapterId = domainTag.getAdapterId();
        final Set<DomainTag> domainTags = adapterToDomainTag.get(adapterId);
        if (domainTags == null || domainTags.isEmpty()) {
            return DomainTagUpdateResult.failed(DomainTagUpdateResult.DomainTagUpdateStatus.ADAPTER_NOT_FOUND,
                    "No adapter with id '{}' was found.");
        }

        final boolean removed = domainTags.removeIf(domainTag1 -> domainTag1.getTagName().equals(tagId));
        if (!removed) {
            return DomainTagUpdateResult.failed(DomainTagUpdateResult.DomainTagUpdateStatus.ADAPTER_NOT_FOUND,
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
            return DomainTagUpdateResult.failed(DomainTagUpdateResult.DomainTagUpdateStatus.ADAPTER_NOT_FOUND,
                    "No adapter with id '{}' was found.");
        }

        final Set<String> alreadyExistingIdsForTheAdapter =
                existingDomainTags.stream().map(DomainTag::getTagName).collect(Collectors.toSet());

        for (final DomainTag domainTag : domainTags) {
            if (alreadyExistingIdsForTheAdapter.contains(domainTag.getTagName())) {
                // adapter already has the tag and updating is ok
                continue;
            }

            if (tagNameToDomainTag.containsKey(domainTag.getTagName())) {
                // this is a problem: Another adapter has a tag with the same name. This is not allowed and we must stop here.
                return DomainTagUpdateResult.failed(DomainTagUpdateResult.DomainTagUpdateStatus.ALREADY_USED_BY_ANOTHER_ADAPTER,
                        domainTag.getTagName());
            }
        }

        // we need to remove all tag names that are not used anymore and add tag names that are used now and were not before
        for (final String tagName : alreadyExistingIdsForTheAdapter) {
            tagNameToDomainTag.remove(tagName);
        }

        for (final DomainTag domainTag : domainTags) {
            tagNameToDomainTag.put(domainTag.getTagName(), domainTag);
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
                    "No adapter with name '{}' was found with DomainTags");
        }

        final boolean removed = domainTags.removeIf(domainTag1 -> domainTag1.getTagName().equals(tagId));
        if (!removed) {
            return DomainTagDeleteResult.failed(DomainTagDeleteResult.DomainTagDeleteStatus.NOT_FOUND,
                    "No tag with name '{}' was found.");
        } else {
            tagNameToDomainTag.remove(tagId);
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

    @Override
    public @NotNull DomainTag getTag(@NotNull final String tagId) {
        final DomainTag domainTag = tagNameToDomainTag.get(tagId);
        if (domainTag == null) {
            throw new TagNotFoundException("Tag'" + tagId + "' was not found in the persistence.");
        }
        return domainTag;
    }

    @Override
    public synchronized void adapterIsGone(@NotNull final String adapterId) {
        log.info("Removing all tags provided by adapter {}", adapterId);
        final Set<DomainTag> removed = adapterToDomainTag.remove(adapterId);

        if(removed != null) {
            removed.forEach(tag -> tagNameToDomainTag.remove(tag.getTagName()));
        }
    }
}
