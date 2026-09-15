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

import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipReadWrite;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
                validateTagDefinition(cipTag);
                registerTag(cipTag);
            }
            validateDirectionConsistency();
            validateCompositesHaveSiblings();
            validateNoByteRangeOverlap();
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

    /**
     * Per-tag sanity checks on the byte-layout fields. These are cheap, device-independent, and reject configs
     * that cannot describe a valid field: a {@code batchByteIndex} must be non-negative, {@code numberOfElements}
     * at least one, and a {@code batchBitIndex} (when set) must be a real bit position 0–7. The JSON schema
     * declares these bounds too, but that is only a UI/schema hint and is not enforced on the config/REST parse
     * path, so a config with e.g. {@code batchBitIndex=8} reaches here unchecked; reject it explicitly.
     */
    private void validateTagDefinition(final @NotNull CipTag cipTag) throws OdvaException {
        final CipTagDefinition definition = cipTag.getDefinition();
        if (definition.getBatchByteIndex() < 0) {
            throw new OdvaException("Tag '"
                    + cipTag.getName()
                    + "' has a negative batchByteIndex "
                    + definition.getBatchByteIndex()
                    + ". The byte index of a tag within its attribute must be 0 or greater.");
        }
        if (definition.getNumberOfElements() < 1) {
            throw new OdvaException("Tag '"
                    + cipTag.getName()
                    + "' has numberOfElements "
                    + definition.getNumberOfElements()
                    + ". A tag must hold at least one element.");
        }
        final Integer bitIndex = definition.getBatchBitIndex();
        if (bitIndex != null && (bitIndex < 0 || bitIndex > 7)) {
            throw new OdvaException("Tag '"
                    + cipTag.getName()
                    + "' has batchBitIndex "
                    + bitIndex
                    + ". A bit index must be between 0 and 7 (a byte has 8 bits).");
        }
    }

    /**
     * Within a {@code (address, readWrite)} group, no two tags may claim overlapping bytes of the attribute. Two
     * tags whose byte ranges overlap decode from the same bytes (silently wrong data) and, for a writable group,
     * <em>corrupt each other on write</em> — a {@code PARTIAL_WRITE} read-modify-write or a {@code COMPLETE_WRITE}
     * lays their bytes down over the same region, last-writer-wins, with no warning. Reject that at registration.
     * <p>
     * A tag's range is {@code [batchByteIndex, batchByteIndex + width * numberOfElements)}, where {@code width}
     * is the type's {@link CipDataType#staticByteWidth() static byte width}. Two cases are deliberately excluded:
     * <ul>
     *   <li><b>Strings</b> ({@code SSTRING}/{@code STRING}) have no static width — their length is only known at
     *       write/read time — so their span cannot be checked up front and they are skipped here.</li>
     *   <li><b>Bit-addressed {@code BOOL}s</b> (a {@code BOOL} with a {@code batchBitIndex}) occupy a single bit,
     *       so several may legitimately share one byte at different bit positions; they are not byte-overlaps.</li>
     * </ul>
     * The composite tag itself carries no bytes (it aggregates its siblings), so it is not considered here.
     */
    private void validateNoByteRangeOverlap() throws OdvaException {
        for (final TagGroup tagGroup : tagAddressToTagGroup.values()) {
            final List<CipTag> byteRangedTags = tagGroup.getTags().stream()
                    .filter(TagGroups::occupiesAStaticByteRange)
                    .toList();
            for (int i = 0; i < byteRangedTags.size(); i++) {
                for (int j = i + 1; j < byteRangedTags.size(); j++) {
                    final CipTag a = byteRangedTags.get(i);
                    final CipTag b = byteRangedTags.get(j);
                    if (byteRangesOverlap(a, b)) {
                        throw new OdvaException("Tags '"
                                + a.getName()
                                + "' and '"
                                + b.getName()
                                + "' at address "
                                + tagGroup.getTagAddress()
                                + " ("
                                + tagGroup.getReadWrite()
                                + ") claim overlapping bytes of the attribute. Each field of an attribute must occupy a"
                                + " distinct byte range; overlapping tags read the same bytes and corrupt each other on"
                                + " write.");
                    }
                }
            }
        }
    }

    /** A tag occupies a checkable byte range if its type has a static width and it is not a bit-addressed BOOL. */
    private static boolean occupiesAStaticByteRange(final @NotNull CipTag cipTag) {
        final CipTagDefinition definition = cipTag.getDefinition();
        final boolean bitAddressedBool =
                definition.getDataType() == CipDataType.BOOL && definition.getBatchBitIndex() != null;
        return definition.getDataType().staticByteWidth().isPresent() && !bitAddressedBool;
    }

    private static boolean byteRangesOverlap(final @NotNull CipTag a, final @NotNull CipTag b) {
        final int aStart = a.getDefinition().getBatchByteIndex();
        final int aEnd = aStart
                + a.getDefinition().getDataType().staticByteWidth().getAsInt()
                        * a.getDefinition().getNumberOfElements();
        final int bStart = b.getDefinition().getBatchByteIndex();
        final int bEnd = bStart
                + b.getDefinition().getDataType().staticByteWidth().getAsInt()
                        * b.getDefinition().getNumberOfElements();
        return aStart < bEnd && bStart < aEnd;
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
