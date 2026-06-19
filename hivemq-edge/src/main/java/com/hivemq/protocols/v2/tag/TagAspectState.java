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
package com.hivemq.protocols.v2.tag;

import com.hivemq.protocols.v2.fsm.FSMState;

/**
 * The common query interface every aspect-machine state implements (design §7.2). A {@code FSM} needs exactly one
 * state type, so each aspect variant has its own enum ({@code TagAspectReadPolledState},
 * {@code TagAspectReadSubscribedState}, and the write enum in a later task), each containing the five shared
 * pre-operating states plus its role-specific states. This interface lets the read-side view fold (design §7.7)
 * and the snapshot builder reason about any aspect's state without knowing its variant.
 */
public interface TagAspectState extends FSMState {

    /**
     * @return {@code true} when the aspect is at rest because its goal is not active (the three-condition rule
     *         fails) — it issues no work and reports no failure.
     */
    boolean isDeactivated();

    /**
     * @return {@code true} when the aspect is suspended after a permanent verification failure — it stays here
     *         until a user-commanded tag retry (design §7.6).
     */
    boolean isPermanentVerificationFailure();

    /**
     * @return {@code true} when the aspect is operating at its role's goal: a read aspect polling or subscribed,
     *         a write aspect ready for writes. In-flight work (awaiting a poll value or a write result) still
     *         counts as operating, so a tag does not flap to {@code ERROR} during a normal round-trip (§7.7).
     */
    boolean isOperating();
}
