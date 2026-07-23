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
package com.hivemq.protocols.v2.wrapper;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessagePriority;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseNode;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ResolvedAttributes;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.protocols.v2.fsm.FSMEvent;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A wrapper event — a protocol-adapter event reported through the tell-façade, a timer expiry generated inside
 * the tick handler, or the synthesized {@link AllVerified} gate signal. Events are the ONLY messages that flow
 * through the adapter machine's {@link com.hivemq.protocols.v2.fsm.FSMTransitionTable}; an event with no
 * matching row runs the table's defensive reset.
 * <p>
 * Band: {@link MailboxMessagePriority#EVENT} (the {@link com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage}
 * default), except {@link DataPointReceived} and the browse events ({@link BrowsePageReceived},
 * {@link AttributesResolved}, {@link BrowseFailed}), which override to
 * {@link MailboxMessagePriority#DATA} so the chatty push channel never starves control, acknowledgments, or time
 *. The timer-expiry events ({@link WatchdogFired}, {@link BackoffFired}, {@link PollTimerFired},
 * {@link VerificationRetryTimerFired}, {@link SubscriptionRetryTimerFired}) never enter the mailbox — they are
 * generated inside the tick handler on the dispatch thread and fed straight to the machine — so their band is
 * never consulted.
 */
public sealed interface ProtocolAdapterWrapperEvent extends ProtocolAdapterWrapperMessage, FSMEvent {

    /**
     * Acknowledges {@code start()}.
     */
    record Started() implements ProtocolAdapterWrapperEvent {}

    /**
     * Acknowledges {@code stop()}.
     */
    record Stopped() implements ProtocolAdapterWrapperEvent {}

    /**
     * Acknowledges {@code connect()}.
     */
    record Connected() implements ProtocolAdapterWrapperEvent {}

    /**
     * Acknowledges {@code disconnect()} — or reports a spontaneous connection loss.
     */
    record Disconnected() implements ProtocolAdapterWrapperEvent {}

    /**
     * Reports an adapter or connection failure.
     *
     * @param scope  which recovery the failure admits ({@link ErrorScope#ADAPTER} vs {@link ErrorScope#CONNECTION}).
     * @param reason a human-readable description.
     */
    record ErrorEvent(@NotNull ErrorScope scope, @NotNull String reason) implements ProtocolAdapterWrapperEvent {}

    /**
     * Reports one node's verification outcome. Feeds both the adapter gate (counting) and the node's tag aspects
     *.
     *
     * @param node    the verified node.
     * @param outcome the verification outcome.
     */
    record VerifyResultReceived(@NotNull Node node, @NotNull VerifyOutcome outcome)
            implements ProtocolAdapterWrapperEvent {}

    /**
     * The synthesized gate signal: every node in the verification batch has reported an outcome.
     * Drives {@code WAITING_FOR_VERIFICATION → CONNECTED}.
     */
    record AllVerified() implements ProtocolAdapterWrapperEvent {}

    /**
     * Reports one value — a poll response or a subscription push; the {@link Node} is the correlation key.
     *
     * @param node  the node the value belongs to.
     * @param value the reused v1 value.
     */
    record DataPointReceived(@NotNull Node node, @NotNull DataPoint value) implements ProtocolAdapterWrapperEvent {
        @Override
        public @NotNull MailboxMessagePriority priority() {
            return MailboxMessagePriority.DATA;
        }
    }

    /**
     * Reports a per-node failure (failed poll, failed or lost subscription).
     *
     * @param node        the node the failure belongs to.
     * @param reason      a human-readable description.
     * @param spontaneous {@code true} if the failure arrived outside a command-response exchange — selects the
     *                    recovery path.
     */
    record NodeErrorReceived(@NotNull Node node, @NotNull String reason, boolean spontaneous)
            implements ProtocolAdapterWrapperEvent {}

    /**
     * Acknowledges one entry of a write batch.
     *
     * @param node    the node the write targeted.
     * @param success whether the write succeeded.
     * @param reason  a human-readable description of the failure, or {@code null} on success.
     */
    record WriteResultReceived(
            @NotNull Node node, boolean success, @Nullable String reason) implements ProtocolAdapterWrapperEvent {}

    /**
     * One page of a paginated browse DISCOVER phase — fed to the wrapper's browse engine, not the adapter
     * machine.
     *
     * @param requestId    correlates the page with its browse.
     * @param entries      the discovered nodes in this page.
     * @param continuation an opaque token to fetch the next page, or {@code null} if this is the last page.
     */
    record BrowsePageReceived(
            int requestId,
            @NotNull List<BrowseNode> entries,
            @Nullable BrowseContinuation continuation) implements ProtocolAdapterWrapperEvent {
        @Override
        public @NotNull MailboxMessagePriority priority() {
            return MailboxMessagePriority.DATA;
        }
    }

    /**
     * The resolved attributes of a RESOLVE batch — fed to the wrapper's browse engine, not the adapter
     * machine.
     *
     * @param requestId  correlates the batch with its browse.
     * @param attributes the resolved attributes, one per requested node.
     */
    record AttributesResolved(int requestId, @NotNull List<ResolvedAttributes> attributes)
            implements ProtocolAdapterWrapperEvent {
        @Override
        public @NotNull MailboxMessagePriority priority() {
            return MailboxMessagePriority.DATA;
        }
    }

    /**
     * A browse DISCOVER page or RESOLVE batch failed — fed to the wrapper's browse engine, not the adapter
     * machine.
     *
     * @param requestId the browse/resolve that failed.
     * @param reason    a human-readable description of the failure.
     */
    record BrowseFailed(int requestId, @NotNull String reason) implements ProtocolAdapterWrapperEvent {
        @Override
        public @NotNull MailboxMessagePriority priority() {
            return MailboxMessagePriority.DATA;
        }
    }

    /**
     * A per-state watchdog expired: the awaited acknowledgment did not arrive in time. Generated in
     * the tick handler, never enqueued.
     */
    record WatchdogFired() implements ProtocolAdapterWrapperEvent {}

    /**
     * The connection backoff elapsed: time to attempt the next {@code connect()}. Generated in the
     * tick handler, never enqueued.
     */
    record BackoffFired() implements ProtocolAdapterWrapperEvent {}

    /**
     * A per-tag poll interval elapsed. Consumed by the tag read aspects (a later task); generated in
     * the tick handler, never enqueued.
     */
    record PollTimerFired() implements ProtocolAdapterWrapperEvent {}

    /**
     * A per-tag verification retry delay elapsed. Consumed by the tag aspects (a later task);
     * generated in the tick handler, never enqueued.
     */
    record VerificationRetryTimerFired() implements ProtocolAdapterWrapperEvent {}

    /**
     * A per-tag subscription retry backoff elapsed. Consumed by the tag read aspects (a later task);
     * generated in the tick handler, never enqueued.
     */
    record SubscriptionRetryTimerFired() implements ProtocolAdapterWrapperEvent {}
}
