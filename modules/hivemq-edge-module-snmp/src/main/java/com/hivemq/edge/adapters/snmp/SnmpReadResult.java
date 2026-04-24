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
package com.hivemq.edge.adapters.snmp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Carries the result of an SNMP GET operation, including the converted value
 * and protocol-level metadata captured from the SNMP response PDU.
 */
public class SnmpReadResult {

    private final @Nullable Object value;
    private final @NotNull String rawType;
    private final int errorStatus;
    private final int errorIndex;

    public SnmpReadResult(
            final @Nullable Object value, final @NotNull String rawType, final int errorStatus, final int errorIndex) {
        this.value = value;
        this.rawType = rawType;
        this.errorStatus = errorStatus;
        this.errorIndex = errorIndex;
    }

    /**
     * The converted Java value (Integer, Long, Double, String, etc.).
     */
    public @Nullable Object getValue() {
        return value;
    }

    /**
     * The SNMP4J class simple name for the variable binding type
     * (e.g. "TimeTicks", "Counter32", "Integer32", "OctetString").
     */
    public @NotNull String getRawType() {
        return rawType;
    }

    /**
     * The PDU error status (0 = noError for successful reads).
     */
    public int getErrorStatus() {
        return errorStatus;
    }

    /**
     * The PDU error index (0 for successful reads).
     */
    public int getErrorIndex() {
        return errorIndex;
    }
}
