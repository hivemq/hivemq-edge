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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import org.jetbrains.annotations.NotNull;

/**
 * The table-driven inputs to a tag aspect machine. Each aspect is its own {@code FSM}; the wrapper
 * routes the protocol-adapter events it receives to the owning tag's aspects, which feed them here, and the
 * aspect's own timers (poll interval, verification retry, subscription retry) feed the timer-expiry events on the
 * actor's single dispatch thread.
 * <p>
 * Aspect goal changes (the three-condition rule) and the adapter-readiness coupling are <b>not</b> events: like
 * the adapter machine's goal-command bypass, they are applied directly by the aspect runtime and never run
 * through the transition table.
 */
public sealed interface TagAspectEvent extends com.hivemq.protocols.v2.fsm.FSMEvent
        permits TagAspectEvent.VerifySucceeded,
                TagAspectEvent.VerifyTransientlyFailed,
                TagAspectEvent.VerifyPermanentlyFailed,
                TagAspectEvent.VerificationRetryElapsed,
                TagAspectEvent.PollIntervalElapsed,
                TagAspectEvent.ValueReceived,
                TagAspectEvent.NodeFailed,
                TagAspectEvent.SubscriptionRetryElapsed,
                TagAspectEvent.WriteRequested,
                TagAspectEvent.WriteSucceeded,
                TagAspectEvent.WriteFailed {

    /**
     * The node verified successfully.
     */
    record VerifySucceeded() implements TagAspectEvent {}

    /**
     * Verification failed transiently — retry after a delay.
     *
     * @param reason a human-readable description of the failure.
     */
    record VerifyTransientlyFailed(@NotNull String reason) implements TagAspectEvent {}

    /**
     * Verification failed permanently — suspend until a user-commanded retry.
     *
     * @param reason a human-readable description of the failure.
     */
    record VerifyPermanentlyFailed(@NotNull String reason) implements TagAspectEvent {}

    /**
     * The verification-retry timer elapsed — verify the node again. Generated in the tick handler.
     */
    record VerificationRetryElapsed() implements TagAspectEvent {}

    /**
     * The poll-interval timer elapsed — request the next poll. Generated in the tick handler.
     */
    record PollIntervalElapsed() implements TagAspectEvent {}

    /**
     * A value arrived for the node — a poll response or a subscription push.
     *
     * @param value the reused v1 value.
     */
    record ValueReceived(@NotNull DataPoint value) implements TagAspectEvent {}

    /**
     * A per-node failure arrived — a failed poll, or a failed or lost subscription.
     *
     * @param reason      a human-readable description of the failure.
     * @param spontaneous {@code true} if the failure arrived outside a command-response exchange — selects the
     *                    subscription recovery path.
     */
    record NodeFailed(@NotNull String reason, boolean spontaneous) implements TagAspectEvent {}

    /**
     * The subscription-retry backoff elapsed — re-add the subscription. Generated in the tick
     * handler.
     */
    record SubscriptionRetryElapsed() implements TagAspectEvent {}

    /**
     * A southbound write arrived for the tag — request the write. Consumed only by the write
     * aspect; the read aspect's table ignores it. The carried value is the reused v1 value to write, and the
     * completion is settled when the write reaches a terminal outcome (or immediately, rejected, if one is
     * already in flight).
     *
     * @param value      the reused v1 value to write.
     * @param completion the one-shot back-pressure signal settled with the write's outcome.
     */
    record WriteRequested(@NotNull DataPoint value, @NotNull SouthboundWriteCompletion completion)
            implements TagAspectEvent {}

    /**
     * The protocol adapter acknowledged the in-flight write successfully. Consumed only by the
     * write aspect.
     */
    record WriteSucceeded() implements TagAspectEvent {}

    /**
     * The protocol adapter reported the in-flight write as failed — logged and counted, the
     * aspect returns to {@code WAITING_FOR_WRITE_REQUEST} without flapping to {@code ERROR}. Consumed only by the
     * write aspect.
     *
     * @param reason a human-readable description of the failure.
     */
    record WriteFailed(@NotNull String reason) implements TagAspectEvent {}
}
