package com.hivemq.edge.modules.adapters.data;


import com.codahale.metrics.NoopMetricRegistry;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.protocols.northbound.TagConsumer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class TagManagerTest {

    @Test
    public void test_allSucceeds() throws Exception{
        var tagManager = new TagManager(new MetricsHolder(new NoopMetricRegistry()));
        var countDownLatch = new CountDownLatch(3);
        tagManager.addConsumer(new SucceedingConsumer("tag1", countDownLatch));
        tagManager.feed("tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));
        tagManager.feed("tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));
        tagManager.feed("tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void test_succeedForMultipleConsumers() throws Exception{
        var tagManager = new TagManager(new MetricsHolder(new NoopMetricRegistry()));
        var countDownLatch = new CountDownLatch(3);
        var countDownLatch1 = new CountDownLatch(3);
        var countDownLatch2 = new CountDownLatch(3);
        tagManager.addConsumer(new SucceedingConsumer("tag1", countDownLatch));
        tagManager.addConsumer(new SucceedingConsumer("tag1", countDownLatch1));
        tagManager.addConsumer(new SucceedingConsumer("tag1", countDownLatch2));
        tagManager.feed("tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));
        tagManager.feed("tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));
        tagManager.feed("tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(countDownLatch1.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(countDownLatch2.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void test_succeedForMultipleConsumers_withOneBroken() throws Exception{
        var tagManager = new TagManager(new MetricsHolder(new NoopMetricRegistry()));
        var countDownLatch = new CountDownLatch(3);
        var countDownLatch1 = new CountDownLatch(3);
        tagManager.addConsumer(new SucceedingConsumer("tag1", countDownLatch));
        tagManager.addConsumer(new FailingTagConsumer("tag1"));
        tagManager.addConsumer(new SucceedingConsumer("tag1", countDownLatch1));
        tagManager.feed("tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));
        tagManager.feed("tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));
        tagManager.feed("tag1", List.of(new DataPointImpl("tag1", 1), new DataPointImpl("tag1", 2)));

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(countDownLatch1.await(5, TimeUnit.SECONDS)).isTrue();
    }

    public static class FailingTagConsumer implements TagConsumer {
        private final @NotNull String tagName;

        public FailingTagConsumer(@NotNull final String tagName) {
            this.tagName = tagName;
        }

        @Override
        public @NotNull String getTagName() {
            return tagName;
        }

        @Override
        public void accept(final List<DataPoint> dataPoints) {
            throw new RuntimeException();
        }
    }

    public static class SucceedingConsumer implements TagConsumer {
        private final @NotNull String tagName;
        private final @NotNull CountDownLatch countDownLatch;

        public SucceedingConsumer(@NotNull final String tagName, @NotNull final CountDownLatch countDownLatch) {
            this.tagName = tagName;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public @NotNull String getTagName() {
            return tagName;
        }

        @Override
        public void accept(final List<DataPoint> dataPoints) {
            countDownLatch.countDown();
        }
    }
}
