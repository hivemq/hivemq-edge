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
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaEncodeException;
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
            final @NotNull CipTag cipTag, final @NotNull ByteBuffer buf, final @NotNull List<Boolean> values)
            throws OdvaEncodeException {

        // FIXME: Implement
        throw new OdvaEncodeException(cipTag, "Not implemented");
    }

    private void setBit(int position, ByteBuffer buf, Integer batchBitIndex, Boolean value) {
        // FIXME: Check bit index?
        if (Boolean.TRUE.equals(value)) {
            // FIXME: limit bit index to 0 to 7?
            buf.put(position, (byte) (buf.get(position) | (1 << batchBitIndex)));
        }
        //        else {
        // NOOP - already 0
        //            buf.put(position, (byte)(buf.get(position) & (0 << batchBitIndex)));
        //       }
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

        CipTagDefinition definition = cipTag.getDefinition();
        // FIXME: Do we want to take into account BitIndex?
        Integer batchBitIndex = definition.getBatchBitIndex() != null ? definition.getBatchBitIndex() : 0;
        return ((definition.getNumberOfElements() + batchBitIndex) % 8) + 1;
    }
}
