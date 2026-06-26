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

/**
 * The states of a <b>subscribed</b> read aspect: the five shared pre-operating states plus the
 * three subscription states. The {@code spontaneous} bit on a per-node error selects the recovery path — a
 * command-response loss backs off and re-adds the subscription ({@link #WAITING_FOR_SUBSCRIPTION_RETRY}); a
 * spontaneous loss power-cycles the aspect back through verification.
 * <p>
 * <b>Known limitation:</b> {@link #SUBSCRIBED} is confirmed only by the first {@code dataPoint}, so
 * a protocol that does not push an initial value parks in {@link #WAITING_FOR_SUBSCRIPTION} (reported as not
 * operating) until the first value arrives.
 */
public enum TagAspectReadSubscribedState implements TagAspectState {

    /**
     * At rest: the aspect's goal is not active (the three-condition rule fails).
     */
    DEACTIVATED,

    /**
     * The goal is active but the adapter is not connected — waiting to verify on the next connection.
     */
    WAITING_FOR_ADAPTER_READY,

    /**
     * Verifying the node against the connected device.
     */
    WAITING_FOR_VERIFICATION,

    /**
     * A transient verification failure occurred; waiting for the retry timer before verifying again.
     */
    WAITING_FOR_VERIFICATION_RETRY,

    /**
     * Verification failed permanently; suspended until a user-commanded tag retry.
     */
    ERROR_PERMANENT_VERIFICATION_FAILURE,

    /**
     * The subscription was requested; waiting for the first pushed value to confirm it.
     */
    WAITING_FOR_SUBSCRIPTION,

    /**
     * Subscribed and operating: receiving pushed values.
     */
    SUBSCRIBED,

    /**
     * A command-response subscription loss occurred; waiting for the backoff timer before re-adding.
     */
    WAITING_FOR_SUBSCRIPTION_RETRY;

    @Override
    public boolean isDeactivated() {
        return this == DEACTIVATED;
    }

    @Override
    public boolean isPermanentVerificationFailure() {
        return this == ERROR_PERMANENT_VERIFICATION_FAILURE;
    }

    @Override
    public boolean isOperating() {
        return this == SUBSCRIBED;
    }
}
