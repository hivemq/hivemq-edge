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
package com.hivemq.persistence.clientsession;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.persistence.Sizable;
import com.hivemq.util.MemoryEstimator;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;

public class ClientSession implements Sizable {

    private final @Nullable Long queueLimit;
    private boolean connected;
    private long sessionExpiryIntervalSec;
    private int inMemorySize = SIZE_NOT_CALCULATED;
    private @Nullable ClientSessionWill willPublish;

    public ClientSession(final boolean connected, final long sessionExpiryInterval) {
        this(connected, sessionExpiryInterval, null, null);
    }

    public ClientSession(
            final boolean connected,
            final long sessionExpiryIntervalSec,
            final @Nullable ClientSessionWill willPublish,
            final @Nullable Long queueLimit) {

        Preconditions.checkArgument(
                sessionExpiryIntervalSec >= SESSION_EXPIRE_ON_DISCONNECT,
                "Session expiry interval must never be less than zero");

        this.connected = connected;
        this.sessionExpiryIntervalSec = sessionExpiryIntervalSec;
        this.willPublish = willPublish;
        this.queueLimit = queueLimit;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(final boolean connected) {
        this.connected = connected;
    }

    public long getSessionExpiryIntervalSec() {
        return sessionExpiryIntervalSec;
    }

    public void setSessionExpiryIntervalSec(final long sessionExpiryIntervalSec) {
        this.sessionExpiryIntervalSec = sessionExpiryIntervalSec;
    }

    public @Nullable ClientSessionWill getWillPublish() {
        return willPublish;
    }

    public void setWillPublish(final @Nullable ClientSessionWill willPublish) {
        this.willPublish = willPublish;
    }

    public @Nullable Long getQueueLimit() {
        return queueLimit;
    }

    public @NotNull ClientSession deepCopy() {
        return new ClientSession(
                connected,
                sessionExpiryIntervalSec,
                willPublish != null ? willPublish.deepCopy() : null,
                queueLimit);
    }

    public @NotNull ClientSession copyWithoutWill() {
        return new ClientSession(connected, sessionExpiryIntervalSec, null, queueLimit);
    }

    @Override
    public int getEstimatedSize() {

        if (inMemorySize != SIZE_NOT_CALCULATED) {
            return inMemorySize;
        }

        int size = MemoryEstimator.OBJECT_SHELL_SIZE;
        size += MemoryEstimator.INT_SIZE; // inMemorySize
        size += MemoryEstimator.BOOLEAN_SIZE; // connected
        size += MemoryEstimator.LONG_SIZE; // sessionExpiryInterval

        size += MemoryEstimator.OBJECT_REF_SIZE; // reference to will
        if (willPublish != null) {
            size += willPublish.getEstimatedSize();
        }
        if (queueLimit != null) {
            size += MemoryEstimator.LONG_SIZE;
        }

        inMemorySize = size;

        return inMemorySize;
    }

    public boolean isExpired(final long timeSinceDisconnectMsec) {
        if (connected) {
            return false;
        }

        return timeSinceDisconnectMsec / 1000 >= sessionExpiryIntervalSec;
    }

    @Override
    public String toString() {
        return "ClientSession{" +
                "queueLimit=" +
                queueLimit +
                ", connected=" +
                connected +
                ", sessionExpiryIntervalSec=" +
                sessionExpiryIntervalSec +
                ", inMemorySize=" +
                inMemorySize +
                ", willPublish=" +
                willPublish +
                '}';
    }
}
