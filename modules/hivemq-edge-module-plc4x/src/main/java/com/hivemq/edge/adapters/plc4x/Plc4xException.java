/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.plc4x;

/**
 * Exception thrown when a PLC4X operation fails.
 *
 * @author Simon L Johnson
 */
public class Plc4xException extends Exception {
    public Plc4xException() {}

    public Plc4xException(final String message) {
        super(message);
    }

    public Plc4xException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public Plc4xException(final Throwable cause) {
        super(cause);
    }
}
