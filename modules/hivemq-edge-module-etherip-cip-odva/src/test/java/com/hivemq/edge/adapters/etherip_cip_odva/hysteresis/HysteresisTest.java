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
package com.hivemq.edge.adapters.etherip_cip_odva.hysteresis;

import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HysteresisTest {

    public static Stream<Arguments> isModified() {
        return Stream.of(
                Arguments.of(null, null, 0d, false),
                Arguments.of("abc", "abc", 1d, false),
                Arguments.of("abc", "abc", 0d, true),
                Arguments.of("abc", "def", 1d, true),
                Arguments.of(true, true, 1d, false),
                Arguments.of(true, true, 0d, true),
                Arguments.of(false, false, 1d, false),
                Arguments.of(false, false, 0d, true),
                Arguments.of(true, false, 1d, true),
                Arguments.of(true, null, 1d, true),
                Arguments.of(null, true, 1d, true),
                Arguments.of(List.of("abc"), List.of("abc"), 1d, false),
                Arguments.of(List.of("abc"), List.of("abc"), 0d, true),
                Arguments.of(List.of("abc"), List.of("def"), 1d, true),
                Arguments.of(List.of("abc", "abc"), List.of("abc"), 1d, true),
                Arguments.of(List.of(), List.of("def"), 1d, true),
                Arguments.of(List.of("def"), List.of(), 1d, true),
                Arguments.of(null, List.of("def"), 1d, true),
                Arguments.of(List.of("abc"), null, 1d, true),
                Arguments.of((byte) 1, (byte) 2, 2d, false),
                Arguments.of((byte) 1, (byte) 3, 2d, true),
                Arguments.of((byte) 1, (byte) 4, 2d, true),
                Arguments.of((byte) 1, (byte) -1, 2d, true),
                Arguments.of((byte) -128, (byte) 127, 2d, true),
                Arguments.of((short) 1, (short) 1, 2d, false),
                Arguments.of((short) 1, (short) 2, 2d, false),
                Arguments.of((short) 1, (short) 3, 2d, true),
                Arguments.of((short) 1, (short) 3, 2d, true),
                Arguments.of((short) 1, (short) -1, 2d, true),
                Arguments.of(1, 1, 2d, false),
                Arguments.of(1, 2, 2d, false),
                Arguments.of(1, 3, 2d, true),
                Arguments.of(1, 4, 2d, true),
                Arguments.of(1, -1, 2d, true),
                Arguments.of(1L, 1L, 1d, false),
                Arguments.of(1L, 2L, 2d, false),
                Arguments.of(1L, 3L, 2d, true),
                Arguments.of(1L, 4L, 2d, true),
                Arguments.of(1L, -1L, 2d, true),
                Arguments.of(1.0, 2.999, 2d, false),
                Arguments.of(1.0, 3.000, 2d, true),
                Arguments.of(1.0, 3.001, 2d, true),
                Arguments.of(1.0, -1.000, 2d, true),
                Arguments.of(1.0d, 2.999, 2d, false),
                Arguments.of(1.0d, 3.000d, 2d, true),
                Arguments.of(1.0d, 3.001d, 2d, true),
                Arguments.of(1.0d, -1.000d, 2d, true));
    }

    @ParameterizedTest
    @MethodSource
    void isModified(Object newValue, Object currentValue, Double hysteresisValue, boolean expected) {
        Hysteresis hysteresis = new Hysteresis();

        Assertions.assertThat(hysteresis.isModified(newValue, currentValue, hysteresisValue))
                .isEqualTo(expected);
    }
}
