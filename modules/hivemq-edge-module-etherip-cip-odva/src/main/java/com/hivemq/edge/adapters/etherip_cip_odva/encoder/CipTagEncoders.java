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

import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.BOOL;
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.DINT;
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.INT;
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.LINT;
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.LREAL;
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.REAL;
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.SINT;
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.SSTRING;
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.STRING;
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.UDINT;
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.UINT;
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.ULINT;
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.USINT;

import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaEncodeException;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.CipTagValueProducer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

public class CipTagEncoders {

    private final Map<CipDataType, CipTagEncoder<?>> encoders = new EnumMap<>(CipDataType.class);

    public CipTagEncoders() {
        encoders.put(BOOL, new BOOLEncoder());
        encoders.put(SINT, new SINTEncoder());
        encoders.put(USINT, new SINTEncoder());
        encoders.put(INT, new INTEncoder());
        encoders.put(UINT, new INTEncoder());
        encoders.put(DINT, new DINTEncoder());
        encoders.put(UDINT, new DINTEncoder());
        encoders.put(LINT, new LINTEncoder());
        encoders.put(ULINT, new LINTEncoder());
        encoders.put(REAL, new REALEncoder());
        encoders.put(LREAL, new LREALEncoder());
        encoders.put(SSTRING, new SSTRINGEncoder());
        encoders.put(STRING, new STRINGEncoder());
    }

    @VisibleForTesting
    @SuppressWarnings("unchecked")
    <T> @NotNull CipTagEncoder<T> getEncoder(@NotNull final CipTag tag) throws OdvaException {
        CipTagDefinition definition = tag.getDefinition();
        CipTagEncoder<T> cipTagEncoder = (CipTagEncoder<T>) encoders.get(definition.getDataType());

        if (cipTagEncoder == null) {
            throw new OdvaEncodeException(tag, "No encoder found for CIP type '" + definition.getDataType() + "'");
        }
        return cipTagEncoder;
    }

    public void encode(
            final @NotNull List<CipTag> tags,
            final @NotNull ByteBuffer buf,
            @NotNull ByteOrder byteOrder,
            final @NotNull CipTagValueProducer<Object> valueProducer)
            throws OdvaException {

        int starPosition = buf.position();

        ByteOrder currentOrder = buf.order();
        buf.order(byteOrder);

        for (CipTag tag : tags) {
            try {
                // FIXME: should it be here? Move inside CipTagEncoder?
                buf.position(starPosition + tag.getDefinition().getBatchByteIndex());

                getEncoder(tag).encode(tag, buf, valueProducer);
            } catch (OdvaException e) {
                throw e;
            } catch (Exception e) {
                throw new OdvaEncodeException(tag, e);
            }
        }

        buf.order(currentOrder);
    }

    public int getRequestSize(
            final @NotNull List<CipTag> tags, final @NotNull CipTagValueProducer<Object> valueProducer) {

        try {
            int maxRequestSize = 0;
            for (CipTag cipTag : tags) {
                maxRequestSize = Math.max(maxRequestSize, getEncoder(cipTag).getRequestSize(cipTag, valueProducer));
            }
            return maxRequestSize;

        } catch (OdvaException e) {
            // Have to swallow as it cannot be thrown due to compatibility with etherIp method. Happens only for unknown
            // DataType in getEncoder. Silently swallow for ProtocolAdapter compatibility. Will be repeated in encode()
            return -1;
        }
    }
}
