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
import dagger.internal.Preconditions;
import javassist.convert.TransformNew;

import javax.sound.midi.VoiceStatus;
import java.util.HashSet;
import java.util.Set;

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
    private final @NotNull Set<String> metricNames = new HashSet<>();
    private final @NotNull Object mutex = new Object();

    public PerBridgeMetrics(final @NotNull String bridgeName, final @NotNull MetricRegistry metricRegistry) {

        publishForwardSuccessCounter = createBridgeCounter(metricRegistry,
                bridgeName,
                "forward.publish",
                "count");

        publishForwardFailCounter = createBridgeCounter(metricRegistry,
                bridgeName,
                "forward.publish.failed",
                "count");

        publishRemoteReceivedCounter = createBridgeCounter(metricRegistry,
                bridgeName,
                "remote.publish.received",
                "count");

        publishLocalReceivedCounter = createBridgeCounter(metricRegistry,
                bridgeName,
                "local.publish.received",
                "count");

        publishLocalSuccessCounter = createBridgeCounter(metricRegistry,
        bridgeName,
                "local.publish",
                "count");

        publishLocalNoSubscriberCounter = createBridgeCounter(metricRegistry,
                bridgeName,
                "local.publish.no-subscriber-present",
                "count");

        publishLocalFailCounter = createBridgeCounter(metricRegistry,
                bridgeName,
                "local.publish.failed",
                "count");

        remotePublishExcludedCounter = createBridgeCounter(metricRegistry,
                bridgeName,
                "forward.publish.excluded",
                "count");

        loopPreventionForwardDropCounter = createBridgeCounter(metricRegistry,
                bridgeName,
                "forward.publish.loop-hops-exceeded",
                "count");

        loopPreventionRemoteDropCounter = createBridgeCounter(metricRegistry,
                bridgeName,
                "remote.publish.loop-hops-exceeded",
                "count");
    }

    private Counter createBridgeCounter(final @NotNull MetricRegistry metricRegistry, final @NotNull String... names){
        final String metricName = MetricRegistry.name(BRIDGE_PREFIX, names);
        synchronized (mutex){
            metricNames.add(metricName);
        }
        return metricRegistry.counter(metricName);
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

    public void clearAll(final @NotNull MetricRegistry metricRegistry){
        Preconditions.checkNotNull(metricRegistry);
        synchronized (mutex){
            metricNames.forEach(metricRegistry::remove);
            metricNames.clear();
        }
    }
}
