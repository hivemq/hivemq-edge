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
 * The states of a <b>polled</b> read aspect (design §7.3): the five shared pre-operating states plus the two
 * poll-cycle states. A poll failure introduces <b>no</b> new state — the aspect returns to
 * {@link #WAITING_FOR_POLL_INTERVAL} and the next scheduled poll is the retry; the failure only increments a
 * counter that drives escalating log severity.
 */
public enum TagAspectReadPolledState implements TagAspectState {

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
     * Verification failed permanently; suspended until a user-commanded tag retry (design §7.6).
     */
    ERROR_PERMANENT_VERIFICATION_FAILURE,

    /**
     * Verified and operating: waiting for the poll interval timer before requesting the next poll.
     */
    WAITING_FOR_POLL_INTERVAL,

    /**
     * A poll was requested; waiting for the value (or a per-node error) to come back.
     */
    WAITING_FOR_POLL_DATAPOINT;

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
        return this == WAITING_FOR_POLL_INTERVAL || this == WAITING_FOR_POLL_DATAPOINT;
    }
}
