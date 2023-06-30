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
package com.hivemq.bridge.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.metrics.HiveMQMetrics;

public class PerBridgeMetrics {

    public static final String BRIDGE_PREFIX = HiveMQMetrics.HIVEMQ_PREFIX + "bridge";

    private final @NotNull Counter publishForwardSuccessCounter;
    private final @NotNull Counter publishForwardFailCounter;
    private final @NotNull Counter publishRemoteReceivedCounter;
    private final @NotNull Counter publishLocalReceivedCounter;
    private final @NotNull Counter publishLocalSuccessCounter;
    private final @NotNull Counter publishLocalNoSubscriberCounter;
    private final @NotNull Counter publishLocalFailCounter;
    private final @NotNull Counter remotePublishExcludedCounter;
    private final @NotNull Counter loopPreventionForwardDropCounter;
    private final @NotNull Counter loopPreventionRemoteDropCounter;

    public PerBridgeMetrics(final @NotNull String bridgeName, final @NotNull MetricRegistry metricRegistry) {

        publishForwardSuccessCounter =
                metricRegistry.counter(MetricRegistry.name(BRIDGE_PREFIX, bridgeName, "forward.publish", "count"));

        publishForwardFailCounter = metricRegistry.counter(MetricRegistry.name(BRIDGE_PREFIX,
                bridgeName,
                "forward.publish.failed",
                "count"));

        publishRemoteReceivedCounter = metricRegistry.counter(MetricRegistry.name(BRIDGE_PREFIX,
                bridgeName,
                "remote.publish.received",
                "count"));

        publishLocalReceivedCounter = metricRegistry.counter(MetricRegistry.name(BRIDGE_PREFIX,
                bridgeName,
                "local.publish.received",
                "count"));

        publishLocalSuccessCounter =
                metricRegistry.counter(MetricRegistry.name(BRIDGE_PREFIX, bridgeName, "local.publish", "count"));

        publishLocalNoSubscriberCounter = metricRegistry.counter(MetricRegistry.name(BRIDGE_PREFIX,
                bridgeName,
                "local.publish.no-subscriber-present",
                "count"));

        publishLocalFailCounter = metricRegistry.counter(MetricRegistry.name(BRIDGE_PREFIX,
                bridgeName,
                "local.publish.failed",
                "count"));

        remotePublishExcludedCounter = metricRegistry.counter(MetricRegistry.name(BRIDGE_PREFIX,
                bridgeName,
                "forward.publish.excluded",
                "count"));

        loopPreventionForwardDropCounter = metricRegistry.counter(MetricRegistry.name(BRIDGE_PREFIX,
                bridgeName,
                "forward.publish.loop-hops-exceeded",
                "count"));

        loopPreventionRemoteDropCounter = metricRegistry.counter(MetricRegistry.name(BRIDGE_PREFIX,
                bridgeName,
                "remote.publish.loop-hops-exceeded",
                "count"));
    }

    public @NotNull Counter getPublishForwardSuccessCounter() {
        return publishForwardSuccessCounter;
    }

    public @NotNull Counter getPublishForwardFailCounter() {
        return publishForwardFailCounter;
    }

    public @NotNull Counter getPublishRemoteReceivedCounter() {
        return publishRemoteReceivedCounter;
    }

    public @NotNull Counter getPublishLocalReceivedCounter() {
        return publishLocalReceivedCounter;
    }

    public @NotNull Counter getPublishLocalSuccessCounter() {
        return publishLocalSuccessCounter;
    }

    public @NotNull Counter getPublishLocalNoSubscriberCounter() {
        return publishLocalNoSubscriberCounter;
    }

    public @NotNull Counter getPublishLocalFailCounter() {
        return publishLocalFailCounter;
    }

    public @NotNull Counter getRemotePublishExcludedCounter() {
        return remotePublishExcludedCounter;
    }

    public @NotNull Counter getLoopPreventionForwardDropCounter() {
        return loopPreventionForwardDropCounter;
    }

    public @NotNull Counter getLoopPreventionRemoteDropCounter() {
        return loopPreventionRemoteDropCounter;
    }
}
