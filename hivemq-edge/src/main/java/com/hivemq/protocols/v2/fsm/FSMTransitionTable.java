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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The declarative transition table at the heart of the SOLID state-machine engine (design §4): a set of
 * {@link FSMTransition} rows plus a mandatory {@code unmatched} action (the defensive-reset slot). New machines
 * are new tables, not new engine code.
 * <p>
 * {@link #dispatch(FSMState, FSMEvent, Object)} looks up the rows for the current state and
 * the event's exact runtime type, evaluates their guards in registration order, and runs the first whose
 * guard passes. A single guardless row for that key is the catch-all, tried only after every guarded row for
 * the key has failed. When nothing matches — no row for the {@code (state, eventType)} key, or every guarded
 * row failed and there is no guardless row — the {@code unmatched} action runs; for the adapter machine that
 * is the defensive reset (§6.4). Goal mutations never reach this table: they go through
 * {@link FSM#onGoalChange(Runnable)} and so can never trigger the defensive reset (§4).
 * <p>
 * A table is immutable once {@link Builder#build()} returns. The builder fails fast on an ambiguous table
 * (more than one guardless row for the same {@code (state, eventType)} key) and on a missing {@code unmatched}
 * action.
 *
 * @param <StateType>   the machine's state type.
 * @param <EventType>   the machine's event type.
 * @param <ContextType> the machine's context.
 */
public final class FSMTransitionTable<StateType extends FSMState, EventType extends FSMEvent, ContextType> {

    private final @NotNull Map<StateType, List<FSMTransition<StateType, EventType, ContextType>>> rowsByState;
    private final @NotNull FSMAction<StateType, EventType, ContextType> unmatched;

    private FSMTransitionTable(
            final @NotNull Map<StateType, List<FSMTransition<StateType, EventType, ContextType>>> rowsByState,
            final @NotNull FSMAction<StateType, EventType, ContextType> unmatched) {
        this.rowsByState = rowsByState;
        this.unmatched = unmatched;
    }

    /**
     * @param <StateType>   the machine's state type.
     * @param <EventType>   the machine's event type.
     * @param <ContextType> the machine's context.
     * @return a fresh builder for a table over the given state, event, and context types.
     */
    public static <StateType extends FSMState, EventType extends FSMEvent, ContextType> @NotNull
            Builder<StateType, EventType, ContextType> builder() {
        return new Builder<>();
    }

    /**
     * Compute the next state for an event arriving in {@code current}. Runs on the actor's dispatch thread.
     *
     * @param current the current machine state.
     * @param event   the event to dispatch.
     * @param context the machine context the matched action acts through.
     * @return the matched action's next state, or the {@code unmatched} action's next state when no row
     *         matches.
     */
    public @NotNull StateType dispatch(
            final @NotNull StateType current, final @NotNull EventType event, final @NotNull ContextType context) {
        final List<FSMTransition<StateType, EventType, ContextType>> rows = rowsByState.get(current);
        if (rows != null) {
            FSMTransition<StateType, EventType, ContextType> guardless = null;
            for (final FSMTransition<StateType, EventType, ContextType> row : rows) {
                if (!row.eventType().equals(event.getClass())) {
                    continue;
                }
                final FSMGuard<StateType, EventType, ContextType> guard = row.guard();
                if (guard == null) {
                    guardless = row;
                } else if (guard.test(current, event, context)) {
                    return row.action().apply(current, event, context);
                }
            }
            if (guardless != null) {
                return guardless.action().apply(current, event, context);
            }
        }
        return unmatched.apply(current, event, context);
    }

    /**
     * Fluent builder for a {@link FSMTransitionTable}. Not thread-safe — build once at construction time.
     *
     * @param <StateType>   the machine's state type.
     * @param <EventType>   the machine's event type.
     * @param <ContextType> the machine's context.
     */
    public static final class Builder<StateType extends FSMState, EventType extends FSMEvent, ContextType> {

        private final @NotNull List<FSMTransition<StateType, EventType, ContextType>> rows = new ArrayList<>();
        private @Nullable FSMAction<StateType, EventType, ContextType> unmatched;

        private Builder() {}

        /**
         * Begin a row for {@code (state, eventType)}. Continue with {@link OnClause#when(FSMGuard)} for a guarded
         * row, or {@link OnClause#then(FSMAction)} / {@link OnClause#otherwise(FSMAction)} for the unconditional
         * row.
         *
         * @param state     the state the row applies in.
         * @param eventType the exact event type the row matches.
         * @return the clause to complete the row.
         */
        public @NotNull OnClause<StateType, EventType, ContextType> on(
                final @NotNull StateType state, final @NotNull Class<? extends EventType> eventType) {
            return new OnClause<>(this, state, eventType);
        }

        /**
         * Set the mandatory action run when no row matches — the defensive-reset slot.
         *
         * @param action the unmatched action.
         * @return this builder.
         */
        public @NotNull Builder<StateType, EventType, ContextType> unmatched(
                final @NotNull FSMAction<StateType, EventType, ContextType> action) {
            this.unmatched = action;
            return this;
        }

        /**
         * @return the immutable table.
         * @throws IllegalStateException if no {@code unmatched} action was set, or the table is ambiguous —
         *                               more than one guardless row for one {@code (state, eventType)} key.
         */
        public @NotNull FSMTransitionTable<StateType, EventType, ContextType> build() {
            final FSMAction<StateType, EventType, ContextType> resolvedUnmatched = unmatched;
            if (resolvedUnmatched == null) {
                throw new IllegalStateException(
                        "a FSMTransitionTable requires an unmatched action (the defensive-reset slot)");
            }
            final Map<StateType, Set<Class<? extends EventType>>> guardlessSeen = new HashMap<>();
            final Map<StateType, List<FSMTransition<StateType, EventType, ContextType>>> byState = new HashMap<>();
            for (final FSMTransition<StateType, EventType, ContextType> row : rows) {
                if (!row.guarded()
                        && !guardlessSeen
                                .computeIfAbsent(row.from(), key -> new HashSet<>())
                                .add(row.eventType())) {
                    throw new IllegalStateException(
                            "ambiguous transition table: more than one unconditional transition for state "
                                    + row.from()
                                    + " on event "
                                    + row.eventType().getSimpleName());
                }
                byState.computeIfAbsent(row.from(), key -> new ArrayList<>()).add(row);
            }
            return new FSMTransitionTable<>(byState, resolvedUnmatched);
        }

        private void add(final @NotNull FSMTransition<StateType, EventType, ContextType> row) {
            rows.add(row);
        }
    }

    /**
     * The clause returned by {@link Builder#on}: gate the row with {@link #when(FSMGuard)}, or register the
     * unconditional row with {@link #then(FSMAction)} / {@link #otherwise(FSMAction)}.
     *
     * @param <StateType>   the machine's state type.
     * @param <EventType>   the machine's event type.
     * @param <ContextType> the machine's context.
     */
    public static final class OnClause<StateType extends FSMState, EventType extends FSMEvent, ContextType> {

        private final @NotNull Builder<StateType, EventType, ContextType> builder;
        private final @NotNull StateType state;
        private final @NotNull Class<? extends EventType> eventType;

        private OnClause(
                final @NotNull Builder<StateType, EventType, ContextType> builder,
                final @NotNull StateType state,
                final @NotNull Class<? extends EventType> eventType) {
            this.builder = builder;
            this.state = state;
            this.eventType = eventType;
        }

        /**
         * Gate this row; complete it with {@link GuardedClause#then(FSMAction)}.
         *
         * @param guard the gating condition.
         * @return the guarded clause.
         */
        public @NotNull GuardedClause<StateType, EventType, ContextType> when(
                final @NotNull FSMGuard<StateType, EventType, ContextType> guard) {
            return new GuardedClause<>(builder, state, eventType, guard);
        }

        /**
         * Register the unconditional row for this {@code (state, eventType)} and return to the builder.
         *
         * @param action the behavior that runs and returns the next state.
         * @return the builder.
         */
        public @NotNull Builder<StateType, EventType, ContextType> then(
                final @NotNull FSMAction<StateType, EventType, ContextType> action) {
            builder.add(new FSMTransition<>(state, eventType, null, action));
            return builder;
        }

        /**
         * Alias of {@link #then(FSMAction)} that reads as the explicit default when guarded rows precede it.
         *
         * @param action the behavior that runs and returns the next state.
         * @return the builder.
         */
        public @NotNull Builder<StateType, EventType, ContextType> otherwise(
                final @NotNull FSMAction<StateType, EventType, ContextType> action) {
            return then(action);
        }
    }

    /**
     * The clause returned by {@link OnClause#when}: complete the guarded row with {@link #then(FSMAction)}.
     *
     * @param <StateType>   the machine's state type.
     * @param <EventType>   the machine's event type.
     * @param <ContextType> the machine's context.
     */
    public static final class GuardedClause<StateType extends FSMState, EventType extends FSMEvent, ContextType> {

        private final @NotNull Builder<StateType, EventType, ContextType> builder;
        private final @NotNull StateType state;
        private final @NotNull Class<? extends EventType> eventType;
        private final @NotNull FSMGuard<StateType, EventType, ContextType> guard;

        private GuardedClause(
                final @NotNull Builder<StateType, EventType, ContextType> builder,
                final @NotNull StateType state,
                final @NotNull Class<? extends EventType> eventType,
                final @NotNull FSMGuard<StateType, EventType, ContextType> guard) {
            this.builder = builder;
            this.state = state;
            this.eventType = eventType;
            this.guard = guard;
        }

        /**
         * Register the guarded row and return to the builder.
         *
         * @param action the behavior that runs and returns the next state when the guard passes.
         * @return the builder.
         */
        public @NotNull Builder<StateType, EventType, ContextType> then(
                final @NotNull FSMAction<StateType, EventType, ContextType> action) {
            builder.add(new FSMTransition<>(state, eventType, guard, action));
            return builder;
        }
    }
}
