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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.protocols.northbound.TagConsumer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.hivemq.protocols.northbound.NorthboundTagConsumer.CONSUMER_NAME_PREFIX_NORTHBOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TagManagerTest {
    public final static String TAG_NAME = "taggy";

    @Test
    public void test_feedingTags_works() {
        final var tagManager = new TagManager(mock(MetricsHolder.class));

        final var consumed = new AtomicReference<List<DataPoint>>();

        tagManager.addConsumer(new TagConsumer() {
            @Override
            public @NotNull String getTagName() {
                return TAG_NAME;
            }

            @Override
            public @NotNull String consumerName() {
                return CONSUMER_NAME_PREFIX_NORTHBOUND + ":adapter1";
            }

            @Override
            public void accept(final List<DataPoint> dataPoints) {
                consumed.set(dataPoints);
            }
        });

        tagManager.feed(TAG_NAME, List.of(
                new DataPointImpl(TAG_NAME, "123", false),
                new DataPointImpl(TAG_NAME, "1234", false)));

        await().until(() -> consumed.get() != null);

        assertThat(consumed.get())
                .containsExactly(
                        new DataPointImpl(TAG_NAME, "123", false),
                        new DataPointImpl(TAG_NAME, "1234", false));
    }

    @Test
    public void test_getConsumers() {
        final var tagManager = new TagManager(mock(MetricsHolder.class));

        final var consumer = mock(TagConsumer.class);
        when(consumer.getTagName()).thenReturn(TAG_NAME);
        when(consumer.consumerName()).thenReturn(CONSUMER_NAME_PREFIX_NORTHBOUND + ":adapter1");

        tagManager.addConsumer(consumer);

        assertThat(tagManager.consumersForTag("not_exisiting")).isEmpty();
        assertThat(tagManager.consumersForTag(TAG_NAME))
                .hasSize(1)
                .containsExactly(CONSUMER_NAME_PREFIX_NORTHBOUND + ":adapter1");
    }

    @Test
    public void test_retainsLastTagValue() {
        final var tagManager = new TagManager(mock(MetricsHolder.class));

        final var consumed = new AtomicReference<List<DataPoint>>();

        //feed twice with different values to check that only the last value is retained
        tagManager.feed(TAG_NAME, List.of(
                new DataPointImpl(TAG_NAME, "123", false),
                new DataPointImpl(TAG_NAME, "1234", false)));

        tagManager.feed(TAG_NAME, List.of(
                new DataPointImpl(TAG_NAME, "223", false),
                new DataPointImpl(TAG_NAME, "2234", false)));

        tagManager.addConsumer(new TagConsumer() {
            @Override
            public @NotNull String getTagName() {
                return TAG_NAME;
            }

            @Override
            public @NotNull String consumerName() {
                return CONSUMER_NAME_PREFIX_NORTHBOUND + ":adapter1";
            }

            @Override
            public void accept(final List<DataPoint> dataPoints) {
                consumed.set(dataPoints);
            }
        });

        await().until(() -> consumed.get() != null);

        assertThat(consumed.get())
                .containsExactly(
                        new DataPointImpl(TAG_NAME, "223", false),
                        new DataPointImpl(TAG_NAME, "2234", false));
    }

}
