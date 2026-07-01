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
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaEncodeException;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.CipTagValueProducer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

abstract class BaseSTRINGEncoder implements CipTagEncoder<String> {

    private final int maxStringLength;
    private final int requesSizeOfStringLength;

    public BaseSTRINGEncoder(final int maxStringLength, final int requestSizeOfStringLength) {
        this.maxStringLength = maxStringLength;
        this.requesSizeOfStringLength = requestSizeOfStringLength;
    }

    @Override
    public void encodeSingle(final @NotNull CipTag cipTag, final @NotNull ByteBuffer buf, final @NotNull String value)
            throws OdvaEncodeException {

        byte[] stringAsBytes = toByteArray(value);
        int stringBytesLength = stringAsBytes.length;
        if (stringBytesLength > maxStringLength) {
            throw new OdvaEncodeException(cipTag, "String length " + stringBytesLength + " > " + maxStringLength);
        }

        encodeStringLength(buf, stringBytesLength);
        buf.put(stringAsBytes);
    }

    abstract void encodeStringLength(ByteBuffer buf, int stringBytesLength);

    @Override
    public int getRequestSizeSingle(
            final @NotNull CipTag cipTag, final @NotNull CipTagValueProducer<String> valueProducer) {

        return requesSizeOfStringLength + toByteArray(valueProducer.produce(cipTag)).length;
    }

    byte[] toByteArray(@NotNull final String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public int getRequestSizeMultiple(
            final @NotNull CipTag cipTag, final @NotNull CipTagValueProducer<List<String>> valueProducer) {

        int requestSize = 0;
        Iterator<String> iterator = valueProducer.produce(cipTag).iterator();
        while (iterator.hasNext()) {
            requestSize += getRequestSizeSingle(cipTag, tag -> iterator.next());
        }
        return requestSize;
    }
}
