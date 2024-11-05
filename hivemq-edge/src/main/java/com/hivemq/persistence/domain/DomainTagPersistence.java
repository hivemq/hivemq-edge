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

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.Set;

public interface DomainTagPersistence {

    @NotNull
    DomainTagAddResult addDomainTag( @NotNull DomainTag domainTag);

    @NotNull
    DomainTagUpdateResult updateDomainTag(
            @NotNull String tagId,
            @NotNull DomainTag domainTag);

    @NotNull
    DomainTagUpdateResult updateDomainTags(
            @NotNull String adapterId,
            @NotNull Set<DomainTag> domainTags);

    @NotNull
    DomainTagDeleteResult deleteDomainTag(@NotNull String adapterId, @NotNull String tagId);

    @NotNull
    List<DomainTag> getDomainTags();

    @NotNull
    List<DomainTag> getTagsForAdapter(@NotNull String adapterId);

    @NotNull
    DomainTag getTag(@NotNull String tagId);

    void adapterIsGone(@NotNull String adapterId);
}
