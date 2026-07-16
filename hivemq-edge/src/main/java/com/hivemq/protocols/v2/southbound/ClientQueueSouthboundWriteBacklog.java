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
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
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
    private final @NotNull SouthboundWriteVerdictReporter verdictReporter;
    private final @NotNull String adapterId;
    private final @NotNull String tagName;

    private @Nullable Runnable wakeup;
    private @Nullable SouthboundCommand head;
    private @Nullable String headCommand;
    private byte @Nullable [] headCorrelationData;
    private boolean fetching;
    private boolean closed;

    /**
     * The crash-replay dedup candidate: the last verdict recovered from durable storage, compared against the
     * <b>first</b> lease only (a crash leaves at most one executed-but-uncommitted command, and it is the head).
     * Cleared after that first comparison, matched or not.
     */
    private @Nullable SouthboundWriteVerdictReporter.ExecutedVerdict pendingDedup;

    /**
     * A backlog that keeps no verdicts — see the reporting constructor.
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
        this(clientQueuePersistence, queueId, translator, SouthboundWriteVerdictReporter.NONE, adapterId, tagName);
    }

    /**
     * Registers on the queue's publish-available callback and prefetches immediately, so commands already queued
     * (e.g. across a restart) surface without waiting for a new arrival. The reporter's recovered verdict primes
     * the crash-replay dedup for the first lease.
     *
     * @param clientQueuePersistence the durable client queue store.
     * @param queueId                the shared-subscription queue id this tag's commands arrive on.
     * @param translator             turns a queued publish into the value to write.
     * @param verdictReporter        where each command's terminal verdict is reported (the correlation reply), and
     *                               where the last executed command is recovered from for crash-replay dedup.
     * @param adapterId              the owning adapter's id, for logging.
     * @param tagName                the tag this backlog feeds, for logging.
     */
    public ClientQueueSouthboundWriteBacklog(
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull String queueId,
            final @NotNull SouthboundPublishTranslator translator,
            final @NotNull SouthboundWriteVerdictReporter verdictReporter,
            final @NotNull String adapterId,
            final @NotNull String tagName) {
        this.clientQueuePersistence = clientQueuePersistence;
        this.queueId = queueId;
        this.translator = translator;
        this.verdictReporter = verdictReporter;
        this.adapterId = adapterId;
        this.tagName = tagName;
        this.pendingDedup = verdictReporter.lastExecutedVerdict();
        clientQueuePersistence.addPublishAvailableCallback(ignored -> prefetch(), queueId);
        prefetch();
    }

    @Override
    public synchronized @Nullable SouthboundCommand head() {
        return head;
    }

    @Override
    public void removeHead(final @NotNull String id) {
        deleteHead(id, SouthboundWriteOutcome.SUCCEEDED, null);
    }

    @Override
    public void deadLetterHead(final @NotNull String id, final @NotNull String reason) {
        log.warn(
                "Dead-lettering southbound command '{}' for tag '{}' on adapter '{}': {}",
                id,
                tagName,
                adapterId,
                reason);
        deleteHead(id, SouthboundWriteOutcome.FAILED, reason);
    }

    @Override
    public synchronized void onAvailable(final @NotNull Runnable wakeup) {
        this.wakeup = wakeup;
    }

    /**
     * Deregister from the queue's publish-available callback and drop the cached lease, <b>clearing its in-flight
     * marker</b>: a leased-but-undisposed head would otherwise stay invisible to a successor backlog in the same
     * process (an adapter recreate), stranding the command until a full restart. The queue itself is left
     * untouched — it is durable, and a successor backlog (or a restarted Edge) picks its contents up.
     */
    @Override
    public void close() {
        final SouthboundCommand leased;
        synchronized (this) {
            closed = true;
            leased = head;
            head = null;
            headCommand = null;
            headCorrelationData = null;
        }
        clientQueuePersistence.removePublishAvailableCallback(queueId);
        if (leased != null) {
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeInFlightMarker(queueId, leased.id()));
        }
    }

    private void deleteHead(
            final @NotNull String id, final @NotNull SouthboundWriteOutcome outcome, final @Nullable String reason) {
        final String command;
        final byte[] correlationData;
        synchronized (this) {
            if (head == null || !id.equals(head.id())) {
                throw new IllegalStateException("dispose of a command that is not the head: " + id);
            }
            command = headCommand;
            correlationData = headCorrelationData;
            head = null;
            headCommand = null;
            headCorrelationData = null;
        }
        // The verdict is retained BEFORE the delete: a crash between the two replays the command, and the retained
        // verdict is what recognizes and re-commits it instead of executing it twice.
        verdictReporter.report(id, outcome, reason, false, command, correlationData);
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
        // Crash-replay dedup, first lease only: a crash between the device acknowledgment and the queue delete
        // replays an already-executed command — recognize it by the recovered verdict, re-commit it, and re-report
        // its verdict flagged as deduplicated instead of executing it twice.
        final SouthboundWriteVerdictReporter.ExecutedVerdict dedupCandidate;
        synchronized (this) {
            dedupCandidate = pendingDedup;
            pendingDedup = null;
        }
        if (dedupCandidate != null && dedupCandidate.commandId().equals(publish.getUniqueId())) {
            synchronized (this) {
                fetching = false;
            }
            log.warn(
                    "Southbound command '{}' for tag '{}' on adapter '{}' already reached {} before the restart — "
                            + "re-committing without executing it again (crash-replay dedup)",
                    publish.getUniqueId(),
                    tagName,
                    adapterId,
                    dedupCandidate.outcome());
            verdictReporter.report(
                    publish.getUniqueId(),
                    dedupCandidate.outcome(),
                    null,
                    true,
                    payloadOf(publish),
                    publish.getCorrelationData());
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(queueId, publish.getUniqueId()));
            prefetch();
            return;
        }
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
            verdictReporter.report(
                    publish.getUniqueId(),
                    SouthboundWriteOutcome.FAILED,
                    "untranslatable payload",
                    false,
                    payloadOf(publish),
                    publish.getCorrelationData());
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
            headCommand = payloadOf(publish);
            headCorrelationData = publish.getCorrelationData();
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

    private static @Nullable String payloadOf(final @NotNull PUBLISH publish) {
        final byte[] payload = publish.getPayload();
        return payload == null ? null : new String(payload, java.nio.charset.StandardCharsets.UTF_8);
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
