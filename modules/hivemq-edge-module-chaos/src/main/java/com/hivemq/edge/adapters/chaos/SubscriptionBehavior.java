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
import org.jetbrains.annotations.NotNull;

/**
 * How the {@link ChaosProtocolAdapter} answers an add-subscription for a node: {@link Accept} the
 * subscription and immediately push the first value (which confirms {@code SUBSCRIBED}); {@link Fail}
 * it with a node error; or {@link LoseAfter} a number of harness ticks, modelling a subscription that drops — as a
 * command-response loss ({@code spontaneous=false}, a backoff + re-add) or a spontaneous loss
 * ({@code spontaneous=true}, a full power-cycle through verification).
 */
public sealed interface SubscriptionBehavior {

    /**
     * @param firstValue the value pushed immediately on subscription, confirming {@code SUBSCRIBED}.
     * @return a behavior that accepts the subscription and pushes the first value.
     */
    static @NotNull SubscriptionBehavior accept(final @NotNull DataPoint firstValue) {
        return new Accept(firstValue);
    }

    /**
     * @param reason the failure reason.
     * @return a behavior that fails the subscription with a node error.
     */
    static @NotNull SubscriptionBehavior fail(final @NotNull String reason) {
        return new Fail(reason);
    }

    /**
     * @param firstValue  the value pushed immediately on subscription, confirming {@code SUBSCRIBED} before the loss.
     * @param ticks       the number of harness ticks after the first value before the subscription is lost.
     * @param spontaneous whether the loss is spontaneous (a full power-cycle) or a command-response loss
     *                    (a backoff + re-add).
     * @param reason      the loss reason.
     * @return a behavior that accepts, confirms, then loses the subscription after the given delay.
     */
    static @NotNull SubscriptionBehavior loseAfter(
            final @NotNull DataPoint firstValue,
            final int ticks,
            final boolean spontaneous,
            final @NotNull String reason) {
        return new LoseAfter(firstValue, ticks, spontaneous, reason);
    }

    /**
     * Accept the subscription and push the first value.
     *
     * @param firstValue the value that confirms {@code SUBSCRIBED}.
     */
    record Accept(@NotNull DataPoint firstValue) implements SubscriptionBehavior {}

    /**
     * Fail the subscription with a node error.
     *
     * @param reason the failure reason.
     */
    record Fail(@NotNull String reason) implements SubscriptionBehavior {}

    /**
     * Accept the subscription, push the first value, then lose it after a delay.
     *
     * @param firstValue  the value pushed immediately, confirming {@code SUBSCRIBED} before the loss.
     * @param ticks       the harness ticks after the first value before the loss.
     * @param spontaneous whether the loss is spontaneous (full power-cycle) or command-response (backoff + re-add).
     * @param reason      the loss reason.
     */
    record LoseAfter(
            @NotNull DataPoint firstValue,
            int ticks,
            boolean spontaneous,
            @NotNull String reason) implements SubscriptionBehavior {}
}
