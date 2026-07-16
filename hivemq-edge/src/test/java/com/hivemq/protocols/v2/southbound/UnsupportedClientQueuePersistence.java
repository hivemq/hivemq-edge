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
package com.hivemq.protocols.v2.southbound;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link ClientQueuePersistence} base whose every method throws — test fakes extend it and override only the
 * shared-subscription surface the southbound backlog uses, so an unexpected call fails loudly instead of
 * silently succeeding.
 */
abstract class UnsupportedClientQueuePersistence implements ClientQueuePersistence {

    @Override
    public @NotNull ListenableFuture<Void> add(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull PUBLISH publish,
            final boolean retained,
            final long queueLimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<Void> add(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull List<PUBLISH> publishes,
            final boolean retained,
            final long queueLimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<ImmutableList<PUBLISH>> readNew(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull ImmutableIntArray packetIds,
            final long byteLimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<ImmutableList<PUBLISH>> peek(
            final @NotNull String queueId, final boolean shared, final long byteLimit, final int maxMessages) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<ImmutableList<MessageWithID>> readInflight(
            final @NotNull String client, final long byteLimit, final int messageLimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<Void> remove(final @NotNull String client, final int packetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<Void> putPubrel(final @NotNull String client, final int packetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<Void> clear(final @NotNull String queueId, final boolean shared) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<Void> closeDB() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<Void> cleanUp(final int bucketIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<Integer> size(final @NotNull String queueId, final boolean shared) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<ImmutableList<PUBLISH>> readShared(
            final @NotNull String sharedSubscription, final int messageLimit, final long byteLimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<Void> removeShared(
            final @NotNull String sharedSubscription, final @NotNull String uniqueId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<Void> removeInFlightMarker(
            final @NotNull String sharedSubscription, final @NotNull String uniqueId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<Void> removeAllInFlightMarkers(final @NotNull String sharedSubscription) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListenableFuture<Void> removeAllQos0Messages(final @NotNull String queueId, final boolean shared) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void publishAvailable(final @NotNull String client) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sharedPublishAvailable(final @NotNull String sharedSubscription) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPublishAvailableCallback(
            final @NotNull PublishAvailableCallback callback, final @NotNull String queueId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePublishAvailableCallback(final @NotNull String queueId) {
        throw new UnsupportedOperationException();
    }
}
