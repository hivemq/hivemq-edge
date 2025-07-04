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

import com.hivemq.exceptions.UnrecoverableException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Singleton
public class TopicFilterPersistenceImpl implements TopicFilterPersistence {

    private static final Logger log = LoggerFactory.getLogger(TopicFilterPersistenceImpl.class);

    private final @NotNull HashMap<String, TopicFilterPojo> filterToTopicFilter = new HashMap<>();
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
        final List<TopicFilterPojo> topicFilters = topicFilterPersistenceReaderWriter.readPersistence();
        for (final TopicFilterPojo topicFilter : topicFilters) {
            if (this.filterToTopicFilter.containsKey(topicFilter.getTopicFilter())) {
                log.error(
                        "Found duplicate topic filter for filter '{}' during initialization of tag persistence. HiveMQ Edge startup will stopped..",
                        topicFilter.getTopicFilter());
                throw new UnrecoverableException(false);
            }
            this.filterToTopicFilter.put(topicFilter.getTopicFilter(), topicFilter);
        }
    }


    @Override
    public synchronized @NotNull TopicFilterAddResult addTopicFilter(
            final @NotNull TopicFilterPojo topicFilter) {
        if (filterToTopicFilter.containsKey(topicFilter.getTopicFilter())) {
            return TopicFilterAddResult.failed(TopicFilterAddResult.TopicFilterPutStatus.TOPIC_FILTER_ALREADY_PRESENT,
                    "An identical TopicFilter exists already for the filter '" + topicFilter.getTopicFilter() + "'");
        }

        this.filterToTopicFilter.put(topicFilter.getTopicFilter(), topicFilter);
        topicFilterPersistenceReaderWriter.writePersistence(filterToTopicFilter.values());
        return TopicFilterAddResult.success();
    }

    @Override
    public synchronized @NotNull TopicFilterUpdateResult updateTopicFilter(final @NotNull TopicFilterPojo topicFilter) {
        final TopicFilterPojo removed = filterToTopicFilter.remove(topicFilter.getTopicFilter());
        if (removed != null) {
            this.filterToTopicFilter.put(topicFilter.getTopicFilter(), topicFilter);
            topicFilterPersistenceReaderWriter.writePersistence(filterToTopicFilter.values());
            return TopicFilterUpdateResult.success();
        } else {
            return TopicFilterUpdateResult.failed(TopicFilterUpdateResult.TopicFilterUpdateStatus.NOT_FOUND,
                    "No topic filter with filter '" + topicFilter.getTopicFilter() + "' was found.");
        }
    }

    @Override
    public synchronized @NotNull TopicFilterUpdateResult updateAllTopicFilters(final @NotNull List<TopicFilterPojo> topicFilters) {
        filterToTopicFilter.clear();
        for (final TopicFilterPojo topicFilter : topicFilters) {
            filterToTopicFilter.put(topicFilter.getTopicFilter(), topicFilter);
        }
        topicFilterPersistenceReaderWriter.writePersistence(topicFilters);
        return TopicFilterUpdateResult.success();
    }

    @Override
    public synchronized @NotNull TopicFilterDeleteResult deleteTopicFilter(final @NotNull String filter) {
        final TopicFilterPojo topicFilter = filterToTopicFilter.remove(filter);
        if (topicFilter == null) {
            return TopicFilterDeleteResult.failed(TopicFilterDeleteResult.TopicFilterDeleteStatus.NOT_FOUND,
                    "No topic filter with name '{}' was found.");
        } else {
            topicFilterPersistenceReaderWriter.writePersistence(filterToTopicFilter.values());
            return TopicFilterDeleteResult.success();
        }
    }

    @Override
    public synchronized @NotNull List<TopicFilterPojo> getTopicFilters() {
        return new ArrayList<>(filterToTopicFilter.values());
    }

    @Override
    public @NotNull TopicFilterPojo getTopicFilter(final @NotNull String filter) {
        return filterToTopicFilter.get(filter);
    }
}
