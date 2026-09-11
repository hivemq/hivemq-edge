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
package com.hivemq.bootstrap.provider;

import static com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.local.memory.ClientQueueMemoryLocalPersistence;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * The cross-repository delivery contract of {@link ClientQueueLocalPersistence} (EDG-882 review v02,
 * R2-01; v01 F-04).
 * <p>
 * The file-native implementation ships in {@code hivemq-edge-mqtt-persistence} and is loaded from
 * {@code HIVEMQ_HOME/modules} at run time, so core and module are compiled separately and an operator
 * can pair any two — including by accident, by replacing the core zip and leaving {@code modules/}
 * alone. When {@code applyMaxToQos0} was added, the new signature was made abstract and the old one a
 * default; that is the direction that <b>guarantees</b> {@code AbstractMethodError} on the first queued
 * publish against an older module, because a default protects callers of the signature it replaces and
 * core has no callers of the old one left. What needs protecting is implementors.
 * <p>
 * {@link LegacyModulePersistence} below is a module built before the addition: it implements exactly
 * the method set such a module has, and nothing else. Every call in these tests goes through a variable
 * typed as the interface, so the dispatch is the same {@code invokeinterface} core performs.
 * <p>
 * <b>What this does not cover.</b> It proves method resolution, which is where the defect lives. It
 * does not boot a node against a released module jar, so it says nothing about module loading,
 * licensing or the classloader those go through.
 */
public class ClientQueueModuleCompatibilityTest {

    private static final @NotNull String QUEUE_ID = "$FORWARDER::bridge-1/plant/a";

    /**
     * A module compiled before {@code applyMaxToQos0} existed: it implements the seven-argument
     * {@code add} and has never heard of the eight-argument one.
     */
    private static class LegacyModulePersistence implements ClientQueueLocalPersistence {

        private final @NotNull List<String> calls = new ArrayList<>();

        @NotNull
        List<String> calls() {
            return calls;
        }

        @Override
        public void add(
                final @NotNull String queueId,
                final boolean shared,
                final @NotNull PUBLISH publish,
                final long max,
                final @NotNull QueuedMessagesStrategy strategy,
                final boolean retained,
                final int bucketIndex) {
            calls.add("single:" + queueId + ":" + max);
        }

        @Override
        public void add(
                final @NotNull String queueId,
                final boolean shared,
                final @NotNull List<PUBLISH> publishes,
                final long max,
                final @NotNull QueuedMessagesStrategy strategy,
                final boolean retained,
                final int bucketIndex) {
            calls.add("batch:" + queueId + ":" + publishes.size());
        }

        // Everything below is irrelevant to the contract under test; a module implements it, this does
        // not need to. Throwing rather than returning a plausible value keeps an accidental dependency
        // on one of them from passing silently.

