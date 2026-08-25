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
     * EDG-882 F-07. Order is not part of a subscription's identity: the filters are what the forwarder
     * subscribes to and the excludes are a match-any test, so a configuration edited only to reorder
     * them is the same configuration. It did not compare that way — {@code equals} was positional while
     * the fingerprint sorted — so a formatting change read as "the bridge changed", and the reload path
     * answers that by restarting the bridge in the mode that clears its queues.
     */
    @Test
    void equals_whenOnlyTheOrderOfFiltersOrExcludesDiffers_thenEqual() {
        final LocalSubscription written = new LocalSubscription(
                List.of("factory/+/temperature", "factory/+/pressure"),
                "destinationTopic",
                List.of("factory/ignored/temperature", "factory/ignored/pressure"),
                List.of(),
                false,
                1,
                null);
        final LocalSubscription reordered = new LocalSubscription(
                List.of("factory/+/pressure", "factory/+/temperature"),
                "destinationTopic",
                List.of("factory/ignored/pressure", "factory/ignored/temperature"),
                List.of(),
                false,
                1,
                null);

        assertEquals(written, reordered, "a reorder-only edit compared as a changed bridge");
        assertEquals(written.hashCode(), reordered.hashCode());
        assertEquals(written.calculateUniqueId(), reordered.calculateUniqueId(), "and it always owned the same queues");
    }

    /**
     * The bridge-level comparison is what {@code updateBridges} actually asks, so the property has to
     * survive being folded into {@link MqttBridge}: an unchanged bridge whose filters were reordered
     * must not be restarted at all.
     */
    @Test
    void mqttBridgeEquals_whenFiltersAreReordered_thenEqual() {
        assertEquals(
                bridge(new LocalSubscription(List.of("a/1", "b/2"), "destinationTopic")),
                bridge(new LocalSubscription(List.of("b/2", "a/1"), "destinationTopic")));
    }

    /**
     * Sorted, not de-duplicated. The fingerprint is taken over the sorted filters including repeats and
     * it names every persisted queue, so collapsing a repeat would rename that queue on upgrade and
     * strand the messages in it — the trade this ticket refused for the encoding itself.
     */
    @Test
    void calculateUniqueId_whenAFilterIsRepeated_thenStillItsOwnIdentity() {
        final LocalSubscription repeated = new LocalSubscription(List.of("a/1", "a/1"), "destinationTopic");
        final LocalSubscription once = new LocalSubscription(List.of("a/1"), "destinationTopic");

        assertNotEquals(once, repeated, "a repeated filter is a different configuration");
        assertNotEquals(
                once.calculateUniqueId(),
                repeated.calculateUniqueId(),
                "de-duplicating would rename this subscription's queues on upgrade");
        assertEquals(List.of("a/1", "a/1"), repeated.getFilters(), "and the repeat survives canonicalisation");
    }

    /**
     * User properties are left ordered on purpose: MQTT user properties are an ordered list and two
     * entries may share a key, so their order is configuration rather than formatting.
     */
    @Test
    void equals_whenOnlyTheOrderOfUserPropertiesDiffers_thenNotEqual() {
        final LocalSubscription first = new LocalSubscription(
                List.of("a/1"),
                "destinationTopic",
                List.of(),
                List.of(CustomUserProperty.of("k1", "v1"), CustomUserProperty.of("k2", "v2")),
                false,
                1,
                null);
        final LocalSubscription swapped = new LocalSubscription(
                List.of("a/1"),
                "destinationTopic",
                List.of(),
                List.of(CustomUserProperty.of("k2", "v2"), CustomUserProperty.of("k1", "v1")),
                false,
                1,
                null);

        assertNotEquals(first, swapped);
    }

    /** Canonicalisation must not lose or invent filters, whatever it is handed. */
    @Test
    void getFilters_returnsTheSameFiltersInCanonicalOrder() {
        assertEquals(List.of("a", "b", "c"), new LocalSubscription(List.of("c", "a", "b"), "d").getFilters());
        assertEquals(List.of(), new LocalSubscription(List.of(), "d").getFilters());
        assertEquals(List.of("only"), new LocalSubscription(List.of("only"), "d").getFilters());
    }

    /**
     * EDG-882 F-01, pinned as known and deliberate. The filters are joined with an <b>empty</b>
     * separator before being digested, so any two filter lists with the same sorted concatenation
     * produce the same fingerprint — and the fingerprint names every persisted queue the subscription
     * owns.
     * <p>
     * This is not fixed here: changing the encoding changes the fingerprint of every configuration,
     * renaming every persisted bridge queue on upgrade and stranding the messages in them, which is
     * why EDG-882 rejected re-encoding. The ambiguity is contained instead by
     * {@code BridgeMqttClient.verifyForwarderIdsAreUnique}, which refuses to start a bridge carrying
     * two subscriptions that collide. This test exists so that anyone who "fixes" the join here sees
     * that the collision is load-bearing and reads why.
     */
    @Test
    void calculateUniqueId_whenFilterConcatenationsAreEqual_thenIdsCollide() {
        final LocalSubscription first = new LocalSubscription(List.of("ab", "c"), "destinationTopic");
        final LocalSubscription second = new LocalSubscription(List.of("a", "bc"), "destinationTopic");

        assertEquals(
                first.calculateUniqueId(),
                second.calculateUniqueId(),
                "the ambiguity documented on calculateUniqueId(); bridge startup is what rejects it");
        assertNotEquals(first, second, "the two configurations are genuinely different");

        // and the digest itself commonly carries a '/', which is what made the collision fatal
        assertEquals("kAFQmDzST7DWlj99KOF/cg==", new LocalSubscription(List.of("abc"), null).calculateUniqueId());
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

    /**
     * The configured order is kept for writing the file back out, and is invisible to identity: two
     * subscriptions that differ only in the order they were written in are one subscription, or F-07's
     * fix would be undone by the field that exists to protect the operator's file (EDG-882 QA round 2).
     */
    @Test
    void configuredOrder_isKeptButDoesNotAffectIdentity() {
        final LocalSubscription asWritten = new LocalSubscription(List.of("zone-b/#", "alarms/#"), "destinationTopic");
        final LocalSubscription reordered = new LocalSubscription(List.of("alarms/#", "zone-b/#"), "destinationTopic");

        assertEquals(List.of("zone-b/#", "alarms/#"), asWritten.getConfiguredFilters());
        assertEquals(List.of("alarms/#", "zone-b/#"), reordered.getConfiguredFilters());
        assertEquals(List.of("alarms/#", "zone-b/#"), asWritten.getFilters());
        assertEquals(asWritten, reordered);
        assertEquals(asWritten.hashCode(), reordered.hashCode());
        assertEquals(asWritten.calculateUniqueId(), reordered.calculateUniqueId());
    }

    @Test
    void configuredExcludes_areKeptInTheOrderTheyWereWritten() {
        final LocalSubscription subscription = new LocalSubscription(
                List.of("topicA/+"),
                "destinationTopic",
                List.of("zone-b/private/#", "alarms/private/#"),
                List.of(),
                false,
                1,
                null);

        assertEquals(List.of("zone-b/private/#", "alarms/private/#"), subscription.getConfiguredExcludes());
        assertEquals(List.of("alarms/private/#", "zone-b/private/#"), subscription.getExcludes());
    }

    /**
     * EDG-882 QA round 1: the order of the {@code <forwarded-topic>} blocks was compared positionally,
     * so moving one above another was a configuration change — and a configuration change restarts the
     * bridge and clears the queues of everything the new configuration cannot match.
     */
    @Test
    void mqttBridgeEquals_whenTheSubscriptionBlocksAreReordered_thenEqual() {
        final LocalSubscription first = subscription("topicA/+", "destinationTopic");
        final LocalSubscription second = subscription("topicB/+", "destinationTopic");
        final MqttBridge asWritten = bridge(List.of(first, second));
        final MqttBridge reordered = bridge(List.of(second, first));

        assertEquals(asWritten, reordered, "a reordered subscription list is the same configuration");
        assertEquals(asWritten.hashCode(), reordered.hashCode());
    }

    /**
     * EDG-882 QA round 4. {@code persist} was in {@code hashCode} and not in {@code equals}, which was
     * unobservable while a REST update was expressed as remove-then-add — every PUT restarted the bridge
     * whatever equality said. Once an update became one transition, this comparison is what decides
     * whether the bridge restarts, so toggling only this flag was silently ignored: the bridge kept
     * forwarding under the old setting, which is what decides whether a local subscription's publishes
     * are downgraded to QoS 0 and therefore whether they are persisted at all.
     */
    @Test
    void mqttBridgeEquals_whenOnlyPersistDiffers_thenNotEqual() {
        final LocalSubscription subscription = subscription("topicA/+", "destinationTopic");
        final MqttBridge persisting = bridgeWithPersist(subscription, true);
        final MqttBridge notPersisting = bridgeWithPersist(subscription, false);

        assertNotEquals(persisting, notPersisting, "toggling persist must be a configuration change");
        assertNotEquals(persisting.hashCode(), notPersisting.hashCode());
    }

    private static MqttBridge bridgeWithPersist(final LocalSubscription subscription, final boolean persist) {
        return new MqttBridge.Builder()
                .withId("edg-884-bridge")
                .withHost("localhost")
                .withPort(1883)
                .withClientId("client")
                .withLocalSubscriptions(List.of(subscription))
                .withRemoteSubscriptions(List.of())
                .persist(persist)
                .build();
    }

    /**
     * The same, for the other half of the comparison (EDG-882 review v02, R2-21).
     * <p>
     * {@code MqttBridge.equals} compares both subscription lists as multisets. Only the local half was
     * covered, so a change that made the remote half positional again would restart the bridge on a
     * reorder — and a restart is what clears the queues of everything the new configuration cannot
     * match. Note that this is about the order of the {@code <remote-subscription>} blocks; the filters
     * <em>inside</em> a remote subscription are still compared positionally, which is the open item in
     * the AUG-20 list.
     */
    @Test
    void mqttBridgeEquals_whenTheRemoteSubscriptionBlocksAreReordered_thenEqual() {
        final RemoteSubscription first = new RemoteSubscription(List.of("remoteA/+"), "destinationTopic");
        final RemoteSubscription second = new RemoteSubscription(List.of("remoteB/+"), "destinationTopic");
        final MqttBridge asWritten = bridgeWithRemote(List.of(first, second));
        final MqttBridge reordered = bridgeWithRemote(List.of(second, first));

        assertEquals(asWritten, reordered, "a reordered remote subscription list is the same configuration");
        assertEquals(asWritten.hashCode(), reordered.hashCode());
    }

    /** And a repeated remote block is a different configuration, exactly as for the local ones. */
    @Test
    void mqttBridgeEquals_whenARemoteSubscriptionIsRepeated_thenNotEqual() {
        final RemoteSubscription only = new RemoteSubscription(List.of("remoteA/+"), "destinationTopic");

        assertNotEquals(
                bridgeWithRemote(List.of(only)),
                bridgeWithRemote(List.of(only, new RemoteSubscription(List.of("remoteA/+"), "destinationTopic"))));
    }

    private static MqttBridge bridgeWithRemote(final List<RemoteSubscription> remoteSubscriptions) {
        return new MqttBridge.Builder()
                .withId("edg-884-bridge")
                .withHost("localhost")
                .withPort(1883)
                .withClientId("client")
                .withLocalSubscriptions(List.of())
                .withRemoteSubscriptions(remoteSubscriptions)
                .build();
    }

    /** But a repeated block is a different configuration, and a multiset keeps them apart. */
    @Test
    void mqttBridgeEquals_whenASubscriptionIsRepeated_thenNotEqual() {
        final LocalSubscription only = subscription("topicA/+", "destinationTopic");

        assertNotEquals(bridge(List.of(only)), bridge(List.of(only, subscription("topicA/+", "destinationTopic"))));
    }

    private static MqttBridge bridge(final List<LocalSubscription> subscriptions) {
        return new MqttBridge.Builder()
                .withId("edg-884-bridge")
                .withHost("localhost")
                .withPort(1883)
                .withClientId("client")
                .withLocalSubscriptions(subscriptions)
                .withRemoteSubscriptions(List.of())
                .build();
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
