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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.util.FutureUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The production {@link SouthboundWriteBacklog}: the durable MQTT client queue itself, adapted to the backlog's
 * synchronous head-without-remove contract by <b>prefetching</b>. {@link ClientQueuePersistence} is asynchronous
 * (futures through the single-writer), so this backlog leases the queue's head ahead of time — one
 * {@code readShared(limit 1)} marks it in-flight and caches it — and {@link #head()} serves the cached lease
 * idempotently until a terminal outcome deletes it ({@code removeShared}) and the next prefetch begins. An
 * abandoned command needs no call, exactly as the contract says: the lease simply stays cached, redelivered when
 * the delivering queue resumes.
 * <p>
 * Durability and at-least-once come from the client queue: a command is {@code removeShared}-deleted only on a
 * terminal outcome, and a crash discards the in-memory lease while the message stays queued — the restarted Edge
 * reads it again (the in-flight marker does not outlive the process; QoS 0 commands are removed on read by the
 * broker and are therefore at-most-once — QoS ≥ 1 is the durability precondition).
 * <p>
 * An <b>untranslatable</b> publish (the {@link SouthboundPublishTranslator} returns {@code null} or throws) is
 * dead-lettered by the backlog itself — removed and logged, so a malformed command never wedges the tag; routing
 * it aside to {@code $invalid/…} arrives with the MQTT-intake task. A failed {@code readShared} is logged at
 * {@code ERROR} and <b>not</b> retried in place (an immediate retry on the direct executor could spin); the next
 * arriving command re-triggers the prefetch via the publish-available callback.
 * <p>
 * Thread-safety: {@link #head()}/{@link #removeHead}/{@link #deadLetterHead} run under the delivering queue's
 * monitor (lock order queue→backlog); the read callback and the publish-available callback run on persistence
 * threads. All state is guarded by this backlog's monitor, and both the registered wakeup and every
 * {@link ClientQueuePersistence} call are made <b>outside</b> it.
 */
public final class ClientQueueSouthboundWriteBacklog implements SouthboundWriteBacklog {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ClientQueueSouthboundWriteBacklog.class);

    private static final int READ_LIMIT = 1;

    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull String queueId;
    private final @NotNull SouthboundPublishTranslator translator;
    private final @NotNull String adapterId;
    private final @NotNull String tagName;

    private @Nullable Runnable wakeup;
    private @Nullable SouthboundCommand head;
    private boolean fetching;
    private boolean closed;

    /**
     * Registers on the queue's publish-available callback and prefetches immediately, so commands already queued
     * (e.g. across a restart) surface without waiting for a new arrival.
     *
     * @param clientQueuePersistence the durable client queue store.
     * @param queueId                the shared-subscription queue id this tag's commands arrive on.
     * @param translator             turns a queued publish into the value to write.
     * @param adapterId              the owning adapter's id, for logging.
     * @param tagName                the tag this backlog feeds, for logging.
     */
    public ClientQueueSouthboundWriteBacklog(
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull String queueId,
            final @NotNull SouthboundPublishTranslator translator,
            final @NotNull String adapterId,
            final @NotNull String tagName) {
        this.clientQueuePersistence = clientQueuePersistence;
        this.queueId = queueId;
        this.translator = translator;
        this.adapterId = adapterId;
        this.tagName = tagName;
        clientQueuePersistence.addPublishAvailableCallback(ignored -> prefetch(), queueId);
        prefetch();
    }

    @Override
    public synchronized @Nullable SouthboundCommand head() {
        return head;
    }

    @Override
    public void removeHead(final @NotNull String id) {
        deleteHead(id);
    }

    @Override
    public void deadLetterHead(final @NotNull String id, final @NotNull String reason) {
        log.warn(
                "Dead-lettering southbound command '{}' for tag '{}' on adapter '{}': {}",
                id,
                tagName,
                adapterId,
                reason);
        deleteHead(id);
    }

    @Override
    public synchronized void onAvailable(final @NotNull Runnable wakeup) {
        this.wakeup = wakeup;
    }

    /**
     * Deregister from the queue's publish-available callback and drop the cached lease. The queue itself is left
     * untouched — it is durable, and a successor backlog (or a restarted Edge) picks its contents up.
     */
    @Override
    public void close() {
        synchronized (this) {
            closed = true;
            head = null;
        }
        clientQueuePersistence.removePublishAvailableCallback(queueId);
    }

    private void deleteHead(final @NotNull String id) {
        synchronized (this) {
            if (head == null || !id.equals(head.id())) {
                throw new IllegalStateException("dispose of a command that is not the head: " + id);
            }
            head = null;
        }
        FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(queueId, id));
        prefetch();
    }

    /**
     * Lease the queue's head if nothing is cached and no read is in flight — a no-op otherwise. Safe to call
     * repeatedly; triggered at construction, by the publish-available callback, and after each deletion.
     */
    private void prefetch() {
        synchronized (this) {
            if (closed || fetching || head != null) {
                return;
            }
            fetching = true;
        }
        final ListenableFuture<ImmutableList<PUBLISH>> read = clientQueuePersistence.readShared(
                queueId, READ_LIMIT, InternalConfigurations.PUBLISH_POLL_BATCH_SIZE_BYTES);
        Futures.addCallback(
                read,
                new FutureCallback<>() {
                    @SuppressWarnings("NullAway") // Guava FutureCallback.onSuccess has @Nullable param
                    @Override
                    public void onSuccess(final ImmutableList<PUBLISH> publishes) {
                        onRead(publishes);
                    }

                    @Override
                    public void onFailure(final @NotNull Throwable throwable) {
                        onReadFailure(throwable);
                    }
                },
                MoreExecutors.directExecutor());
    }

    private void onRead(final @Nullable ImmutableList<PUBLISH> publishes) {
        if (publishes == null || publishes.isEmpty()) {
            synchronized (this) {
                fetching = false;
            }
            return;
        }
        final PUBLISH publish = publishes.get(0); // READ_LIMIT = 1
        final DataPoint value = translate(publish);
        if (value == null) {
            // Untranslatable: dead-letter it here and lease the next — a malformed command never wedges the tag.
            synchronized (this) {
                fetching = false;
            }
            log.warn(
                    "Southbound publish '{}' on topic '{}' for tag '{}' on adapter '{}' is untranslatable — "
                            + "dead-lettered",
                    publish.getUniqueId(),
                    publish.getTopic(),
                    tagName,
                    adapterId);
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(queueId, publish.getUniqueId()));
            prefetch();
            return;
        }
        final Runnable nudge;
        synchronized (this) {
            fetching = false;
            if (closed) {
                return;
            }
            head = new SouthboundCommand(publish.getUniqueId(), value);
            nudge = wakeup;
        }
        if (nudge != null) {
            nudge.run();
        }
    }

    private void onReadFailure(final @NotNull Throwable throwable) {
        synchronized (this) {
            fetching = false;
            if (closed) {
                return;
            }
        }
        // No in-place retry: an immediate retry on the direct executor could spin on a persistent failure. The next
        // arriving command re-triggers the prefetch via the publish-available callback.
        log.error(
                "Failed to read the southbound queue '{}' for tag '{}' on adapter '{}' — will retry on the next "
                        + "arriving command",
                queueId,
                tagName,
                adapterId,
                throwable);
    }

    private @Nullable DataPoint translate(final @NotNull PUBLISH publish) {
        try {
            return translator.translate(publish);
        } catch (final RuntimeException failure) {
            log.debug("Southbound publish translation threw", failure);
            return null;
        }
    }
}
