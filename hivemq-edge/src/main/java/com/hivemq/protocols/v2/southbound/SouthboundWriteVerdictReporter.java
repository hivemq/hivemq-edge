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

import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Where one tag's backlog reports each command's terminal verdict — the correlation reply. The production
 * implementation publishes a <b>retained</b> JSON verdict to the command topic's {@code /result} sibling (see
 * {@link SouthboundMqttIntake}); that retained message doubles as the durable "last executed command" record that
 * {@link #lastExecutedVerdict()} recovers on startup for <b>crash-replay deduplication</b>: a crash after the
 * device acknowledged a write but before the command was deleted from the durable queue replays the command on
 * restart — the backlog compares the replayed head against the recovered verdict and, on a match, re-commits it
 * without executing it twice.
 * <p>
 * The reporter is asked to {@link #report} <b>before</b> the command is deleted from the queue, narrowing the
 * duplicate-execution window to a crash landing between the device acknowledgment and the verdict's retention —
 * the delivery contract stays at-least-once; the dedup is best-effort narrowing, not exactly-once.
 * <p>
 * Invoked on whatever thread completes the write (the wrapper's dispatch thread via the queue, or a persistence
 * thread on a self-dead-letter); implementations must not block.
 */
public interface SouthboundWriteVerdictReporter {

    /** A reporter that records nothing and recovers nothing — for tests and the interim in-memory plane. */
    @NotNull
    SouthboundWriteVerdictReporter NONE = new SouthboundWriteVerdictReporter() {
        @Override
        public void report(
                final @NotNull String commandId,
                final @NotNull SouthboundWriteOutcome outcome,
                final @Nullable String reason,
                final boolean deduplicated,
                final @Nullable String command,
                final byte @Nullable [] correlationData) {}

        @Override
        public @Nullable ExecutedVerdict lastExecutedVerdict() {
            return null;
        }
    };

    /**
     * The last command that reached a terminal outcome before a restart, recovered from the retained verdict.
     *
     * @param commandId the command's unique id.
     * @param outcome   the terminal outcome it reached.
     */
    record ExecutedVerdict(
            @NotNull String commandId, @NotNull SouthboundWriteOutcome outcome) {}

    /**
     * Report one command's terminal verdict. Called before the command is deleted from the durable queue.
     *
     * @param commandId       the command's unique id (the broker-side correlation key).
     * @param outcome         the terminal outcome ({@code SUCCEEDED} or {@code FAILED}).
     * @param reason          the failure reason, or {@code null} on success.
     * @param deduplicated    whether this verdict re-reports a command recognized as already executed (crash
     *                        replay).
     * @param command         the executed command's payload, decoded as UTF-8 — echoed so a publisher can
     *                        recognize its own command in the verdict; {@code null} when the publish carried none.
     * @param correlationData the MQTT 5 correlation data the publisher attached to the command, echoed verbatim —
     *                        the request/response correlation key the publisher chose; {@code null} when absent.
     */
    void report(
            @NotNull String commandId,
            @NotNull SouthboundWriteOutcome outcome,
            @Nullable String reason,
            boolean deduplicated,
            @Nullable String command,
            byte @Nullable [] correlationData);

    /**
     * @return the verdict of the last command that reached a terminal outcome, recovered from durable storage —
     *         or {@code null} when there is none. Consulted once, for the first lease after construction.
     */
    @Nullable
    ExecutedVerdict lastExecutedVerdict();
}
