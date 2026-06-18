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
package com.hivemq.protocols.v2.fsm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * A small goal-seeking demo that mirrors the adapter machine's central principle (design §1, §6.5): an
 * external actor sets a goal, the machine takes <em>one</em> step toward it per message — issuing exactly one
 * command each step — and reaches the goal in N steps. The goal change drives the first step
 * ({@code STOPPED → WAITING_FOR_STARTED} through {@code stepTowardGoal}); the acknowledgment event drives the
 * next through the table. A goal flip mid-wait is absorbed when the acknowledgment lands (stop intent).
 */
class FSMDemoTest {

    private enum DemoState implements FSMState {
        STOPPED,
        WAITING_FOR_STARTED,
        RUNNING
    }

    private record Started() implements FSMEvent {}

    private static final class Context {
        private boolean wantRunning;
        private final List<String> commands = new ArrayList<>();

        private void stepTowardGoal(final @NotNull FSM<DemoState, FSMEvent, Context> machine) {
            if (machine.state() == DemoState.STOPPED && wantRunning) {
                commands.add("start");
                machine.transitionTo(DemoState.WAITING_FOR_STARTED);
            }
        }
    }

    private static @NotNull FSMTransitionTable<DemoState, FSMEvent, Context> demoTable() {
        return FSMTransitionTable.<DemoState, FSMEvent, Context>builder()
                .on(DemoState.WAITING_FOR_STARTED, Started.class)
                .when((current, event, context) -> context.wantRunning)
                .then((current, event, context) -> {
                    context.commands.add("run");
                    return DemoState.RUNNING;
                })
                .on(DemoState.WAITING_FOR_STARTED, Started.class)
                .otherwise((current, event, context) -> DemoState.STOPPED)
                .unmatched((current, event, context) -> DemoState.STOPPED)
                .build();
    }

    @Test
    void reachesTheGoalInTwoSteps_oneCommandPerStep() {
        final Context context = new Context();
        final FSM<DemoState, FSMEvent, Context> machine = new FSM<>(DemoState.STOPPED, demoTable(), context);

        // Step 1: the goal becomes "running" — a goal command, bypassing the table; stepTowardGoal issues start().
        machine.onGoalChange(() -> {
            context.wantRunning = true;
            context.stepTowardGoal(machine);
        });
        assertThat(machine.state()).isEqualTo(DemoState.WAITING_FOR_STARTED);
        assertThat(context.commands).containsExactly("start");

        // Step 2: the adapter acknowledges started() — a table event drives the final step to the goal.
        machine.onEvent(new Started());
        assertThat(machine.state()).isEqualTo(DemoState.RUNNING);
        assertThat(context.commands).containsExactly("start", "run");
    }

    @Test
    void goalFlipMidWait_isAbsorbedWhenTheAcknowledgmentLands() {
        final Context context = new Context();
        final FSM<DemoState, FSMEvent, Context> machine = new FSM<>(DemoState.STOPPED, demoTable(), context);

        machine.onGoalChange(() -> {
            context.wantRunning = true;
            context.stepTowardGoal(machine);
        });
        assertThat(machine.state()).isEqualTo(DemoState.WAITING_FOR_STARTED);

        // The goal flips back to stopped while still waiting for the start acknowledgment.
        machine.onGoalChange(() -> {
            context.wantRunning = false;
            context.stepTowardGoal(machine);
        });
        assertThat(machine.state()).isEqualTo(DemoState.WAITING_FOR_STARTED);

        // started() now routes to STOPPED via the unconditional fallback — no "run" command is issued.
        machine.onEvent(new Started());
        assertThat(machine.state()).isEqualTo(DemoState.STOPPED);
        assertThat(context.commands).containsExactly("start");
    }
}
