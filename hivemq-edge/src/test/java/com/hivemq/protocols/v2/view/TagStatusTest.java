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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The five-value tag-status fold — a pure function of the {@link TagStatusSnapshot}. Each
 * direction-activation combination maps to its own value; an active-but-not-operating aspect or a permanent
 * failure is {@code ERROR}; and a healthy write-only tag folds to {@code SOUTHBOUND_ONLY}, not {@code ERROR} (the
 * S30 regression the v01 fold got wrong).
 */
class TagStatusTest {

    private static @NotNull TagStatusSnapshot snapshot(
            final boolean readActive,
            final boolean writeActive,
            final boolean readOperating,
            final boolean writeOperating,
            final boolean readPermanentFailure,
            final boolean writePermanentFailure) {
        return new TagStatusSnapshot(
                "tag",
                true,
                true,
                true,
                true,
                "readState",
                "writeState",
                readActive,
                writeActive,
                readOperating,
                writeOperating,
                readPermanentFailure,
                writePermanentFailure,
                0,
                null,
                0L);
    }

    @Test
    void everyAspectDeactivated_isDeactivated() {
        assertThat(TagStatus.of(snapshot(false, false, false, false, false, false)))
                .isEqualTo(TagStatus.DEACTIVATED);
    }

    @Test
    void readActiveAndOperating_isNorthboundOnly() {
        assertThat(TagStatus.of(snapshot(true, false, true, false, false, false)))
                .isEqualTo(TagStatus.NORTHBOUND_ONLY);
    }

    @Test
    void writeActiveAndOperating_isSouthboundOnly() {
        assertThat(TagStatus.of(snapshot(false, true, false, true, false, false)))
                .isEqualTo(TagStatus.SOUTHBOUND_ONLY);
    }

    @Test
    void bothActiveAndOperating_isNorthboundAndSouthbound() {
        assertThat(TagStatus.of(snapshot(true, true, true, true, false, false)))
                .isEqualTo(TagStatus.NORTHBOUND_AND_SOUTHBOUND);
    }

    @Test
    void aPermanentFailure_isError() {
        assertThat(TagStatus.of(snapshot(true, false, true, false, true, false)))
                .isEqualTo(TagStatus.ERROR);
    }

    @Test
    void readActiveButNotOperating_isError() {
        assertThat(TagStatus.of(snapshot(true, false, false, false, false, false)))
                .isEqualTo(TagStatus.ERROR);
    }

    @Test
    void writeActiveButNotOperating_isError() {
        assertThat(TagStatus.of(snapshot(false, true, false, false, false, false)))
                .isEqualTo(TagStatus.ERROR);
    }

    @Test
    void healthyWriteOnlyTag_isSouthboundOnly_notError() {
        // S30: a write-only tag (read deactivated, write resting at WAITING_FOR_WRITE_REQUEST) is operating, so it
        // is SOUTHBOUND_ONLY — the v01 fold wrongly reported ERROR.
        assertThat(TagStatus.of(snapshot(false, true, false, true, false, false)))
                .isEqualTo(TagStatus.SOUTHBOUND_ONLY);
    }
}
