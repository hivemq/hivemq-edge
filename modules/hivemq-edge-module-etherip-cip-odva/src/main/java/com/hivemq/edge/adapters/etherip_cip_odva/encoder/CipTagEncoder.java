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
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface CipTagEncoder<T> {
    default void encode(
            final @NotNull CipTag cipTag,
            final @NotNull ByteBuffer buf,
            final @NotNull CipTagValueProducer<?> valueProducer)
            throws OdvaEncodeException {

        if (cipTag.getDefinition().getNumberOfElements() > 1) {
            List<T> value = ((CipTagValueProducer<List<T>>) valueProducer).produce(cipTag);
            assertValueNotNull(cipTag, value);
            encodeMultiple(cipTag, buf, value);

        } else {
            T value = ((CipTagValueProducer<T>) valueProducer).produce(cipTag);
            assertValueNotNull(cipTag, value);
            encodeSingle(cipTag, buf, value);
        }
    }

    private void assertValueNotNull(CipTag cipTag, Object value) throws OdvaEncodeException {
        if (value == null) {
            throw new OdvaEncodeException(cipTag, "Expected not null value");
        }
    }

    void encodeSingle(final @NotNull CipTag cipTag, final @NotNull ByteBuffer buf, final @NotNull T value)
            throws OdvaEncodeException;

    default void encodeMultiple(
            final @NotNull CipTag cipTag, final @NotNull ByteBuffer buf, final @NotNull List<T> values)
            throws OdvaEncodeException {

        assertNumberOfElementsEqualValues(cipTag, values);
        for (T value : values) {
            encodeSingle(cipTag, buf, value);
        }
    }

    private void assertNumberOfElementsEqualValues(@NotNull CipTag cipTag, @NotNull List<T> values)
            throws OdvaEncodeException {
        Integer numberOfElements = cipTag.getDefinition().getNumberOfElements();
        int valuesSize = values.size();

        if (valuesSize != numberOfElements) {
            throw new OdvaEncodeException(
                    cipTag, "Error encoding. Expected " + numberOfElements + " but received " + valuesSize);
        }
    }

    default int getRequestSize(final @NotNull CipTag cipTag, final @NotNull CipTagValueProducer<?> valueProducer)
            throws OdvaEncodeException {
        try {
            return cipTag.getDefinition().getBatchByteIndex()
                    + (cipTag.getDefinition().getNumberOfElements() > 1
                            ? getRequestSizeMultiple(cipTag, (CipTagValueProducer<List<T>>) valueProducer)
                            : getRequestSizeSingle(cipTag, (CipTagValueProducer<T>) valueProducer));
        } catch (Exception e) {
            throw new OdvaEncodeException(cipTag, e);
        }
    }

    int getRequestSizeSingle(final @NotNull CipTag cipTag, final @NotNull CipTagValueProducer<T> valueProducer);

    int getRequestSizeMultiple(final @NotNull CipTag cipTag, final @NotNull CipTagValueProducer<List<T>> valueProducer);
}
