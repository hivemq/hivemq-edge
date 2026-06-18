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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One row of a {@link FSMTransitionTable} (design §4): in state {@link #from()}, on an event of exactly type
 * {@link #eventType()}, when the optional {@link #guard()} passes, run {@link #action()} to compute the next
 * state. A {@code null} guard makes the row unconditional — the catch-all for its {@code (from, eventType)}
 * key, evaluated only after every guarded row for that key has failed. Built through
 * {@link FSMTransitionTable.Builder}, never constructed directly by machine authors.
 *
 * @param <StateType>   the machine's state type.
 * @param <EventType>   the machine's event type.
 * @param <ContextType> the machine's context.
 * @param from          the state this row applies in.
 * @param eventType     the exact event type this row matches.
 * @param guard         the optional gating condition; {@code null} for an unconditional row.
 * @param action        the behavior that runs and returns the next state.
 */
public record FSMTransition<StateType extends FSMState, EventType extends FSMEvent, ContextType>(
        @NotNull StateType from,
        @NotNull Class<? extends EventType> eventType,
        @Nullable FSMGuard<StateType, EventType, ContextType> guard,
        @NotNull FSMAction<StateType, EventType, ContextType> action) {

    /**
     * @return whether this row carries a guard; an unconditional (guardless) row is the catch-all for its
     *         {@code (from, eventType)} key.
     */
    public boolean guarded() {
        return guard != null;
    }
}
