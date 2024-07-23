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
package com.hivemq.edge.adapters.opcua.writing;

public enum OpcUaValueType {

    INTEGER(true),
    UINTEGER(false),

    BOOLEAN(true),
    BYTE(false),
    UBYTE(false),

    Short(false),
    USHORT(false),

    // LONG is kind of primitive to Jackson, but if the long fits into a Integer, it will create a Integer. Not sure if OpcUA would have a problem with that.
    LONG(false),
    ULONG(false),

    // default for floating point numbers in jackson is double, not float.
    FLOAT(false),
    DOUBLE(true),

    STRING(true),
    CUSTOM_STRUCT(false);

    private final boolean jacksonDefaultPrimitive;

    OpcUaValueType(final boolean jacksonDefaultPrimitive) {
        this.jacksonDefaultPrimitive = jacksonDefaultPrimitive;
    }

    public boolean isJacksonDefaultPrimitive() {
        return jacksonDefaultPrimitive;
    }
}
