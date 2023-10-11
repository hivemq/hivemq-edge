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
package com.hivemq.edge.modules.adapters.model;

import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Simon L Johnson
 */
public interface ProtocolAdapterPollingSampler<U extends ProtocolAdapterDataSample> {

    long getInitialDelay();

    long getPeriod();

    TimeUnit getUnit();

    /**
     * Do the work associated with this polling job. It is acceptable to throw exceptions from this method,
     * they will be caught and the process will be backed off accordingly
     */
    CompletableFuture<U> execute() ;

    /**
     * Called when the job is remove from the pool
     */
    void close();

    boolean isClosed();

    UUID getId();
    Date getCreated();
    String getAdapterId();

    default String getReferenceId(){
        return String.format("%s:%s", getAdapterId(), getId());
    }

    default void error(@NotNull final Throwable t, final boolean continuing) {}

    default int getMaxErrorsBeforeRemoval(){
        return 25;
    }

    ScheduledFuture getScheduledFuture();
    void setScheduledFuture(ScheduledFuture future);
}
