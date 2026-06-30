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

import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.CipTagValueProducer;
import java.nio.ByteBuffer;
import java.util.List;
import org.jetbrains.annotations.NotNull;

class BOOLEncoder extends BaseStaticCipTagEncoder<Boolean> {
    public static final byte CIP_FALSE = 0;
    public static final byte CIP_TRUE = -1; // 255

    @Override
    public void encodeSingle(
            final @NotNull CipTag cipTag, final @NotNull ByteBuffer buf, final @NotNull Boolean value) {
        int position = buf.position();
        CipTagDefinition definition = cipTag.getDefinition();

        if (definition.getBatchBitIndex() != null) {
            setBit(position, buf, definition.getBatchBitIndex(), value);
        } else {
            buf.put(value ? CIP_TRUE : CIP_FALSE);
        }
    }

    @Override
    public void encodeMultiple(
            final @NotNull CipTag cipTag, final @NotNull ByteBuffer buf, final @NotNull List<Boolean> values) {

        // Pack the flags into consecutive bits, mirroring the decoder: element i lives in bit (i % 8) of the
        // (i / 8)-th byte from the tag's start position.
        final int start = buf.position();
        for (int i = 0; i < values.size(); i++) {
            setBit(start + (i / 8), buf, i % 8, values.get(i));
        }
    }

    private static void setBit(int position, ByteBuffer buf, int bitIndex, Boolean value) {
        // Set or clear the bit. Clearing matters for read-modify-write, where the byte is pre-filled with the
        // current attribute value and a false must actually clear an already-set bit.
        final byte current = buf.get(position);
        final byte updated =
                Boolean.TRUE.equals(value) ? (byte) (current | (1 << bitIndex)) : (byte) (current & ~(1 << bitIndex));
        buf.put(position, updated);
    }

    @Override
    protected int getStaticRequestSizeSingle() {
        return 1;
    }

    @Override
    public int getRequestSizeSingle(
            final @NotNull CipTag cipTag, final @NotNull CipTagValueProducer<Boolean> valueProducer) {
        return 1;
    }

    @Override
    public int getRequestSizeMultiple(
            final @NotNull CipTag cipTag, final @NotNull CipTagValueProducer<List<Boolean>> valueProducer) {

        // Number of bytes needed to hold numberOfElements consecutive bits (ceiling division).
        final int numberOfElements = cipTag.getDefinition().getNumberOfElements();
        return (numberOfElements + 7) / 8;
    }
}
