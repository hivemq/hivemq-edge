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
