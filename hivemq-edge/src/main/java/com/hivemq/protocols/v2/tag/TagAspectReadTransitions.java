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
package com.hivemq.protocols.v2.tag;

import com.hivemq.protocols.v2.fsm.FSMGuard;
import com.hivemq.protocols.v2.fsm.FSMTransitionTable;
import org.jetbrains.annotations.NotNull;

/**
 * The read-aspect transition tables (design §7.3, §7.4) — direct encodings of the polled and subscribed state
 * diagrams. The five shared pre-operating rows (verification success / transient failure / permanent failure /
 * verification retry) are built once by {@link TagAspectPreOperatingTransitions} — the same builder the write
 * table uses — parameterized by each variant's state constants, so engine reuse is achieved without a shared
 * state enum (design §7.2). The role-specific rows — the poll cycle and the subscription cycle — are added per
 * variant.
 * <p>
 * Each table is built once and shared by every aspect of that variant; an action acts through the
 * {@link TagAspectRead} passed as the machine context. Goal and adapter-readiness changes never reach these tables —
 * they are applied directly by {@link TagAspectRead} (design §7.1, §7.2). The mandatory {@code unmatched} slot is
 * lenient: an unexpected event (a stale value, a late acknowledgment) is logged and ignored, never a reset — a
 * stray data point must not kill a tag.
 */
public final class TagAspectReadTransitions {

    private TagAspectReadTransitions() {}

    private static final @NotNull FSMTransitionTable<TagAspectState, TagAspectEvent, TagAspectRead> POLLED =
            buildPolledTable();
    private static final @NotNull FSMTransitionTable<TagAspectState, TagAspectEvent, TagAspectRead> SUBSCRIBED =
            buildSubscribedTable();

    /**
     * @return the polled read-aspect transition table (design §7.3).
     */
    public static @NotNull FSMTransitionTable<TagAspectState, TagAspectEvent, TagAspectRead> polledTable() {
        return POLLED;
    }

    /**
     * @return the subscribed read-aspect transition table (design §7.4).
     */
    public static @NotNull FSMTransitionTable<TagAspectState, TagAspectEvent, TagAspectRead> subscribedTable() {
        return SUBSCRIBED;
    }

    private static @NotNull FSMTransitionTable<TagAspectState, TagAspectEvent, TagAspectRead> buildPolledTable() {
        final FSMTransitionTable.Builder<TagAspectState, TagAspectEvent, TagAspectRead> builder =
                FSMTransitionTable.builder();
        TagAspectPreOperatingTransitions.addPreOperatingRows(
                builder,
                TagAspectReadPolledState.WAITING_FOR_VERIFICATION,
                TagAspectReadPolledState.WAITING_FOR_VERIFICATION_RETRY,
                TagAspectReadPolledState.ERROR_PERMANENT_VERIFICATION_FAILURE);
        return builder
                // The poll interval elapsed: request the next poll (design §7.3).
                .on(TagAspectReadPolledState.WAITING_FOR_POLL_INTERVAL, TagAspectEvent.PollIntervalElapsed.class)
                .then((current, event, aspect) -> {
                    aspect.requestPoll();
                    return TagAspectReadPolledState.WAITING_FOR_POLL_DATAPOINT;
                })
                // A value came back: schedule the next poll. No new state — the cadence simply continues.
                .on(TagAspectReadPolledState.WAITING_FOR_POLL_DATAPOINT, TagAspectEvent.ValueReceived.class)
                .then((current, event, aspect) -> {
                    aspect.scheduleNextPoll();
                    return TagAspectReadPolledState.WAITING_FOR_POLL_INTERVAL;
                })
                // A poll failed: count it, schedule the next poll. The next scheduled poll IS the retry (§7.3).
                .on(TagAspectReadPolledState.WAITING_FOR_POLL_DATAPOINT, TagAspectEvent.NodeFailed.class)
                .then((current, event, aspect) -> {
                    aspect.onPollFailure(TagAspectPreOperatingTransitions.reasonOf(event));
                    return TagAspectReadPolledState.WAITING_FOR_POLL_INTERVAL;
                })
                .unmatched((current, event, aspect) -> {
                    aspect.logUnexpectedEvent(event);
                    return current;
                })
                .build();
    }

    private static @NotNull FSMTransitionTable<TagAspectState, TagAspectEvent, TagAspectRead> buildSubscribedTable() {
        final FSMGuard<TagAspectState, TagAspectEvent, TagAspectRead> spontaneous = (current, event, aspect) ->
                event instanceof final TagAspectEvent.NodeFailed failed && failed.spontaneous();
        final FSMTransitionTable.Builder<TagAspectState, TagAspectEvent, TagAspectRead> builder =
                FSMTransitionTable.builder();
        TagAspectPreOperatingTransitions.addPreOperatingRows(
                builder,
                TagAspectReadSubscribedState.WAITING_FOR_VERIFICATION,
                TagAspectReadSubscribedState.WAITING_FOR_VERIFICATION_RETRY,
                TagAspectReadSubscribedState.ERROR_PERMANENT_VERIFICATION_FAILURE);
        return builder
                // The first pushed value confirms the subscription (design §7.4).
                .on(TagAspectReadSubscribedState.WAITING_FOR_SUBSCRIPTION, TagAspectEvent.ValueReceived.class)
                .then((current, event, aspect) -> {
                    aspect.confirmSubscription();
                    return TagAspectReadSubscribedState.SUBSCRIBED;
                })
                // The add-subscription request failed: back off and re-add (command-response loss, §7.4).
                .on(TagAspectReadSubscribedState.WAITING_FOR_SUBSCRIPTION, TagAspectEvent.NodeFailed.class)
                .then((current, event, aspect) -> {
                    aspect.onSubscriptionFailure(TagAspectPreOperatingTransitions.reasonOf(event));
                    return TagAspectReadSubscribedState.WAITING_FOR_SUBSCRIPTION_RETRY;
                })
                // Subsequent pushed values keep the subscription operating.
                .on(TagAspectReadSubscribedState.SUBSCRIBED, TagAspectEvent.ValueReceived.class)
                .then((current, event, aspect) -> TagAspectReadSubscribedState.SUBSCRIBED)
                // A spontaneous loss power-cycles through verification (design §7.4).
                .on(TagAspectReadSubscribedState.SUBSCRIBED, TagAspectEvent.NodeFailed.class)
                .when(spontaneous)
                .then((current, event, aspect) -> {
                    aspect.onSpontaneousSubscriptionLoss(TagAspectPreOperatingTransitions.reasonOf(event));
                    return TagAspectReadSubscribedState.WAITING_FOR_VERIFICATION;
                })
                // A command-response loss backs off and re-adds (design §7.4).
                .on(TagAspectReadSubscribedState.SUBSCRIBED, TagAspectEvent.NodeFailed.class)
                .otherwise((current, event, aspect) -> {
                    aspect.onSubscriptionFailure(TagAspectPreOperatingTransitions.reasonOf(event));
                    return TagAspectReadSubscribedState.WAITING_FOR_SUBSCRIPTION_RETRY;
                })
                // The backoff elapsed: re-add the subscription (design §7.4).
                .on(
                        TagAspectReadSubscribedState.WAITING_FOR_SUBSCRIPTION_RETRY,
                        TagAspectEvent.SubscriptionRetryElapsed.class)
                .then((current, event, aspect) -> {
                    aspect.requestAddSubscription();
                    return TagAspectReadSubscribedState.WAITING_FOR_SUBSCRIPTION;
                })
                .unmatched((current, event, aspect) -> {
                    aspect.logUnexpectedEvent(event);
                    return current;
                })
                .build();
    }
}
