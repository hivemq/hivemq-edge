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

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import org.junit.jupiter.api.Test;

/**
 * The derived mapping-status fold (design §11.3): adapter connection, the three-condition goal, operating, and
 * permanent failure resolve to one of the four statuses, with a deactivated aspect taking precedence over an
 * adapter that is down.
 */
class MappingStatusTest {

    @Test
    void connectedActiveAndOperating_isActive() {
        assertThat(MappingStatus.of(ProtocolAdapterWrapperState.CONNECTED, readSide(true, true, false), false))
                .isEqualTo(MappingStatus.ACTIVE);
    }

    @Test
    void connectedActiveButNotOperating_isBlockedByTagError() {
        assertThat(MappingStatus.of(ProtocolAdapterWrapperState.CONNECTED, readSide(true, false, false), false))
                .isEqualTo(MappingStatus.BLOCKED_BY_TAG_ERROR);
    }

    @Test
    void connectedActiveButPermanentlyFailed_isBlockedByTagError() {
        assertThat(MappingStatus.of(ProtocolAdapterWrapperState.CONNECTED, readSide(true, true, true), false))
                .isEqualTo(MappingStatus.BLOCKED_BY_TAG_ERROR);
    }

    @Test
    void connectedButDeactivated_isDeactivatedByTag() {
        assertThat(MappingStatus.of(ProtocolAdapterWrapperState.CONNECTED, readSide(false, false, false), false))
                .isEqualTo(MappingStatus.DEACTIVATED_BY_TAG);
    }

    @Test
    void notConnectedButActive_isBlockedByAdapter() {
        assertThat(MappingStatus.of(
                        ProtocolAdapterWrapperState.WAITING_FOR_CONNECTION_RETRY, readSide(true, false, false), false))
                .isEqualTo(MappingStatus.BLOCKED_BY_ADAPTER);
    }

    @Test
    void deactivatedTakesPrecedenceOverAdapterDown() {
        assertThat(MappingStatus.of(ProtocolAdapterWrapperState.STOPPED, readSide(false, false, false), false))
                .isEqualTo(MappingStatus.DEACTIVATED_BY_TAG);
    }

    @Test
    void writeSideReadsTheWriteAspect() {
        // Read aspect is fully active+operating, but the write side is deactivated → a southbound mapping is
        // DEACTIVATED_BY_TAG, proving the fold reads the write aspect, not the read aspect.
        final TagStatusSnapshot tag = new TagStatusSnapshot(
                "t", true, false, true, false, "R", "W", true, false, true, false, false, false, 0, null, 0L);
        assertThat(MappingStatus.of(ProtocolAdapterWrapperState.CONNECTED, tag, true))
                .isEqualTo(MappingStatus.DEACTIVATED_BY_TAG);
    }

    private static @org.jetbrains.annotations.NotNull TagStatusSnapshot readSide(
            final boolean goalActive, final boolean operating, final boolean permanentFailure) {
        return new TagStatusSnapshot(
                "t",
                true,
                false,
                true,
                false,
                "readState",
                "writeState",
                goalActive,
                false,
                operating,
                false,
                permanentFailure,
                false,
                0,
                null,
                0L);
    }
}
