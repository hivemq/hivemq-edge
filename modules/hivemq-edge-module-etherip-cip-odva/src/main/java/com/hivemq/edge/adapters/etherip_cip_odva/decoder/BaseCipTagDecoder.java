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

import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaDecodeException;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.CipTagValueConsumer;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseCipTagDecoder<T> implements CipTagDecoder<T> {

    @Override
    public void decode(
            final @NotNull CipTag cipTag,
            final @NotNull ByteBuffer buf,
            final @Nullable StringBuilder log,
            final @NotNull CipTagValueConsumer<T> valueConsumer)
            throws OdvaDecodeException {
        decode(cipTag, 0, buf, log, valueConsumer);
    }

    @Override
    public void decode(
            final @NotNull CipTag cipTag,
            final int elementIndex,
            final @NotNull ByteBuffer buf,
            final @Nullable StringBuilder log,
            final @NotNull CipTagValueConsumer<T> valueConsumer)
            throws OdvaDecodeException {

        try {
            T value = internalDecode(cipTag, elementIndex, buf, log);

            if (log != null) {
                log.append("Read ")
                        .append(cipTag.toConciseString())
                        .append("=")
                        .append(value)
                        .append(" at position ")
                        .append(buf.position())
                        .append("\n");
            }

            valueConsumer.consume(cipTag, value);
        } catch (OdvaDecodeException e) {
            throw e;
        } catch (Exception e) {
            throw new OdvaDecodeException(cipTag, e);
        }
    }

    protected abstract T internalDecode(
            final @NotNull CipTag cipTag,
            final int elementIndex,
            final @NotNull ByteBuffer buf,
            final @Nullable StringBuilder log)
            throws OdvaDecodeException;

    protected void assertAvailableBuffer(
            @NotNull CipTag cipTag, int elementIndex, @NotNull ByteBuffer buf, int expectedRemaining)
            throws OdvaDecodeException {
        if (buf.remaining() < expectedRemaining) {
            throw new OdvaDecodeException(
                    cipTag,
                    "Not enough data in buffer. expected=" + expectedRemaining + " and remaining=" + buf.remaining()
                            + (cipTag.getDefinition().getNumberOfElements() > 0
                                    ? " reading element at index=" + elementIndex
                                    : ""));
        }
    }
}
