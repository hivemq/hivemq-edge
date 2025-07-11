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
package com.hivemq.persistence.clientsession;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hivemq.configuration.service.InternalConfigurations.SHARED_SUBSCRIBER_CACHE_CONCURRENCY_LEVEL;
import static com.hivemq.configuration.service.InternalConfigurations.SHARED_SUBSCRIBER_CACHE_MAX_SIZE_SUBSCRIBERS;
import static com.hivemq.configuration.service.InternalConfigurations.SHARED_SUBSCRIBER_CACHE_TIME_TO_LIVE_MSEC;
import static com.hivemq.configuration.service.InternalConfigurations.SHARED_SUBSCRIPTION_CACHE_CONCURRENCY_LEVEL;
import static com.hivemq.configuration.service.InternalConfigurations.SHARED_SUBSCRIPTION_CACHE_MAX_SIZE_SUBSCRIPTIONS;
import static com.hivemq.configuration.service.InternalConfigurations.SHARED_SUBSCRIPTION_CACHE_TIME_TO_LIVE_MSEC;

/**
 * @author Florian Limpöck
 * @since 4.0.1
 */
@Singleton
public class SharedSubscriptionServiceImpl implements SharedSubscriptionService {

    private static final String SHARED_SUBSCRIPTION_PREFIX = "$share/";
    private static final Pattern SHARED_SUBSCRIPTION_PATTERN = Pattern.compile("\\$share(/(.*?)/(.*))");

    private static final int GROUP_INDEX = 2;
    private static final int TOPIC_INDEX = 3;

    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull ClientSessionSubscriptionPersistence subscriptionPersistence;

    private @Nullable Cache<String, ImmutableSet<SubscriberWithQoS>> sharedSubscriberCache;
    private @Nullable Cache<String, ImmutableSet<Topic>> sharedSubscriptionCache;

    @Inject
    public SharedSubscriptionServiceImpl(
            final @NotNull LocalTopicTree topicTree,
            final @NotNull ClientSessionSubscriptionPersistence subscriptionPersistence) {

        this.topicTree = topicTree;
        this.subscriptionPersistence = subscriptionPersistence;
    }

    /**
     * Returns a {@link Matcher} for shared subscription topics
     *
     * @param topic the topic to check
     * @return Matcher for topic
     */
    private static Matcher getSharedSubscriptionMatcher(final @NotNull String topic) {
        return SHARED_SUBSCRIPTION_PATTERN.matcher(topic);
    }

    @Inject //method injection, this gets called once after instantiation
    void postConstruct() {

        sharedSubscriberCache = CacheBuilder.newBuilder()
                .expireAfterWrite(SHARED_SUBSCRIBER_CACHE_TIME_TO_LIVE_MSEC, TimeUnit.MILLISECONDS)
                .concurrencyLevel(SHARED_SUBSCRIBER_CACHE_CONCURRENCY_LEVEL.get())
                .maximumSize(SHARED_SUBSCRIBER_CACHE_MAX_SIZE_SUBSCRIBERS)
                .recordStats()
                .build();

        sharedSubscriptionCache = CacheBuilder.newBuilder()
                .expireAfterWrite(SHARED_SUBSCRIPTION_CACHE_TIME_TO_LIVE_MSEC, TimeUnit.MILLISECONDS)
                .concurrencyLevel(SHARED_SUBSCRIPTION_CACHE_CONCURRENCY_LEVEL.get())
                .maximumSize(SHARED_SUBSCRIPTION_CACHE_MAX_SIZE_SUBSCRIPTIONS)
                .recordStats()
                .build();
    }

    /**
     * This check is only for the shared subscription service, the subscription topic validation happens for
     * performance reasons in com.hivemq.util.Topics. Changes to the shared subscription syntax need to be reflected
     * there as well.
     *
     * @param topic a validated subscription topic string
     * @return a SharedSubscription or null if $share keyword not present
     */
    @Nullable
    public SharedSubscription checkForSharedSubscription(final @NotNull String topic) {

        final Matcher matcher = getSharedSubscriptionMatcher(topic);
        if (matcher.matches()) {
            final String shareGroup;
            final String subscriptionTopic;
            shareGroup = matcher.group(GROUP_INDEX);
            subscriptionTopic = matcher.group(TOPIC_INDEX);
            return new SharedSubscription(subscriptionTopic, shareGroup);
        }

        return null;
    }

