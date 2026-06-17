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

import org.jetbrains.annotations.NotNull;

/**
 * The reusable state-machine engine (design §4). It owns the current state and drives it two ways:
 * <ul>
 * <li>{@link #onEvent(StateMachineEvent)} — table-driven. A protocol-adapter event or timer expiry runs
 * through the {@link TransitionTable}; an event with no matching row runs the table's {@code unmatched} action
 * (the defensive reset).</li>
 * <li>{@link #onGoalChange(Runnable)} — the goal-command bypass. Activation, stop, tag-set updates, and retry
 * are goal mutations, valid in <em>every</em> state; they run a caller-supplied mutation (mutate the goal,
 * then {@code stepTowardGoal}) that never consults the table, so a goal command can never trigger a defensive
 * reset (§4).</li>
 * </ul>
 * {@code stepTowardGoal} lives in the machine's context and advances the current state one step toward the
 * goal through {@link #transitionTo(StateMachineState)}. All methods run on the actor's single dispatch
 * thread; the machine holds no locks.
 *
 * @param <StateType>   the machine's state type.
 * @param <EventType>   the machine's event type.
 * @param <ContextType> the machine's context.
 */
public final class StateMachine<StateType extends StateMachineState, EventType extends StateMachineEvent, ContextType> {

    private final @NotNull TransitionTable<StateType, EventType, ContextType> table;
    private final @NotNull ContextType context;
    private @NotNull StateType state;

    /**
     * @param initial the starting state.
     * @param table   the transition table that drives {@link #onEvent(StateMachineEvent)}.
     * @param context the context the table's guards and actions act through.
     */
    public StateMachine(
            final @NotNull StateType initial,
            final @NotNull TransitionTable<StateType, EventType, ContextType> table,
            final @NotNull ContextType context) {
        this.state = initial;
        this.table = table;
        this.context = context;
    }

    /**
     * @return the current state.
     */
    public @NotNull StateType state() {
        return state;
    }

    /**
     * Feed a table event (a protocol-adapter event or timer expiry). The next state is the matched
     * transition's result, or the {@code unmatched} action's result when no row matches.
     *
     * @param event the event to dispatch.
     */
    public void onEvent(final @NotNull EventType event) {
        state = table.dispatch(state, event, context);
    }

    /**
     * Run a goal mutation that bypasses the transition table. The runnable mutates the goal (and aspect
     * goals) and then calls {@code stepTowardGoal}, which may advance the current state through
     * {@link #transitionTo(StateMachineState)}. Because it never consults the table, a goal command is valid
     * in every state and can never trigger the defensive reset.
     *
     * @param mutateGoalThenStep the goal mutation followed by {@code stepTowardGoal}.
     */
    public void onGoalChange(final @NotNull Runnable mutateGoalThenStep) {
        mutateGoalThenStep.run();
    }

    /**
     * Advance the current state directly — the primitive {@code stepTowardGoal} uses to take one step toward
     * the goal (for example {@code STOPPED → WAITING_FOR_STARTED} when the goal becomes {@code CONNECTED}).
     * Owner thread only.
     *
     * @param next the new current state.
     */
    public void transitionTo(final @NotNull StateType next) {
        state = next;
    }
}
