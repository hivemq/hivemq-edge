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
package com.hivemq.protocols.v2.runtime;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * The minimal per-adapter metric set the risk model promises, registered on the shared
 * {@link MetricRegistry}. One instance belongs to one adapter wrapper, created when the wrapper is and
 * {@link #close() removed} when the wrapper is, so its metric names ({@code protocol-adapter-v2.adapter.<id>.*}) never outlive
 * the adapter.
 * <ul>
 * <li>{@code protocol-adapter-v2.adapter.<id>.mailbox.depth} — gauge over the wrapper mailbox's {@code size()};</li>
 * <li>{@code protocol-adapter-v2.adapter.<id>.state.transitions} — counter, one per adapter-machine transition;</li>
 * <li>{@code protocol-adapter-v2.adapter.<id>.defensive.resets} — counter, one per defensive (unmatched) reset;</li>
 * <li>{@code protocol-adapter-v2.adapter.<id>.tag.<name>.failures} — counter per tag (poll/write/subscription failures);</li>
 * <li>{@code protocol-adapter-v2.adapter.<id>.tag.<name>.writes.rejected} — counter per tag (southbound writes rejected because one was already in flight — should stay zero under a back-pressuring producer);</li>
 * <li>{@code protocol-adapter-v2.adapter.<id>.tick.lag} — gauge of {@code now - tick.nowMillis} at processing time.</li>
 * </ul>
 * Counters and the tick-lag holder are updated from the owning actor's dispatch thread; the gauges are read from
 * the metrics reporter's thread, which is why the mailbox-depth source is the thread-safe {@code Mailbox.size()}
 * and the tick-lag source is an {@link AtomicLong}.
 */
public final class ProtocolAdapterMetrics implements AutoCloseable {

    public static final @NotNull String ADAPTER_PREFIX = "protocol-adapter-v2.adapter.";

    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull String adapterId;
    private final @NotNull String mailboxDepthName;
    private final @NotNull String stateTransitionsName;
    private final @NotNull String defensiveResetsName;
    private final @NotNull String tickLagName;
    private final @NotNull Counter stateTransitions;
    private final @NotNull Counter defensiveResets;
    private final @NotNull AtomicLong tickLagMillis = new AtomicLong();
    private final @NotNull Set<String> tagFailureNames = ConcurrentHashMap.newKeySet();
    private final @NotNull Set<String> tagWriteRejectedNames = ConcurrentHashMap.newKeySet();

    /**
     * @param metricRegistry the shared registry to register on.
     * @param adapterId      the adapter instance id; embedded in every metric name.
     * @param mailboxDepth   the source of the mailbox-depth gauge — typically the wrapper mailbox's
     *                       {@code size}, which is safe to read from any thread.
     */
    public ProtocolAdapterMetrics(
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull String adapterId,
            final @NotNull IntSupplier mailboxDepth) {
        this.metricRegistry = metricRegistry;
        this.adapterId = adapterId;
        this.mailboxDepthName = name("mailbox.depth");
        this.stateTransitionsName = name("state.transitions");
        this.defensiveResetsName = name("defensive.resets");
        this.tickLagName = name("tick.lag");
        metricRegistry.registerGauge(mailboxDepthName, mailboxDepth::getAsInt);
        metricRegistry.registerGauge(tickLagName, tickLagMillis::get);
        this.stateTransitions = metricRegistry.counter(stateTransitionsName);
        this.defensiveResets = metricRegistry.counter(defensiveResetsName);
    }

    /**
     * Record one adapter-machine state transition.
     */
    public void incrementStateTransition() {
        stateTransitions.inc();
    }

    /**
     * Record one defensive (unmatched) reset.
     */
    public void incrementDefensiveReset() {
        defensiveResets.inc();
    }

    /**
     * Record one failure (poll, write, or subscription) for the named tag, creating its counter on first use.
     *
     * @param tagName the tag the failure belongs to.
     */
    public void incrementTagFailure(final @NotNull String tagName) {
        final String tagFailureName = name("tag." + tagName + ".failures");
        tagFailureNames.add(tagFailureName);
        metricRegistry.counter(tagFailureName).inc();
    }

    /**
     * Record one southbound write rejected for the named tag because one was already in flight (the aspect never
     * queues), creating the counter on first use. Under a back-pressuring producer this stays at zero; a non-zero
     * value means a producer submitted the next write before the current one completed.
     *
     * @param tagName the tag whose write was rejected.
     */
    public void incrementWriteRejected(final @NotNull String tagName) {
        final String writeRejectedName = name("tag." + tagName + ".writes.rejected");
        tagWriteRejectedNames.add(writeRejectedName);
        metricRegistry.counter(writeRejectedName).inc();
    }

    /**
     * Publish the latest tick lag for the gauge to report.
     *
     * @param lagMillis {@code now - tick.nowMillis} measured when the tick was handled, in milliseconds.
     */
    public void recordTickLag(final long lagMillis) {
        tickLagMillis.set(lagMillis);
    }

    /**
     * Deregister every metric this instance registered, so a recreated adapter with the same id starts clean.
     */
    @Override
    public void close() {
        metricRegistry.remove(mailboxDepthName);
        metricRegistry.remove(tickLagName);
        metricRegistry.remove(stateTransitionsName);
        metricRegistry.remove(defensiveResetsName);
        for (final String tagFailureName : tagFailureNames) {
            metricRegistry.remove(tagFailureName);
        }
        for (final String tagWriteRejectedName : tagWriteRejectedNames) {
            metricRegistry.remove(tagWriteRejectedName);
        }
    }

    private @NotNull String name(final @NotNull String suffix) {
        return ADAPTER_PREFIX + adapterId + "." + suffix;
    }
}
