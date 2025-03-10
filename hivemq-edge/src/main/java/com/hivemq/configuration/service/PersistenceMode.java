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
package com.hivemq.configuration.service;

import org.jetbrains.annotations.NotNull;

public enum PersistenceMode {
    /**
     * All persistent data like queued messages, retained messages subscriptions and so on, will be stored in RAM.
     */
    IN_MEMORY,

    FILE_NATIVE,

    FILE;

    private static final @NotNull PersistenceMode @NotNull [] VALUES = values();

    public static @NotNull PersistenceMode forCode(final int code) {
        try {
            return VALUES[code];
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("No persistence type found for code: " + code, e);
        }
    }
}
