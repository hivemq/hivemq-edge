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

import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipReadWrite;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipWriteMode;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.LogicalAddressPathFactory;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TagGroupsTest {

    @Test
    void shouldRegisterTagsOnlyOnceAndReturnInOrder() throws OdvaException {
        // given
        TagGroups tagGroups = new TagGroups();

        CipTag batch1 =
                new CipTag("batch1", "batch1", new CipTagDefinition("@1/2/3", 1, CipDataType.INT, 1d, null, 0, null));
        CipTag batch2 =
                new CipTag("batch2", "batch2", new CipTagDefinition("@1/2/3", 1, CipDataType.SINT, 1d, null, 1, null));
        CipTag single =
                new CipTag("single", "single", new CipTagDefinition("@1/2/4", 1, CipDataType.INT, 1d, null, 0, null));

        // when
        List<CipTag> tags = List.of(batch1, batch2, single);
        Assertions.assertThat(tagGroups.registerTagsIfEmpty(tags)).isTrue();
        Assertions.assertThat(tagGroups.registerTagsIfEmpty(tags)).isFalse();

        // then
        Collection<TagGroup> registeredTags = tagGroups.getTagGroups();
        Assertions.assertThat(registeredTags).size().isEqualTo(2);
        Iterator<TagGroup> iterator = registeredTags.iterator();

        TagGroup batchGroup = iterator.next();
        Assertions.assertThat(batchGroup.getTagAddress())
                .isEqualTo(batch1.getDefinition().getAddress());
        Assertions.assertThat(batchGroup.getLogicalAddressPath())
                .isEqualTo(
                        LogicalAddressPathFactory.create(batch1.getDefinition().getAddress()));
        Assertions.assertThat(batchGroup.getTags()).containsExactly(batch1, batch2);

        TagGroup singleGroup = iterator.next();
        Assertions.assertThat(singleGroup.getTagAddress())
                .isEqualTo(single.getDefinition().getAddress());
        Assertions.assertThat(singleGroup.getLogicalAddressPath())
                .isEqualTo(
                        LogicalAddressPathFactory.create(single.getDefinition().getAddress()));
        Assertions.assertThat(singleGroup.getTags()).containsExactly(single);
    }

    @Test
    void shouldSeparateTagsAtSameAddressByDirection() throws OdvaException {
        // given: same CIP address, different read/write direction
        final CipTag read = new CipTag(
                "read",
                "read",
                new CipTagDefinition("@1/2/3", 1, CipDataType.INT, 0d, null, 0, null, CipReadWrite.READ_ONLY, null));
        final CipTag write = new CipTag(
                "write",
                "write",
                new CipTagDefinition(
                        "@1/2/3",
                        1,
                        CipDataType.INT,
                        0d,
                        null,
                        0,
                        null,
                        CipReadWrite.WRITE_ONLY,
                        CipWriteMode.COMPLETE_WRITE));

        // when
        final TagGroups tagGroups = new TagGroups();
        Assertions.assertThat(tagGroups.registerTagsIfEmpty(List.of(read, write)))
                .isTrue();

        // then: two groups despite the shared address, one readable, one not
        final Collection<TagGroup> groups = tagGroups.getTagGroups();
        Assertions.assertThat(groups).hasSize(2);
        Assertions.assertThat(groups)
                .anyMatch(g -> g.isReadable() && g.getTags().contains(read));
        Assertions.assertThat(groups)
                .anyMatch(g -> !g.isReadable() && g.getTags().contains(write));
    }

    @Test
    void shouldRejectReadWriteMixedWithReadOnlyAtSameAddress() {
        final CipTag readWrite = tag("rw", "@1/2/3", CipReadWrite.READ_WRITE, CipWriteMode.COMPLETE_WRITE);
        final CipTag readOnly = tag("ro", "@1/2/3", CipReadWrite.READ_ONLY, null);

        final TagGroups tagGroups = new TagGroups();
        Assertions.assertThatThrownBy(() -> tagGroups.registerTagsIfEmpty(List.of(readWrite, readOnly)))
                .isInstanceOf(OdvaException.class)
                .hasMessageContaining("READ_WRITE");
    }

    @Test
    void shouldAllowReadOnlyAndWriteOnlyAtSameAddress() throws OdvaException {
        final CipTag readOnly = tag("ro", "@1/2/3", CipReadWrite.READ_ONLY, null);
        final CipTag writeOnly = tag("wo", "@1/2/3", CipReadWrite.WRITE_ONLY, CipWriteMode.COMPLETE_WRITE);

        final TagGroups tagGroups = new TagGroups();
        Assertions.assertThat(tagGroups.registerTagsIfEmpty(List.of(readOnly, writeOnly)))
                .isTrue();
        Assertions.assertThat(tagGroups.getTagGroups()).hasSize(2);
    }

    @Test
    void shouldRejectTwoCompositesAtSameAddressAndDirection() {
        final CipTag composite1 = composite("motor", "@1/2/3", CipReadWrite.READ_ONLY, null);
        final CipTag composite2 = composite("pump", "@1/2/3", CipReadWrite.READ_ONLY, null);

        final TagGroups tagGroups = new TagGroups();
        Assertions.assertThatThrownBy(() -> tagGroups.registerTagsIfEmpty(List.of(composite1, composite2)))
                .isInstanceOf(OdvaException.class)
                .hasMessageContaining("more than one composite")
                .hasMessageContaining("motor")
                .hasMessageContaining("pump");
    }

    @Test
    void shouldAllowOneCompositeWithScalarSiblings() throws OdvaException {
        final CipTag composite = composite("motor", "@1/2/3", CipReadWrite.READ_ONLY, null);
        final CipTag sibling = tag("speed", "@1/2/3", CipReadWrite.READ_ONLY, null);

        final TagGroups tagGroups = new TagGroups();
        Assertions.assertThat(tagGroups.registerTagsIfEmpty(List.of(composite, sibling)))
                .isTrue();
        Assertions.assertThat(tagGroups.getTagGroups()).hasSize(1);
        Assertions.assertThat(tagGroups.getTagGroups().iterator().next().hasComposite())
                .isTrue();
    }

    @Test
    void shouldAllowCompositesAtSameAddressButDifferentDirection() throws OdvaException {
        // Distinct (address, direction) groups, so each may carry its own composite.
        final CipTag readComposite = composite("motorIn", "@1/2/3", CipReadWrite.READ_ONLY, null);
        final CipTag writeComposite =
                composite("motorOut", "@1/2/3", CipReadWrite.WRITE_ONLY, CipWriteMode.COMPLETE_WRITE);

        final TagGroups tagGroups = new TagGroups();
        Assertions.assertThat(tagGroups.registerTagsIfEmpty(List.of(readComposite, writeComposite)))
                .isTrue();
        Assertions.assertThat(tagGroups.getTagGroups()).hasSize(2);
    }

    @Test
    void shouldRetryRegistrationAfterAFailedFirstAttempt() throws OdvaException {
        // A first attempt with an invalid config throws and must NOT mark registration as done, nor leave
        // partial group state behind; a later attempt with a valid config must register cleanly.
        final CipTag readWrite = tag("rw", "@1/2/3", CipReadWrite.READ_WRITE, CipWriteMode.COMPLETE_WRITE);
        final CipTag readOnly = tag("ro", "@1/2/3", CipReadWrite.READ_ONLY, null);

        final TagGroups tagGroups = new TagGroups();
        Assertions.assertThatThrownBy(() -> tagGroups.registerTagsIfEmpty(List.of(readWrite, readOnly)))
                .isInstanceOf(OdvaException.class);
        Assertions.assertThat(tagGroups.getTagGroups()).isEmpty();

        final CipTag valid = tag("ok", "@1/2/4", CipReadWrite.READ_ONLY, null);
        Assertions.assertThat(tagGroups.registerTagsIfEmpty(List.of(valid))).isTrue();
        Assertions.assertThat(tagGroups.getTagGroups()).hasSize(1);
        Assertions.assertThat(tagGroups.getTagGroups().iterator().next().getTags())
                .containsExactly(valid);
    }

    private static CipTag tag(final String name, final String address, final CipReadWrite rw, final CipWriteMode wm) {
        return new CipTag(name, name, new CipTagDefinition(address, 1, CipDataType.INT, 0d, null, 0, null, rw, wm));
    }

    private static CipTag composite(
            final String name, final String address, final CipReadWrite rw, final CipWriteMode wm) {
        return new CipTag(
                name, name, new CipTagDefinition(address, 1, CipDataType.COMPOSITE, 0d, null, 0, null, rw, wm));
    }
}
