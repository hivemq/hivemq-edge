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
 * How the {@link ChaosProtocolAdapter} answers a poll for a node: deliver a {@link Value} (a reused
 * v1 {@link DataPoint}), report a {@link NodeErrorResponse} (a failed poll, counted by the read aspect), or stay
 * silent with {@link NoResponse} (the aspect parks in {@code WAITING_FOR_POLL_DATAPOINT} until the next event).
 */
public sealed interface PollBehavior {

    /**
     * @param value the value to deliver on a poll.
     * @return a behavior that answers a poll with the given value.
     */
    static @NotNull PollBehavior value(final @NotNull DataPoint value) {
        return new Value(value);
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
     * Deliver a value.
     *
     * @param value the reused v1 value to deliver.
     */
    record Value(@NotNull DataPoint value) implements PollBehavior {}

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
