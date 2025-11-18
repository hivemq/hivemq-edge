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
package com.hivemq.extensions.services.subscription;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.exception.InvalidTopicException;
import com.hivemq.extension.sdk.api.services.exception.NoSuchClientIdException;
import com.hivemq.extension.sdk.api.services.exception.RateLimitExceededException;
import com.hivemq.extension.sdk.api.services.subscription.SubscriptionStore;
import com.hivemq.extension.sdk.api.services.subscription.SubscriptionsForClientResult;
import com.hivemq.extension.sdk.api.services.subscription.TopicSubscription;
import com.hivemq.extensions.iteration.AllItemsItemCallback;
import com.hivemq.extensions.iteration.AsyncIterator;
import com.hivemq.extensions.iteration.AsyncIteratorFactory;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.extensions.iteration.ChunkResult;
import com.hivemq.extensions.iteration.FetchCallback;
import com.hivemq.extensions.iteration.MultipleChunkResult;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.extensions.services.executor.GlobalManagedExtensionExecutorService;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.callback.SubscriptionResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class SubscriptionStoreImplTest {

    private SubscriptionStore subscriptionStore;

    @Mock
    private ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;

    @Mock
    private PluginServiceRateLimitService rateLimitService;

    @Mock
    private LocalTopicTree topicTree;

    @Mock
    private AsyncIteratorFactory asyncIteratorFactory;
    @BeforeEach
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        subscriptionStore = new SubscriptionStoreImpl(clientSessionSubscriptionPersistence, rateLimitService, topicTree,
                getManagedExtensionExecutorService(), asyncIteratorFactory);
        when(rateLimitService.rateLimitExceeded()).thenReturn(false);
    }

    @Test
    @Timeout(10)
    public void test_get_rate_limit_exceeded() {

        when(rateLimitService.rateLimitExceeded()).thenReturn(true);

        subscriptionStore.getSubscriptions("client");

        verify(clientSessionSubscriptionPersistence, never()).getSubscriptions("client");

    }

    @Test
    @Timeout(10)
    public void test_add_rate_limit_exceeded() {

        when(rateLimitService.rateLimitExceeded()).thenReturn(true);

        subscriptionStore.addSubscription(
                "client", new TopicSubscriptionImpl("topic", Qos.AT_MOST_ONCE, false, false, 0));

        verify(clientSessionSubscriptionPersistence, never()).addSubscription(eq("client"), any(Topic.class));

    }

    @Test
    @Timeout(10)
    public void test_add_multi_rate_limit_exceeded() {

        when(rateLimitService.rateLimitExceeded()).thenReturn(true);

        subscriptionStore.addSubscriptions(
                "client", ImmutableSet.of(new TopicSubscriptionImpl("topic", Qos.AT_MOST_ONCE, false, false, 0)));

        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(eq("client"), any(ImmutableSet.class));

    }

    @Test
    @Timeout(10)
    public void test_remove_rate_limit_exceeded() {

        when(rateLimitService.rateLimitExceeded()).thenReturn(true);

        subscriptionStore.removeSubscription("client", "topic");

        verify(clientSessionSubscriptionPersistence, never()).remove("client", "topic");

    }

    @Test
    @Timeout(10)
    public void test_remove_multi_rate_limit_exceeded() {

        when(rateLimitService.rateLimitExceeded()).thenReturn(true);

        subscriptionStore.removeSubscriptions("client", Sets.newHashSet("topic"));

        verify(clientSessionSubscriptionPersistence, never()).removeSubscriptions(anyString(), any(ImmutableSet.class));

    }

    @Test
    @Timeout(10)
    public void test_get_null() {
        assertThatThrownBy(() -> subscriptionStore.getSubscriptions(null).get())
                .hasCauseInstanceOf(NullPointerException.class);

        verify(clientSessionSubscriptionPersistence, never()).getSubscriptions("client");

    }

    @Test
    @Timeout(10)
    public void test_add_null_client_id() {

        assertThatThrownBy(() -> subscriptionStore.addSubscription(
                null, new TopicSubscriptionImpl("topic", Qos.AT_MOST_ONCE, false, false, 0)).get())
                .hasCauseInstanceOf(NullPointerException.class);

        verify(clientSessionSubscriptionPersistence, never()).addSubscription(eq("client"), any(Topic.class));

    }

    @Test
    @Timeout(10)
    public void test_add_null_topic() {

        assertThatThrownBy(() ->
                subscriptionStore.addSubscription("client", null).get())
                .hasCauseInstanceOf(NullPointerException.class);

        verify(clientSessionSubscriptionPersistence, never()).addSubscription(eq("client"), any(Topic.class));

    }

    @Test
    @Timeout(10)
    public void test_add_multi_null_client_id() {

        assertThatThrownBy(() -> subscriptionStore.addSubscriptions(
                null, ImmutableSet.of(new TopicSubscriptionImpl("topic", Qos.AT_MOST_ONCE, false, false, 0))).get())
                .hasCauseInstanceOf(NullPointerException.class);

        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(anyString(), any(ImmutableSet.class));

    }

    @Test
    @Timeout(10)
    public void test_add_multi_null_topics() {

        assertThatThrownBy(() -> subscriptionStore.addSubscriptions("client", null))
                .isInstanceOf(NullPointerException.class);

        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(eq("client"), any(ImmutableSet.class));

    }

    @Test
    @Timeout(10)
    public void test_add_multi_empty_topics() {

        assertThatThrownBy(() -> subscriptionStore.addSubscriptions("client", ImmutableSet.of()))
                .isInstanceOf(IllegalArgumentException.class);

        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(eq("client"), any(ImmutableSet.class));

    }

    @Test
    @Timeout(10)
    public void test_remove_null_client_id() {
        assertThatThrownBy(() -> subscriptionStore.removeSubscription(null, "topic").get())
                .hasCauseInstanceOf(NullPointerException.class);

        verify(clientSessionSubscriptionPersistence, never()).remove("client", "topic");

    }

    @Test
    @Timeout(10)
    public void test_remove_null_topic() {
        assertThatThrownBy(() -> subscriptionStore.removeSubscription("client", null).get())
                .hasCauseInstanceOf(NullPointerException.class);

        verify(clientSessionSubscriptionPersistence, never()).remove("client", "topic");

    }

    @Test
    @Timeout(10)
    public void test_remove_multi_null_client_id() {

        assertThatThrownBy(() -> subscriptionStore.removeSubscriptions(null, ImmutableSet.of("topic")))
                .isInstanceOf(NullPointerException.class);

        verify(clientSessionSubscriptionPersistence, never()).removeSubscriptions(anyString(), any(ImmutableSet.class));

    }

    @Test
    @Timeout(10)
    public void test_remove_multi_null_topic() {

        assertThatThrownBy(() -> subscriptionStore.removeSubscriptions("client", null))
                .isInstanceOf(NullPointerException.class);

        verify(clientSessionSubscriptionPersistence, never()).removeSubscriptions(anyString(), any(ImmutableSet.class));

    }

    @Test
    @Timeout(10)
    public void test_remove_multi_empty_topics() {

        assertThatThrownBy(() -> subscriptionStore.removeSubscriptions("client", ImmutableSet.of()))
                .isInstanceOf(IllegalArgumentException.class);

        verify(clientSessionSubscriptionPersistence, never()).removeSubscriptions(anyString(), any(ImmutableSet.class));

    }
    
    @Test
    @Timeout(10)
    public void test_get_success() throws ExecutionException, InterruptedException {

        final Topic topic = new Topic("topic", QoS.AT_LEAST_ONCE, true,
                true, Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);

        when(clientSessionSubscriptionPersistence.getSubscriptions("client")).thenReturn(ImmutableSet.of(topic));

        final Set<TopicSubscription> subscriptions = subscriptionStore.getSubscriptions("client").get();

        assertEquals(1, subscriptions.size());

        verify(clientSessionSubscriptionPersistence).getSubscriptions("client");

    }
    
    @Test
    @Timeout(10)
    public void test_get_success_unmodifiable() throws ExecutionException, InterruptedException {

        final Topic topic = new Topic("topic", QoS.AT_LEAST_ONCE, true, true,
                Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);

        when(clientSessionSubscriptionPersistence.getSubscriptions("client")).thenReturn(ImmutableSet.of(topic));

        final Set<TopicSubscription> subscriptions = subscriptionStore.getSubscriptions("client").get();

        assertEquals(1, subscriptions.size());

        verify(clientSessionSubscriptionPersistence).getSubscriptions("client");

        assertThatThrownBy(() -> subscriptions.add(new TopicSubscriptionImpl(topic)))
                .isInstanceOf(UnsupportedOperationException.class);

    }
    
    @Test
    @Timeout(10)
    public void test_add_success() throws ExecutionException, InterruptedException {

        final Topic topic = new Topic("topic", QoS.AT_LEAST_ONCE, true,
                true, Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);

        when(clientSessionSubscriptionPersistence.addSubscription("client", topic)).thenReturn(
                Futures.immediateFuture(new SubscriptionResult(topic, false, null)));

        subscriptionStore.addSubscription("client", new TopicSubscriptionImpl(topic)).get();

        verify(clientSessionSubscriptionPersistence).addSubscription(eq("client"), any(Topic.class));

    }

    @Test
    @Timeout(10)
    public void test_add_multi_success() throws ExecutionException, InterruptedException {

        final Topic topic1 = new Topic("topic1", QoS.AT_LEAST_ONCE, true,
                true, Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);
        final Topic topic2 = new Topic("topic2", QoS.AT_LEAST_ONCE, true,
                true, Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);

        when(clientSessionSubscriptionPersistence.addSubscriptions(
                "client", ImmutableSet.of(topic1, topic2))).thenReturn(
                Futures.immediateFuture(ImmutableList.of()));

        subscriptionStore.addSubscriptions(
                "client", ImmutableSet.of(new TopicSubscriptionImpl(topic1), new TopicSubscriptionImpl(topic2))).get();

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), any(ImmutableSet.class));

    }

    @Test
    @Timeout(10)
    public void test_add_multi_one_null() {

        final Topic topic1 = new Topic("topic1", QoS.AT_LEAST_ONCE, true,
                true, Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);

        final Set<TopicSubscription> set = new HashSet<>();

        set.add(new TopicSubscriptionImpl(topic1));
        set.add(null);

        assertThatThrownBy(() -> subscriptionStore.addSubscriptions("client", set).get())
                .isInstanceOf(NullPointerException.class);

    }

    @Test
    @Timeout(10)
    public void test_remove_multi_one_null() {

        final Set<String> set = new HashSet<>();

        set.add("topic1");
        set.add(null);

        assertThatThrownBy(() -> subscriptionStore.removeSubscriptions("client", set).get())
                .isInstanceOf(NullPointerException.class);

    }

    @Test
    @Timeout(10)
    public void test_add_failed_client_session_not_existent() {

        final Topic topic = new Topic("topic", QoS.AT_LEAST_ONCE, true, true,
                Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);

        when(clientSessionSubscriptionPersistence.addSubscription("client", topic)).thenReturn(
                Futures.immediateFuture(null));

        assertThatThrownBy(() -> subscriptionStore.addSubscription("client", new TopicSubscriptionImpl(topic)).get())
                .hasCauseInstanceOf(NoSuchClientIdException.class);
    }

    @Test
    @Timeout(10)
    public void test_add_multi_failed_client_session_not_existent() {

        final Topic topic = new Topic("topic", QoS.AT_LEAST_ONCE, true, true,
                Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);

        when(clientSessionSubscriptionPersistence.addSubscriptions("client", ImmutableSet.of(topic))).thenReturn(
                Futures.immediateFuture(null));

        assertThatThrownBy(() -> subscriptionStore.addSubscriptions("client", ImmutableSet.of(new TopicSubscriptionImpl(topic))).get())
                .hasCauseInstanceOf(NoSuchClientIdException.class);
    }

    @Test
    @Timeout(10)
    public void test_add_failed() {

        final Topic topic = new Topic("topic", QoS.AT_LEAST_ONCE, true, true,
                Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);

        when(clientSessionSubscriptionPersistence.addSubscription("client", topic)).thenReturn(
                Futures.immediateFailedFuture(TestException.INSTANCE));

        assertThatThrownBy(() -> subscriptionStore.addSubscription("client", new TopicSubscriptionImpl(topic)).get())
                .isInstanceOf(ExecutionException.class);

        verify(clientSessionSubscriptionPersistence).addSubscription(eq("client"), any(Topic.class));

    }

    @Test
    @Timeout(10)
    public void test_add_multi_failed() {

        final Topic topic = new Topic("topic", QoS.AT_LEAST_ONCE, true, true,
                Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);

        when(clientSessionSubscriptionPersistence.addSubscriptions("client", ImmutableSet.of(topic))).thenReturn(
                Futures.immediateFailedFuture(TestException.INSTANCE));

        assertThatThrownBy(() -> subscriptionStore.addSubscriptions("client", ImmutableSet.of(new TopicSubscriptionImpl(topic))).get())
                .isInstanceOf(ExecutionException.class);

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), any(ImmutableSet.class));

    }

    @Test
    @Timeout(20)
    public void test_add_subscription_falsely_implemented_class() {
        assertThatThrownBy(() -> subscriptionStore.addSubscription("client", new TestSubscriptionImpl()).get())
                .hasCauseInstanceOf(DoNotImplementException.class);
    }

    @Test
    @Timeout(20)
    public void test_add_multi_subscription_falsely_implemented_class() {

        final Topic topic = new Topic("topic", QoS.AT_LEAST_ONCE, true, true,
                Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);

        assertThatThrownBy(() -> subscriptionStore.addSubscriptions(
                "client", ImmutableSet.of(new TopicSubscriptionImpl(topic), new TestSubscriptionImpl())).get())
                .hasCauseInstanceOf(DoNotImplementException.class);

    }

    @Test
    @Timeout(10)
    public void test_remove_success() throws ExecutionException, InterruptedException {

        when(clientSessionSubscriptionPersistence.remove("client", "topic")).thenReturn(Futures.immediateFuture(null));

        subscriptionStore.removeSubscription("client", "topic").get();

        verify(clientSessionSubscriptionPersistence).remove("client", "topic");

    }

    @Test
    @Timeout(10)
    public void test_remove_multi_success() throws ExecutionException, InterruptedException {

        when(clientSessionSubscriptionPersistence.removeSubscriptions(
                "client", ImmutableSet.of("topic", "topic2"))).thenReturn(Futures.immediateFuture(null));

        subscriptionStore.removeSubscriptions("client", ImmutableSet.of("topic", "topic2")).get();

        verify(clientSessionSubscriptionPersistence).removeSubscriptions("client", ImmutableSet.of("topic", "topic2"));

    }

    @Test
    @Timeout(10)
    public void test_remove_failed_topic_empty() {
        assertThatThrownBy(() -> subscriptionStore.removeSubscription("client", "").get())
                .hasCauseInstanceOf(InvalidTopicException.class);
    }

    @Test
    @Timeout(10)
    public void test_remove_multi_failed_topic_empty() {
        assertThatThrownBy(() -> subscriptionStore.removeSubscriptions("client", ImmutableSet.of("topic", "", "huhu")).get())
                .hasCauseInstanceOf(InvalidTopicException.class);

    }

    @Test
    @Timeout(10)
    public void test_remove_failed_topic_bad_char() {
        assertThatThrownBy(() -> subscriptionStore.removeSubscription("client", "123" + "\u0000").get())
                .hasCauseInstanceOf(InvalidTopicException.class);

    }

    @Test
    @Timeout(10)
    public void test_remove_multi_failed_topic_bad_char() {
        assertThatThrownBy(() -> subscriptionStore.removeSubscriptions("client", ImmutableSet.of("topic", "123" + "\u0000")).get())
                .hasCauseInstanceOf(InvalidTopicException.class);

    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_invalid_topic_wildcard() {
        assertThatThrownBy(() -> subscriptionStore.iterateAllSubscribersForTopic("topic/#", (context, value) -> {
        }, MoreExecutors.directExecutor()).get())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_invalid_topic_plus_wildcard() {
        assertThatThrownBy(() -> subscriptionStore.iterateAllSubscribersForTopic("+/topic", (context, value) -> {
        }, MoreExecutors.directExecutor()).get())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_invalid_topic_null() {
        assertThatThrownBy(() -> subscriptionStore.iterateAllSubscribersForTopic(null, (context, value) -> {
        }, MoreExecutors.directExecutor()).get())
                .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_invalid_topic_bad_char() {
        assertThatThrownBy(() -> subscriptionStore.iterateAllSubscribersForTopic("123" + "\u0000", (context, value) -> {
        }, MoreExecutors.directExecutor()).get())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_all_subscribers_iterated() throws Exception {

        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (int i = 0; i < 1000; i++) {
            builder.add("client-" + i);
        }

        when(topicTree.getSubscribersForTopic(
                anyString(), any(Predicate.class), anyBoolean())).thenReturn(builder.build());


        final ImmutableSet.Builder<String> resultBuilder = ImmutableSet.builder();
        subscriptionStore.iterateAllSubscribersForTopic("topic", (context, value) -> {
            resultBuilder.add(value.getClientId());
        }, MoreExecutors.directExecutor()).get();

        assertEquals(1000, resultBuilder.build().size());
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_empty_result() throws Exception {

        when(topicTree.getSubscribersForTopic(anyString(), any(Predicate.class), anyBoolean()))
                .thenReturn(ImmutableSet.of());

        subscriptionStore.iterateAllSubscribersForTopic("topic", (context, value) -> {
        }, MoreExecutors.directExecutor()).get();

        //test checks if the future does return even if no item is returned
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_abort() throws Exception {

        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (int i = 0; i < 1000; i++) {
            builder.add("client-" + i);
        }

        when(topicTree.getSubscribersForTopic(anyString(), any(Predicate.class), anyBoolean()))
                .thenReturn(builder.build());

        final ImmutableSet.Builder<String> resultBuilder = ImmutableSet.builder();
        final AtomicInteger counter = new AtomicInteger(0);
        subscriptionStore.iterateAllSubscribersForTopic("topic", (context, value) -> {
            resultBuilder.add(value.getClientId());
            final int i = counter.incrementAndGet();
            if (i == 100) {
                context.abortIteration();
            }
        }, MoreExecutors.directExecutor()).get();

        assertEquals(100, resultBuilder.build().size());
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_throw_exception() {

        when(topicTree.getSubscribersForTopic(anyString(), any(Predicate.class), anyBoolean()))
                .thenReturn(ImmutableSet.of("client"));

        final CompletableFuture<Void> future =
                subscriptionStore.iterateAllSubscribersForTopic("topic", (context, value) -> {
                    throw new RuntimeException("test");
                }, MoreExecutors.directExecutor());

        //test checks if the future does return with an exception if an exception is thrown in the iterate callback
        assertThatThrownBy(() -> future.get())
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_filter_invalid_topic_null() {
        assertThatThrownBy(() -> subscriptionStore.iterateAllSubscribersWithTopicFilter(null, (context, value) -> {
        }, MoreExecutors.directExecutor()).get())
                .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_filter_invalid_topic_bad_char() {
        assertThatThrownBy(() -> subscriptionStore.iterateAllSubscribersWithTopicFilter("123" + "\u0000", (context, value) -> {
        }, MoreExecutors.directExecutor()).get())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_filter_all_subscribers_iterated() throws Exception {

        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (int i = 0; i < 1000; i++) {
            builder.add("client-" + i);
        }

        when(topicTree.getSubscribersWithFilter(anyString(), any(Predicate.class)))
                .thenReturn(builder.build());


        final ImmutableSet.Builder<String> resultBuilder = ImmutableSet.builder();
        subscriptionStore.iterateAllSubscribersWithTopicFilter("topic", (context, value) -> {
            resultBuilder.add(value.getClientId());
        }, MoreExecutors.directExecutor()).get();

        assertEquals(1000, resultBuilder.build().size());
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_filter_empty_result() throws Exception {

        when(topicTree.getSubscribersForTopic(anyString(), any(Predicate.class), anyBoolean()))
                .thenReturn(ImmutableSet.of());

        subscriptionStore.iterateAllSubscribersForTopic("topic", (context, value) -> {
        }, MoreExecutors.directExecutor()).get();

        //test checks if the future does return even if no item is returned
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_filter_abort() throws Exception {

        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (int i = 0; i < 1000; i++) {
            builder.add("client-" + i);
        }

        when(topicTree.getSubscribersWithFilter(anyString(), any(Predicate.class))).thenReturn(
                builder.build());

        final ImmutableSet.Builder<String> resultBuilder = ImmutableSet.builder();
        final AtomicInteger counter = new AtomicInteger(0);
        subscriptionStore.iterateAllSubscribersWithTopicFilter("topic", (context, value) -> {
            resultBuilder.add(value.getClientId());
            final int i = counter.incrementAndGet();
            if (i == 100) {
                context.abortIteration();
            }
        }, MoreExecutors.directExecutor()).get();

        assertEquals(100, resultBuilder.build().size());
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_filter_throw_exception() {

        when(topicTree.getSubscribersWithFilter(anyString(), any(Predicate.class)))
                .thenReturn(ImmutableSet.of("client"));

        final CompletableFuture<Void> future =
                subscriptionStore.iterateAllSubscribersWithTopicFilter("topic", (context, value) -> {
                    throw new RuntimeException("test");
                }, MoreExecutors.directExecutor());

        //test checks if the future does return with an exception if an exception is thrown in the iterate callback
        assertThatThrownBy(() -> future.get())
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_filter_rate_limit_exceeded() {
        when(rateLimitService.rateLimitExceeded()).thenReturn(true);

        assertThatThrownBy(() -> subscriptionStore.iterateAllSubscribersWithTopicFilter("topic/#", (context, value) -> {
        }).get())
                .hasCauseInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_topic_rate_limit_exceeded() {
        when(rateLimitService.rateLimitExceeded()).thenReturn(true);

        assertThatThrownBy(() -> subscriptionStore.iterateAllSubscribersForTopic("topic", (context, value) -> {
        }).get())
                .hasCauseInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_all_rate_limit_exceeded() {
        when(rateLimitService.rateLimitExceeded()).thenReturn(true);

        assertThatThrownBy(() -> subscriptionStore.iterateAllSubscriptions((context, value) -> {
        }).get())
                .hasCauseInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_all_callback_null() {
        assertThatThrownBy(() -> subscriptionStore.iterateAllSubscriptions(null).get())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_all_callback_executor_null() {
        assertThatThrownBy(() -> subscriptionStore.iterateAllSubscriptions((context, value) -> {
        }, null).get())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Timeout(10)
    public void test_item_callback() throws Exception {
        final ArrayList<SubscriptionsForClientResult> items = Lists.newArrayList();

        final CountDownLatch latch = new CountDownLatch(1);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final AllItemsItemCallback<SubscriptionsForClientResult> itemCallback = new AllItemsItemCallback<>(executor, (context, value) -> {
            items.add(value);
            latch.countDown();
        });

        final ListenableFuture<Boolean> onItems = itemCallback.onItems(List.of(
                new SubscriptionsForClientResultImpl("client", Set.of(new TopicSubscriptionImpl("topic1", Qos.AT_LEAST_ONCE, false, false, 1))),
                new SubscriptionsForClientResultImpl("client2", Set.of(new TopicSubscriptionImpl("topic2", Qos.AT_LEAST_ONCE, false, false, 1))),
                new SubscriptionsForClientResultImpl("client3", Set.of(new TopicSubscriptionImpl("topic3", Qos.AT_LEAST_ONCE, false, false, 1)))
        ));

        assertTrue(onItems.get());

        assertEquals(3, items.size());

        executor.shutdownNow();
    }

    @Test
    @Timeout(10)
    public void test_item_callback_abort() throws Exception {

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final AllItemsItemCallback<SubscriptionsForClientResult> itemCallback = new AllItemsItemCallback<>(executor, (context, value) -> {
            context.abortIteration();
        });

        final ListenableFuture<Boolean> onItems = itemCallback.onItems(List.of(
                new SubscriptionsForClientResultImpl("client", Set.of(new TopicSubscriptionImpl("topic1", Qos.AT_LEAST_ONCE, false, false, 1))),
                new SubscriptionsForClientResultImpl("client2", Set.of(new TopicSubscriptionImpl("topic2", Qos.AT_LEAST_ONCE, false, false, 1))),
                new SubscriptionsForClientResultImpl("client3", Set.of(new TopicSubscriptionImpl("topic3", Qos.AT_LEAST_ONCE, false, false, 1)))
        ));

        assertFalse(onItems.get());

        executor.shutdownNow();
    }

    @Test
    @Timeout(10)
    public void test_item_callback_exception() {

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final AllItemsItemCallback<SubscriptionsForClientResult> itemCallback = new AllItemsItemCallback<>(executor, (context, value) -> {
            throw new RuntimeException("test-exception");
        });

        final ListenableFuture<Boolean> onItems = itemCallback.onItems(List.of(
                new SubscriptionsForClientResultImpl("client", Set.of(new TopicSubscriptionImpl("topic1", Qos.AT_LEAST_ONCE, false, false, 1))),
                new SubscriptionsForClientResultImpl("client2", Set.of(new TopicSubscriptionImpl("topic2", Qos.AT_LEAST_ONCE, false, false, 1))),
                new SubscriptionsForClientResultImpl("client3", Set.of(new TopicSubscriptionImpl("topic3", Qos.AT_LEAST_ONCE, false, false, 1)))
        ));

        assertThatThrownBy(() -> onItems.get())
                .hasCauseInstanceOf(RuntimeException.class);

        executor.shutdownNow();
    }


    @Test
    @Timeout(10)
    public void test_iteration_started() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        //noinspection unchecked
        when(asyncIteratorFactory.createIterator(any(FetchCallback.class), any(AsyncIterator.ItemCallback.class)))
                .thenReturn(new AsyncIterator() {
                    @Override
                    public void fetchAndIterate() {
                        latch.countDown();
                    }

                    @Override
                    public @NotNull CompletableFuture<Void> getFinishedFuture() {
                        return resultFuture;
                    }
                });

        final CompletableFuture<Void> finishFuture = subscriptionStore.iterateAllSubscriptions((context, value) -> {
        });

        resultFuture.complete(null);

        latch.await();
    }

    @Test
    public void test_test_fetch_callback_conversion() {

        final SubscriptionStoreImpl.AllSubscribersFetchCallback fetchCallback = new SubscriptionStoreImpl.AllSubscribersFetchCallback(null);

        final ChunkResult<SubscriptionsForClientResult> chunkResult = fetchCallback.convertToChunkResult(new MultipleChunkResult<Map<String, ImmutableSet<Topic>>>(
                Map.of(
                        1, new BucketChunkResult<>(Map.of(
                                "client1", ImmutableSet.of(
                                        new Topic("topic1", QoS.AT_LEAST_ONCE)
                                )), true, "client1", 1),
                        2, new BucketChunkResult<>(Map.of(
                                "client2", ImmutableSet.of(
                                        new Topic("topic2", QoS.AT_LEAST_ONCE), new Topic("topic3", QoS.AT_LEAST_ONCE)
                                ),
                                "client3", ImmutableSet.of(
                                        new Topic("topic4", QoS.AT_LEAST_ONCE)
                                )
                        ), false, "client3", 2)
                )
        ));

        assertTrue(chunkResult.getCursor().getFinishedBuckets().contains(1));
        assertFalse(chunkResult.getCursor().getFinishedBuckets().contains(2));
        assertEquals(3, chunkResult.getResults().size());
    }


    private static class TestSubscriptionImpl implements TopicSubscription {

        @NotNull
        @Override
        public String getTopicFilter() {
            return null;
        }

        @NotNull
        @Override
        public Qos getQos() {
            return null;
        }

        @Override
        public boolean getRetainAsPublished() {
            return false;
        }

        @Override
        public boolean getNoLocal() {
            return false;
        }

        @NotNull
        @Override
        public Optional<Integer> getSubscriptionIdentifier() {
            return Optional.empty();
        }
    }

    private GlobalManagedExtensionExecutorService getManagedExtensionExecutorService() {
        final GlobalManagedExtensionExecutorService globalManagedPluginExecutorService =
                new GlobalManagedExtensionExecutorService(mock(ShutdownHooks.class));
        globalManagedPluginExecutorService.postConstruct();
        return globalManagedPluginExecutorService;
    }
}
