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

import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.encoder.CipTagEncoders;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.junit.jupiter.api.Test;

class CipTagEncodingAttributeProtocolTest {

    // A single DINT (4 bytes, little-endian) at byte offset 4 of an 8-byte attribute.
    private static CipTag dintAtOffset4() {
        return new CipTag("d", "d", new CipTagDefinition("@1/2/3", 1, CipDataType.DINT, 0d, null, 4, null));
    }

    private static byte[] run(final CipTagEncodingAttributeProtocol protocol) throws Exception {
        final int size = protocol.getRequestSize();
        final ByteBuffer buf = ByteBuffer.allocate(size);
        protocol.encode(buf, new StringBuilder());
        final byte[] out = new byte[size];
        buf.position(0);
        buf.get(out, 0, size);
        return out;
    }

    @Test
    void completeWrite_zeroesUnsuppliedBytes() throws Exception {
        final CipTag tag = dintAtOffset4();
        final CipTagEncodingAttributeProtocol protocol = new CipTagEncodingAttributeProtocol(
                new CipTagEncoders(), List.of(tag), ByteOrder.LITTLE_ENDIAN, t -> 1L);

        final byte[] out = run(protocol);

        // bytes 0-3 (not supplied) are zero, bytes 4-7 hold the DINT value 1 little-endian
        assertThat(out).startsWith((byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 0, (byte) 0);
    }

    @Test
    void partialWrite_preservesUnsuppliedBytes() throws Exception {
        final CipTag tag = dintAtOffset4();
        // current attribute: bytes 0-3 carry meaningful data we must not clobber.
        final byte[] prefill = new byte[] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, 9, 9, 9, 9};

        final CipTagEncodingAttributeProtocol protocol = new CipTagEncodingAttributeProtocol(
                new CipTagEncoders(), List.of(tag), ByteOrder.LITTLE_ENDIAN, t -> 1L, prefill);

        final byte[] out = run(protocol);

        // bytes 0-3 preserved from prefill; bytes 4-7 overwritten with the DINT value 1 little-endian
        assertThat(out)
                .containsExactly(
                        (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 1, (byte) 0, (byte) 0, (byte) 0);
    }
}
