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
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.protocols.northbound.SingleTagConsumer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TagManager {

    private static final Logger log = LoggerFactory.getLogger(TagManager.class);

    // TODO this is basically a memory leak. The problem is when shall we remove the last value?
    // We would need to add a callback/logic to the lifecycle of tags
    // is it intended that we might send very old data?
    // perhaps it is good enough if we ensure that northbound mappings are created before tags as adapters are restarted
    // on config change anyway
    private final Map<String, DataPoint> lastDataPointByTag = new ConcurrentHashMap<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    @Inject
    public TagManager() {}

    private final @NotNull Map<String, List<SingleTagConsumer>> singleTagConsumers = new ConcurrentHashMap<>();

    public void feed(@NotNull final String tag, @NotNull final List<DataPoint> dataPoints) {
        // This is still correct since every dataPoint contains the tagname it came from.
        feed(dataPoints);
    }

    public void feed(@NotNull final List<DataPoint> dataPoints) {
        final var readlock = readWriteLock.readLock();
        final var consumers = new HashMap<String, List<SingleTagConsumer>>();
        readlock.lock();
        try {
            dataPoints.forEach(dataPoint -> {
                final var consuemr = singleTagConsumers.get(dataPoint.getTagName());
                if (consuemr != null) {
                    consumers.put(dataPoint.getTagName(), consuemr);
                }
            });
        } finally {
            readlock.unlock();
        }

        dataPoints.forEach(dataPoint -> {
            final var tagName = dataPoint.getTagName();
            lastDataPointByTag.put(dataPoint.getTagName(), dataPoint);
            if (consumers.containsKey(tagName)) {
                final var tagConsumers = consumers.get(tagName);
                tagConsumers.forEach(consumer -> {
                    try {
                        consumer.accept(dataPoint);
                    } catch (final Exception e) {
                        log.error("An error was thrown while processing tag {} with consumer {}", tagName, consumer, e);
                    }
                });
            }
        });
    }

    public void addConsumer(final @NotNull SingleTagConsumer consumer) {
        final var writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            final String tagName = consumer.getTagName();
            singleTagConsumers.compute(tagName, (tag, current) -> {
                if (current != null) {
                    current.add(consumer);
                    return current;
                } else {
                    final List<SingleTagConsumer> consumers = new ArrayList<>();
                    consumers.add(consumer);
                    return consumers;
                }
            });

            // if there is a value present in the cache, we sent it to the consumer
            final DataPoint dataPoint = lastDataPointByTag.get(tagName);
            if (dataPoint != null) {
                consumer.accept(dataPoint);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void removeConsumer(final @NotNull SingleTagConsumer consumer) {
        final var writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            singleTagConsumers.computeIfPresent(consumer.getTagName(), (tag, current) -> {
                current.remove(consumer);
                return current;
            });
        } finally {
            writeLock.unlock();
        }
    }
}
