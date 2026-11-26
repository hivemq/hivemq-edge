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
package com.hivemq.edge.adapters.plc4x;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/// Guards the decision whether a polled value is published when `publishChangedDataOnly` is on.
public class PublishChangedDataOnlyHandlerTest {

    /// A value is new the first time and not when it repeats.
    @Test
    public void test() {
        final var toTest = new PublishChangedDataOnlyHandler();
        final var initial = toTest.replaceIfValueIsNew("tag1", "value1");
        final var secondTry = toTest.replaceIfValueIsNew("tag1", "value1");

        assertThat(initial).isTrue();
        assertThat(secondTry).isFalse();
    }

    /// `isNew` answers as `replaceIfValueIsNew` would and records nothing, so the adapter can choose what to publish
    /// before it knows the publish reached the sampler, and the value stays new until it is recorded.
    @Test
    void isNew_answersWithoutRecording() {
        final var handler = new PublishChangedDataOnlyHandler();

        assertThat(handler.isNew("tag", 1_000)).isTrue();
        assertThat(handler.isNew("tag", 1_000)).isTrue();
        handler.replaceIfValueIsNew("tag", 1_000);

        assertThat(handler.isNew("tag", Integer.valueOf(1_000))).isFalse();
        assertThat(handler.isNew("tag", 1_001)).isTrue();
        assertThat(handler.isNew("tag", 1_001)).isTrue();
        assertThat(handler.replaceIfValueIsNew("tag", 1_001)).isTrue();
    }

    /// A value whose publish may not have reached the sampler is forgotten, so the next poll publishes it again; a
    /// sample equal to the recorded one is kept, since that one was published.
    @Test
    void forgetIfChanged_dropsOnlyADifferentSample() {
        final var handler = new PublishChangedDataOnlyHandler();
        handler.replaceIfValueIsNew("tag", "on");
        handler.replaceIfValueIsNew("other", "on");

        handler.forgetIfChanged("tag", "off");
        handler.forgetIfChanged("other", "on");
        handler.forgetIfChanged("absent", "on");

        assertThat(handler.isNew("tag", "on")).isTrue();
        assertThat(handler.isNew("tag", "off")).isTrue();
        assertThat(handler.isNew("other", "on")).isFalse();
        assertThat(handler.isNew("absent", "on")).isTrue();
    }

    /// A raw byte array tag converts to a new array on every read, so samples compare by content: an unchanged array is
    /// not new, is not recorded again, and is kept when an uncertain publish is forgotten.
    @Test
    void byteArrays_compareByContent() {
        final var handler = new PublishChangedDataOnlyHandler();
        assertThat(handler.replaceIfValueIsNew("raw", new byte[] {1, 2, 3})).isTrue();

        assertThat(handler.isNew("raw", new byte[] {1, 2, 3})).isFalse();
        assertThat(handler.replaceIfValueIsNew("raw", new byte[] {1, 2, 3})).isFalse();
        handler.forgetIfChanged("raw", new byte[] {1, 2, 3});
        assertThat(handler.isNew("raw", new byte[] {1, 2, 3})).isFalse();

        assertThat(handler.isNew("raw", new byte[] {1, 2, 4})).isTrue();
        handler.forgetIfChanged("raw", new byte[] {1, 2, 4});
        assertThat(handler.isNew("raw", new byte[] {1, 2, 3})).isTrue();
    }
}
