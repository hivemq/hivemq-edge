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
package com.hivemq.bridge.mqtt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.bridge.MqttForwarder;
import com.hivemq.bridge.config.CustomUserProperty;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * EDG-882 F-01. A bridge whose local subscriptions do not resolve to distinct forwarder ids must not
 * start.
 * <p>
 * The id is a digest over the sorted topic filters joined with an <b>empty</b> separator and the
 * destination, so {@code ["ab","c"]} and {@code ["a","bc"]} produce the same one. It names every
 * persisted queue the subscription owns, so two subscriptions sharing an id share their queues: the
 * second registration takes the first's queues out of the ownership index, and the periodic clean-up
 * deletes what it finds unowned. The id cannot be disambiguated without renaming every persisted
 * queue on upgrade, so the configuration is rejected instead — loudly, naming both subscriptions,
 * with nothing cleared.
 */
class BridgeMqttClientForwarderIdTest {

    private static final @NotNull String BRIDGE_ID = "plant-bridge";

    private static @NotNull LocalSubscription subscription(
            final @NotNull List<String> filters, final @Nullable String destination) {
        return new LocalSubscription(filters, destination);
    }

    private static @NotNull MqttBridge bridge(final @NotNull List<LocalSubscription> localSubscriptions) {
        return new MqttBridge.Builder()
                .withId(BRIDGE_ID)
                .withHost("remote.example.com")
                .withPort(1883)
                .withClientId(BRIDGE_ID + "-client")
                .withLocalSubscriptions(localSubscriptions)
                .withRemoteSubscriptions(List.of())
                .build();
    }

    private static @NotNull BridgeMqttClient client(final @NotNull MqttBridge bridge) {
        return new BridgeMqttClient(
                mock(SystemInformation.class),
                bridge,
                mock(BridgeInterceptorHandler.class),
                new HivemqId(),
                new MetricRegistry(),
                mock(EventService.class));
    }

    /** The customer-shaped case: distinct subscriptions, one of them hashing to an id with a '/'. */
    @Test
    void verifyForwarderIdsAreUnique_whenSubscriptionsAreDistinct_thenAccepted() {
        final MqttBridge bridge = bridge(List.of(
                subscription(List.of("miele/v1/production/sapdm/dev/+/+/from-plc-to-dm"), "remote/from-plc"),
                subscription(List.of("plant/+/pressure", "plant/+/temperature"), "remote/readings"),
                subscription(List.of("plant/+/alarms"), "remote/alarms")));

        assertDoesNotThrow(() -> BridgeMqttClient.verifyForwarderIdsAreUnique(bridge));
        assertEquals(3, client(bridge).createForwarders().size());
    }

    /**
     * The collision itself. Both subscriptions are live at once — a bridge builds one forwarder per
     * local subscription — so this is not a re-registration, and neither of the two can be dropped in
     * favour of the other.
     */
    @Test
    void verifyForwarderIdsAreUnique_whenFilterConcatenationsCollide_thenRejected() {
        final LocalSubscription first = subscription(List.of("ab", "c"), "remote/dest");
        final LocalSubscription second = subscription(List.of("a", "bc"), "remote/dest");
        assertEquals(
                first.calculateUniqueId(),
                second.calculateUniqueId(),
                "the premise of this test: the two subscriptions collide");

        final IllegalStateException rejected = assertThrows(
                IllegalStateException.class,
                () -> BridgeMqttClient.verifyForwarderIdsAreUnique(bridge(List.of(first, second))));

        // the operator has to be able to find both offending subscriptions from the message alone
        final String message = rejected.getMessage();
        assertTrue(message.contains(BRIDGE_ID), message);
        assertTrue(message.contains("[ab, c]"), message);
        assertTrue(message.contains("[a, bc]"), message);
        assertTrue(message.contains(BRIDGE_ID + "-" + first.calculateUniqueId()), message);
    }

    /** The rejection has to happen before any forwarder is built, registered or started. */
    @Test
    void createForwarders_whenFilterConcatenationsCollide_thenRejectedAndNothingIsCreated() {
        final BridgeMqttClient client = client(bridge(List.of(
                subscription(List.of("ab", "c"), "remote/dest"), subscription(List.of("a", "bc"), "remote/dest"))));

        assertThrows(IllegalStateException.class, client::createForwarders);
        assertEquals(List.of(), client.getActiveForwarders(), "a rejected bridge must hold no forwarders");
    }

    /**
     * A subscription duplicated in the configuration. The two own exactly the same queues, so today
     * both forwarders poll them and every message is forwarded twice; removing either stops both.
     * Rejecting is the same answer as for any other collision, and the fix is to delete one block.
     */
    @Test
    void verifyForwarderIdsAreUnique_whenASubscriptionIsDuplicated_thenRejected() {
        final LocalSubscription sub = subscription(List.of("plant/+/temperature"), "remote/readings");

        assertThrows(
                IllegalStateException.class,
                () -> BridgeMqttClient.verifyForwarderIdsAreUnique(
                        bridge(List.of(sub, subscription(List.of("plant/+/temperature"), "remote/readings")))));
    }

    /**
     * The id sorts the filters before digesting them, so a subscription repeated with its filters in
     * another order is the same subscription as far as queue ownership is concerned — and collides.
     * (It compares unequal under {@code LocalSubscription.equals}, which is EDG-884's remaining
     * order-sensitivity, tracked as F-07; it does not change the answer here.)
     */
    @Test
    void verifyForwarderIdsAreUnique_whenFiltersAreOnlyReordered_thenRejected() {
        assertThrows(
                IllegalStateException.class,
                () -> BridgeMqttClient.verifyForwarderIdsAreUnique(bridge(List.of(
                        subscription(List.of("plant/+/pressure", "plant/+/temperature"), "remote/readings"),
                        subscription(List.of("plant/+/temperature", "plant/+/pressure"), "remote/readings")))));
    }

