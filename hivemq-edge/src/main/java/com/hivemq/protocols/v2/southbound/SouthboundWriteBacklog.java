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

import org.jetbrains.annotations.NotNull;

/**
 * The durable store of southbound commands for one tag — the system of record behind a
 * {@link SouthboundWriteQueue}. In production this is the broker's MQTT client queue; the delivery channel in
 * front of the write aspect asks it for the head command and tells it what became of that command.
 * <p>
 * <b>Every method here records an intent and returns immediately.</b> Nothing returns a result, nothing blocks,
 * and nothing takes a callback: the answers to {@link #requestRead} and {@link #requestSize} arrive later as
 * {@code ProtocolAdapterWrapperSouthboundMessage} mailbox messages, on the wrapper's dispatch thread, where all
 * delivery state lives. That is the whole point of the shape — the store's asynchrony is confined to a shim that
 * turns a completed future into a message and touches no delivery state, so the delivery logic itself reads as
 * ordinary single-threaded code.
 * <p>
 * The durability contract is unchanged and is still the one rule everything rests on: <b>a command is deleted only
 * on a terminal outcome</b>. A read leases the head without removing it, {@link #delete} is called on commit and on
 * dead-letter, and an <b>abandoned</b> command needs no call at all — it was never removed, so it is still the head
 * and is read again when delivery resumes.
 * <p>
 * Implementations must not throw <b>on a store failure</b>: a store that cannot submit an operation reports it in
 * the answer message, or logs it where there is no answer, and lets the backstop poll retry. Throwing into the
 * dispatch thread would fault the whole adapter for a hiccup on one tag. A caller that violates the contract — for
 * example deleting a command it does not hold as head — may throw, since that is a bug rather than a fault.
 */
public interface SouthboundWriteBacklog extends AutoCloseable {

    /**
     * Ask for the head command, leasing it without removing it. Answered with a
     * {@code SouthboundRead} message carrying this token — a command, an empty result, or a failure.
     *
     * @param readToken the caller's correlation for this read; echoed in the answer so a stale one is detectable.
     */
    void requestRead(long readToken);

    /**
     * Ask how many commands the queue holds — the cross-check that catches a queue reading empty while it still
     * holds commands, which is what a head hidden behind an ownerless in-flight marker looks like. Answered with a
     * {@code SouthboundSize} message carrying this token.
     *
     * @param readToken the caller's correlation for this check; echoed in the answer.
     */
    void requestSize(long readToken);

    /**
     * Delete a command — the commit that ends its at-least-once journey, or the dead-letter of one the device
     * refused. Fire-and-forget: a failure is logged, and because the command was not in fact removed, the backstop
     * poll finds it again and at-least-once still holds.
     *
     * @param commandId the id of the command to delete, as carried by the leased publish.
     */
    void delete(@NotNull String commandId);

    /**
     * Release every in-flight marker on this tag's queue. Used for two things: recovering a head hidden behind an
     * ownerless marker (a read that failed after marking), and teardown, where it subsumes releasing both the
     * leased head and the lease of any read still in flight.
     */
    void releaseMarkers();

    /**
     * Destroy every command this tag's queue holds — the <b>only</b> operation here that discards commands wholesale,
     * and the one exception to "deleted only on a terminal outcome".
     * <p>
     * It exists for exactly one situation: the tag has been <b>re-pointed at a different node</b>. Everything queued
     * was authored against the old target, and the queue is keyed by the mapping <i>topic</i>, never by the node — so
     * without this the successor reads the very same commands back and executes them on a device the operator never
     * addressed. In industrial control that is the dangerous failure, worse than losing the commands, which is why
     * the ruling is to destroy them and say so rather than deliver them onward.
     * <p>
     * Fire-and-forget like everything else here; a failure is logged. The caller is responsible for the observable
     * record (the dead-letter count and the warning), because only it knows which tag and which node change caused
     * this.
     */
    void discardAll();

    /**
     * Release whatever this backlog holds beyond the stored commands — its arrival callback, and any lease it is
     * still holding. A durable backlog's <b>storage</b> is deliberately untouched: it outlives the backlog object
     * by design (that is the durability), and a successor picks its contents up.
     * <p>
     * This is the one method callable from a thread other than the wrapper's dispatch thread — the manager's,
     * during teardown — so implementations must keep it free of delivery state.
     */
    @Override
    void close();
}
