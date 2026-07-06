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

import static java.util.Objects.requireNonNull;

import com.hivemq.edge.adapters.etherip_cip_odva.config.CipReadWrite;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private @NotNull TagGroup registerTag(final @NotNull CipTag cipTag) throws OdvaException {
        final @NotNull TagGroup tagGroup = getOrCreateTagGroup(cipTag.getDefinition());
        tagGroup.add(cipTag);
        return tagGroup;
    }

    public boolean registerTagsIfEmpty(Collection<CipTag> cipTags) throws OdvaException {
        if (tagsRegistered.get()) {
            return false;
        }

        // Build and validate first, and only mark registration complete once it has fully succeeded. If any
        // step throws, discard the partial group state and leave the flag unset, so a later start() retries
        // registration from scratch instead of continuing on stale or half-built groups.
        try {
            for (CipTag cipTag : cipTags) {
                registerTag(cipTag);
            }
            validateDirectionConsistency();
            validateCompositesHaveSiblings();
        } catch (final OdvaException e) {
            clear();
            throw e;
        }

        tagsRegistered.set(true);
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

    /**
     * A composite aggregates the scalar siblings in its {@code (address, readWrite)} group; with no sibling it
     * has nothing to read or write. Such a group would start successfully but then publish nothing on poll and,
     * on a southbound write, encode no fields — issuing a zero-length {@code COMPLETE_WRITE} or a no-op
     * {@code PARTIAL_WRITE} that still reports success. Reject it at registration instead.
     */
    private void validateCompositesHaveSiblings() throws OdvaException {
        for (final TagGroup tagGroup : tagAddressToTagGroup.values()) {
            if (tagGroup.hasComposite() && tagGroup.getTags().isEmpty()) {
                throw new OdvaException(
                        "Composite tag '"
                                + requireNonNull(tagGroup.getComposite()).getName()
                                + "' at address "
                                + tagGroup.getTagAddress()
                                + " ("
                                + tagGroup.getReadWrite()
                                + ") has no scalar siblings to aggregate. A composite needs at least one scalar tag at the same address and direction.");
            }
        }
    }

    private @NotNull TagGroup getOrCreateTagGroup(final @NotNull CipTagDefinition definition) throws OdvaException {
        final GroupKey key = new GroupKey(definition.getAddress(), definition.getReadWrite());
        // Plain get-then-create: TagGroup's constructor is checked-throwing, so don't smuggle the exception out
        // of a computeIfAbsent lambda — just create it directly and let OdvaException propagate.
        final @Nullable TagGroup existing = tagAddressToTagGroup.get(key);
        if (existing != null) {
            return existing;
        }
        final TagGroup created = new TagGroup(key.address(), key.readWrite());
        tagAddressToTagGroup.put(key, created);
        return created;
    }

    public Collection<TagGroup> getTagGroups() {
        return tagAddressToTagGroup.values();
    }

    public void clear() {
        tagAddressToTagGroup.clear();
        tagsRegistered.set(false);
    }
}