        @Override
        public @NotNull ImmutableList<PUBLISH> readNew(
                final @NotNull String queueId,
                final boolean shared,
                final @NotNull ImmutableIntArray packetIds,
                final long bytesLimit,
                final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ImmutableList<PUBLISH> peek(
                final @NotNull String queueId,
                final boolean shared,
                final long bytesLimit,
                final int maxMessages,
                final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ImmutableList<MessageWithID> readInflight(
                final @NotNull String client,
                final boolean shared,
                final int batchSize,
                final long bytesLimit,
                final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable String replace(
                final @NotNull String client, final @NotNull PUBREL pubrel, final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable String remove(final @NotNull String client, final int packetId, final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable String remove(
                final @NotNull String client,
                final int packetId,
                final @Nullable String uniqueId,
                final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size(final @NotNull String queueId, final boolean shared, final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear(final @NotNull String queueId, final boolean shared, final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAllQos0Messages(final @NotNull String queueId, final boolean shared, final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ImmutableSet<String> cleanUp(final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeShared(
                final @NotNull String sharedSubscription, final @NotNull String uniqueId, final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeInFlightMarker(
                final @NotNull String queueId, final @NotNull String uniqueId, final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAllInFlightMarkers(final @NotNull String queueId, final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void closeDB(final int bucketIndex) {
            throw new UnsupportedOperationException();
        }
    }

    /** A module built against the current contract: it answers the call core actually makes. */
    private static class CurrentModulePersistence extends LegacyModulePersistence {

        @Override
        public void add(
                final @NotNull String queueId,
                final boolean shared,
                final @NotNull PUBLISH publish,
                final long max,
                final @NotNull QueuedMessagesStrategy strategy,
                final boolean retained,
                final boolean applyMaxToQos0,
                final int bucketIndex) {
            calls().add("single-with-policy:" + queueId + ":" + applyMaxToQos0);
        }

        @Override
        public void add(
                final @NotNull String queueId,
                final boolean shared,
                final @NotNull List<PUBLISH> publishes,
                final long max,
                final @NotNull QueuedMessagesStrategy strategy,
                final boolean retained,
                final boolean applyMaxToQos0,
                final int bucketIndex) {
            calls().add("batch-with-policy:" + queueId + ":" + applyMaxToQos0);
        }
    }

    /**
     * A module that took half the contract: it overrides the single-publish form and leaves the batch
     * form on the interface default. Nothing stops one being built — the two overloads carry separate
     * defaults — and the batch form is the one the poll path uses, so a probe that looked only at the
     * single-publish form would call this module current and say nothing.
     */
    private static class HalfMigratedModulePersistence extends LegacyModulePersistence {

        @Override
        public void add(
                final @NotNull String queueId,
                final boolean shared,
                final @NotNull PUBLISH publish,
                final long max,
                final @NotNull QueuedMessagesStrategy strategy,
                final boolean retained,
                final boolean applyMaxToQos0,
                final int bucketIndex) {
            calls().add("single-with-policy:" + queueId + ":" + applyMaxToQos0);
        }
    }

    private static @NotNull PUBLISH publish() {
        return new PUBLISHFactory.Mqtt5Builder()
                .withTopic("plant/a")
                .withQoS(QoS.AT_MOST_ONCE)
                .withOnwardQos(QoS.AT_MOST_ONCE)
                .withPayload("payload".getBytes())
                .withHivemqId("hivemqId")
                .build();
    }

    /**
     * The whole point. This call is the one {@code ClientQueuePersistenceImpl} makes on every queued
     * publish; against a module that predates the parameter it must land on the historical method
     * rather than throw {@link AbstractMethodError}. On a file-native node that error is the entire
     * message path, with a stack trace that names no version.
     */
    @Test
    @Timeout(5)
    public void test_a_module_predating_the_queue_policy_answers_the_call_core_makes() {
        final LegacyModulePersistence legacy = new LegacyModulePersistence();
        final ClientQueueLocalPersistence persistence = legacy;

        persistence.add(QUEUE_ID, true, publish(), 10, QueuedMessagesStrategy.DISCARD_OLDEST, false, true, 0);
        persistence.add(
                QUEUE_ID,
                true,
                List.of(publish(), publish()),
                10,
                QueuedMessagesStrategy.DISCARD_OLDEST,
                false,
                true,
                0);

        assertEquals(List.of("single:" + QUEUE_ID + ":10", "batch:" + QUEUE_ID + ":2"), legacy.calls());
    }

    /** And a module that does implement it is called directly, with the flag intact. */
    @Test
    @Timeout(5)
    public void test_a_current_module_receives_the_queue_policy_flag() {
        final CurrentModulePersistence current = new CurrentModulePersistence();
        final ClientQueueLocalPersistence persistence = current;

        persistence.add(QUEUE_ID, true, publish(), 10, QueuedMessagesStrategy.DISCARD_OLDEST, false, true, 0);
        persistence.add(QUEUE_ID, true, List.of(publish()), 10, QueuedMessagesStrategy.DISCARD_OLDEST, false, false, 0);

        assertEquals(
                List.of("single-with-policy:" + QUEUE_ID + ":true", "batch-with-policy:" + QUEUE_ID + ":false"),
                current.calls());
    }

    /**
     * The degradation above is safe but silent, so the provider says it out loud once at start-up. This
     * pins the predicate that decides whether it does.
     */
    @Test
    @Timeout(5)
    public void test_the_probe_tells_a_stale_module_from_a_current_one() {
        assertFalse(ClientQueueLocalPersistenceProvider.implementsQueuePolicyAdd(LegacyModulePersistence.class));
        assertTrue(ClientQueueLocalPersistenceProvider.implementsQueuePolicyAdd(CurrentModulePersistence.class));
    }

    /**
     * Both overloads are the contract, so overriding one of them is not it. The probe reports on "the
     * queue-policy contract", and answering that from the single-publish form alone would call a module
     * current on the strength of the half that was migrated (EDG-882 QA, 2026-08-25).
     */
    @Test
    @Timeout(5)
    public void test_the_probe_is_not_satisfied_by_half_the_contract() {
        assertFalse(ClientQueueLocalPersistenceProvider.implementsQueuePolicyAdd(HalfMigratedModulePersistence.class));
    }

    /**
     * And the implementation that ships inside core must never be the one the probe warns about — it is
     * compiled against this very interface, so a warning here would mean the override was dropped.
     */
    @Test
    @Timeout(5)
    public void test_the_in_memory_persistence_carries_the_current_contract() {
        assertTrue(
                ClientQueueLocalPersistenceProvider.implementsQueuePolicyAdd(ClientQueueMemoryLocalPersistence.class));
    }
}
