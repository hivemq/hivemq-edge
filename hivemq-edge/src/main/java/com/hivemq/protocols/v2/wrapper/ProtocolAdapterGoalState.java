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
 * The adapter goal: the commanded state the wrapper continuously steps toward. The adapter must be
 * connected exactly when at least one direction is activated; when neither is, the goal is to be stopped.
 * <p>
 * Initial values come from the {@code <v2>} configuration section. A REST direction activation
 * changes the <em>live</em> goal only and is never persisted — after a restart the config-declared flags apply
 * again (D7).
 *
 * @param northboundActivated whether the read side (adapter → MQTT) is activated.
 * @param southboundActivated whether the write side (MQTT → adapter) is activated.
 */
public record ProtocolAdapterGoalState(boolean northboundActivated, boolean southboundActivated) {

    /**
     * @return the goal in which neither direction is activated — the adapter should be stopped.
     */
    public static ProtocolAdapterGoalState stopped() {
        return new ProtocolAdapterGoalState(false, false);
    }

    /**
     * @return {@code true} when the adapter should be connected — i.e. at least one direction is activated.
     */
    public boolean wantConnected() {
        return northboundActivated || southboundActivated;
    }

    /**
     * @param direction the direction to activate ({@link ProtocolAdapterDirection#BOTH} activates both).
     * @return a copy of this goal with the given direction activated.
     */
    public ProtocolAdapterGoalState withActivated(final ProtocolAdapterDirection direction) {
        return switch (direction) {
            case NORTHBOUND -> new ProtocolAdapterGoalState(true, southboundActivated);
            case SOUTHBOUND -> new ProtocolAdapterGoalState(northboundActivated, true);
            case BOTH -> new ProtocolAdapterGoalState(true, true);
        };
    }

    /**
     * @param direction the direction to deactivate ({@link ProtocolAdapterDirection#BOTH} deactivates both).
     * @return a copy of this goal with the given direction deactivated.
     */
    public ProtocolAdapterGoalState withDeactivated(final ProtocolAdapterDirection direction) {
        return switch (direction) {
            case NORTHBOUND -> new ProtocolAdapterGoalState(false, southboundActivated);
            case SOUTHBOUND -> new ProtocolAdapterGoalState(northboundActivated, false);
            case BOTH -> new ProtocolAdapterGoalState(false, false);
        };
    }
}
