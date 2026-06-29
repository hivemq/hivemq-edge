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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class FSMTransitionTableTest {

    private enum DemoState implements FSMState {
        A,
        B,
        C,
        RESET
    }

    private record Advance() implements FSMEvent {}

    private record Other() implements FSMEvent {}

    private static final class Recorder {
        private final List<String> actions = new ArrayList<>();
        private boolean flag;
    }

    @Test
    void matchedTransition_runsActionAndReturnsNextState() {
        final Recorder context = new Recorder();
        final FSMTransitionTable<DemoState, FSMEvent, Recorder> table =
                FSMTransitionTable.<DemoState, FSMEvent, Recorder>builder()
                        .on(DemoState.A, Advance.class)
                        .then((current, event, recorder) -> {
                            recorder.actions.add("a->b");
                            return DemoState.B;
                        })
                        .unmatched((current, event, recorder) -> {
                            recorder.actions.add("reset");
                            return DemoState.RESET;
                        })
                        .build();

        final DemoState next = table.dispatch(DemoState.A, new Advance(), context);

        assertThat(next).isEqualTo(DemoState.B);
        assertThat(context.actions).containsExactly("a->b");
    }

    @Test
    void passingGuard_isChosenOverTheUnconditionalFallback() {
        final FSMTransitionTable<DemoState, FSMEvent, Recorder> table = guardedTable();

        final Recorder context = new Recorder();
        context.flag = true;

        final DemoState next = table.dispatch(DemoState.A, new Advance(), context);

        assertThat(next).isEqualTo(DemoState.B);
        assertThat(context.actions).containsExactly("guarded");
    }

    @Test
    void failingGuard_fallsThroughToTheUnconditionalRow() {
        final FSMTransitionTable<DemoState, FSMEvent, Recorder> table = guardedTable();

        final Recorder context = new Recorder();
        context.flag = false;

        final DemoState next = table.dispatch(DemoState.A, new Advance(), context);

        assertThat(next).isEqualTo(DemoState.C);
        assertThat(context.actions).containsExactly("default");
    }

    @Test
    void guardedRowsAreEvaluatedInRegistrationOrder() {
        final Recorder context = new Recorder();
        final FSMTransitionTable<DemoState, FSMEvent, Recorder> table =
                FSMTransitionTable.<DemoState, FSMEvent, Recorder>builder()
                        .on(DemoState.A, Advance.class)
                        .when((current, event, recorder) -> true)
                        .then((current, event, recorder) -> {
                            recorder.actions.add("first");
                            return DemoState.B;
                        })
                        .on(DemoState.A, Advance.class)
                        .when((current, event, recorder) -> true)
                        .then((current, event, recorder) -> {
                            recorder.actions.add("second");
                            return DemoState.C;
                        })
                        .unmatched((current, event, recorder) -> DemoState.RESET)
                        .build();

        final DemoState next = table.dispatch(DemoState.A, new Advance(), context);

        assertThat(next).isEqualTo(DemoState.B);
        assertThat(context.actions).containsExactly("first");
    }

    @Test
    void unknownEventForTheState_triggersTheUnmatchedAction() {
        final Recorder context = new Recorder();
        final FSMTransitionTable<DemoState, FSMEvent, Recorder> table =
                FSMTransitionTable.<DemoState, FSMEvent, Recorder>builder()
                        .on(DemoState.A, Advance.class)
                        .then((current, event, recorder) -> DemoState.B)
                        .unmatched((current, event, recorder) -> {
                            recorder.actions.add("reset");
                            return DemoState.RESET;
                        })
                        .build();

        final DemoState next = table.dispatch(DemoState.A, new Other(), context);

        assertThat(next).isEqualTo(DemoState.RESET);
        assertThat(context.actions).containsExactly("reset");
    }

    @Test
    void everyGuardFailsAndNoFallback_triggersTheUnmatchedAction() {
        final Recorder context = new Recorder();
        final FSMTransitionTable<DemoState, FSMEvent, Recorder> table =
                FSMTransitionTable.<DemoState, FSMEvent, Recorder>builder()
                        .on(DemoState.A, Advance.class)
                        .when((current, event, recorder) -> false)
                        .then((current, event, recorder) -> DemoState.B)
                        .unmatched((current, event, recorder) -> {
                            recorder.actions.add("reset");
                            return DemoState.RESET;
                        })
                        .build();

        final DemoState next = table.dispatch(DemoState.A, new Advance(), context);

        assertThat(next).isEqualTo(DemoState.RESET);
        assertThat(context.actions).containsExactly("reset");
    }

    @Test
    void moreThanOneUnconditionalRowForOneKey_isRejectedAtBuild() {
        assertThatThrownBy(() -> FSMTransitionTable.<DemoState, FSMEvent, Recorder>builder()
                        .on(DemoState.A, Advance.class)
                        .then((current, event, recorder) -> DemoState.B)
                        .on(DemoState.A, Advance.class)
                        .otherwise((current, event, recorder) -> DemoState.C)
                        .unmatched((current, event, recorder) -> DemoState.RESET)
                        .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ambiguous");
    }

    @Test
    void missingUnmatchedAction_isRejectedAtBuild() {
        assertThatThrownBy(() -> FSMTransitionTable.<DemoState, FSMEvent, Recorder>builder()
                        .on(DemoState.A, Advance.class)
                        .then((current, event, recorder) -> DemoState.B)
                        .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unmatched");
    }

    private static @NotNull FSMTransitionTable<DemoState, FSMEvent, Recorder> guardedTable() {
        return FSMTransitionTable.<DemoState, FSMEvent, Recorder>builder()
                .on(DemoState.A, Advance.class)
                .when((current, event, recorder) -> recorder.flag)
                .then((current, event, recorder) -> {
                    recorder.actions.add("guarded");
                    return DemoState.B;
                })
                .on(DemoState.A, Advance.class)
                .otherwise((current, event, recorder) -> {
                    recorder.actions.add("default");
                    return DemoState.C;
                })
                .unmatched((current, event, recorder) -> DemoState.RESET)
                .build();
    }
}
