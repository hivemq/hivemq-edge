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
package com.hivemq.edge.modules.adapters.model.impl;

import com.google.common.base.Preconditions;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterPollingSampler;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Simon L Johnson
 */
public abstract class ProtocolAdapterPollingSamplerImpl<U extends ProtocolAdapterDataSample> implements ProtocolAdapterPollingSampler<U> {

    private final long initialDelay;
    private final long period;
    private final TimeUnit unit;
    private final int maxErrorsBeforeRemoval;
    protected AtomicBoolean closed = new AtomicBoolean(false);
    private final String adapterId;
    private final UUID uuid;
    private final Date created;
    private @Nullable ScheduledFuture<?> future;

    public ProtocolAdapterPollingSamplerImpl(final String adapterId, final long initialDelay, final long period, final @NotNull TimeUnit unit, final int maxErrorsBeforeRemoval) {
        Preconditions.checkNotNull(adapterId);
        Preconditions.checkNotNull(unit);
        this.adapterId = adapterId;
        this.initialDelay = Math.max(initialDelay, 100);
        this.period = Math.max(period, 10);
        this.unit = unit;
        this.maxErrorsBeforeRemoval = maxErrorsBeforeRemoval;
        this.uuid = UUID.randomUUID();
        this.created = new Date();
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
    public TimeUnit getUnit() {
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
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ProtocolAdapterPollingSamplerImpl that = (ProtocolAdapterPollingSamplerImpl) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public UUID getId() {
        return uuid;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public String getAdapterId() {
        return adapterId;
    }

    @Override
    public ScheduledFuture getScheduledFuture() {
        return future;
    }

    @Override
    public void setScheduledFuture(final ScheduledFuture future) {
        this.future = future;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