    @NotNull
    public Subscription createSubscription(final @NotNull Topic topic) {

        final SharedSubscription sharedSubscription = checkForSharedSubscription(topic.getTopic());
        if (sharedSubscription == null) {
            return new Subscription(topic, SubscriptionFlag.getDefaultFlags(false, topic.isRetainAsPublished(), topic.isNoLocal()), null);
        } else {
            return new Subscription(new Topic(sharedSubscription.getTopicFilter(), topic.getQoS(), topic.isNoLocal(), topic.isRetainAsPublished(), topic.getRetainHandling(), topic.getSubscriptionIdentifier()),
                    SubscriptionFlag.getDefaultFlags(true, topic.isRetainAsPublished(), topic.isNoLocal()), sharedSubscription.getShareName());
        }
    }

    /**
     * Requests all shared subscribers for a given shared subscription.
     *
     * @param sharedSubscription is the share name and the topic filter separated by a '/'
     * @return a set of subscribers
     */
    @NotNull
    public ImmutableSet<SubscriberWithQoS> getSharedSubscriber(final @NotNull String sharedSubscription) {
        //calling this method before post construct will return an empty set
        if (sharedSubscriberCache == null) {
            return ImmutableSet.of();
        }
        try {
            return sharedSubscriberCache.get(sharedSubscription, () -> {
                final SharedSubscription split = splitTopicAndGroup(sharedSubscription);
                return topicTree.getSharedSubscriber(split.getShareName(), split.getTopicFilter());
            });
        } catch (final ExecutionException e) {
            return ImmutableSet.of();
        }
    }

    /**
     * Requests all shared subscriptions for a given client id.
     *
     * @param client of which the subscriptions are requested.
     * @return a set of subscriptions
     */
    @NotNull
    public ImmutableSet<Topic> getSharedSubscriptions(final @NotNull String client) throws ExecutionException {
        //calling this method before post construct will return an empty set
        if (sharedSubscriptionCache == null) {
            return ImmutableSet.of();
        }
        return sharedSubscriptionCache.get(client, () -> subscriptionPersistence.getSharedSubscriptions(client));
    }

    /**
     * Converts a string of share name and topic filter separated by a '/' into a shared subscription object which
     * contains both separably.
     *
     * @param sharedSubscription is the share name and the topic filter separated by a '/'
     * @return a shared subscription object which shared name and topic filter
     */
    @NotNull
    public static SharedSubscription splitTopicAndGroup(final @NotNull String sharedSubscription) {
        final int slashIndex = sharedSubscription.indexOf("/");
        final String group = sharedSubscription.substring(0, slashIndex);
        final String topicFilter = sharedSubscription.substring(slashIndex + 1);
        return new SharedSubscription(topicFilter, group);
    }

    public void invalidateSharedSubscriberCache(final @NotNull String sharedSubscription) {
        if (sharedSubscriberCache != null) {
            sharedSubscriberCache.invalidate(sharedSubscription);
        }
    }

    public void invalidateSharedSubscriptionCache(final @NotNull String clientId) {
        if (sharedSubscriptionCache != null) {
            sharedSubscriptionCache.invalidate(clientId);
        }
    }

    /**
     * Removes the '$share/' from a given topic.
     * The remaining string has the same pattern that is used for she shared subscription message queue.
     *
     * @param topic from which the prefix will be removed
     * @return the topic without the leading '$share/' or the original topic if it does't start with '$share/'
     */
    @NotNull
    public String removePrefix(final @NotNull String topic) {
        if (topic.startsWith(SHARED_SUBSCRIPTION_PREFIX)) {
            return topic.substring(SHARED_SUBSCRIPTION_PREFIX.length());
        }
        return topic;
    }


    public static class SharedSubscription {

        private final @NotNull String topicFilter;
        private final @NotNull String shareName;

        public SharedSubscription(final @NotNull String topic, final @NotNull String shareName) {
            this.topicFilter = topic;
            this.shareName = shareName;
        }

        @NotNull
        public String getTopicFilter() {
            return topicFilter;
        }

        @NotNull
        public String getShareName() {
            return shareName;
        }
    }

}
