/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.method;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// Whether a climb up the type hierarchy reports itself as having read every level it tried.
///
/// **EDG-1087, Miguel's round 2.** A transient failure on one level used to vanish: the level contributed
/// nothing, the climb continued, and the result was a `Verified` that {@link MethodCache} keeps for the
/// session. One `Bad_Timeout` while climbing therefore hid every static method and every inherited
/// declaration until the session ended -- the same defect `43923822f` fixed one level down, on the object's
/// own argument reads, reappearing on the type climb because each path decided for itself what a failure means.
///
/// The fix is a `complete` flag that the cache treats like an unanswerable result. What this class pins is
/// that **the flag survives the recursion**: the climb is recursive, one call per type level, so a failure
/// four levels up must still mark the answer partial after four successful levels follow it back down. Against
/// a healthy server every end-to-end test passes whether or not that holds, which is why it is pinned here.
///
/// Tested here rather than against a server because no server will fail exactly one level of a hierarchy on
/// demand. {@link MethodVerifier.Climb} is package-private for this reason.
class TypeClimbCompletenessTest {

    private static final @NotNull NodeId LEVEL_1 = NodeId.parse("ns=2;s=BoilerType");
    private static final @NotNull NodeId LEVEL_2 = NodeId.parse("ns=2;s=DeviceType");

    private static @NotNull MethodVerifier.Climb complete() {
        return new MethodVerifier.Climb(List.of(), true, null);
    }

    @Test
    void everyLevelRead_staysComplete() {
        final MethodVerifier.Climb climb = complete()
                .andThisLevel(true, LEVEL_2)
                .andThisLevel(true, LEVEL_1)
                .andThisLevel(true, LEVEL_1);

        assertThat(climb.complete()).isTrue();
    }

    @Test
    void oneFailedLevel_makesTheWholeClimbIncomplete() {
        final MethodVerifier.Climb climb = complete().andThisLevel(false, null);

        assertThat(climb.complete()).isFalse();
    }

    @Test
    void aFailureSurvivesEveryLaterLevel() {
        // The decisive case. The failure happens far from the top, and four successful levels follow it on the
        // way back down the recursion. Each of them must leave the answer partial.
        final MethodVerifier.Climb climb = complete()
                .andThisLevel(false, null)
                .andThisLevel(true, LEVEL_1)
                .andThisLevel(true, LEVEL_1)
                .andThisLevel(true, LEVEL_1)
                .andThisLevel(true, LEVEL_1);

        assertThat(climb.complete()).isFalse();
    }

    @Test
    void aFailureAnywhereInTheChainIsEnough() {
        assertThat(complete()
                        .andThisLevel(true, LEVEL_1)
                        .andThisLevel(false, null)
                        .andThisLevel(true, LEVEL_1)
                        .complete())
                .isFalse();
        assertThat(complete()
                        .andThisLevel(true, LEVEL_1)
                        .andThisLevel(true, LEVEL_1)
                        .andThisLevel(false, null)
                        .complete())
                .isFalse();
    }

    @Test
    void theMethodsAreCarriedThroughUnchanged() {
        // Marking the climb partial must not drop what it did find: those methods are served to the caller,
        // and only the remembering is declined.
        final MethodSignature square = MethodSignature.ofObject(
                "Square", NodeId.parse("ns=2;s=Boiler1.Square"), NodeId.parse("ns=2;s=Boiler1"), List.of(), List.of());

        final MethodVerifier.Climb climb =
                new MethodVerifier.Climb(List.of(square), true, null).andThisLevel(false, null);

        assertThat(climb.complete()).isFalse();
        assertThat(climb.methods()).containsExactly(square);
    }

    @Test
    void theFurthestLevelRead_isTheOneFurthestFromTheObject() {
        // `readTo` says how far the walk got, for the log line an operator reads when a climb came back
        // incomplete. The climb above already names a level further from the object, so it wins over the
        // caller's own.
        final MethodVerifier.Climb climb =
                new MethodVerifier.Climb(List.of(), false, LEVEL_2).andThisLevel(true, LEVEL_1);

        assertThat(climb.readTo()).isEqualTo(LEVEL_2);
    }

    @Test
    void theCallersLevelIsRecorded_whenNothingAboveItWasRead() {
        // The case the finding was about: a level answered, the browse above it did not. The level that
        // answered is the furthest the walk reached, and must not be lost.
        final MethodVerifier.Climb climb = new MethodVerifier.Climb(List.of(), false, null).andThisLevel(true, LEVEL_1);

        assertThat(climb.readTo()).isEqualTo(LEVEL_1);
        assertThat(climb.complete()).isFalse();
    }
}
