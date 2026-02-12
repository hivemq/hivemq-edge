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


import com.codahale.metrics.NoopMetricRegistry;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.protocols.northbound.TagConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class TagManagerTest {

    private static final @NotNull String ADAPTER_1 = "adapter-1";

    @Test
    public void test_allSucceeds() throws Exception {
        var tagManager = new TagManager(new MetricsHolder(new NoopMetricRegistry()));
        var countDownLatch = new CountDownLatch(3);
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", countDownLatch));
        tagManager.feed(ADAPTER_1, "tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));
        tagManager.feed(ADAPTER_1, "tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));
        tagManager.feed(ADAPTER_1, "tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void test_succeedForMultipleConsumers() throws Exception {
        var tagManager = new TagManager(new MetricsHolder(new NoopMetricRegistry()));
        var countDownLatch = new CountDownLatch(3);
        var countDownLatch1 = new CountDownLatch(3);
        var countDownLatch2 = new CountDownLatch(3);
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", countDownLatch));
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", countDownLatch1));
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", countDownLatch2));
        tagManager.feed(ADAPTER_1, "tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));
        tagManager.feed(ADAPTER_1, "tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));
        tagManager.feed(ADAPTER_1, "tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(countDownLatch1.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(countDownLatch2.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void test_succeedForMultipleConsumers_withOneBroken() throws Exception {
        var tagManager = new TagManager(new MetricsHolder(new NoopMetricRegistry()));
        var countDownLatch = new CountDownLatch(3);
        var countDownLatch1 = new CountDownLatch(3);
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", countDownLatch));
        tagManager.addConsumer(new FailingTagConsumer(ADAPTER_1, "tag1"));
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", countDownLatch1));
        tagManager.feed(ADAPTER_1, "tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));
        tagManager.feed(ADAPTER_1, "tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));
        tagManager.feed(ADAPTER_1, "tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(countDownLatch1.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void test_differentScopes_isolateConsumers() throws Exception {
        var tagManager = new TagManager(new MetricsHolder(new NoopMetricRegistry()));
        var adapter1Latch = new CountDownLatch(1);
        var adapter2Latch = new CountDownLatch(1);
        tagManager.addConsumer(new SucceedingConsumer("adapter-1", "temp", adapter1Latch));
        tagManager.addConsumer(new SucceedingConsumer("adapter-2", "temp", adapter2Latch));

        tagManager.feed("adapter-1", "temp", List.of(new DataPointImpl("temp", 25)));

        assertThat(adapter1Latch.await(5, TimeUnit.SECONDS)).isTrue();
        // adapter-2 consumer should NOT have been triggered
        assertThat(adapter2Latch.await(200, TimeUnit.MILLISECONDS)).isFalse();
    }

    @Test
    public void test_feedDoesNotTriggerConsumerForDifferentScope() throws Exception {
        var tagManager = new TagManager(new MetricsHolder(new NoopMetricRegistry()));
        var latch = new CountDownLatch(1);
        tagManager.addConsumer(new SucceedingConsumer("adapter-2", "temp", latch));

        tagManager.feed("adapter-1", "temp", List.of(new DataPointImpl("temp", 25)));

        // consumer is for adapter-2, feed is for adapter-1 — should not trigger
        assertThat(latch.await(200, TimeUnit.MILLISECONDS)).isFalse();
    }

    @Test
    public void test_cachedValueReplayedOnAddConsumer() throws Exception {
        var tagManager = new TagManager(new MetricsHolder(new NoopMetricRegistry()));
        // Feed first, then add consumer — cached value should be replayed
        tagManager.feed(ADAPTER_1, "tag1", List.of(new DataPointImpl("tag1", 42)));

        var latch = new CountDownLatch(1);
        tagManager.addConsumer(new SucceedingConsumer(ADAPTER_1, "tag1", latch));

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void test_cachedValueNotReplayedForDifferentScope() throws Exception {
        var tagManager = new TagManager(new MetricsHolder(new NoopMetricRegistry()));
        tagManager.feed("adapter-1", "tag1", List.of(new DataPointImpl("tag1", 42)));

        var latch = new CountDownLatch(1);
        tagManager.addConsumer(new SucceedingConsumer("adapter-2", "tag1", latch));

        // cached value is for adapter-1, consumer is for adapter-2
        assertThat(latch.await(200, TimeUnit.MILLISECONDS)).isFalse();
    }

    @Test
    public void test_removeConsumer_stopsDelivery() throws Exception {
        var tagManager = new TagManager(new MetricsHolder(new NoopMetricRegistry()));
        var latch = new CountDownLatch(2);
        var consumer = new SucceedingConsumer(ADAPTER_1, "tag1", latch);
        tagManager.addConsumer(consumer);

        tagManager.feed(ADAPTER_1, "tag1", List.of(new DataPointImpl("tag1", 1)));
        tagManager.removeConsumer(consumer);
        tagManager.feed(ADAPTER_1, "tag1", List.of(new DataPointImpl("tag1", 2)));

        // Only the first feed should have triggered the consumer
        assertThat(latch.await(200, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(latch.getCount()).isEqualTo(1);
    }

    public static class FailingTagConsumer implements TagConsumer {
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
        public void accept(final List<DataPoint> dataPoints) {
            throw new RuntimeException();
        }
    }

    public static class SucceedingConsumer implements TagConsumer {
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
        public void accept(final List<DataPoint> dataPoints) {
            countDownLatch.countDown();
        }
    }
}
