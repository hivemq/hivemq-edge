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

import com.hivemq.api.format.DataUrl;
import com.hivemq.persistence.topicfilter.xml.TopicFilterXmlEntity;
import org.jetbrains.annotations.NotNull;

public class TopicFilterMapper {

    public static @NotNull TopicFilterPojo topicFilterFromDomainTagEntity(final @NotNull TopicFilterXmlEntity topicFilterXmlEntity) {
        final String schema = topicFilterXmlEntity.getSchema();
        return new TopicFilterPojo(topicFilterXmlEntity.getTopicFilter(),
                topicFilterXmlEntity.getDescription(),
                schema != null && !schema.isBlank() ? DataUrl.create(schema) : null);
    }

    public static @NotNull TopicFilterXmlEntity topicFilterEntityFromDomainTag(final @NotNull TopicFilterPojo topicFilter) {
        return new TopicFilterXmlEntity(topicFilter.getTopicFilter(),
                topicFilter.getDescription(),
                topicFilter.getSchema() != null ? topicFilter.getSchema().toString() : null);
    }
}
