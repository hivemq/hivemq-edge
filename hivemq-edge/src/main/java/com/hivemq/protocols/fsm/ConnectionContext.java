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
package com.hivemq.protocols.fsm;

import org.jetbrains.annotations.NotNull;

/**
 * Context provided during {@link ProtocolAdapter2#connect} and {@link ProtocolAdapter2#disconnect} operations.
 */
public interface ConnectionContext {

    enum Direction {
        NORTHBOUND,
        SOUTHBOUND
    }

    /**
     * @return the direction of this connection (northbound = device to MQTT, southbound = MQTT to device)
     */
    @NotNull
    Direction getDirection();

    /**
     * Create a {@link ConnectionContext} for the given direction.
     */
    static @NotNull ConnectionContext of(final @NotNull Direction direction) {
        return () -> direction;
    }
}
