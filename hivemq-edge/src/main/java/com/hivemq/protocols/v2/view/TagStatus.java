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
package com.hivemq.protocols.v2.view;

import org.jetbrains.annotations.NotNull;

/**
 * The externally visible status of a tag: a distinct value per direction-activation combination,
 * plus deactivated and error. It is a <b>pure function of the {@link TagStatusSnapshot}</b> computed by
 * {@link #of(TagStatusSnapshot)} — no tag owns this state.
 * <p>
 * "Operating" includes in-flight work ({@code WAITING_FOR_POLL_DATAPOINT}, {@code WAITING_FOR_WRITE_RESULT}), so a
 * tag does not flap to {@link #ERROR} during a normal poll or write round-trip; a healthy write-only tag folds to
 * {@link #SOUTHBOUND_ONLY}, not {@link #ERROR}.
 */
public enum TagStatus {

    /**
     * Both sides active and healthy: the read aspect is producing (poll cycle or subscription) and the write
     * aspect is ready.
     */
    NORTHBOUND_AND_SOUTHBOUND,

    /**
     * The read side is active and producing; the write side is deactivated (by direction, aspect switch, or
     * unused).
     */
    NORTHBOUND_ONLY,

    /**
     * The write side is active and ready (verified, accepting writes); the read side is deactivated.
     */
    SOUTHBOUND_ONLY,

    /**
     * Every aspect is deactivated.
     */
    DEACTIVATED,

    /**
     * Any aspect is permanently failed, or any active aspect is not operating yet (verifying, waiting for the
     * adapter, retrying).
     */
    ERROR;

    /**
     * Fold a tag's snapshot into its externally visible status.
     *
     * @param tag the per-tag snapshot.
     * @return the tag status.
     */
    public static @NotNull TagStatus of(final @NotNull TagStatusSnapshot tag) {
        final boolean anyPermanent = tag.readAspectPermanentFailure() || tag.writeAspectPermanentFailure();
        final boolean readActive = tag.readAspectGoalActive();
        final boolean writeActive = tag.writeAspectGoalActive();
        final boolean readOperating = tag.readAspectOperating();
        final boolean writeOperating = tag.writeAspectOperating();
        if (anyPermanent) {
            return ERROR;
        }
        if (readActive && !readOperating) {
            return ERROR; // active but not yet operating (verifying, waiting for the adapter, retrying)
        }
        if (writeActive && !writeOperating) {
            return ERROR;
        }
        if (readActive && writeActive) {
            return NORTHBOUND_AND_SOUTHBOUND;
        }
        if (readActive) {
            return NORTHBOUND_ONLY;
        }
        if (writeActive) {
            return SOUTHBOUND_ONLY;
        }
        return DEACTIVATED;
    }
}
