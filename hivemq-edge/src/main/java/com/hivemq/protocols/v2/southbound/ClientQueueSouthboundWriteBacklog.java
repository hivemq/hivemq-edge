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
import com.hivemq.mqtt.message.QoS;
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
 * QoS ≥ 1 is the durability precondition, and QoS 0 is the documented exception: such a command is at-most-once by
 * broker semantics — this read hands it out and <b>removes</b> it in one step, leaving nothing to redeliver — so it
 * is delivered <b>best-effort</b>, outside every guarantee this class otherwise provides, rather than refused. A
 * command Edge can execute probably should be executed; but the delivery side is then holding the only copy, in
 * memory, so each one is logged at WARN as the operator's only notice that it will not survive an unready adapter,
 * an abandoned write, or a restart.
 * <p>
 * <b>Nothing here needs to be lossless.</b> An earlier design carried a recovery ladder — remembered wakeups,
 * evidence flags, one-shot latches — because the broker's publish-available callback is edge-triggered: it fires
 * only on the queue's 0→1 transition, so a missed wakeup stranded a durable command invisibly. Here that callback
 * is a pure latency hint and the delivery side polls as a backstop, so a lost hint costs at most one poll interval
 * and a failed submission costs nothing but a retry. That is why this class has no flags and no elaborate
 * synchronous-throw handling: a throw at submission is reported as a failed read and retried, exactly like a
 * failed future.
 * <p>
 * An <b>undeliverable</b> publish — untranslatable, meaning the {@link SouthboundPublishTranslator} returned
 * {@code null} or threw — is <b>reported with its reason</b>, not deleted here: the answer names it, and
 * the delivery side dead-letters it beside every other disposition. Recognizing one is a pure function and can run
 * wherever the answer is built; deciding a command's fate is not, and belongs on the one thread that owns that
 * decision.
 */
