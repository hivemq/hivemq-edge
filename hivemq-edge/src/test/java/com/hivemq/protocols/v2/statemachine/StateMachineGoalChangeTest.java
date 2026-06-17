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
package com.hivemq.protocols.v2.statemachine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The goal-command bypass (design §4): {@link StateMachine#onGoalChange(Runnable)} mutations apply in
 * <em>every</em> state without consulting the table, so a goal command never triggers the defensive reset —
 * in contrast to an unhandled event, which does.
 */
class StateMachineGoalChangeTest {

    private enum DemoState implements StateMachineState {
        S1,
        S2,
        S3,
        ERROR
    }

    private record SomeEvent() implements StateMachineEvent {}

    private static final class Context {
        private String goal = "stop";
        private int defensiveResets;
    }

    @Test
    void goalChangeAppliesInEveryStateAndNeverTriggersTheDefensiveReset() {
        final Context context = new Context();
        // A table with NO event rows: any event would hit the unmatched (defensive reset) slot.
        final TransitionTable<DemoState, StateMachineEvent, Context> table =
                TransitionTable.<DemoState, StateMachineEvent, Context>builder()
                        .unmatched((current, event, ctx) -> {
                            ctx.defensiveResets++;
                            return DemoState.ERROR;
                        })
                        .build();
        final StateMachine<DemoState, StateMachineEvent, Context> machine =
                new StateMachine<>(DemoState.S1, table, context);

        for (final DemoState start : DemoState.values()) {
            machine.transitionTo(start);

            machine.onGoalChange(() -> {
                context.goal = "go";
                machine.transitionTo(DemoState.S2);
            });

            assertThat(machine.state()).isEqualTo(DemoState.S2);
            assertThat(context.goal).isEqualTo("go");
        }
        assertThat(context.defensiveResets).isZero();
    }

    @Test
    void unhandledEvent_triggersTheDefensiveReset() {
        final Context context = new Context();
        final TransitionTable<DemoState, StateMachineEvent, Context> table =
                TransitionTable.<DemoState, StateMachineEvent, Context>builder()
                        .unmatched((current, event, ctx) -> {
                            ctx.defensiveResets++;
                            return DemoState.ERROR;
                        })
                        .build();
        final StateMachine<DemoState, StateMachineEvent, Context> machine =
                new StateMachine<>(DemoState.S1, table, context);

        machine.onEvent(new SomeEvent());

        assertThat(machine.state()).isEqualTo(DemoState.ERROR);
        assertThat(context.defensiveResets).isEqualTo(1);
    }
}
