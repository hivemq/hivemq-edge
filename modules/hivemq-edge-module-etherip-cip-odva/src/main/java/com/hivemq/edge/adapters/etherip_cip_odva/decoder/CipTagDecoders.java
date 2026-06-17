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
package com.hivemq.edge.adapters.etherip_cip_odva.decoder;

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
import static com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType.USINT;

import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaDecodeException;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.CipTagValueConsumer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public class CipTagDecoders {
    private final Map<CipDataType, CipTagDecoder<?>> decoders = new EnumMap<>(CipDataType.class);

    private final Map<CipDataType, ListOfDecoder<?>> listDecoders = new EnumMap<>(CipDataType.class);

    public CipTagDecoders() {
        decoders.put(BOOL, new BOOLDecoder());
        decoders.put(SINT, new SINTDecoder());
        decoders.put(USINT, new USINTDecoder());
        decoders.put(INT, new INTDecoder());
        decoders.put(UINT, new UINTDecoder());
        decoders.put(DINT, new DINTDecoder());
        decoders.put(UDINT, new UDINTDecoder());
        decoders.put(LINT, new LINTDecoder());
        decoders.put(REAL, new REALDecoder());
        decoders.put(LREAL, new LREALDecoder());
        decoders.put(SSTRING, new SSTRINGDecoder());
        decoders.put(STRING, new STRINGDecoder());
    }

    @VisibleForTesting
    <T> @NotNull CipTagDecoder<T> getDecoder(@NotNull final CipTag tag) throws OdvaDecodeException {
        CipDataType dataType = tag.getDefinition().getDataType();
        CipTagDecoder<T> cipTagDecoder = (CipTagDecoder<T>) decoders.get(dataType);

        if (cipTagDecoder == null) {
            throw new OdvaDecodeException(tag, "No decoder found for CIP type '" + dataType + "'");
        }

        if (tag.getDefinition().getNumberOfElements() > 1) {
            return (CipTagDecoder<T>)
                    listDecoders.computeIfAbsent(dataType, currentDataType -> new ListOfDecoder<>(cipTagDecoder));
        } else {
            return cipTagDecoder;
        }
    }

    public void decode(
            @NotNull List<CipTag> tags,
            @NotNull ByteBuffer buf,
            @NotNull ByteOrder byteOrder,
            @Nullable StringBuilder log,
            @NotNull CipTagValueConsumer<Object> valueConsumer)
            throws OdvaException {

        ByteOrder currentOrder = buf.order();
        try {
            buf.order(byteOrder);
            decode(tags, buf, log, valueConsumer, buf.position());
        } finally {
            buf.order(currentOrder);
        }
    }

    private void decode(
            @NotNull List<CipTag> tags,
            @NotNull ByteBuffer buf,
            @Nullable StringBuilder log,
            @NotNull CipTagValueConsumer<Object> valueConsumer,
            int startPosition)
            throws OdvaException {

        for (CipTag tag : tags) {
            CipTagDefinition definition = tag.getDefinition();

            if (tag.isComposite()) {
                continue;
            }

            buf.position(startPosition + definition.getBatchByteIndex());
            getDecoder(tag).decode(tag, buf, log, valueConsumer);
        }
    }
}
