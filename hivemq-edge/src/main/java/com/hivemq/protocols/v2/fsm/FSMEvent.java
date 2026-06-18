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

/**
 * Marker for an event fed to a {@link FSM} through {@link FSM#onEvent(FSMEvent)}
 * (design §4). Events — protocol-adapter acknowledgments, errors, and timer expiries — are the only inputs
 * that flow through the {@link FSMTransitionTable}; goal mutations bypass the table via
 * {@link FSM#onGoalChange(Runnable)} and can never trigger the defensive reset.
 */
public interface FSMEvent {}
