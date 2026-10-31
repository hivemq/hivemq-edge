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
package com.hivemq.edge.adapters.etherip_cip_odva.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import etherip.types.LogicalAddressPath;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LogicalAddressPathFactoryTest {

    @Test
    void shouldCreate() throws OdvaException {
        // test 1
        LogicalAddressPath logicalAddressPath = LogicalAddressPathFactory.create("@1/2/3");
        Assertions.assertThat(logicalAddressPath.getClass_code()).isEqualTo(1);
        Assertions.assertThat(logicalAddressPath.getInstance()).isEqualTo(2);
        Assertions.assertThat(logicalAddressPath.getAttr()).isEqualTo(3);

        // test 2
        logicalAddressPath = LogicalAddressPathFactory.create("@11111/22222/33333");
        Assertions.assertThat(logicalAddressPath.getClass_code()).isEqualTo(11111);
        Assertions.assertThat(logicalAddressPath.getInstance()).isEqualTo(22222);
        Assertions.assertThat(logicalAddressPath.getAttr()).isEqualTo(33333);
    }

    @Test
    void shouldFailCreate() {
        Assertions.assertThatThrownBy(() -> LogicalAddressPathFactory.create(null))
                .isInstanceOf(OdvaException.class);
        Assertions.assertThatThrownBy(() -> LogicalAddressPathFactory.create(""))
                .isInstanceOf(OdvaException.class);
        Assertions.assertThatThrownBy(() -> LogicalAddressPathFactory.create("@1/2/"))
                .isInstanceOf(OdvaException.class);
        Assertions.assertThatThrownBy(() -> LogicalAddressPathFactory.create("@1/b/3"))
                .isInstanceOf(OdvaException.class);
    }

    /// Guards EDG-1181: a tag address must never reach the device as a different object. A wrong object means a
    /// read publishes another object's value under the tag's name, and a write changes an object nobody configured.
    @ParameterizedTest
    @CsvSource({
        "@70000/1/1, 70000, 1, 1",
        "@300/70000/1, 300, 70000, 1",
        "@300/65536/1, 300, 65536, 1",
        "@1/1/70000, 1, 1, 70000",
    })
    void address_beyond16Bits_isRefusedOrSentUnchanged(
            final String address, final long classCode, final long instance, final long attribute) {
        final LogicalAddressPath path;
        try {
            path = LogicalAddressPathFactory.create(address);
        } catch (final OdvaException refused) {
            return;
        }

        assertThat(onTheWire(path))
                .as("tag address %s must be refused or sent unchanged, never as another object", address)
                .containsExactly(classCode, instance, attribute);
    }

    /// Reads class, instance and attribute back from the encoded CIP path: 8-, 16- or 32-bit logical segments.
    private static long[] onTheWire(final LogicalAddressPath path) {
        final ByteBuffer buf = ByteBuffer.allocate(1 + path.getRequestSize()).order(ByteOrder.LITTLE_ENDIAN);
        path.encode(buf, null);
        buf.flip();
        buf.get(); // path size in words
        final long[] values = new long[3];
        while (buf.hasRemaining()) {
            final int segment = buf.get() & 0xFF;
            final int format = segment & 0x03;
            if (format != 0) {
                buf.get(); // pad byte
            }
            final long value =
                    switch (format) {
                        case 0 -> buf.get() & 0xFF;
                        case 1 -> buf.getShort() & 0xFFFF;
                        default -> buf.getInt() & 0xFFFFFFFFL;
                    };
            switch (segment & 0xFC) {
                case 0x20 -> values[0] = value;
                case 0x24 -> values[1] = value;
                case 0x30 -> values[2] = value;
                default -> throw new AssertionError("unexpected segment 0x" + Integer.toHexString(segment));
            }
        }
        return values;
    }
}