public final class ClientQueueSouthboundWriteBacklog implements SouthboundWriteBacklog {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ClientQueueSouthboundWriteBacklog.class);

    private static final int READ_LIMIT = 1;

    /** How many times a delete is attempted before the queue entry is declared leaked. */
    private static final int DELETE_ATTEMPTS = 3;

    /** How many times a discard is attempted before the commands it was meant to destroy are declared live. */
    private static final int DISCARD_ATTEMPTS = 3;

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
        // Start from a queue with no in-flight markers on it. A marker is what makes a command invisible — reads
        // skip marked entries — and it is removed either when the command is deleted or by the sweep in close().
        // Neither covers a marker applied AFTER that sweep: a read submitted before teardown can reach the store's
        // single writer behind it, mark its entry, and answer into a mailbox nobody drains. Nothing owns that
        // marker afterwards, and the delivery side cannot find it to release it — its own reads skip it, and its
        // depth cross-check only arms after three consecutive EMPTY reads, which a queue with other commands in it
        // never produces. The command would stay invisible until this Edge restarts.
        //
        // Sweeping here closes that regardless of how the predecessor ended: whoever comes next starts clean. It is
        // safe for the same reason close()'s sweep is — this synthetic shared queue has exactly one consumer, and
        // the previous one is fully closed before a successor is built. It also nudges the broker to re-announce
        // what the queue holds, so a recreated adapter picks up immediately rather than on its first backstop poll.
        releaseMarkers();
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
                            tell(new SouthboundRead(tagName, readToken, null, null, null, throwable));
                        }
                    },
                    MoreExecutors.directExecutor());
        } catch (final VirtualMachineError fatal) {
            throw fatal;
        } catch (final Throwable submissionFailure) {
            // The read never became a future (a rejected submission while the single-writer shuts down, say).
            // Reporting it as a failed read is all that is needed: the backstop poll retries.
            tell(new SouthboundRead(tagName, readToken, null, null, null, submissionFailure));
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
     *     non-empty store and sweeps the markers — after which the command surfaces again, and again on every later
     *     sweep, until this Edge restarts. What that <b>no longer</b> means is a second execution: the delivery
     *     side remembers the ids it has disposed of and refuses to deliver one twice, counting every sighting on
     *     {@code redeliveriesRefused}. So the device is safe and the loop is observable; what leaks is the queue
     *     entry itself, which only the store can fix.</li>
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
                                                + "attempts. The command's outcome was already decided — executed, "
                                                + "refused by the device, or dead-lettered as undeliverable — but it "
                                                + "cannot be removed from the queue, so it will keep resurfacing "
                                                + "until this Edge restarts. The delivery side refuses to execute it "
                                                + "a second time (see redeliveriesRefused), so the device is safe; "
                                                + "what leaks is the queue entry. Investigate the client-queue "
                                                + "persistence for this queue.",
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
    public void discardAll() {
        // Reported here rather than by the caller because only the store knows how many commands were actually
        // destroyed: clear() returns no count, and the delivery side knows about at most the one command it holds
        // as head. The size read is best-effort and purely for this line — nothing waits on it, and a failure to
        // obtain it must not stop the clear, which is the part that matters.
        submitQuietly(
                "read the depth of southbound queue '" + queueId + "' before discarding it",
                () -> Futures.addCallback(
                        clientQueuePersistence.size(queueId, true),
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(final @Nullable Integer size) {
                                log.warn(
                                        "Destroying {} queued southbound command(s) for tag '{}' on adapter '{}': the "
                                                + "tag now addresses a different node, and commands authored for the "
                                                + "previous one must not be executed on it.",
                                        size == null ? 0 : size,
                                        tagName,
                                        adapterId);
                            }

                            @Override
                            public void onFailure(final @NotNull Throwable throwable) {
                                log.warn(
                                        "Destroying the queued southbound commands for tag '{}' on adapter '{}' (the "
                                                + "tag now addresses a different node); the queue depth could not be "
                                                + "read, so the count is unknown",
                                        tagName,
                                        adapterId,
                                        throwable);
                            }
                        },
                        MoreExecutors.directExecutor()));
        discardAll(DISCARD_ATTEMPTS);
    }

    /**
     * Destroy the queue's contents, retrying a bounded number of times.
     * <p>
     * The retry is not housekeeping — it defends a <b>safety</b> guarantee. By the time this runs the delivery side
     * has already dropped its head and re-pointed at the new node, and the rebuilt aspect will reopen the window as
     * soon as it verifies. If the clear silently fails, the very next read returns a command authored for the
     * <i>previous</i> node and executes it on the new one: exactly the outcome the ruling on this case exists to
     * prevent, reached again with nothing but a log line to show for it. A destroyed-commands guarantee that
     * degrades quietly into writing to the wrong device is worse than no guarantee, because nobody looks.
     * <p>
     * Bounded for the same reason {@link #delete} is: a store that refuses every attempt is broken, and saying so is
     * more useful than retrying into it forever. The attempts share {@code delete}'s honest limitation — they are
     * not spaced, so they recover a transient race rather than a persistently failing store.
     *
     * @param attemptsLeft how many attempts remain, this one included.
     */
    private void discardAll(final int attemptsLeft) {
        submitQuietly(
                "discard the contents of southbound queue '" + queueId + "'",
                () -> Futures.addCallback(
                        clientQueuePersistence.clear(queueId, true),
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(final @Nullable Void ignored) {}

                            @Override
                            public void onFailure(final @NotNull Throwable throwable) {
                                if (attemptsLeft > 1) {
                                    log.debug(
                                            "Retrying the discard of southbound queue '{}' for tag '{}' on adapter "
                                                    + "'{}'",
                                            queueId,
                                            tagName,
                                            adapterId,
                                            throwable);
                                    discardAll(attemptsLeft - 1);
                                    return;
                                }
                                log.error(
                                        "Failed to discard the queued southbound commands of tag '{}' on adapter '{}' "
                                                + "after {} attempts. The tag now addresses a DIFFERENT node, and "
                                                + "those commands were authored for the previous one — they are still "
                                                + "queued and WILL be executed on the new node. Quiesce the command "
                                                + "topic and clear this queue by hand, and investigate the "
                                                + "client-queue persistence.",
                                        tagName,
                                        adapterId,
                                        DISCARD_ATTEMPTS,
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
            tell(new SouthboundRead(tagName, readToken, null, null, null, null));
            return;
        }
        final PUBLISH publish = publishes.getFirst(); // READ_LIMIT = 1
        if (publish.getQoS() == QoS.AT_MOST_ONCE) {
            // Delivered, but explicitly outside the durability guarantee — a deliberate product choice: a command
            // Edge can execute probably should be executed, and refusing it helps nobody.
            //
            // What the publisher gave up by choosing QoS 0 is real and cannot be recovered here. The broker stores
            // such a publish on the queue's at-most-once side, hands it out and REMOVES it in this very read — no
            // in-flight marker, nothing left to redeliver. So if the adapter is not ready, or the write is
            // abandoned, or Edge stops before it lands, the command is simply gone: the delivery side is holding
            // the only copy, in memory. Every other command in this path survives all three.
            //
            // Hence the warning on each one. It is the operator's only notice that this particular command carries
            // none of the guarantees the rest of the path advertises.
            log.warn(
                    "Southbound command '{}' for tag '{}' on adapter '{}' was published at QoS 0: delivering it "
                            + "best-effort, but it is NOT durable — it is already gone from the queue, so it will "
                            + "not survive an adapter outage or an Edge restart. Publish at QoS 1 or 2 for the "
                            + "at-least-once guarantee.",
                    publish.getUniqueId(),
                    tagName,
                    adapterId);
        }
        final DataPoint value = translate(publish);
        if (value == null) {
            tell(new SouthboundRead(
                    tagName, readToken, null, publish.getUniqueId(), "the command could not be decoded", null));
            return;
        }
        tell(new SouthboundRead(
                tagName, readToken, new SouthboundCommand(publish.getUniqueId(), value), null, null, null));
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
