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

import com.google.common.base.Strings;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CipTagDecodersTest extends BaseCipTagDecoderTest {
    private final CipTagDecoders cipTagDecoders = new CipTagDecoders();

    static Stream<Arguments> shouldDecodeSingleTag() {
        return Stream.of(
                // List cases
                testCaseSingleTag(4, BOOL, 0, null, new byte[] {(byte) 0b10101011}, List.of(true, true, false, true)),
                testCaseSingleTag(4, SINT, new byte[] {1, 2, 3, 4}, List.of((byte) 1, (byte) 2, (byte) 3, (byte) 4)),

                // Single value cases
                testCaseSingleTag(1, BOOL, 0, null, new byte[] {1}, true),
                testCaseSingleTag(1, BOOL, 0, 0, new byte[] {2}, false),
                testCaseSingleTag(1, BOOL, 0, 1, new byte[] {2}, true),
                testCaseSingleTag(1, BOOL, 0, 2, new byte[] {2}, false),
                testCaseSingleTag(1, SINT, new byte[] {127}, (byte) 127),
                testCaseSingleTag(1, SINT, new byte[] {-128}, (byte) -128),
                testCaseSingleTag(1, USINT, new byte[] {127}, (short) Byte.MAX_VALUE),
                testCaseSingleTag(1, USINT, new byte[] {-1}, (short) (Byte.MAX_VALUE * 2 + 1)),
                testCaseSingleTag(1, INT, new byte[] {-1, -1}, (short) -1),
                testCaseSingleTag(1, INT, new byte[] {127, -1}, Short.MAX_VALUE),
                testCaseSingleTag(1, INT, new byte[] {-128, 0}, Short.MIN_VALUE),
                testCaseSingleTag(1, UINT, new byte[] {127, -1}, (int) Short.MAX_VALUE),
                testCaseSingleTag(1, UINT, new byte[] {-128, 0}, (int) (Short.MAX_VALUE) + 1),
                testCaseSingleTag(1, UINT, new byte[] {-1, -1}, (int) Short.MAX_VALUE * 2 + 1),
                testCaseSingleTag(1, DINT, new byte[] {127, -1, -1, -1}, Integer.MAX_VALUE),
                testCaseSingleTag(1, DINT, new byte[] {-128, 0, 0, 0}, Integer.MIN_VALUE),
                testCaseSingleTag(1, DINT, new byte[] {-1, -1, -1, -1}, -1),
                testCaseSingleTag(1, UDINT, new byte[] {127, -1, -1, -1}, (long) Integer.MAX_VALUE),
                testCaseSingleTag(1, UDINT, new byte[] {-128, 0, 0, 0}, (long) Integer.MAX_VALUE + 1),
                testCaseSingleTag(1, UDINT, new byte[] {-1, -1, -1, -1}, (long) Integer.MAX_VALUE * 2 + 1),
                testCaseSingleTag(1, LINT, new byte[] {127, -1, -1, -1, -1, -1, -1, -1}, Long.MAX_VALUE),
                testCaseSingleTag(1, LINT, new byte[] {-128, 0, 0, 0, 0, 0, 0, 0}, Long.MIN_VALUE),
                testCaseSingleTag(1, LINT, new byte[] {-1, -1, -1, -1, -1, -1, -1, -1}, -1L),
                testCaseSingleTag(1, SSTRING, new byte[] {0}, ""),
                testCaseSingleTag(1, SSTRING, new byte[] {3, 'A', 'B', 'C'}, "ABC"),
                testCaseSingleTag(1, STRING, new byte[] {0, 0}, ""),
                testCaseSingleTag(1, STRING, new byte[] {0, 4, 'A', 'B', 'C', 'D'}, "ABCD"),
                testCaseSingleTag(
                        1,
                        SSTRING,
                        toByteArray(() -> {
                            ByteBuffer buffer = ByteBuffer.allocate(255 + 1);
                            buffer.put((byte) -1); // length 256
                            buffer.put(Strings.padStart("", 255, 'A').getBytes(StandardCharsets.UTF_8));
                            return buffer.array();
                        }),
                        Strings.padStart("", 255, 'A')),
                testCaseSingleTag(
                        1,
                        STRING,
                        toByteArray(() -> {
                            int stringLength = (1 << 16) - 1;
                            ByteBuffer buffer = ByteBuffer.allocate(stringLength + 2);
                            buffer.putShort((short) -1); // max length
                            buffer.put(Strings.padStart("", stringLength, 'A').getBytes(StandardCharsets.UTF_8));
                            return buffer.array();
                        }),
                        Strings.padStart("", (1 << 16) - 1, 'A')),
                testCaseSingleTag(
                        1,
                        REAL,
                        toByteArray(() -> {
                            ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
                            buffer.putFloat(123.45f);
                            return buffer.array();
                        }),
                        123.45f),
                testCaseSingleTag(
                        1,
                        LREAL,
                        toByteArray(() -> {
                            ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
                            buffer.putDouble(1234567890.123456789d);
                            return buffer.array();
                        }),
                        1234567890.123456789d));
    }

    @ParameterizedTest
    @MethodSource
    void shouldDecodeSingleTag(CipTagDefinition definition, byte[] bytes, Object expected) throws OdvaException {

        // given
        CipTag cipTag = new CipTag("tag", "description", definition);
        CipTagDecoder<Object> decoder = cipTagDecoders.getDecoder(cipTag);
        AtomicReference<Object> valueReference = new AtomicReference<>();

        // when
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        decoder.decode(cipTag, buffer, null, (tag, value) -> valueReference.set(value));

        // then
        Assertions.assertThat(valueReference.get()).isEqualTo(expected);
    }

    @NotNull
    protected static Arguments testCaseSingleTag(
            @NotNull final Integer numberOfElements,
            @NotNull final CipDataType dataType,
            @NotNull byte[] bytes,
            Object expected) {
        return testCaseSingleTag(numberOfElements, dataType, 0, null, bytes, expected);
    }

    @NotNull
    protected static Arguments testCaseSingleTag(
            @NotNull final Integer numberOfElements,
            @NotNull final CipDataType dataType,
            @NotNull final Integer batchByteIndex,
            @Nullable final Integer batchBitIndex,
            @NotNull byte[] bytes,
            Object expected) {
        return Arguments.of(
                new CipTagDefinition("", numberOfElements, dataType, 1d, null, batchByteIndex, batchBitIndex),
                bytes,
                expected);
    }

    public static Stream<Arguments> shouldDecodeTagList() {
        return Stream.of(
                Arguments.of(List.of(testCipTag("sint", 1, SINT, 2, null)), new byte[] {0, 0, 1, 0}, List.of((byte) 1)),
                Arguments.of(
                        List.of(testCipTag("int", 1, INT, 1, null)), new byte[] {0, 1, 0, 0}, List.of((short) 256)),
                Arguments.of(
                        List.of(testCipTag("int", 2, INT, 1, null)),
                        new byte[] {0, 1, 0, 1, 0},
                        List.of(List.of((short) 256, (short) 256))),
                Arguments.of(
                        List.of(testCipTag("int", 1, INT, 1, null), testCipTag("sint", 1, SINT, 3, null)),
                        new byte[] {0, 1, 0, 1, 0},
                        List.of((short) 256, (byte) 1)));
    }

    @ParameterizedTest
    @MethodSource
    void shouldDecodeTagList(List<CipTag> tagsOfaSingleGroup, byte[] bytes, List<Object> expectedValues)
            throws OdvaException {
        // given
        final List<CipTag> tagNames = new ArrayList<>();
        final List<Object> values = new ArrayList<>();

        // when
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        cipTagDecoders.decode(tagsOfaSingleGroup, buffer, ByteOrder.BIG_ENDIAN, null, (tag, value) -> {
            tagNames.add(tag);
            values.add(value);
        });

        // then
        Assertions.assertThat(tagNames).containsExactlyElementsOf(tagsOfaSingleGroup);
        Assertions.assertThat(values).containsExactlyElementsOf(expectedValues);
    }

    @NotNull
    protected static CipTag testCipTag(
            @NotNull final String name,
            @NotNull final Integer numberOfElements,
            @NotNull final CipDataType dataType,
            @NotNull final Integer batchByteIndex,
            @Nullable final Integer batchBitIndex) {
        return new CipTag(
                name,
                name + " description",
                new CipTagDefinition(
                        name + " address", numberOfElements, dataType, 1d, null, batchByteIndex, batchBitIndex));
    }
}
