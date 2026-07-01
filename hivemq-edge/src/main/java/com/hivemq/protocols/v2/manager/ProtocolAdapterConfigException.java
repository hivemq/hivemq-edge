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
package com.hivemq.protocols.v2.manager;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown by the wrapper factory when an otherwise well-formed adapter configuration cannot be turned into a running
 * wrapper — for example a {@code node-string} that does not deserialize into the adapter type's node class, or an
 * {@code adapter-configuration} that does not match the adapter type's configuration schema. The manager catches it
 * and surfaces the adapter as an {@code ERROR} registry handle with the message, rather than failing the whole
 * reload.
 */
public class ProtocolAdapterConfigException extends RuntimeException {

    /**
     * @param message a human-readable description of why the configuration could not be instantiated.
     */
    public ProtocolAdapterConfigException(final @NotNull String message) {
        super(message);
    }

    /**
     * @param message a human-readable description of why the configuration could not be instantiated.
     * @param cause   the underlying failure.
     */
    public ProtocolAdapterConfigException(final @NotNull String message, final @NotNull Throwable cause) {
        super(message, cause);
    }
}
