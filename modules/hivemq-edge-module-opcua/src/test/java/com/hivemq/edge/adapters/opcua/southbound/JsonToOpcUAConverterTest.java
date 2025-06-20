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
package com.hivemq.edge.adapters.opcua.southbound;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonToOpcUAConverterTest {

    @Test
    void extractUShort() {
        final UShort value = JsonToOpcUAConverterUtil.extractUShort(IntNode.valueOf(12));
        assertEquals(12, value.intValue());
    }

    @Test
    void extractUShort_overflow() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonToOpcUAConverterUtil.extractUShort(IntNode.valueOf(Integer.MAX_VALUE)));
    }

    @Test
    void extractUShort_underflow() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonToOpcUAConverterUtil.extractUShort(IntNode.valueOf(Integer.MIN_VALUE)));
    }

    @Test
    void extractSignedShort() {
        final int value = JsonToOpcUAConverterUtil.extractSignedShort(IntNode.valueOf(12));
        assertEquals(12, value);
    }

    @Test
    void extractSignedShort_overflow() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonToOpcUAConverterUtil.extractSignedShort(IntNode.valueOf(Integer.MAX_VALUE)));
    }

    @Test
    void extractSignedShort_underflow() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonToOpcUAConverterUtil.extractSignedShort(IntNode.valueOf(Integer.MIN_VALUE)));
    }

    @Test
    void extractUInteger() {
        final UInteger value = JsonToOpcUAConverterUtil.extractUInteger(IntNode.valueOf(12));
        assertEquals(12, value.intValue());
    }

    @Test
    void extractUInteger_overflow() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonToOpcUAConverterUtil.extractUInteger(LongNode.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void extractUInteger_underflow() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonToOpcUAConverterUtil.extractUInteger(LongNode.valueOf(Long.MIN_VALUE)));
    }


    @Test
    void extractSignedInteger() {
        final int value = JsonToOpcUAConverterUtil.extractSignedInteger(IntNode.valueOf(12));
        assertEquals(12, value);
    }

    @Test
    void extractSignedInteger_overflow() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonToOpcUAConverterUtil.extractSignedInteger(LongNode.valueOf(Integer.MAX_VALUE + 1000L)));
    }


    @Test
    void extractSByte() {
        final byte value = JsonToOpcUAConverterUtil.extractSByte(IntNode.valueOf(12));
        assertEquals(12, value);
    }

    @Test
    void extractUnsignedByte() {
        final @NotNull UByte value = JsonToOpcUAConverterUtil.extractUnsignedByte(IntNode.valueOf(12));
        assertEquals(12, value.intValue());
    }

    @Test
    void extractFloat() {
        final float value = JsonToOpcUAConverterUtil.extractFloat(DoubleNode.valueOf(12.2));
        assertEquals(12.2, value, 0.0001);
    }
}
