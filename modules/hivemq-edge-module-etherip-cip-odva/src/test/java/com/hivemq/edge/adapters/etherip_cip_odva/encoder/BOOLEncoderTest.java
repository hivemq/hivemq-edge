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
package com.hivemq.edge.adapters.etherip_cip_odva.encoder;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class BOOLEncoderTest {

    private final BOOLEncoder encoder = new BOOLEncoder();

    private static CipTag bool(final int numberOfElements, final Integer bitIndex) {
        return new CipTag(
                "b", "b", new CipTagDefinition("@1/2/3", numberOfElements, CipDataType.BOOL, 0d, null, 0, bitIndex));
    }

    @Test
    void single_setsBitAtIndex() {
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] {0});
        encoder.encodeSingle(bool(1, 3), buf, true);
        assertThat(buf.get(0)).isEqualTo((byte) 0b0000_1000);
    }

    @Test
    void single_clearsBitAtIndex_forReadModifyWrite() {
        // byte already has bit 3 set (as if read from the device); writing false must clear it.
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] {(byte) 0b1111_1111});
        encoder.encodeSingle(bool(1, 3), buf, false);
        assertThat(buf.get(0)).isEqualTo((byte) 0b1111_0111);
    }

    @Test
    void single_noBitIndex_writesWholeByte() {
        final ByteBuffer bufTrue = ByteBuffer.wrap(new byte[] {0});
        encoder.encodeSingle(bool(1, null), bufTrue, true);
        assertThat(bufTrue.get(0)).isEqualTo((byte) 0xFF); // CIP_TRUE

        final ByteBuffer bufFalse = ByteBuffer.wrap(new byte[] {(byte) 0xFF});
        encoder.encodeSingle(bool(1, null), bufFalse, false);
        assertThat(bufFalse.get(0)).isEqualTo((byte) 0x00);
    }

    @Test
    void multiple_packsBitsAcrossBytes() {
        // 10 flags: bit i in byte i/8. true at 0, 2, 9.
        final List<Boolean> values = List.of(true, false, true, false, false, false, false, false, false, true);
        final ByteBuffer buf = ByteBuffer.wrap(new byte[] {0, 0});
        encoder.encodeMultiple(bool(10, null), buf, values);
        assertThat(buf.get(0)).isEqualTo((byte) 0b0000_0101); // bits 0 and 2
        assertThat(buf.get(1)).isEqualTo((byte) 0b0000_0010); // bit 9 -> bit 1 of byte 1
    }

    @Test
    void requestSizeMultiple_isCeilingDivisionByEight() {
        assertThat(encoder.getRequestSizeMultiple(bool(1, null), t -> List.of()))
                .isEqualTo(1);
        assertThat(encoder.getRequestSizeMultiple(bool(8, null), t -> List.of()))
                .isEqualTo(1);
        assertThat(encoder.getRequestSizeMultiple(bool(9, null), t -> List.of()))
                .isEqualTo(2);
        assertThat(encoder.getRequestSizeMultiple(bool(16, null), t -> List.of()))
                .isEqualTo(2);
    }
}
