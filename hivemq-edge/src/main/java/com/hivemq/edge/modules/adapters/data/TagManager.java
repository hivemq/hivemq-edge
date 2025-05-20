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
package com.hivemq.edge.modules.adapters.data;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.protocols.northbound.TagConsumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class TagManager implements ProtocolAdapterTagStreamingService {

    private static final Logger log = LoggerFactory.getLogger(TagManager.class);

    private final @NotNull MetricsHolder metricsHolder;

    // TODO this is basically a memory leak. The problem is when shall we remove the last value?
    // We would need to add a callback/logic to the lifecycle of tags
    // is it intended that we might send very old data?
    // perhaps it is good enough if we ensure that northbound mappings are created before tags as adapters are restarted on config change anyway
    private final Map<String, List<DataPoint>> lastValueForTag = new ConcurrentHashMap<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    @Inject
    public TagManager(final @NotNull MetricsHolder metricsHolder) {
        this.metricsHolder = metricsHolder;
    }

    private final @NotNull ConcurrentHashMap<String, List<TagConsumer>> consumers = new ConcurrentHashMap<>();

    @Override
    public void feed(final @NotNull String tagName, final @NotNull List<DataPoint> dataPoints) {
        lastValueForTag.put(tagName, dataPoints);
        try {
            readWriteLock.readLock().lock();
            final var tagConsumers = consumers.get(tagName);
            if (tagConsumers != null) {
                consumers.get(tagName).forEach(consumer -> {
                    try {
                        consumer.accept(dataPoints);
                    } catch (final Exception e) {
                        log.error("An error was thrown while processing tag {} with consumer {}", tagName, consumer, e);
                    }
                });
            }
        } finally {
            readWriteLock.readLock().unlock();
        }
    }


    public void addConsumer(final @NotNull TagConsumer consumer) {
        try {
            readWriteLock.writeLock().lock();
            final String tagName = consumer.getTagName();
            consumers.compute(tagName, (tag, current) -> {
                if (current != null) {
                    current.add(consumer);
                    return current;
                } else {
                    final List<TagConsumer> consumers = new ArrayList<>();
                    consumers.add(consumer);
                    return consumers;
                }
            });

            // if there is a value present in the cache, we sent it to the consumer
            final List<DataPoint> dataPoints = lastValueForTag.get(tagName);
            if (dataPoints != null) {
                consumer.accept(dataPoints);
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }


    public void removeConsumer(final @NotNull TagConsumer consumer) {
        try {
            readWriteLock.writeLock().lock();
            consumers.computeIfPresent(consumer.getTagName(), (tag, current) -> {
                current.remove(consumer);
                return current;
            });
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}
