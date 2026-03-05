/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.edge.modules.adapters.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.protocols.northbound.SingleTagConsumer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class TagManagerTest {

    private static final @NotNull String ADAPTER_1 = "adapter-1";

    @Test
    public void test_allSucceeds() throws Exception {
        final var tagManager = new TagManager();
        final var countDownLatch = new CountDownLatch(3);
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", countDownLatch));
        tagManager.feed(List.of(new DataPointImpl("tag1", 1, ADAPTER_1), new DataPointImpl("tag1", 2, ADAPTER_1)));
        tagManager.feed(List.of(new DataPointImpl("tag1", 1, ADAPTER_1), new DataPointImpl("tag1", 2, ADAPTER_1)));
        tagManager.feed(List.of(new DataPointImpl("tag1", 1, ADAPTER_1), new DataPointImpl("tag1", 2, ADAPTER_1)));

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void test_succeedForMultipleConsumers() throws Exception {
        final var tagManager = new TagManager();
        final var countDownLatch = new CountDownLatch(3);
        final var countDownLatch1 = new CountDownLatch(3);
        final var countDownLatch2 = new CountDownLatch(3);
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", countDownLatch));
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", countDownLatch1));
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", countDownLatch2));
        tagManager.feed(List.of(new DataPointImpl("tag1", 1, ADAPTER_1), new DataPointImpl("tag1", 2, ADAPTER_1)));
        tagManager.feed(List.of(new DataPointImpl("tag1", 1, ADAPTER_1), new DataPointImpl("tag1", 2, ADAPTER_1)));
        tagManager.feed(List.of(new DataPointImpl("tag1", 1, ADAPTER_1), new DataPointImpl("tag1", 2, ADAPTER_1)));

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(countDownLatch1.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(countDownLatch2.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void test_succeedForMultipleConsumers_withOneBroken() throws Exception {
        final var tagManager = new TagManager();
        final var countDownLatch = new CountDownLatch(3);
        final var countDownLatch1 = new CountDownLatch(3);
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", countDownLatch));
        tagManager.addConsumer(new FailingTagConsumer(ADAPTER_1, "tag1"));
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", countDownLatch1));
        tagManager.feed(List.of(new DataPointImpl("tag1", 1, ADAPTER_1), new DataPointImpl("tag1", 2, ADAPTER_1)));
        tagManager.feed(List.of(new DataPointImpl("tag1", 1, ADAPTER_1), new DataPointImpl("tag1", 2, ADAPTER_1)));
        tagManager.feed(List.of(new DataPointImpl("tag1", 1, ADAPTER_1), new DataPointImpl("tag1", 2, ADAPTER_1)));

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(countDownLatch1.await(5, TimeUnit.SECONDS)).isTrue();
    }

    public static class FailingTagConsumer implements SingleTagConsumer {
        private final @NotNull String scope;
        private final @NotNull String tagName;

        public FailingTagConsumer(@NotNull final String scope, @NotNull final String tagName) {
            this.scope = scope;
            this.tagName = tagName;
        }

        @Override
        public @NotNull String getTagName() {
            return tagName;
        }

        @Override
        public @Nullable String getScope() {
            return scope;
        }

        @Override
        public void accept(final DataPoint dataPoint) {
            throw new RuntimeException();
        }
    }

    public static class SucceedingConsumer implements SingleTagConsumer {
        private final @NotNull String scope;
        private final @NotNull String tagName;
        private final @NotNull CountDownLatch countDownLatch;

        public SucceedingConsumer(
                @NotNull final String scope,
                @NotNull final String tagName,
                @NotNull final CountDownLatch countDownLatch) {
            this.scope = scope;
            this.tagName = tagName;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public @NotNull String getTagName() {
            return tagName;
        }

        @Override
        public @Nullable String getScope() {
            return scope;
        }

        @Override
        public void accept(final DataPoint dataPoint) {
            countDownLatch.countDown();
        }
    }
}
