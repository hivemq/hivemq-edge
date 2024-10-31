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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DomainTagPersistenceImplTest {

    private final @NotNull DomainTagPersistenceImpl domainTagPersistence = new DomainTagPersistenceImpl(mock());

    @Test
    void addDomainTag_whenNewTag_thenAddTag() {
        final DomainTag domainTag = DomainTag.simpleAddress("someTagName", "someAddress");

        final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag(domainTag);

        assertSame(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.SUCCESS);
        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(1, tagsForAdapter.size());
        assertEquals(domainTag, tagsForAdapter.get(0));
    }

    @Test
    void addDomainTag_wheDuplicateTag_thenAddTag() {
        final DomainTag domainTag = DomainTag.simpleAddress("someTagName", "someAddress");
        final DomainTag duplicate = DomainTag.simpleAddress("someTagName", "someAddress2");

        domainTagPersistence.addDomainTag(domainTag);
        final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag(duplicate);

        assertSame(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS);
        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(1, tagsForAdapter.size());
        assertEquals(domainTag, tagsForAdapter.get(0));
    }

    @Test
    void updateDomainTag_whenTagExists_thenUpdate() {
        final DomainTag domainTag = DomainTag.simpleAddress("someTagName", "someAddress");

        final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag(domainTag);
        assertSame(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.SUCCESS);

        final DomainTagUpdateResult domainTagUpdateResult =
                domainTagPersistence.updateDomainTag(domainTag.getTagName(), domainTag);
        assertSame(domainTagUpdateResult.getDomainTagUpdateStatus(),
                DomainTagUpdateResult.DomainTagUpdateStatus.SUCCESS);

        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(1, tagsForAdapter.size());
        assertEquals(domainTag, tagsForAdapter.get(0));
    }

    @Test
    void updateDomainTag_whenAdapterDoesNotExist_thenReturnNotFound() {
        final DomainTag domainTag = DomainTag.simpleAddress("someTagName", "someAddress");

        final DomainTagUpdateResult domainTagUpdateResult =
                domainTagPersistence.updateDomainTag(domainTag.getTagName(), domainTag);
        assertSame(domainTagUpdateResult.getDomainTagUpdateStatus(),
                DomainTagUpdateResult.DomainTagUpdateStatus.ADAPTER_NOT_FOUND);

        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(0, tagsForAdapter.size());
    }


    @Test
    void updateDomainTag_whenTagDoesNotExist_thenReturnNotFound() {
        final DomainTag domainTag = DomainTag.simpleAddress("otherTag", "someAddress");
        final DomainTag updatedDomainTag = DomainTag.simpleAddress("someTagName", "someAddress");

        final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag(domainTag);
        assertSame(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.SUCCESS);

        final DomainTagUpdateResult domainTagUpdateResult =
                domainTagPersistence.updateDomainTag(updatedDomainTag.getTagName(), updatedDomainTag);
        assertSame(domainTagUpdateResult.getDomainTagUpdateStatus(),
                DomainTagUpdateResult.DomainTagUpdateStatus.ADAPTER_NOT_FOUND);

        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(1, tagsForAdapter.size());
        assertEquals(domainTag, tagsForAdapter.get(0));
    }


    @Test
    void updateDomainTag_whenTagNameDoesExistForAnotherAdapter_thenReturnError() {

        final DomainTag domainTag = DomainTag.simpleAddress("tag1", "someAddress");
        final DomainTag domainTag2 = DomainTag.simpleAddress("tag2", "someAddress");
        final DomainTag domainTagForAnotherAdapter = DomainTag.simpleAddress("tag3", "otherAdapter", "someAddress");

        domainTagPersistence.addDomainTag(domainTag);
        domainTagPersistence.addDomainTag(domainTag2);
        // this is for another adapter, tag3 should not be able to be used by the adapter as it would be a duplicate
        domainTagPersistence.addDomainTag(domainTagForAnotherAdapter);
        // this tag has a duplicate tag id and should not be able to be put into the persistence
        final DomainTag domainTag3 = DomainTag.simpleAddress("tag3", "someAddress");

        final DomainTagUpdateResult domainTagUpdateResult =
                domainTagPersistence.updateDomainTags("adapter", Set.of(domainTag, domainTag2, domainTag3));
        assertSame(DomainTagUpdateResult.DomainTagUpdateStatus.ALREADY_USED_BY_ANOTHER_ADAPTER,
                domainTagUpdateResult.getDomainTagUpdateStatus());

        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(2, tagsForAdapter.size());
        assertTrue(tagsForAdapter.contains(domainTag));
        assertTrue(tagsForAdapter.contains(domainTag2));
    }

    @Test
    void updateDomainTag_whenLessTagsThanBeforeAreAdded_thenOnlyPersistTheNewTags() {
        final DomainTag domainTag = DomainTag.simpleAddress("tag1", "someAddress");
        final DomainTag domainTag2 = DomainTag.simpleAddress("tag2", "someAddress");
        final DomainTag domainTag3 = DomainTag.simpleAddress("tag3", "someAddress");


        domainTagPersistence.addDomainTag(domainTag);
        domainTagPersistence.addDomainTag(domainTag2);
        domainTagPersistence.addDomainTag(domainTag3);

        final DomainTagUpdateResult domainTagUpdateResult =
                domainTagPersistence.updateDomainTags("adapter", Set.of(domainTag, domainTag2));
        assertSame(domainTagUpdateResult.getDomainTagUpdateStatus(),
                DomainTagUpdateResult.DomainTagUpdateStatus.SUCCESS);

        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(2, tagsForAdapter.size());
        assertTrue(tagsForAdapter.contains(domainTag));
        assertTrue(tagsForAdapter.contains(domainTag2));
        assertFalse(tagsForAdapter.contains(domainTag3));
    }

    @Test
    void updateDomainTag_whenMoreTagsThanBeforeAreAdded_thenPersistTheNewTags() {
        final DomainTag domainTag = DomainTag.simpleAddress("tag1", "someAddress");
        final DomainTag domainTag2 = DomainTag.simpleAddress("tag2", "someAddress");
        final DomainTag domainTag3 = DomainTag.simpleAddress("tag3", "someAddress");

        domainTagPersistence.addDomainTag(domainTag);

        final DomainTagUpdateResult domainTagUpdateResult =
                domainTagPersistence.updateDomainTags("adapter", Set.of(domainTag, domainTag2));
        assertSame(domainTagUpdateResult.getDomainTagUpdateStatus(),
                DomainTagUpdateResult.DomainTagUpdateStatus.SUCCESS);

        final List<DomainTag> tagsForAdapter = domainTagPersistence.getTagsForAdapter("adapter");
        assertEquals(2, tagsForAdapter.size());
        assertTrue(tagsForAdapter.contains(domainTag));
        assertTrue(tagsForAdapter.contains(domainTag2));
        assertFalse(tagsForAdapter.contains(domainTag3));
    }


    @Test
    void deleteDomainTag_whenTagExists_thenTagGetsDeleted() {
        final DomainTag domainTag = DomainTag.simpleAddress("someTagName", "someAddress");

        final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag(domainTag);
        assertSame(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.SUCCESS);

        final DomainTagDeleteResult result = domainTagPersistence.deleteDomainTag("adapter", domainTag.getTagName());
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
            final DomainTag domainTag = DomainTag.simpleAddress("tag" + i, "address" + i);
            final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag(domainTag);
            assertEquals(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.SUCCESS);

            final List<DomainTag> domainTags = domainTagPersistence.getDomainTags();
            assertEquals(i, domainTags.size());
            assertTrue(domainTags.contains(domainTag));
        }
    }

    @Test
    void getTagsForAdapter() {
        for (int i = 1; i < 10; i++) {
            final DomainTag domainTag = DomainTag.simpleAddress("tag" + i, "address" + i);
            final DomainTagAddResult domainTagAddResult = domainTagPersistence.addDomainTag(domainTag);
            assertEquals(domainTagAddResult.getDomainTagPutStatus(), DomainTagAddResult.DomainTagPutStatus.SUCCESS);

            final List<DomainTag> domainTags = domainTagPersistence.getTagsForAdapter("adapter");
            assertEquals(i, domainTags.size());
            assertTrue(domainTags.contains(domainTag));
        }
    }
}