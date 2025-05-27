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
package com.hivemq.persistence.topicfilter;

import com.hivemq.edge.api.model.TopicFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopicFilterTest {

    @Test
    void fromTopicFilterModel_emptyString_nullSchema() {
        final TopicFilterPojo topicFilter = TopicFilterPojo.fromModel(new TopicFilter().topicFilter("a").description( "b").schema(""));
        assertEquals("a", topicFilter.getTopicFilter());
        assertEquals("b", topicFilter.getDescription());
        assertNull(topicFilter.getSchema());
    }
}
