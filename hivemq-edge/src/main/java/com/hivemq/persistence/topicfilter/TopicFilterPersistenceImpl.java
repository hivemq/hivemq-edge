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

import com.hivemq.adapter.sdk.api.exceptions.TagNotFoundException;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class TopicFilterPersistenceImpl implements TopicFilterPersistence {

    private static final Logger log = LoggerFactory.getLogger(TopicFilterPersistenceImpl.class);

    private final @NotNull HashMap<String, TopicFilter> nameToTopicFilter = new HashMap<>();
    private final @NotNull TopicFilterPersistenceReaderWriter topicFilterPersistenceReaderWriter;

    @Inject
    public TopicFilterPersistenceImpl(
            final @NotNull TopicFilterPersistenceReaderWriter topicFilterPersistenceReaderWriter) {
        this.topicFilterPersistenceReaderWriter = topicFilterPersistenceReaderWriter;
        loadPersistence();
    }

    @Override
    public void sync() {
        loadPersistence();
    }

    private void loadPersistence() {
        final List<TopicFilter> topicFilters = topicFilterPersistenceReaderWriter.readPersistence();
        for (final TopicFilter topicFilter : topicFilters) {
            final String name = topicFilter.getTopicFilter();
            if (nameToTopicFilter.containsKey(topicFilter.getName())) {
                log.error(
                        "Found duplicate topic filter for name '{}' during initialization of tag persistence. HiveMQ Edge startup will stopped..",
                        name);
                throw new UnrecoverableException(false);
            }
            nameToTopicFilter.put(name, topicFilter);
        }
    }


    @Override
    public synchronized @NotNull TopicFilterAddResult addTopicFilter(
            final @NotNull TopicFilter topicFilter) {
        if (nameToTopicFilter.containsKey(topicFilter.getName())) {
            return TopicFilterAddResult.failed(TopicFilterAddResult.TopicFilterPutStatus.TOPIC_NAME_ALREADY_USED,
                    "An identical TopicFilter exists already for name '" + topicFilter.getName() + "'");
        }
        if (nameToTopicFilter.values()
                .stream()
                .map(TopicFilter::getTopicFilter)
                .collect(Collectors.toList())
                .contains(topicFilter.getTopicFilter())) {
            return TopicFilterAddResult.failed(TopicFilterAddResult.TopicFilterPutStatus.TOPIC_FILTER_ALREADY_PRESENT,
                    "An identical TopicFilter exists already for the filter '" + topicFilter.getTopicFilter() + "'");
        }


        nameToTopicFilter.put(topicFilter.getName(), topicFilter);
        topicFilterPersistenceReaderWriter.writePersistence(nameToTopicFilter.values());
        return TopicFilterAddResult.success();
    }

    @Override
    public synchronized @NotNull TopicFilterUpdateResult updateTopicFilter(
            @NotNull final String name, @NotNull final TopicFilter topicFilter) {
        final TopicFilter topicFilters = nameToTopicFilter.get(name);
        if (topicFilters == null) {
            return TopicFilterUpdateResult.failed(TopicFilterUpdateResult.TopicFilterUpdateStatus.NOT_FOUND,
                    "No topic filter with name '{}' was found.");
        }
        nameToTopicFilter.put(name, topicFilter);
        return TopicFilterUpdateResult.success();
    }

    @Override
    public synchronized @NotNull TopicFilterDeleteResult deleteTopicFilter(@NotNull final String name) {
        final TopicFilter topicFilter = nameToTopicFilter.remove(name);
        if (topicFilter == null) {
            return TopicFilterDeleteResult.failed(TopicFilterDeleteResult.TopicFilterDeleteStatus.NOT_FOUND,
                    "No topic filter with name '{}' was found.");
        } else {
            return TopicFilterDeleteResult.success();
        }
    }

    @Override
    public synchronized @NotNull List<TopicFilter> getTopicFilters() {
        return new ArrayList<>(nameToTopicFilter.values());
    }

    @Override
    public @NotNull TopicFilter getTag(@NotNull final String name) {
        final TopicFilter topicFilter = nameToTopicFilter.get(name);
        if (topicFilter == null) {
            throw new TagNotFoundException("TopicFilter '" + name + "' was not found in the persistence.");
        }
        return topicFilter;
    }
}
