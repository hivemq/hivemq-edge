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
package com.hivemq.bridge.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LocalSubscriptionTest {

    @Test
    void calculateUniqueId_whenOrderWithinListsIsChanged_thenSameUniqueIdMustResult() {
        final LocalSubscription localSubscription = new LocalSubscription(
                List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(
                        CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2,
                1000L);
        final LocalSubscription topicSwitched = new LocalSubscription(
                List.of("topicC/#", "topicB/#", "topicA/+"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(
                        CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2,
                1000L);
        assertEquals(localSubscription.calculateUniqueId(), topicSwitched.calculateUniqueId());

        final LocalSubscription excludesSwitched = new LocalSubscription(
                List.of("topicC/#", "topicB/#", "topicA/+"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(
                        CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2,
                1000L);

        assertEquals(localSubscription.calculateUniqueId(), excludesSwitched.calculateUniqueId());

        final LocalSubscription customPropertiesSwitched = new LocalSubscription(
                List.of("topicC/#", "topicB/#", "topicA/+"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2,
                1000L);

        assertEquals(localSubscription.calculateUniqueId(), customPropertiesSwitched.calculateUniqueId());
    }

    @Test
    void calculateUniqueId_whenChangeInTopic_thenUniqueIdsAreDifferent() {
        final LocalSubscription localSubscription = new LocalSubscription(
                List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(
                        CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2,
                1000L);

        // "topicA/+" => "topicB/+"
        final LocalSubscription otherSubscription = new LocalSubscription(
                List.of("topicB/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(
                        CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2,
                1000L);
        assertNotEquals(localSubscription.calculateUniqueId(), otherSubscription.calculateUniqueId());
    }

    @Test
    void calculateUniqueId_whenChangeInDestinationTopic_thenUniqueIdsAreDifferent() {
        final LocalSubscription localSubscription = new LocalSubscription(
                List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(
                        CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2,
                1000L);

        // "topicA/+" => "topicB/+"
        final LocalSubscription otherSubscription = new LocalSubscription(
                List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic2",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(
                        CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2,
                1000L);
        assertNotEquals(localSubscription.calculateUniqueId(), otherSubscription.calculateUniqueId());
    }

    @Test
    void calculateUniqueId_whenStringInDifferentFieldsAreSwapped_thenUniqueIdsAreDifferent() {
        final LocalSubscription localSubscription = new LocalSubscription(
                List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(
                        CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2,
                1000L);

        // "topicA/+" and "topicA/topic/" are swapped
        final LocalSubscription otherSubscription = new LocalSubscription(
                List.of("topicA/topic/", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/+", "topicA/topic/", "otherTopic"),
                List.of(
                        CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                1,
                1000L);
        assertNotEquals(localSubscription.calculateUniqueId(), otherSubscription.calculateUniqueId());
    }

    private static LocalSubscription subscription(final String filter, final String destination) {
        return new LocalSubscription(
                List.of(filter),
                destination,
                List.of("excluded/topic"),
                List.of(CustomUserProperty.of("key1", "value1")),
                true,
                2,
                1000L);
    }

    /**
     * EDG-884. The fingerprint is computed lazily, so a running bridge's subscription has it cached
     * while one freshly read from the configuration file does not. If equality compared it, the two
     * would differ even for a byte-identical configuration — and the reload path reads that as "the
     * bridge changed", restarts it with {@code clearQueue = true} and destroys every queued message.
     */
    @Test
    void equals_whenOneSideHasMemoisedItsUniqueId_thenStillEqual() {
        final LocalSubscription running = subscription("topicA/+", "destinationTopic");
        final LocalSubscription freshlyParsed = subscription("topicA/+", "destinationTopic");

        assertEquals(running, freshlyParsed, "identical configurations must start out equal");

        // what a running bridge does: BridgeMqttClient.createForwarderId asks for the fingerprint
        running.calculateUniqueId();

        assertEquals(
                running,
                freshlyParsed,
                "memoising the derived fingerprint must not make an unchanged configuration look changed");
        assertEquals(freshlyParsed, running, "equality must stay symmetric regardless of which side memoised first");
    }

    /**
     * The hash must not change over an object's lifetime — a config object whose hash moves the first
     * time a derived field is computed is a trap for any future map keyed on it, and it would break
     * {@code MqttBridge.hashCode}, which folds this one in.
     */
    @Test
    void hashCode_whenUniqueIdIsMemoised_thenUnchanged() {
        final LocalSubscription subscription = subscription("topicA/+", "destinationTopic");
        final int before = subscription.hashCode();

        subscription.calculateUniqueId();

        assertEquals(before, subscription.hashCode(), "hashCode changed once the fingerprint was cached");
        assertEquals(
                subscription("topicA/+", "destinationTopic").hashCode(),
                subscription.hashCode(),
                "equal objects must still agree on hashCode after one of them memoised");
    }

    /** A genuine configuration change must still be detected — the fix must not blind the reload path. */
    @Test
    void equals_whenTheConfigurationGenuinelyChanges_thenNotEqual() {
        final LocalSubscription original = subscription("topicA/+", "destinationTopic");
        original.calculateUniqueId();

        assertNotEquals(original, subscription("topicB/+", "destinationTopic"), "a changed filter must be detected");
        assertNotEquals(
                original, subscription("topicA/+", "otherDestination"), "a changed destination must be detected");
    }

    /**
     * The whole point of EDG-884, one level up: {@code BridgeService.updateBridges} compares
     * {@link MqttBridge} objects, and that comparison walks into the subscription list. An unchanged
     * bridge must compare equal even after it has been running.
     */
    @Test
    void mqttBridgeEquals_whenARunningBridgeIsComparedToItsReparsedConfig_thenEqual() {
        final LocalSubscription runningSubscription = subscription("topicA/+", "destinationTopic");
        final MqttBridge running = bridge(runningSubscription);
        final MqttBridge freshlyParsed = bridge(subscription("topicA/+", "destinationTopic"));

        runningSubscription.calculateUniqueId(); // the bridge has been up; something asked for the id

        assertEquals(
                running,
                freshlyParsed,
                "an unchanged bridge compared as changed; the reload path would clear its live queue");
        assertEquals(running.hashCode(), freshlyParsed.hashCode());
    }

    private static MqttBridge bridge(final LocalSubscription subscription) {
        return new MqttBridge.Builder()
                .withId("edg-884-bridge")
                .withHost("localhost")
                .withPort(1883)
                .withClientId("client")
                .withLocalSubscriptions(List.of(subscription))
                .withRemoteSubscriptions(List.of())
                .build();
    }
}
