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
package com.hivemq.protocols.v2.wrapper;

import com.hivemq.protocols.v2.fsm.FSMState;

/**
 * The ten states of the adapter machine (design §6.1, §6.2). Apart from {@link #STOPPED}, {@link #CONNECTED}, and
 * {@link #ERROR}, every state is a {@code WAITING_*} state: the machine has issued a command to the protocol
 * adapter and is waiting for the acknowledgment that drives the next step toward the goal. Each
 * acknowledgment-waiting state arms a watchdog on entry (design §6.3); {@link #WAITING_FOR_CONNECTION_RETRY} is
 * the one waiting state that is bounded by its backoff timer and the retry policy instead.
 */
public enum ProtocolAdapterWrapperState implements FSMState {
    /**
     * The resting state when the goal is "stopped": no resources allocated, no connection.
     */
    STOPPED,
    /**
     * {@code start()} issued; waiting for {@code started()}.
     */
    WAITING_FOR_STARTED,
    /**
     * {@code connect()} issued; waiting for {@code connected()}.
     */
    WAITING_FOR_CONNECTED,
    /**
     * {@code verifyBatch(...)} issued; waiting for every node to report a verification outcome (design §6.3).
     */
    WAITING_FOR_VERIFICATION,
    /**
     * The goal state when connected: tag aspects poll, subscribe, and write from here.
     */
    CONNECTED,
    /**
     * Waiting out the connection backoff before the next {@code connect()} (design §6.3). The one waiting state
     * with no watchdog — bounded by the backoff timer and the retry policy.
     */
    WAITING_FOR_CONNECTION_RETRY,
    /**
     * {@code disconnect()} issued on the way to {@link #STOPPED}; waiting for {@code disconnected()}.
     */
    WAITING_FOR_DISCONNECTED,
    /**
     * {@code disconnect()} issued on the way to a reconnect (after {@code error(CONNECTION)} or a verification
     * watchdog); waiting for {@code disconnected()} before backing off (design §6.2).
     */
    WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT,
    /**
     * {@code stop()} issued; waiting for {@code stopped()}.
     */
    WAITING_FOR_STOPPED,
    /**
     * The adapter and wrapper are no longer consistent (design §6.4). {@code ERROR} absorbs every protocol-adapter
     * event as a named, logged transition so the defensive reset fires at most once per incident. The only ways
     * out are a goal change to "stopped" and a manager-driven recreate.
     */
    ERROR
}
