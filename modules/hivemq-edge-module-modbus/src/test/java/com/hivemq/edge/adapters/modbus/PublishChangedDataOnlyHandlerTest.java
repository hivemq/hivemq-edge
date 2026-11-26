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
package com.hivemq.edge.adapters.modbus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/// Guards the decision whether a polled value is published when `publishChangedDataOnly` is on (EDG-1209).
///
/// The cases cover the values the Modbus adapter actually produces. `Boolean` (coils, discrete inputs) and integers
/// in -128..127 come from the JVM's box caches, so an old and a new sample can be the same object; larger values and
/// floating-point values are distinct objects even when equal. A handler that compares objects instead of values gets
/// one of the two groups wrong.
class PublishChangedDataOnlyHandlerTest {

    private static Stream<Arguments> changes() {
        return Stream.of(
                Arguments.of(Boolean.TRUE, Boolean.FALSE),
                Arguments.of((short) 5, (short) 6),
                Arguments.of((short) 1_000, (short) 1_001),
                Arguments.of(5, 6),
                Arguments.of(-128, 127),
                Arguments.of(100_000, 100_001),
                Arguments.of(5L, 6L),
                Arguments.of(1.5f, 2.5f),
                Arguments.of(1.5, 2.5),
                Arguments.of("on", "off"));
    }

    /// `isNew` answers as `replaceIfValueIsNew` would and records nothing, so the adapter can choose what to publish
    /// before it knows the publish reached the sampler, and the value stays new until it is recorded.
    @ParameterizedTest
    @MethodSource("changes")
    void isNew_answersWithoutRecording(final Object first, final Object second) {
        final var handler = new PublishChangedDataOnlyHandler();
        handler.replaceIfValueIsNew("tag", first);

        assertThat(handler.isNew("tag", copyOf(first))).isFalse();
        assertThat(handler.isNew("tag", second)).isTrue();
        assertThat(handler.isNew("tag", second)).isTrue();
        assertThat(handler.replaceIfValueIsNew("tag", second)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("changes")
    void replaceIfValueIsNew_whenTheFirstSampleArrives_thenItIsPublished(final Object first, final Object ignored) {
        final var handler = new PublishChangedDataOnlyHandler();

        assertThat(handler.replaceIfValueIsNew("tag", first)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("changes")
    void replaceIfValueIsNew_whenTheValueChanges_thenItIsPublished(final Object first, final Object second) {
        final var handler = new PublishChangedDataOnlyHandler();
        handler.replaceIfValueIsNew("tag", first);

        assertThat(handler.replaceIfValueIsNew("tag", second)).isTrue();
        assertThat(handler.replaceIfValueIsNew("tag", first)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("changes")
    void replaceIfValueIsNew_whenTheValueStaysTheSame_thenItIsNotPublished(final Object first, final Object ignored) {
        final var handler = new PublishChangedDataOnlyHandler();
        handler.replaceIfValueIsNew("tag", first);

        assertThat(handler.replaceIfValueIsNew("tag", copyOf(first))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("changes")
    void replaceIfValueIsNew_whenAnotherTagChanges_thenThisTagIsNotAffected(final Object first, final Object second) {
        final var handler = new PublishChangedDataOnlyHandler();
        handler.replaceIfValueIsNew("tag", first);
        handler.replaceIfValueIsNew("other", first);
        handler.replaceIfValueIsNew("other", second);

        assertThat(handler.replaceIfValueIsNew("tag", copyOf(first))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("changes")
    void clear_whenCalled_thenTheNextSampleIsPublishedAgain(final Object first, final Object ignored) {
        final var handler = new PublishChangedDataOnlyHandler();
        handler.replaceIfValueIsNew("tag", first);

        handler.clear();

        assertThat(handler.replaceIfValueIsNew("tag", copyOf(first))).isTrue();
    }

    /// Boxes the value again, as the next poll does: cached values come back as the same object, others as a new one.
    private static Object copyOf(final Object value) {
        return switch (value) {
            case final Short s -> Short.valueOf(s.shortValue());
            case final Integer i -> Integer.valueOf(i.intValue());
            case final Long l -> Long.valueOf(l.longValue());
            case final Float f -> Float.valueOf(f.floatValue());
            case final Double d -> Double.valueOf(d.doubleValue());
            case final String s -> new String(s);
            default -> value;
        };
    }
}
