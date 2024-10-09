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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainTagPersistenceImplTest {


    private final DomainTagPersistenceImpl domainTagPersistence = new DomainTagPersistenceImpl();

    @Test
    void addDomainTag_whenNewTag_thenAddTag() {
        final DomainTag domainTag = DomainTag.simpleAddress("someAddress", "someTagName");

        final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag("adapter", domainTag);

        assertSame(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.SUCCESS);
        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(1, tagsForAdapter.size());
        assertEquals(domainTag, tagsForAdapter.get(0));
    }

    @Test
    void addDomainTag_wheDuplicateTag_thenAddTag() {
        final DomainTag domainTag = DomainTag.simpleAddress("someAddress", "someTagName");
        final DomainTag duplicate = DomainTag.simpleAddress("someAddress2", "someTagName");

        domainTagPersistence.addDomainTag("adapter", domainTag);
        final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag("adapter", duplicate);

        assertSame(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS);
        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(1, tagsForAdapter.size());
        assertEquals(domainTag, tagsForAdapter.get(0));
    }

    @Test
    void updateDomainTag_whenTagExists_thenUpdate() {
        final DomainTag domainTag = DomainTag.simpleAddress("someAddress", "someTagName");

        final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag("adapter", domainTag);
        assertSame(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.SUCCESS);

        final DomainTagUpdateResult domainTagUpdateResult =
                domainTagPersistence.updateDomainTag("adapter", domainTag.getTag(), domainTag);
        assertSame(domainTagUpdateResult.getDomainTagUpdateStatus(),
                DomainTagUpdateResult.DomainTagUpdateStatus.SUCCESS);

        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(1, tagsForAdapter.size());
        assertEquals(domainTag, tagsForAdapter.get(0));
    }

    @Test
    void updateDomainTag_whenAdapterDoesNotExist_thenReturnNotFound() {
        final DomainTag domainTag = DomainTag.simpleAddress("someAddress", "someTagName");

        final DomainTagUpdateResult domainTagUpdateResult =
                domainTagPersistence.updateDomainTag("adapter", domainTag.getTag(), domainTag);
        assertSame(domainTagUpdateResult.getDomainTagUpdateStatus(),
                DomainTagUpdateResult.DomainTagUpdateStatus.NOT_FOUND);

        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(0, tagsForAdapter.size());
    }


    @Test
    void updateDomainTag_whenTagDoesNotExist_thenReturnNotFound() {
        final DomainTag domainTag = DomainTag.simpleAddress("someAddress", "otherTag");
        final DomainTag updatedDomainTag = DomainTag.simpleAddress("someAddress", "someTagName");

        final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag("adapter", domainTag);
        assertSame(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.SUCCESS);

        final DomainTagUpdateResult domainTagUpdateResult =
                domainTagPersistence.updateDomainTag("adapter", updatedDomainTag.getTag(), updatedDomainTag);
        assertSame(domainTagUpdateResult.getDomainTagUpdateStatus(),
                DomainTagUpdateResult.DomainTagUpdateStatus.NOT_FOUND);

        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(1, tagsForAdapter.size());
        assertEquals(domainTag, tagsForAdapter.get(0));
    }


    @Test
    void deleteDomainTag_whenTagExists_thenTagGetsDeleted() {
        final DomainTag domainTag = DomainTag.simpleAddress("someAddress", "someTagName");

        final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag("adapter", domainTag);
        assertSame(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.SUCCESS);

        final DomainTagDeleteResult result = domainTagPersistence.deleteDomainTag("adapter", domainTag.getTag());
        assertSame(result.getDomainTagDeleteStatus(), DomainTagDeleteResult.DomainTagDeleteStatus.SUCCESS);

        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(0, tagsForAdapter.size());
    }


    @Test
    void deleteDomainTag_whenTagDoesNotExist_thenReturnNotFound() {
        final DomainTagDeleteResult result = domainTagPersistence.deleteDomainTag("adapter", "does not exist");
        assertSame(result.getDomainTagDeleteStatus(), DomainTagDeleteResult.DomainTagDeleteStatus.NOT_FOUND);

        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(0, tagsForAdapter.size());
    }


    @Test
    void getDomainTags() {
        for (int i = 1; i < 10; i++) {
            final DomainTag domainTag = DomainTag.simpleAddress("address" + i, "tag" + i);
            final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag("adapter", domainTag);
            assertEquals(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.SUCCESS);

            final List<DomainTag> domainTags = domainTagPersistence.getDomainTags();
            assertEquals(i, domainTags.size());
            assertTrue(domainTags.contains(domainTag));
        }
    }

    @Test
    void getTagsForAdapter() {
        for (int i = 1; i < 10; i++) {
            final DomainTag domainTag = DomainTag.simpleAddress("address" + i, "tag" + i);
            final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag("adapter", domainTag);
            assertEquals(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.SUCCESS);

            final List<DomainTag> domainTags = domainTagPersistence.getTagsForAdapter("adapter");
            assertEquals(i, domainTags.size());
            assertTrue(domainTags.contains(domainTag));
        }
    }
}
