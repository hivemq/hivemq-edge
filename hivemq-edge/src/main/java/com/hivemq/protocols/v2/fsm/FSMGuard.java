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

/**
 * The condition that gates a transition (design §4). Several guarded rows may share one
 * {@code (state, eventType)} key; the table evaluates them in registration order and runs the first whose
 * guard passes. A guard must be side-effect free — it runs on the actor's dispatch thread and may be tested
 * without being chosen.
 *
 * @param <StateType>   the machine's state type.
 * @param <EventType>   the machine's event type.
 * @param <ContextType> the machine's context.
 */
@FunctionalInterface
public interface FSMGuard<StateType extends FSMState, EventType extends FSMEvent, ContextType> {

    /**
     * @param current the current machine state.
     * @param event   the event being dispatched.
     * @param context the machine context.
     * @return whether this transition applies.
     */
    boolean test(@NotNull StateType current, @NotNull EventType event, @NotNull ContextType context);
}
