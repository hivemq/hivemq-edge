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
package com.hivemq.persistence.local;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.metrics.HiveMQMetrics;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import org.jetbrains.annotations.NotNull;

/**
 * Packet identifiers per client, each set guarded by its map bin: every read and write of a set runs inside a
 * {@link ConcurrentHashMap} compute on its client. Deleting a client is one removal, not a scan over every client.
 */
@Singleton
public class IncomingMessageFlowInMemoryLocalPersistence implements IncomingMessageFlowLocalPersistence {

    private final @NotNull ConcurrentHashMap<String, Set<Integer>> flows = new ConcurrentHashMap<>();
    // a LongAdder, not an AtomicLong: one shared counter written on every PUBLISH and PUBREL cut throughput 3x at
    // 8 threads. The node limit becomes approximate: adds for different clients that check it together can pass
    // it by at most one each, which does not matter for a memory bound.
    private final @NotNull LongAdder size = new LongAdder();
    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;

    @Inject
    public IncomingMessageFlowInMemoryLocalPersistence(
            final @NotNull RestrictionsConfigurationService restrictionsConfigurationService,
            final @NotNull MetricRegistry metricRegistry) {
        this.restrictionsConfigurationService = restrictionsConfigurationService;
        metricRegistry.register(HiveMQMetrics.INCOMING_QOS2_FLOWS_CURRENT.name(), (Gauge<Long>) size::sum);
    }

    @Override
    public void closeDB() {}

    @Override
    public @NotNull AddResult add(final @NotNull String client, final int messageId) {
        final AddResult[] result = new AddResult[1];
        flows.compute(client, (key, messageIds) -> {
            final Set<Integer> ids = messageIds != null ? messageIds : new HashSet<>();
            if (ids.contains(messageId)) {
                result[0] = AddResult.ALREADY_PRESENT;
            } else if (ids.size() >= restrictionsConfigurationService.maxIncomingQos2FlowsPerClient()) {
                result[0] = AddResult.LIMIT_REACHED;
            } else if (size.sum() >= restrictionsConfigurationService.maxIncomingQos2Flows()) {
                result[0] = AddResult.LIMIT_REACHED;
            } else {
                ids.add(messageId);
                size.increment();
                result[0] = AddResult.ADDED;
            }
            return ids.isEmpty() ? null : ids;
        });
        return result[0];
    }

    @Override
    public void remove(final @NotNull String client, final int messageId) {
        flows.computeIfPresent(client, (key, ids) -> {
            if (ids.remove(messageId)) {
                size.decrement();
            }
            return ids.isEmpty() ? null : ids;
        });
    }

    @Override
    public void delete(final @NotNull String client) {
        // the removal takes the bin lock, so every change made to the set under a compute is visible here
        final Set<Integer> ids = flows.remove(client);
        if (ids != null) {
            size.add(-ids.size());
        }
    }

    @Override
    public long size() {
        return size.sum();
    }
}
