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
package com.hivemq.protocols.v2.wrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.runtime.BatchCollector;
import com.hivemq.protocols.v2.runtime.FakeClock;
import com.hivemq.protocols.v2.runtime.PriorityTimerQueue;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
import com.hivemq.protocols.v2.tag.SharedNodeVerification;
import com.hivemq.protocols.v2.tag.SouthboundWriteCompletion;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import com.hivemq.protocols.v2.tag.TagAspectGoal;
import com.hivemq.protocols.v2.tag.TagAspectWrite;
import com.hivemq.protocols.v2.tag.TagWriteReadinessListener;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The write aspect's ownership of the in-flight completion, driven directly rather than through the wrapper so the
 * batch collector can be made to fail.
 * <p>
 * The aspect's standing invariant is that <b>every write settles exactly once, whatever happens</b>: a completion
 * the aspect accepted but never settled can be settled by nobody else, so the sender's single delivery slot would
 * stay occupied for good and that tag would silently stop accepting writes — with no recovery short of an adapter
 * recreate. The dangerous window is the moment between accepting a write and taking ownership of its completion,
 * which is why the aspect records the completion <b>before</b> it posts the request.
 */
class TagAspectWriteCompletionOwnershipTest {

    private static final @NotNull String TAG = "setpoint";

    @Test
    void aWriteThatCannotBePostedSettlesAborted_ratherThanLeakingItsCompletion() {
        final BatchCollector batches = mock(BatchCollector.class);
        doThrow(new IllegalStateException("the batch collector refused the write"))
                .when(batches)
                .write(any());
        final List<SouthboundWriteOutcome> outcomes = new ArrayList<>();
        final TagAspectWrite aspect = restingWriteAspect(batches);

        assertThatThrownBy(() -> aspect.onWriteRequested(
                        WrapperTestSupport.dataPoint(TAG, "42"), (outcome, reason) -> outcomes.add(outcome)))
                .isInstanceOf(IllegalStateException.class);

        // The failure still escapes — the wrapper's contract guard is what faults the adapter — but the sender is
        // released rather than left waiting on a completion that can never be settled.
        assertThat(outcomes).containsExactly(SouthboundWriteOutcome.ABORTED);
    }

    @Test
    void afterAFailedPost_theAspectStillAcceptsTheNextWrite_theTagIsNotWedged() {
        final BatchCollector batches = mock(BatchCollector.class);
        doThrow(new IllegalStateException("the batch collector refused the write"))
                .when(batches)
                .write(any());
        final TagAspectWrite aspect = restingWriteAspect(batches);
        assertThatThrownBy(() -> aspect.onWriteRequested(
                        WrapperTestSupport.dataPoint(TAG, "1"), SouthboundWriteCompletion.IGNORED))
                .isInstanceOf(IllegalStateException.class);

        // The aspect never took the write, so it must still be resting ready — a stuck in-flight completion would
        // have made the retry settle ABORTED as "superseded" instead of being posted.
        final List<SouthboundWriteOutcome> outcomes = new ArrayList<>();
        doThrow(new IllegalStateException("still refusing")).when(batches).write(any());
        assertThatThrownBy(() -> aspect.onWriteRequested(
                        WrapperTestSupport.dataPoint(TAG, "2"), (outcome, reason) -> outcomes.add(outcome)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(outcomes).containsExactly(SouthboundWriteOutcome.ABORTED);
        assertThat(aspect.stateName()).isEqualTo("WAITING_FOR_WRITE_REQUEST");
    }

    /** A write aspect driven to its resting goal state, ready to accept a write. */
    private static @NotNull TagAspectWrite restingWriteAspect(final @NotNull BatchCollector batches) {
        final NodeTagPair pair = WrapperTestSupport.pair(TAG);
        final TagAspectWrite aspect = new TagAspectWrite(
                "a1",
                pair.node(),
                pair.tag(),
                new FakeClock(),
                mock(PriorityTimerQueue.class),
                batches,
                mock(ProtocolAdapterMetrics.class),
                mock(SharedNodeVerification.class),
                TagWriteReadinessListener.NONE,
                new RetryPolicy(1000, 2.0, 30000, 100));
        aspect.applyGoal(new TagAspectGoal(true, true, true));
        aspect.onAdapterReady();
        return aspect;
    }
}
