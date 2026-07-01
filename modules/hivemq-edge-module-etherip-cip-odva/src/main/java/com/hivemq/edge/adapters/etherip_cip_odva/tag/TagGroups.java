/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.etherip_cip_odva.tag;

import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;

public class TagGroups {
    private final @NotNull Map<String, TagGroup> tagAddressToTagGroup = new TreeMap<>();

    private final @NotNull AtomicBoolean tagsRegistered = new AtomicBoolean(false);

    private TagGroup registerTag(CipTag cipTag) throws OdvaException {
        TagGroup tagGroup = getOrCreateTagGroup(cipTag.getDefinition());
        tagGroup.add(cipTag);
        return tagGroup;
    }

    public boolean registerTagsIfEmpty(Collection<CipTag> cipTags) throws OdvaException {
        if (tagsAlreadyRegistered()) {
            return false;
        }

        for (CipTag cipTag : cipTags) {
            registerTag(cipTag);
        }

        return true;
    }

    private boolean tagsAlreadyRegistered() {
        return !tagsRegistered.compareAndSet(false, true);
    }

    @NotNull
    private TagGroup getOrCreateTagGroup(CipTagDefinition definition) throws OdvaException {
        AtomicReference<OdvaException> maybeException = new AtomicReference<>();

        TagGroup tagGroup = tagAddressToTagGroup.computeIfAbsent(definition.getAddress(), address -> {
            try {
                return new TagGroup(address);
            } catch (OdvaException e) {
                maybeException.set(e);
                return null;
            }
        });

        if (maybeException.get() != null) {
            throw maybeException.get();
        }

        return tagGroup;
    }

    public Collection<TagGroup> getTagGroups() {
        return tagAddressToTagGroup.values();
    }

    public void clear() {
        tagAddressToTagGroup.clear();
    }
}
