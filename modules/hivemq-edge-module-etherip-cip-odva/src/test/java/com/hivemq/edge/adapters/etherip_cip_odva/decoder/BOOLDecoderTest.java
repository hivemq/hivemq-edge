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

import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaDecodeException;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BOOLDecoderTest {
    public static Stream<Arguments> shouldInternalDecode() {
        return Stream.of(
                Arguments.of(1, 0, createCipTagDefinition(1, null, null), true),
                Arguments.of(2, 0, createCipTagDefinition(1, null, null), true),
                Arguments.of(-128, 0, createCipTagDefinition(1, null, null), true),
                Arguments.of(0, 0, createCipTagDefinition(1, null, null), false),
                Arguments.of(2, 0, createCipTagDefinition(1, 0, 0), false),
                Arguments.of(2, 0, createCipTagDefinition(1, 0, 1), true),
                Arguments.of(4, 0, createCipTagDefinition(1, 0, 2), true),
                Arguments.of(1 << 3, 5, createCipTagDefinition(10, 0, 3), true),
                Arguments.of(1 << 5, 5, createCipTagDefinition(10, 0, null), true),
                Arguments.of(1 << 5, 5, createCipTagDefinition(10, 0, null), true));
    }

    @NotNull
    private static CipTagDefinition createCipTagDefinition(
            Integer numberOfElements, Integer batchByteIndex, Integer batchBitIndex) {
        return new CipTagDefinition(
                "address", numberOfElements, CipDataType.BOOL, 0d, null, batchByteIndex, batchBitIndex);
    }

    @ParameterizedTest
    @MethodSource
    void shouldInternalDecode(int b, int elementIndex, CipTagDefinition cipTagDefinition, boolean expectedValue)
            throws OdvaDecodeException {

        // given
        CipTag cipTag = new CipTag("name", "description", cipTagDefinition);
        BOOLDecoder boolDecoder = new BOOLDecoder();
        ByteBuffer buf = ByteBuffer.wrap(new byte[] {(byte) b});

        // when
        Boolean result = boolDecoder.internalDecode(cipTag, elementIndex, buf, null);

        // then
        Assertions.assertThat(result).isEqualTo(expectedValue);
    }
}
