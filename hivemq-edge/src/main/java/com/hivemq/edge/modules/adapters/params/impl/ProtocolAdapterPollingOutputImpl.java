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

import com.hivemq.edge.modules.adapters.params.ProtocolAdapterPollingInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterPollingOutput;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Simon L Johnson
 */
public class ProtocolAdapterPollingOutputImpl
        implements ProtocolAdapterPollingOutput {

    private final String adapterId;
    private final UUID uuid;
    private final Date created;
    private ScheduledFuture<?> future;
    private String description;
    private ProtocolAdapterPollingInput input;

    public ProtocolAdapterPollingOutputImpl(
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapterPollingInput input) {
        this.adapterId = adapterId;
        this.input = input;
        this.uuid = UUID.randomUUID();
        this.created = new Date();
    }

    public void setFuture(final ScheduledFuture<?> future) {
        this.future = future;
    }

    public UUID getUuid() {
        return uuid;
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
    public String getDescription() {
        return description;
    }

    @Override
    public String getAdapterId() {
        return adapterId;
    }

    @Override
    public ScheduledFuture<?> getFuture() {
        return future;
    }

    public ProtocolAdapterPollingInput getInput() {
        return input;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProtocolAdapterPollingOutputImpl that = (ProtocolAdapterPollingOutputImpl) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
