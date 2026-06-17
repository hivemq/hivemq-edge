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
 * The behavior a matched transition runs (design §4). An action may issue commands and arm or cancel timers
 * through the context, and returns the machine's next state. It runs on the actor's single dispatch thread.
 *
 * @param <StateType>   the machine's state type.
 * @param <EventType>   the machine's event type.
 * @param <ContextType> the machine's context — the bag of collaborators an action acts through.
 */
@FunctionalInterface
public interface Action<StateType extends StateMachineState, EventType extends StateMachineEvent, ContextType> {

    /**
     * @param current the state the machine was in when the event arrived.
     * @param event   the event that matched this transition.
     * @param context the machine context.
     * @return the next state.
     */
    @NotNull
    StateType apply(@NotNull StateType current, @NotNull EventType event, @NotNull ContextType context);
}
