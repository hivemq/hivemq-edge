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
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundArrival;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundRead;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundSize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The production {@link SouthboundWriteBacklog}: the durable MQTT client queue, exposed as intents whose answers
 * come back as mailbox messages.
 * <p>
 * This class holds <b>no delivery state</b> — no cached head, no in-flight flag, no recovery latches. It is a
 * translator between two vocabularies: {@link ClientQueuePersistence}'s futures and callbacks on one side, and the
 * wrapper's mailbox on the other. Every completion it observes does exactly one thing — build a message and
 * {@code tell} it — so all reasoning about what to do next happens on the dispatch thread, in
 * {@link SouthboundWriteQueue}, as ordinary single-threaded code.
 * <p>
 * "Completion" rather than "the persistence thread" on purpose: the callbacks run on {@code directExecutor()}, and
 * the in-memory single writer executes an uncontended submission inline, so a read frequently answers on the
 * <b>dispatch thread</b>, re-entrantly, inside the very call that requested it. That is safe precisely because the
 * shim does nothing but {@code tell} — an enqueue, never a dispatch — and because the requesting side records its
 * token before issuing the request. Put any logic in these callbacks and that stops holding.
 * <p>
 * A read leases the queue head without removing it ({@code readShared} marks it in-flight); only a terminal outcome
 * {@code removeShared}-deletes it. That is the at-least-once contract: a crash discards nothing but the lease — the
 * message stays queued and the restarted Edge reads it again. Note that the marker itself is <b>durable</b>, not
 * in-memory: the Xodus client queue writes the packet id into the store. What clears it is the successor process's
 * bootstrap, which resets every shared-queue marker on load; that is the step the at-least-once argument actually
 * rests on, so a persistence implementation that skipped it would strand every leased head across a restart.
 * QoS 0 commands are removed on read by broker semantics and are therefore at-most-once; QoS ≥ 1 is the durability
 * precondition.
 * <p>
 * <b>Nothing here needs to be lossless.</b> An earlier design carried a recovery ladder — remembered wakeups,
 * evidence flags, one-shot latches — because the broker's publish-available callback is edge-triggered: it fires
 * only on the queue's 0→1 transition, so a missed wakeup stranded a durable command invisibly. Here that callback
 * is a pure latency hint and the delivery side polls as a backstop, so a lost hint costs at most one poll interval
 * and a failed submission costs nothing but a retry. That is why this class has no flags and no elaborate
 * synchronous-throw handling: a throw at submission is reported as a failed read and retried, exactly like a
 * failed future.
 * <p>
 * An <b>untranslatable</b> publish (the {@link SouthboundPublishTranslator} returns {@code null} or throws) is
 * <b>reported</b>, not deleted here: the answer names it, and the delivery side dead-letters it beside every other
 * disposition. Decoding is a pure function and can run wherever the answer is built; deciding a command's fate is
 * not, and belongs on the one thread that owns that decision.
 */
