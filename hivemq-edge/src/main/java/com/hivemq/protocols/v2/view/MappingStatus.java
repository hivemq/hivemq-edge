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

import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import org.jetbrains.annotations.NotNull;

/**
 * The derived runtime status of one mapping — a <b>pure function</b> of the owning adapter's machine
 * state and the referenced tag's per-aspect snapshot, computed by {@link #of}. A mapping owns no state of its own;
 * its status is read entirely from the snapshot of the tag it references, on the side it consumes (a northbound
 * mapping reads the tag's read aspect; a southbound mapping its write aspect).
 */
public enum MappingStatus {

    /** The adapter is {@code CONNECTED}, the direction and tag aspect are activated, and the aspect is operating. */
    ACTIVE,

    /** The referenced tag's relevant aspect is deactivated (a config switch, the direction off, or the tag unused). */
    DEACTIVATED_BY_TAG,

    /** The referenced tag's relevant aspect is permanently failed or not yet operating (verifying, retrying). */
    BLOCKED_BY_TAG_ERROR,

    /** The adapter is not {@code CONNECTED}, so no mapping on it can be active. */
    BLOCKED_BY_ADAPTER;

    /**
     * Fold a mapping's status from the adapter machine state and the referenced tag's snapshot.
     * <p>
     * A deactivated aspect is reported as {@link #DEACTIVATED_BY_TAG} even when the adapter is down: the mapping is
     * intentionally off, which is more informative than the incidental {@link #BLOCKED_BY_ADAPTER}.
     *
     * @param machineState the owning adapter's machine state.
     * @param tag          the snapshot of the referenced tag.
     * @param writeSide    {@code true} for a southbound (write) mapping, {@code false} for a northbound (read) one.
     * @return the derived mapping status.
     */
    public static @NotNull MappingStatus of(
            final @NotNull ProtocolAdapterWrapperState machineState,
            final @NotNull TagStatusSnapshot tag,
            final boolean writeSide) {
        final boolean goalActive = writeSide ? tag.writeAspectGoalActive() : tag.readAspectGoalActive();
        final boolean operating = writeSide ? tag.writeAspectOperating() : tag.readAspectOperating();
        final boolean permanentFailure =
                writeSide ? tag.writeAspectPermanentFailure() : tag.readAspectPermanentFailure();
        if (!goalActive) {
            return DEACTIVATED_BY_TAG;
        }
        if (machineState != ProtocolAdapterWrapperState.CONNECTED) {
            return BLOCKED_BY_ADAPTER;
        }
        if (permanentFailure || !operating) {
            return BLOCKED_BY_TAG_ERROR;
        }
        return ACTIVE;
    }
}
