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
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.protocols.northbound.TagConsumer;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class TagManager {


    private final @NotNull MetricsHolder metricsHolder;
    // TODO this is basically a memory leak. The problem is when shall we remove the last value?
    // We would need to add a callback/logic to the lifecycle of tags
    // is it intended that we might send very old data?
    // perhaps it is good enough if we ensure that northbound mappings are created before tags as adapters are restarted on config change anyway
    private final Map<String, List<DataPoint>> lastValueForTag = new ConcurrentHashMap<>();

    @Inject
    public TagManager(final @NotNull MetricsHolder metricsHolder) {
        this.metricsHolder = metricsHolder;
    }

    private final @NotNull ConcurrentHashMap<String, List<TagConsumer>> consumers = new ConcurrentHashMap<>();

    public void feed(final @NotNull String tagName, final @NotNull List<DataPoint> dataPoints) {
        // TODO handle null
        lastValueForTag.put(tagName, dataPoints);
        final List<TagConsumer> tagConsumers = consumers.get(tagName);
        if (tagConsumers != null) {
            consumers.get(tagName).forEach(c -> c.accept(dataPoints));
        }
    }


    public void addConsumer(final @NotNull String tagName, final @NotNull TagConsumer consumer) {
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
    }


    public void removeConsumer(final @NotNull TagConsumer consumer) {
        consumers.computeIfPresent(consumer.getTagName(), (tag, current) -> {
            current.remove(consumer);
            return current;
        });
    }

    // TODO remove consumer


}
