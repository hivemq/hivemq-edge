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
package com.hivemq.sampling;

import static com.hivemq.sampling.SamplingService.SAMPLER_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

public class SamplingServiceTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "topic",
                "a/b",
                "a/b/c",
                "a//b",
                "sport/tennis/player1/#",
                "$share",
                "/",
                "//",
                "aa",
                "a/a",
                "$SAMPLER::x",
                "$FORWARDER::bridge-hash/t"
            })
    public void test_extractSampledTopic_recovers_every_sampled_topic(final String topic) {
        assertEquals(topic, SamplingService.extractSampledTopic(SamplingService.createQueueId(topic)));
    }

    /** The empty topic is degenerate but reachable through the same code path; it must round-trip. */
    @Test
    public void test_extractSampledTopic_recovers_the_empty_topic() {
        assertEquals("", SamplingService.extractSampledTopic(SamplingService.createQueueId("")));
        assertEquals(SAMPLER_PREFIX + "/", SamplingService.createQueueId(""));
    }

    @Test
    public void test_extractSampledTopic_rejects_ids_that_are_not_sampler_queues() {
        assertNull(SamplingService.extractSampledTopic("group/topic"));
        assertNull(SamplingService.extractSampledTopic("$FORWARDER::bridge-hash/topic"));
        assertNull(SamplingService.extractSampledTopic(""));
        assertNull(SamplingService.extractSampledTopic("$SAMPLER:")); // prefix truncated
        // right prefix, but not the <topic>/<topic> shape a sampler queue has
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX + "a/b"));
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX + "ab/ba"));
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX + "topic"));
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX));
        // odd length and a '/' at the midpoint, but the halves differ
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX + "ab/cd"));
        // even length can never be <topic>/<topic>
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX + "ab/c"));
        // right shape, wrong separator character
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX + "ab.ab"));
    }

    /**
     * The worst case for the in-place half comparison: odd length, a '/' exactly at the midpoint, and
     * halves that agree everywhere except the final character. A comparison that stopped early, or one
     * that compared the wrong span, would accept this.
     */
    @Test
    public void test_extractSampledTopic_rejects_a_near_miss_differing_only_in_the_last_character() {
        for (int length = 1; length <= 64; length++) {
            final String left = "a".repeat(length);
            final String right = "a".repeat(length - 1) + "b";
            assertNull(
                    SamplingService.extractSampledTopic(SAMPLER_PREFIX + left + "/" + right),
                    "halves of length " + length + " differing in the last character");
            assertNull(
                    SamplingService.extractSampledTopic(SAMPLER_PREFIX + right + "/" + left),
                    "halves of length " + length + " differing in the first-half last character");
        }
    }

    /**
     * Topics that are themselves well-formed queue IDs of some other kind. Nothing about the sampler
     * shape is special-cased, so these must round-trip like any other topic — and must not be confused
     * with the queues they resemble.
     */
    @ParameterizedTest
    @ValueSource(
            strings = {
                "$SAMPLER::a/a",
                "$FORWARDER::bridge-Ab/cd==/t",
                "$share/group/topic",
                "$SAMPLER::",
                "a/a/a",
                "//",
                "äöü/中文/🚀"
            })
    public void test_extractSampledTopic_round_trips_topics_that_look_like_other_queue_ids(final String topic) {
        assertEquals(topic, SamplingService.extractSampledTopic(SamplingService.createQueueId(topic)));
    }

    /**
     * A topic long enough that the two halves span far beyond any small-string optimisation, checked
     * both for a hit and for a single-character corruption in the middle of the second half.
     */
    @Test
    public void test_extractSampledTopic_handles_long_topics_and_their_near_misses() {
        final String longTopic = "plant/line/".repeat(500) + "sensor";
        assertEquals(longTopic, SamplingService.extractSampledTopic(SamplingService.createQueueId(longTopic)));

        final String corrupted = SAMPLER_PREFIX
                + longTopic
                + "/"
                + longTopic.substring(0, longTopic.length() / 2)
                + "X"
                + longTopic.substring(longTopic.length() / 2 + 1);
        assertNull(SamplingService.extractSampledTopic(corrupted));
    }

    /**
     * A sampler queue ID whose two halves are equal but whose midpoint character is not the separator
     * — the shape a comparison-only check would wrongly accept.
     */
    @Test
    public void test_extractSampledTopic_requires_the_separator_at_the_midpoint() {
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX + "ab" + "x" + "ab"));
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX + "a/b" + "x" + "a/b"));
        // separator present but off-centre
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX + "a/bcd"));
    }

    /**
     * Differential test against the implementation this one replaced. The rewrite is a legibility
     * change and must be observationally identical, so the strongest available evidence is exhaustive
     * agreement over a generated corpus rather than a handful of chosen examples.
     * <p>
     * The corpus is every string over {@code {a, b, /}} up to length 6, taken both bare and with the
     * sampler prefix attached — every arrangement of separator placement, parity and half-equality the
     * decision procedure can encounter.
     */
    @Test
    @Timeout(30)
    public void test_extractSampledTopic_agrees_with_the_previous_implementation_exhaustively() {
        int checked = 0;
        for (final String candidate : corpus()) {
            assertEquals(
                    referenceExtractSampledTopic(candidate),
                    SamplingService.extractSampledTopic(candidate),
                    "bare candidate: " + candidate);
            final String prefixed = SAMPLER_PREFIX + candidate;
            assertEquals(
                    referenceExtractSampledTopic(prefixed),
                    SamplingService.extractSampledTopic(prefixed),
                    "prefixed candidate: " + prefixed);
            checked++;
        }
        // guards against the corpus silently collapsing to nothing if the generator is edited
        assertEquals(1093, checked, "corpus size");
    }

    private static List<String> corpus() {
        final char[] alphabet = {'a', 'b', '/'};
        final List<String> corpus = new ArrayList<>();
        corpus.add("");
        List<String> frontier = List.of("");
        for (int length = 1; length <= 6; length++) {
            final List<String> next = new ArrayList<>();
            for (final String prefix : frontier) {
                for (final char c : alphabet) {
                    next.add(prefix + c);
                }
            }
            corpus.addAll(next);
            frontier = next;
        }
        return corpus;
    }

    /** The implementation in place before the rewrite, kept verbatim as the behavioural oracle. */
    private static String referenceExtractSampledTopic(final String queueId) {
        if (!queueId.startsWith(SAMPLER_PREFIX)) {
            return null;
        }
        final String topicTwice = queueId.substring(SAMPLER_PREFIX.length());
        final int separator = topicTwice.length() / 2;
        if (topicTwice.length() % 2 == 0 || topicTwice.charAt(separator) != '/') {
            return null;
        }
        final String topic = topicTwice.substring(0, separator);
        return topic.equals(topicTwice.substring(separator + 1)) ? topic : null;
    }

    /**
     * Binds the queue-ID convention to what {@code startSampling} actually registers: the queue is
     * named {@code <share name>/<topic filter>}, so if the share name ever stops being
     * {@code $SAMPLER::<topic>} the clean-up would resolve the wrong owner and wipe live samples.
     */
    @ParameterizedTest
    @ValueSource(strings = {"topic", "a/b/c"})
    public void test_createQueueId_matches_the_subscription_startSampling_registers(final String topic) {
        final LocalTopicTree topicTree = mock(LocalTopicTree.class);
        new SamplingService(topicTree, mock(ClientQueuePersistence.class)).startSampling(topic);

        final ArgumentCaptor<Topic> registeredTopic = ArgumentCaptor.forClass(Topic.class);
        final ArgumentCaptor<String> shareName = ArgumentCaptor.forClass(String.class);
        verify(topicTree).addTopic(anyString(), registeredTopic.capture(), anyByte(), shareName.capture());

        assertEquals(
                shareName.getValue() + "/" + registeredTopic.getValue().getTopic(),
                SamplingService.createQueueId(topic));
    }

    private static @NotNull SamplingService samplingService() {
        return new SamplingService(mock(LocalTopicTree.class), mock(ClientQueuePersistence.class));
    }

    /**
     * EDG-882 F-05. The publish path asks this before applying a policy that discards the oldest
     * messages, so it must answer about what the service owns, not about how the ID is spelled.
     */
    @Test
    @Timeout(5)
    public void test_isSamplerQueue_only_while_the_topic_is_actually_sampled() {
        final SamplingService samplingService = samplingService();
        final String queueId = SamplingService.createQueueId("plant/line1");

        assertFalse(samplingService.isSamplerQueue(queueId), "nothing is being sampled yet");

        samplingService.startSampling("plant/line1");
        assertTrue(samplingService.isSamplerQueue(queueId));

        samplingService.stopSampling("plant/line1");
        assertFalse(samplingService.isSamplerQueue(queueId), "the last watcher let go");
    }

    /**
     * The case the ticket is about: a client subscribing to {@code $share/$SAMPLER::customer/alerts}
     * produces this queue ID. Its messages are its own and must not be evicted under the sampler's
     * policy — and the two halves differ, so the ID is not even the shape sampling produces.
     */
    @Test
    @Timeout(5)
    public void test_isSamplerQueue_rejects_a_client_shared_subscription_named_like_a_sampler() {
        final SamplingService samplingService = samplingService();
        samplingService.startSampling("customer");

        assertFalse(samplingService.isSamplerQueue("$SAMPLER::customer/alerts"));
    }

    /**
     * And a client that contrives the doubled shape is still not a sampler: nobody asked for samples
     * of that topic, so the watchers map — the same one {@code startSampling} maintains — says no.
     */
    @Test
    @Timeout(5)
    public void test_isSamplerQueue_rejects_the_sampler_shape_when_nothing_is_sampled() {
        final SamplingService samplingService = samplingService();
        samplingService.startSampling("some/other/topic");

        assertFalse(samplingService.isSamplerQueue(SamplingService.createQueueId("alerts/alerts")));
        assertFalse(samplingService.isSamplerQueue("ordinary/queue"));
        assertFalse(samplingService.isSamplerQueue(""));
    }

    /** A second watcher keeps the queue a sampler until the last one lets go. */
    @Test
    @Timeout(5)
    public void test_isSamplerQueue_follows_the_watcher_count() {
        final SamplingService samplingService = samplingService();
        final String queueId = SamplingService.createQueueId("plant/line1");
        samplingService.startSampling("plant/line1");
        samplingService.startSampling("plant/line1");

        samplingService.stopSampling("plant/line1");
        assertTrue(samplingService.isSamplerQueue(queueId), "one watcher is still looking");

        samplingService.stopSampling("plant/line1");
        assertFalse(samplingService.isSamplerQueue(queueId));
    }
}
