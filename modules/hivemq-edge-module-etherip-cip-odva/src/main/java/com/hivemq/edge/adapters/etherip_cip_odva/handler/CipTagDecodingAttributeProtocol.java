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
import com.hivemq.edge.adapters.etherip_cip_odva.decoder.CipTagDecoders;
import etherip.protocol.BaseDecodingAttributeProtocol;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Delegates direct decoding from ByteBuffer to CipTagDecoders. Passes decoded Tags with values to the valueConsumer for further use
 */
public class CipTagDecodingAttributeProtocol extends BaseDecodingAttributeProtocol<Void> {

    private final CipTagDecoders cipTagDecoders;
    private final List<CipTag> tags;
    private final ByteOrder byteOrder;
    private final CipTagValueConsumer<Object> valueConsumer;

    public CipTagDecodingAttributeProtocol(
            final CipTagDecoders cipTagsDecoders,
            final List<CipTag> tags,
            final ByteOrder byteOrder,
            final CipTagValueConsumer<Object> valueConsumer) {

        super();

        this.tags = tags;
        this.byteOrder = byteOrder;
        this.valueConsumer = valueConsumer;
        this.cipTagDecoders = cipTagsDecoders;
    }

    @Override
    protected Void readFromBuffer(final ByteBuffer buf, final int available, final StringBuilder log) throws Exception {

        cipTagDecoders.decode(tags, buf, byteOrder, log, valueConsumer);
        return null;
    }
}
