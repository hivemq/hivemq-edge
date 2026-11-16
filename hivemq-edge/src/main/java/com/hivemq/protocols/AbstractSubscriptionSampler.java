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
package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingSampler;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("FutureReturnValueIgnored")
public abstract class AbstractSubscriptionSampler implements ProtocolAdapterPollingSampler {

    /**
     * Time a poll's output may take before the sampler fails the poll with a {@code TimeoutException}, an ordinary
     * polling error counted against the adapter's {@code maxPollingErrorsBeforeRemoval}. The polling watchdog's
     * minimum timeout lies above it. The value is the one both samplers always used; it was never derived from a
     * measurement, which would be the slowest adapter's poll time at the 99th percentile.
     */
    public static final int POLL_OUTPUT_TIMEOUT_MILLIS = 10_000;

    private final long initialDelay;
    private final long period;
    private final int maxErrorsBeforeRemoval;

    private final @NotNull TimeUnit unit = TimeUnit.MILLISECONDS;
    private final @NotNull String adapterId;
    private final @NotNull UUID uuid;
    private final @NotNull Date created;

    private volatile @Nullable ScheduledFuture<?> future;

    protected final @NotNull AtomicBoolean closed = new AtomicBoolean(false);
    protected final @NotNull ProtocolAdapterWrapper protocolAdapter;
    protected final @NotNull EventService eventService;

    @SuppressWarnings("JavaUtilDate")
    public AbstractSubscriptionSampler(
            final @NotNull ProtocolAdapterWrapper protocolAdapter, final @NotNull EventService eventService) {
        this.protocolAdapter = protocolAdapter;
        this.adapterId = protocolAdapter.getId();

        if (protocolAdapter.getAdapter() instanceof final PollingProtocolAdapter adapter) {
            this.initialDelay = Math.max(adapter.getPollingIntervalMillis(), 100);
            this.period = Math.max(adapter.getPollingIntervalMillis(), 10);
            this.maxErrorsBeforeRemoval = adapter.getMaxPollingErrorsBeforeRemoval();
        } else if (protocolAdapter.getAdapter() instanceof final BatchPollingProtocolAdapter adapter) {
            this.initialDelay = Math.max(adapter.getPollingIntervalMillis(), 100);
            this.period = Math.max(adapter.getPollingIntervalMillis(), 10);
            this.maxErrorsBeforeRemoval = adapter.getMaxPollingErrorsBeforeRemoval();
        } else {
            throw new IllegalArgumentException("Adapter must be a polling or batch polling protocol adapter");
        }
        this.eventService = eventService;
        this.uuid = UUID.randomUUID();
        this.created = new Date();
    }

    @Override
    public abstract @NotNull CompletableFuture<?> execute();

    @Override
    public void error(final @NotNull Throwable t, final boolean continuing) {
        onSamplerError(t, continuing);
    }

    /**
     * Hook Method is invoked by the sampling engine when the sampler throws an exception. It contains
     * details of whether the sampler will continue or be removed from the scheduler along with
     * the cause of the error.
     */
    protected void onSamplerError(final @NotNull Throwable exception, final boolean continuing) {
        protocolAdapter.setErrorConnectionStatus(exception, null);
        if (!continuing) {
            protocolAdapter.stopAsync(false);
        }
    }

    @Override
    public @NotNull ProtocolAdapter getAdapter() {
        return protocolAdapter.getAdapter();
    }

    @Override
    public long getInitialDelay() {
        return initialDelay;
    }

    @Override
    public long getPeriod() {
        return period;
    }

    @Override
    public @NotNull TimeUnit getUnit() {
        return unit;
    }

    @Override
    public int getMaxErrorsBeforeRemoval() {
        return maxErrorsBeforeRemoval;
    }

    @Override
    public void close() {
        closed.set(true);
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractSubscriptionSampler that)) {
            return false;
        }
        return uuid.equals(that.uuid);
    }

    @Override
    public @NotNull UUID getId() {
        return uuid;
    }

    @Override
    public @NotNull Date getCreated() {
        return created;
    }

    @Override
    public @NotNull String getAdapterId() {
        return adapterId;
    }

    @Override
    public @NotNull String getProtocolId() {
        return protocolAdapter.getProtocolAdapterInformation().getProtocolId();
    }

    @Override
    public @Nullable ScheduledFuture<?> getScheduledFuture() {
        return future;
    }

    @Override
    public void setScheduledFuture(final @NotNull ScheduledFuture<?> future) {
        this.future = future;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    /**
     * The future handed to the polling task: completed with the poll's own outcome before {@code reporter} runs, so a
     * slow log appender cannot hold back a failure the poll reports later, and the watchdog's copy of this future sees
     * that failure instead of timing out. When the poll is already complete at this call, {@code reporter} runs on
     * the calling thread before this method returns; the polling task then finds the future complete and still counts
     * a polling error, not a watchdog hit. A failure of {@code reporter} itself is not the poll's outcome and does not
     * reach the polling task.
     *
     * @param poll     the poll's own future
     * @param reporter logs the poll's outcome, after the returned future is complete
     * @return a future completed exactly as {@code poll} completes
     */
    protected static @NotNull CompletableFuture<Void> completeBeforeReporting(
            final @NotNull CompletableFuture<?> poll, final @NotNull BiConsumer<Object, Throwable> reporter) {
        final CompletableFuture<Void> watched = new CompletableFuture<>();
        poll.whenComplete((result, throwable) -> {
            if (throwable == null) {
                watched.complete(null);
            } else {
                watched.completeExceptionally(throwable);
            }
            reporter.accept(result, throwable);
        });
        return watched;
    }
}
