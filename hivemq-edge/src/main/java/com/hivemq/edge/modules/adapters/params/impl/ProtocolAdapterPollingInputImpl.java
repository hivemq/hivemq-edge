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
package com.hivemq.edge.modules.adapters.params.impl;

import com.google.common.base.Preconditions;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterPollingInput;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.reactivex.internal.schedulers.NewThreadWorker;

import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Simon L Johnson
 */
public abstract class ProtocolAdapterPollingInputImpl implements ProtocolAdapterPollingInput {

    private final long initialDelay;
    private final long period;
    private final TimeUnit unit;
    private final int maxErrorsBeforeRemoval;
    protected AtomicBoolean closed = new AtomicBoolean(false);

    public ProtocolAdapterPollingInputImpl(final long initialDelay, final long period, final @NotNull TimeUnit unit, final int maxErrorsBeforeRemoval) {
        Preconditions.checkNotNull(unit);
        this.initialDelay = Math.max(initialDelay, 100);
        this.period = Math.max(period, 10);
        this.unit = unit;
        this.maxErrorsBeforeRemoval = maxErrorsBeforeRemoval;
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
}
