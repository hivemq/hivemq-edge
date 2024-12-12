package com.hivemq.persistence.topicfilter;

import com.hivemq.api.model.topicFilters.TopicFilterModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopicFilterTest {

    @Test
    void fromTopicFilterModel_emptyString_nullSchema() {
        final TopicFilter topicFilter = TopicFilter.fromTopicFilterModel(new TopicFilterModel("a", "b", ""));
        assertEquals("a", topicFilter.getTopicFilter());
        assertEquals("b", topicFilter.getDescription());
        assertNull(topicFilter.getSchema());
    }
}