public final class ClientQueueSouthboundWriteBacklog implements SouthboundWriteBacklog {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ClientQueueSouthboundWriteBacklog.class);

    private static final int READ_LIMIT = 1;

    /** How many times a delete is attempted before the queue entry is declared leaked. */
    private static final int DELETE_ATTEMPTS = 3;

    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull String queueId;
    private final @NotNull SouthboundPublishTranslator translator;
    private final @NotNull String adapterId;
    private final @NotNull String tagName;
    private final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender;

    /**
     * Registers the arrival hint on the queue's publish-available callback. No initial read is issued here: the
     * delivery side polls on the wrapper's own tick, so commands already queued (across a restart, say) surface
     * there like any other.
     *
     * @param clientQueuePersistence the durable client queue store.
     * @param queueId                the shared-subscription queue id this tag's commands arrive on.
     * @param translator             turns a queued publish into the value to write.
     * @param adapterId              the owning adapter's id, for logging.
     * @param tagName                the tag this backlog feeds — the key every answer is addressed to.
     * @param wrapperSender          the wrapper mailbox every answer is told to.
     */
    public ClientQueueSouthboundWriteBacklog(
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull String queueId,
            final @NotNull SouthboundPublishTranslator translator,
            final @NotNull String adapterId,
            final @NotNull String tagName,
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender) {
        this.clientQueuePersistence = clientQueuePersistence;
        this.queueId = queueId;
        this.translator = translator;
        this.adapterId = adapterId;
        this.tagName = tagName;
        this.wrapperSender = wrapperSender;
        clientQueuePersistence.addPublishAvailableCallback(ignored -> tell(new SouthboundArrival(tagName)), queueId);
    }

    @Override
    public void requestRead(final long readToken) {
        try {
            Futures.addCallback(
                    clientQueuePersistence.readShared(
                            queueId, READ_LIMIT, InternalConfigurations.PUBLISH_POLL_BATCH_SIZE_BYTES),
                    new FutureCallback<>() {
                        @SuppressWarnings("NullAway") // Guava FutureCallback.onSuccess has @Nullable param
                        @Override
                        public void onSuccess(final ImmutableList<PUBLISH> publishes) {
                            answerRead(readToken, publishes);
                        }

                        @Override
                        public void onFailure(final @NotNull Throwable throwable) {
                            tell(new SouthboundRead(tagName, readToken, null, null, throwable));
                        }
                    },
                    MoreExecutors.directExecutor());
        } catch (final VirtualMachineError fatal) {
            throw fatal;
        } catch (final Throwable submissionFailure) {
            // The read never became a future (a rejected submission while the single-writer shuts down, say).
            // Reporting it as a failed read is all that is needed: the backstop poll retries.
            tell(new SouthboundRead(tagName, readToken, null, null, submissionFailure));
        }
    }

    @Override
    public void requestSize(final long readToken) {
        try {
            Futures.addCallback(
                    clientQueuePersistence.size(queueId, true),
                    new FutureCallback<>() {
                        @SuppressWarnings("NullAway") // Guava FutureCallback.onSuccess has @Nullable param
                        @Override
                        public void onSuccess(final Integer size) {
                            tell(new SouthboundSize(tagName, readToken, size == null ? 0 : size, null));
                        }

                        @Override
                        public void onFailure(final @NotNull Throwable throwable) {
                            tell(new SouthboundSize(tagName, readToken, 0, throwable));
                        }
                    },
                    MoreExecutors.directExecutor());
        } catch (final VirtualMachineError fatal) {
            throw fatal;
        } catch (final Throwable submissionFailure) {
            tell(new SouthboundSize(tagName, readToken, 0, submissionFailure));
        }
    }

    @Override
    public void delete(final @NotNull String commandId) {
        delete(commandId, DELETE_ATTEMPTS);
    }

    /**
     * Delete a command, retrying a bounded number of times.
     * <p>
     * The retry is what keeps this from leaking. A delete that fails leaves the command queued <b>with the in-flight
     * marker this channel's read set still on it</b>, and {@code readShared} skips marked entries — so every later
     * read returns the command <i>behind</i> it, not this one. The channel's empty-read ladder, which is what
     * releases an ownerless marker, arms only after three consecutive <b>empty</b> reads, and a tag with steady
     * traffic never produces one. Without a retry the entry would therefore survive until the process restarts and
     * its bootstrap sweep clears the marker: a durable leak on exactly the tags that are busiest.
     * <p>
     * Bounded rather than indefinite, because a store that refuses every attempt is a broken store and saying so is
     * more useful than retrying into it forever. Two honest limitations:
     * <ul>
     * <li><b>The attempts are not spaced.</b> The callbacks run on {@code directExecutor()}, and an uncontended
     *     submission to the in-memory single writer completes inline — so all three attempts can execute in one
     *     stack against identical store state. They recover a transient race, not a persistently failing entry.</li>
     * <li><b>The consequence of exhausting them is not an inert queue entry.</b> The entry stays marked, so reads
     *     skip it and the queue appears to drain; three empty reads then arm the depth check, which finds a
     *     non-empty store and sweeps the markers — after which the command is read again and <b>written to the
     *     device again</b>, settles, fails to delete again, and the cycle repeats every few seconds. At-least-once
     *     permits the duplicate execution; what is missing is a signal, so the {@code ERROR} below is the only
     *     thing an operator has. Ending that loop needs a way to refuse a command whose delete is known to fail,
     *     which the store cannot decide on its own.</li>
     * </ul>
     *
     * @param commandId     the command to delete.
     * @param attemptsLeft  how many attempts remain, this one included.
     */
    private void delete(final @NotNull String commandId, final int attemptsLeft) {
        submitQuietly(
                "delete southbound command '" + commandId + "'",
                () -> Futures.addCallback(
                        clientQueuePersistence.removeShared(queueId, commandId),
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(final @Nullable Void ignored) {}

                            @Override
                            public void onFailure(final @NotNull Throwable throwable) {
                                if (attemptsLeft > 1) {
                                    log.debug(
                                            "Retrying the delete of southbound command '{}' for tag '{}' on adapter '{}'",
                                            commandId,
                                            tagName,
                                            adapterId,
                                            throwable);
                                    delete(commandId, attemptsLeft - 1);
                                    return;
                                }
                                log.error(
                                        "Gave up deleting southbound command '{}' for tag '{}' on adapter '{}' after {} "
                                                + "attempts. The command WAS executed, but it cannot be removed from "
                                                + "the queue: expect it to be delivered and executed on the device "
                                                + "again every few seconds until this Edge restarts. Investigate the "
                                                + "client-queue persistence for this queue.",
                                        commandId,
                                        tagName,
                                        adapterId,
                                        DELETE_ATTEMPTS,
                                        throwable);
                            }
                        },
                        MoreExecutors.directExecutor()));
    }

    @Override
    public void releaseMarkers() {
        submitQuietly(
                "release the in-flight markers of southbound queue '" + queueId + "'",
                () -> FutureUtils.addExceptionLogger(clientQueuePersistence.removeAllInFlightMarkers(queueId)));
    }

    /**
     * Deregister the arrival hint and release every in-flight marker on this queue. The unconditional sweep is what
     * makes teardown safe without sharing any state with the dispatch thread: it covers both the head this backlog
     * had leased and the lease of a read still in flight at close time, either of which would otherwise be
     * invisible to a successor backlog in the same process (an adapter recreate) and strand its command until a
     * full restart.
     * <p>
     * Sweeping the whole queue cannot disturb a successor that has already leased something: this synthetic shared
     * queue has exactly one consumer, so a transiently un-marked command can only be re-leased by that consumer,
     * which holds it behind its own head gate and deletes it on disposal.
     * <p>
     * The queue and its contents are deliberately untouched — they are the durability.
     */
    @Override
    public void close() {
        submitQuietly(
                "deregister the arrival callback of southbound queue '" + queueId + "'",
                () -> clientQueuePersistence.removePublishAvailableCallback(queueId));
        releaseMarkers();
    }

    /**
     * Turn the read's result into one answer message. Everything this does is a pure function of the result: decode
     * the payload, or report that it could not be decoded. It decides nothing — an untranslatable publish is
     * <b>reported</b>, not deleted here, so that disposing of a command remains the delivery side's job and happens
     * where every other disposition happens.
     */
    private void answerRead(final long readToken, final @Nullable ImmutableList<PUBLISH> publishes) {
        if (publishes == null || publishes.isEmpty()) {
            tell(new SouthboundRead(tagName, readToken, null, null, null));
            return;
        }
        final PUBLISH publish = publishes.getFirst(); // READ_LIMIT = 1
        final DataPoint value = translate(publish);
        if (value == null) {
            tell(new SouthboundRead(tagName, readToken, null, publish.getUniqueId(), null));
            return;
        }
        tell(new SouthboundRead(tagName, readToken, new SouthboundCommand(publish.getUniqueId(), value), null, null));
    }

    private @Nullable DataPoint translate(final @NotNull PUBLISH publish) {
        try {
            return translator.translate(publish);
        } catch (final StackOverflowError failure) {
            // A pathological payload (absurd nesting, say) — attributable to the command, and the stack has
            // unwound by the time we are here: treat it as any other untranslatable publish.
            log.debug("Southbound publish translation threw", failure);
            return null;
        } catch (final VirtualMachineError fatal) {
            throw fatal; // OOM and friends are not the command's fault — never swallow those
        } catch (final Throwable failure) {
            // Not just RuntimeException: an Error or a sneak-thrown checked exception escaping into the future
            // listener would leave this read unanswered, and the tag waiting for an answer that never comes.
            log.debug("Southbound publish translation threw", failure);
            return null;
        }
    }

    /**
     * Run one persistence interaction whose outcome nobody waits for, turning a synchronous throw into a log line.
     * These are the calls with no answer message to carry a failure, and none of them may throw: two run on the
     * dispatch thread, where an escaping throwable would fault the whole adapter, and {@link #close()} runs on the
     * manager's thread mid-teardown, where it would abandon the steps after it.
     */
    private void submitQuietly(final @NotNull String what, final @NotNull Runnable interaction) {
        try {
            interaction.run();
        } catch (final VirtualMachineError fatal) {
            throw fatal;
        } catch (final Throwable failure) {
            log.warn("Failed to {} for tag '{}' on adapter '{}'", what, tagName, adapterId, failure);
        }
    }

    /** Hand an answer to the dispatch thread. Never throws: a dead mailbox must not break a persistence thread. */
    private void tell(final @NotNull ProtocolAdapterWrapperMessage message) {
        try {
            wrapperSender.tell(message);
        } catch (final VirtualMachineError fatal) {
            throw fatal;
        } catch (final Throwable failure) {
            log.warn(
                    "Failed to hand a southbound answer to the wrapper of tag '{}' on adapter '{}'",
                    tagName,
                    adapterId,
                    failure);
        }
    }
}
