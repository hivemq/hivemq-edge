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

import com.hivemq.edge.adapters.etherip_cip_odva.config.CipReadWrite;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;

public class TagGroups {

    /**
     * Tags are grouped by device address <em>and</em> direction. Keeping the {@link CipReadWrite} in the key
     * keeps read-only, write-only, and read-write tags at the same CIP address in separate groups, so the
     * poll loop never reads write-only tags and a composite only aggregates siblings of the same direction.
     */
    private record GroupKey(
            @NotNull String address, @NotNull CipReadWrite readWrite) implements Comparable<GroupKey> {
        @Override
        public int compareTo(final @NotNull GroupKey o) {
            final int byAddress = address.compareTo(o.address);
            return byAddress != 0 ? byAddress : readWrite.compareTo(o.readWrite);
        }
    }

    private final @NotNull Map<GroupKey, TagGroup> tagAddressToTagGroup = new TreeMap<>();

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

        validateDirectionConsistency();

        return true;
    }

    /**
     * Per address, either every tag is {@link CipReadWrite#READ_WRITE} or none is. Mixing READ_WRITE with a
     * READ_ONLY or WRITE_ONLY tag at the same address is rejected as confusing; READ_ONLY together with
     * WRITE_ONLY at one address is allowed.
     */
    private void validateDirectionConsistency() throws OdvaException {
        final Map<String, Boolean> addressHasReadWrite = new HashMap<>();
        final Map<String, Boolean> addressHasOnly = new HashMap<>();
        for (final GroupKey key : tagAddressToTagGroup.keySet()) {
            if (key.readWrite() == CipReadWrite.READ_WRITE) {
                addressHasReadWrite.put(key.address(), Boolean.TRUE);
            } else {
                addressHasOnly.put(key.address(), Boolean.TRUE);
            }
        }
        for (final String address : addressHasReadWrite.keySet()) {
            if (addressHasOnly.containsKey(address)) {
                throw new OdvaException(
                        "Address "
                                + address
                                + " mixes READ_WRITE with READ_ONLY/WRITE_ONLY tags. Per address, either all tags are READ_WRITE or none is.");
            }
        }
    }

    private boolean tagsAlreadyRegistered() {
        return !tagsRegistered.compareAndSet(false, true);
    }

    @NotNull
    private TagGroup getOrCreateTagGroup(CipTagDefinition definition) throws OdvaException {
        AtomicReference<OdvaException> maybeException = new AtomicReference<>();

        final GroupKey key = new GroupKey(definition.getAddress(), definition.getReadWrite());
        TagGroup tagGroup = tagAddressToTagGroup.computeIfAbsent(key, k -> {
            try {
                return new TagGroup(k.address(), k.readWrite());
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