    /**
     * The destination is part of the digest, so filter sets that would otherwise collide do not when
     * they forward somewhere else. Rejecting these too would fail configurations that are perfectly
     * unambiguous.
     */
    @Test
    void verifyForwarderIdsAreUnique_whenCollidingFiltersHaveDifferentDestinations_thenAccepted() {
        assertDoesNotThrow(() -> BridgeMqttClient.verifyForwarderIdsAreUnique(bridge(List.of(
                subscription(List.of("ab", "c"), "remote/one"), subscription(List.of("a", "bc"), "remote/two")))));
    }

    /**
     * Subscriptions that differ only in a field outside the digest — excludes, user properties, QoS,
     * retain handling, queue limit — still collide, because the id is what names the queues. They
     * would forward the same messages under two different rule sets from one set of queues.
     */
    @Test
    void verifyForwarderIdsAreUnique_whenOnlyNonIdentityFieldsDiffer_thenRejected() {
        final List<String> filters = List.of("plant/+/temperature");
        assertThrows(
                IllegalStateException.class,
                () -> BridgeMqttClient.verifyForwarderIdsAreUnique(bridge(List.of(
                        new LocalSubscription(filters, "remote/readings", List.of(), List.of(), false, 1, null),
                        new LocalSubscription(
                                filters,
                                "remote/readings",
                                List.of("plant/ignored/temperature"),
                                List.of(CustomUserProperty.of("k", "v")),
                                true,
                                2,
                                1000L)))));
    }

    /** A null destination is a legal configuration and must not collide with a named one. */
    @Test
    void verifyForwarderIdsAreUnique_whenDestinationIsNull_thenTreatedAsAnyOther() {
        assertDoesNotThrow(() -> BridgeMqttClient.verifyForwarderIdsAreUnique(bridge(List.of(
                subscription(List.of("plant/+/temperature"), null),
                subscription(List.of("plant/+/temperature"), "remote/readings")))));

        assertThrows(
                IllegalStateException.class,
                () -> BridgeMqttClient.verifyForwarderIdsAreUnique(bridge(List.of(
                        subscription(List.of("plant/+/temperature"), null),
                        subscription(List.of("plant/+/temperature"), null)))));
    }

    /**
     * A subscription without filters digests the destination alone, so two of them collide. Degenerate
     * but reachable through the API, and it must not slip through for want of anything to hash.
     */
    @Test
    void verifyForwarderIdsAreUnique_whenSubscriptionsHaveNoFilters_thenStillChecked() {
        assertDoesNotThrow(() -> BridgeMqttClient.verifyForwarderIdsAreUnique(
                bridge(List.of(subscription(List.of(), "remote/one"), subscription(List.of(), "remote/two")))));

        assertThrows(
                IllegalStateException.class,
                () -> BridgeMqttClient.verifyForwarderIdsAreUnique(
                        bridge(List.of(subscription(List.of(), "remote/one"), subscription(List.of(), "remote/one")))));
    }

    /** Nothing to compare: the empty and single-subscription cases must not be made to fail. */
    @Test
    void verifyForwarderIdsAreUnique_whenFewerThanTwoSubscriptions_thenAccepted() {
        assertDoesNotThrow(() -> BridgeMqttClient.verifyForwarderIdsAreUnique(bridge(List.of())));
        assertDoesNotThrow(() -> BridgeMqttClient.verifyForwarderIdsAreUnique(
                bridge(List.of(subscription(List.of("plant/+/temperature"), "remote/readings")))));
    }

    /**
     * Uniqueness is required within a bridge, not across bridges. The bridge id prefixes the digest,
     * so the same subscription on two bridges owns two distinct sets of queues; failing that
     * configuration would break every deployment that fans one topic out to two remotes.
     */
    @Test
    void verifyForwarderIdsAreUnique_whenTheSameSubscriptionIsOnTwoBridges_thenAccepted() {
        final LocalSubscription sub = subscription(List.of("plant/+/temperature"), "remote/readings");
        final MqttBridge other = new MqttBridge.Builder()
                .withId("other-bridge")
                .withHost("other.example.com")
                .withPort(1883)
                .withClientId("other-bridge-client")
                .withLocalSubscriptions(List.of(sub))
                .withRemoteSubscriptions(List.of())
                .build();

        assertDoesNotThrow(() -> BridgeMqttClient.verifyForwarderIdsAreUnique(bridge(List.of(sub))));
        assertDoesNotThrow(() -> BridgeMqttClient.verifyForwarderIdsAreUnique(other));
        assertEquals(
                1,
                client(other).createForwarders().stream()
                        .map(MqttForwarder::getId)
                        .filter(id -> id.startsWith("other-bridge-"))
                        .count());
    }

    /**
     * The three subscriptions collide pairwise; the first two are enough to reject, and the message
     * must name the pair it found rather than a summary the operator cannot act on.
     */
    @Test
    void verifyForwarderIdsAreUnique_whenMoreThanTwoCollide_thenRejectedNamingAPair() {
        final IllegalStateException rejected = assertThrows(
                IllegalStateException.class,
                () -> BridgeMqttClient.verifyForwarderIdsAreUnique(bridge(List.of(
                        subscription(List.of("ab", "c"), "remote/dest"),
                        subscription(List.of("a", "bc"), "remote/dest"),
                        subscription(List.of("abc"), "remote/dest")))));

        assertTrue(rejected.getMessage().contains("[ab, c]"), rejected.getMessage());
        assertTrue(rejected.getMessage().contains("[a, bc]"), rejected.getMessage());
    }
}
