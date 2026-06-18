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

/**
 * A communication direction an adapter goal can activate (design §6.1, §7.1): {@code NORTHBOUND} is the read
 * side (adapter → MQTT), {@code SOUTHBOUND} is the write side (MQTT → adapter), and {@code BOTH} addresses both
 * in one command. {@link ProtocolAdapterWrapperCommand.ActivateDirection} and
 * {@link ProtocolAdapterWrapperCommand.DeactivateDirection} carry one of these.
 * <p>
 * Defined here because the wrapper is the first component to need it; the manager (a later task) reuses the same
 * type rather than declaring its own (rule N1).
 */
public enum ProtocolAdapterDirection {
    /**
     * The read side — adapter to MQTT. Gated by {@code northboundActivated} (design §7.1).
     */
    NORTHBOUND,
    /**
     * The write side — MQTT to adapter. Gated by {@code southboundActivated} (design §7.1).
     */
    SOUTHBOUND,
    /**
     * Both directions at once — a convenience for a single REST command that activates or deactivates the whole
     * adapter.
     */
    BOTH
}
