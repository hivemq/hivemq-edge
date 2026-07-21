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
package com.hivemq.edge.adapters.chaos;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * How the {@link ChaosProtocolAdapter} answers a poll for a node. A poll produces any number of values (reused v1
 * {@link DataPoint}s) and is ended by an explicit completion, a per-node error, or never — and the script controls
 * each piece separately:
 * <ul>
 *     <li>{@link Value} — one value, completed within the same poll (the common synchronous shape);</li>
 *     <li>{@link Values} — every value in order, then one completion (a multi-value poll);</li>
 *     <li>{@link ValueThenDeferredCompletion} — the value now, the completion only when its tick comes due: the
 *         poll stays observably open in {@code WAITING_FOR_POLL_DATAPOINT} with the value already published;</li>
 *     <li>{@link CompletionWithoutValue} — a zero-value poll: only the completion, nothing published;</li>
 *     <li>{@link NodeErrorResponse} — a failed poll, counted by the read aspect (a failure ends the poll on its
 *         own, so no completion follows);</li>
 *     <li>{@link NoResponse} — nothing at all: the aspect parks in {@code WAITING_FOR_POLL_DATAPOINT} until the
 *         next event.</li>
 * </ul>
 */
public sealed interface PollBehavior {

    /**
     * @param value the value to deliver on a poll.
     * @return a behavior that answers a poll with the given value and completes it within the same poll.
     */
    static @NotNull PollBehavior value(final @NotNull DataPoint value) {
        return new Value(value);
    }

    /**
     * @param values the values to deliver on a poll, in order.
     * @return a behavior that answers a poll with every given value and then one completion.
     */
    static @NotNull PollBehavior values(final @NotNull DataPoint... values) {
        return new Values(List.of(values));
    }

    /**
     * @param value the value to deliver within the poll call.
     * @param ticks the harness ticks after which the completion fires (minimum one).
     * @return a behavior that splits the poll: the value now, the completion only when its tick comes due.
     */
    static @NotNull PollBehavior valueThenCompletionAfterTicks(final @NotNull DataPoint value, final int ticks) {
        return new ValueThenDeferredCompletion(value, ticks);
    }

    /**
     * @return a behavior that answers a poll with only the completion — a zero-value poll.
     */
    static @NotNull PollBehavior completionWithoutValue() {
        return new CompletionWithoutValue();
    }

    /**
     * @param reason the failure reason.
     * @return a behavior that answers a poll with a node error.
     */
    static @NotNull PollBehavior nodeError(final @NotNull String reason) {
        return new NodeErrorResponse(reason);
    }

    /**
     * @return a behavior that answers a poll with nothing.
     */
    static @NotNull PollBehavior noResponse() {
        return new NoResponse();
    }

    /**
     * Deliver a value and complete the poll within the same call.
     *
     * @param value the reused v1 value to deliver.
     */
    record Value(@NotNull DataPoint value) implements PollBehavior {}

    /**
     * Deliver every value in order, then complete the poll — a multi-value poll.
     *
     * @param values the reused v1 values to deliver.
     */
    record Values(@NotNull List<DataPoint> values) implements PollBehavior {}

    /**
     * Deliver the value within the poll call but complete the poll only when the given tick comes due — the poll
     * stays open in between, with the value already published.
     *
     * @param value the reused v1 value to deliver.
     * @param ticks the harness ticks after which the completion fires (minimum one).
     */
    record ValueThenDeferredCompletion(@NotNull DataPoint value, int ticks) implements PollBehavior {}

    /**
     * Complete the poll without delivering anything — a zero-value poll.
     */
    record CompletionWithoutValue() implements PollBehavior {}

    /**
     * Report a per-node read failure (the poll's own retry).
     *
     * @param reason the failure reason.
     */
    record NodeErrorResponse(@NotNull String reason) implements PollBehavior {}

    /**
     * Answer with nothing — the poll never returns.
     */
    record NoResponse() implements PollBehavior {}
}
