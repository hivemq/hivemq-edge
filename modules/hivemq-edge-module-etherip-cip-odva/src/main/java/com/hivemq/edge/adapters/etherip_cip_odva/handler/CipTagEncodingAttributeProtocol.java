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

import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.encoder.CipTagEncoders;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import etherip.protocol.BaseEncodingAttributeProtocol;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class CipTagEncodingAttributeProtocol extends BaseEncodingAttributeProtocol<Void> {

    private final CipTagEncoders cipTagEncoders;
    private final List<CipTag> tags;
    private final ByteOrder byteOrder;
    private final CipTagValueProducer<Object> valueProducer;

    private int totalRequestSize = 0;

    public CipTagEncodingAttributeProtocol(
            final CipTagEncoders cipTagsEncoders,
            final List<CipTag> tags,
            final ByteOrder byteOrder,
            final CipTagValueProducer<Object> valueProducer) {

        super();

        this.tags = tags;
        this.byteOrder = byteOrder;
        this.valueProducer = valueProducer;
        this.cipTagEncoders = cipTagsEncoders;
    }

    @Override
    protected void writeToBuffer(final ByteBuffer buf, final StringBuilder log) throws OdvaException {

        int starPosition = buf.position();

        // EIP reuses buffer, we should clear it as we might be making partial writes
        // totalRequestSize is set by previous call to internalGetRequestSize
        clearBuffer(buf, totalRequestSize);

        buf.position(starPosition);
        cipTagEncoders.encode(tags, buf, byteOrder, valueProducer);
        buf.position(starPosition + totalRequestSize);
    }

    private static void clearBuffer(@NotNull ByteBuffer buf, int requestSize) {
        for (int i = 0; i < requestSize; i++) {
            buf.put((byte) 0);
        }
    }

    @Override
    protected int internalGetRequestSize() {
        this.totalRequestSize = cipTagEncoders.getRequestSize(tags, valueProducer);
        return totalRequestSize;
    }
}
