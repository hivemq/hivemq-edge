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
 * The states of a tag's <b>write</b> aspect: the five shared pre-operating states plus the two
 * write-cycle states. {@link #WAITING_FOR_WRITE_REQUEST} is the healthy <b>goal</b> state — verified and ready to
 * accept southbound writes; a write arriving there requests the write and moves to
 * {@link #WAITING_FOR_WRITE_RESULT}, which returns to the request state on the acknowledgment (success or failure;
 * a failure is logged and counted but does <b>not</b> flap the aspect to {@code ERROR}). One write is in flight at
 * a time; multi-write ordering and back-pressure are a reserved extension point.
 */
public enum TagAspectWriteState implements TagAspectState {

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
     * Verified and operating: the resting goal state, ready to accept a southbound write.
     */
    WAITING_FOR_WRITE_REQUEST,

    /**
     * A write was requested; waiting for the adapter's write acknowledgment (still operating — a normal write
     * round-trip does not flap the tag to {@code ERROR}).
     */
    WAITING_FOR_WRITE_RESULT;

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
        return this == WAITING_FOR_WRITE_REQUEST || this == WAITING_FOR_WRITE_RESULT;
    }
}
