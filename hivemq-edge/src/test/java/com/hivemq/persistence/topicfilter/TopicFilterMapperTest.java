package com.hivemq.persistence.topicfilter;

import com.hivemq.persistence.topicfilter.xml.TopicFilterXmlEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopicFilterMapperTest {

    @Test
    void topicFilterFromDomainTagEntity_whenSchemaIsBlank_thenSchemaIsNull() {
        final TopicFilter topicFilter =
                TopicFilterMapper.topicFilterFromDomainTagEntity(new TopicFilterXmlEntity("a", "b", ""));
        assertEquals("a", topicFilter.getTopicFilter());
        assertEquals("b", topicFilter.getDescription());
        assertNull(topicFilter.getSchema());
    }
}
