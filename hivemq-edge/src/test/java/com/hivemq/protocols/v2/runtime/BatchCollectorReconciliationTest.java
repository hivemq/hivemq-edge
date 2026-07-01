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
package com.hivemq.protocols.v2.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The per-tick batch collector: same-tick subscription requests are reconciled per node (last request wins) so a
 * node appears in at most one of the add/remove batches; poll and write batches keep duplicates in order; a dispatch
 * sends the non-empty batches in the fixed order remove, add, poll, write, then clears them; and a throwing adapter
 * command still clears every batch so it is never re-dispatched on the next tick.
 */
class BatchCollectorReconciliationTest {

    @Test
    void subscribeThenCancelSameTick_netsToASingleRemove() {
        final BatchCollector collector = new BatchCollector();
        final RecordingProtocolAdapter adapter = new RecordingProtocolAdapter();
        final Node node = new TestNode("n1");

        collector.addSubscription(node);
        collector.removeSubscription(node);
        collector.dispatch(adapter);

        assertThat(adapter.added).isEmpty();
        assertThat(adapter.removed).containsExactly(List.of(node));
    }

    @Test
    void cancelThenResubscribeSameTick_netsToASingleAdd() {
        final BatchCollector collector = new BatchCollector();
        final RecordingProtocolAdapter adapter = new RecordingProtocolAdapter();
        final Node node = new TestNode("n1");

        collector.removeSubscription(node);
        collector.addSubscription(node);
        collector.dispatch(adapter);

        assertThat(adapter.removed).isEmpty();
        assertThat(adapter.added).containsExactly(List.of(node));
    }

    @Test
    void eachNodeAppearsInAtMostOneSubscriptionBatch() {
        final BatchCollector collector = new BatchCollector();
        final RecordingProtocolAdapter adapter = new RecordingProtocolAdapter();
        final Node node1 = new TestNode("n1");
        final Node node2 = new TestNode("n2");
        final Node node3 = new TestNode("n3");

        collector.addSubscription(node1);
        collector.removeSubscription(node2);
        collector.removeSubscription(node1); // node1's last request wins: remove
        collector.addSubscription(node3);
        collector.dispatch(adapter);

        assertThat(adapter.removed).containsExactly(List.of(node1, node2));
        assertThat(adapter.added).containsExactly(List.of(node3));
    }

    @Test
    void pollAndWriteDuplicatesArePreservedInOrder() {
        final BatchCollector collector = new BatchCollector();
        final RecordingProtocolAdapter adapter = new RecordingProtocolAdapter();
        final Node node1 = new TestNode("n1");
        final Node node2 = new TestNode("n2");
        final WriteEntry write1 = new WriteEntry(node1, new TestDataPoint("a"));
        final WriteEntry write2 = new WriteEntry(node2, new TestDataPoint("b"));

        collector.poll(node1);
        collector.poll(node2);
        collector.poll(node1); // duplicate kept
        collector.write(write1);
        collector.write(write2);
        collector.write(write1); // duplicate kept
        collector.dispatch(adapter);

        assertThat(adapter.polled).containsExactly(List.of(node1, node2, node1));
        assertThat(adapter.written).containsExactly(List.of(write1, write2, write1));
    }

    @Test
    void dispatchSendsBatchesInTheFixedRemoveAddPollWriteOrder() {
        final BatchCollector collector = new BatchCollector();
        final RecordingProtocolAdapter adapter = new RecordingProtocolAdapter();

        collector.poll(new TestNode("p"));
        collector.write(new WriteEntry(new TestNode("w"), new TestDataPoint("v")));
        collector.addSubscription(new TestNode("a"));
        collector.removeSubscription(new TestNode("r"));
        collector.dispatch(adapter);

        assertThat(adapter.callOrder).containsExactly("remove", "add", "poll", "write");
    }

    @Test
    void emptyBatchesAreNotSent_andStateIsClearedBetweenDispatches() {
        final BatchCollector collector = new BatchCollector();
        final RecordingProtocolAdapter adapter = new RecordingProtocolAdapter();
        final Node node = new TestNode("n1");

        collector.dispatch(adapter); // nothing posted
        assertThat(adapter.callOrder).isEmpty();

        collector.poll(node);
        collector.dispatch(adapter);
        collector.dispatch(adapter); // batches cleared by the first dispatch

        assertThat(adapter.polled).containsExactly(List.of(node));
    }

    @Test
    void dispatch_clearsEveryBatch_evenWhenAnAdapterCommandThrows() {
        final BatchCollector collector = new BatchCollector();
        final Node node = new TestNode("n1");
        collector.addSubscription(node);
        collector.poll(node);
        collector.write(new WriteEntry(node, new TestDataPoint("v")));

        // The adapter throws on the first batch command: the exception propagates to the wrapper's dispatch loop (its
        // fence), but the collector must still have cleared every batch.
        assertThatThrownBy(() -> collector.dispatch(new ThrowingProtocolAdapter()))
                .isInstanceOf(IllegalStateException.class);

        // A second dispatch to a healthy adapter sends nothing: no batch survived the throw to be re-dispatched.
        final RecordingProtocolAdapter recording = new RecordingProtocolAdapter();
        collector.dispatch(recording);
        assertThat(recording.callOrder).isEmpty();
    }

    private static final class TestNode extends Node {

        private final @NotNull String identifier;

        private TestNode(final @NotNull String identifier) {
            this.identifier = identifier;
        }

        @Override
        public @NotNull String nodeId() {
            return identifier;
        }

        @Override
        public @NotNull String nodeString() {
            return "{\"identifier\":\"" + identifier + "\"}";
        }

        @Override
        public @NotNull EnumSet<NodeProperty> properties() {
            return EnumSet.of(NodeProperty.UNIQUE);
        }

        @Override
        public @NotNull String toString() {
            return identifier;
        }
    }

    private record TestDataPoint(@NotNull String value) implements DataPoint {
        @Override
        public @NotNull Object getTagValue() {
            return value;
        }

        @Override
        public boolean treatTagValueAsJson() {
            return false;
        }

        @Override
        public @NotNull String getTagName() {
            return "tag";
        }
    }

    private static final class RecordingProtocolAdapter implements ProtocolAdapter {

        private final @NotNull List<String> callOrder = new ArrayList<>();
        private final @NotNull List<List<Node>> removed = new ArrayList<>();
        private final @NotNull List<List<Node>> added = new ArrayList<>();
        private final @NotNull List<List<Node>> polled = new ArrayList<>();
        private final @NotNull List<List<WriteEntry>> written = new ArrayList<>();

        @Override
        public @NotNull String adapterId() {
            return "recording";
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        public void connect() {}

        @Override
        public void disconnect() {}

        @Override
        public void verifyBatch(final @NotNull List<Node> nodes) {}

        @Override
        public void pollBatch(final @NotNull List<Node> nodes) {
            callOrder.add("poll");
            polled.add(nodes);
        }

        @Override
        public void addSubscriptionBatch(final @NotNull List<Node> nodes) {
            callOrder.add("add");
            added.add(nodes);
        }

        @Override
        public void removeSubscriptionBatch(final @NotNull List<Node> nodes) {
            callOrder.add("remove");
            removed.add(nodes);
        }

        @Override
        public void writeBatch(final @NotNull List<WriteEntry> entries) {
            callOrder.add("write");
            written.add(entries);
        }

        @Override
        public void browse(final int requestId, final @NotNull BrowseFilter filter, final int maxReferences) {}

        @Override
        public void browseNext(final int requestId, final @NotNull BrowseContinuation continuation) {}

        @Override
        public void readNodeAttributes(final int requestId, final @NotNull List<Node> nodes) {}
    }

    /**
     * A protocol-adapter double whose every batch command throws — the misbehaving adapter the collector must clear
     * its batches around.
     */
    private static final class ThrowingProtocolAdapter implements ProtocolAdapter {

        @Override
        public @NotNull String adapterId() {
            return "throwing";
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        public void connect() {}

        @Override
        public void disconnect() {}

        @Override
        public void verifyBatch(final @NotNull List<Node> nodes) {}

        @Override
        public void pollBatch(final @NotNull List<Node> nodes) {
            throw new IllegalStateException("poll blew up");
        }

        @Override
        public void addSubscriptionBatch(final @NotNull List<Node> nodes) {
            throw new IllegalStateException("subscribe blew up");
        }

        @Override
        public void removeSubscriptionBatch(final @NotNull List<Node> nodes) {
            throw new IllegalStateException("unsubscribe blew up");
        }

        @Override
        public void writeBatch(final @NotNull List<WriteEntry> entries) {
            throw new IllegalStateException("write blew up");
        }

        @Override
        public void browse(final @NotNull BrowseFilter filter) {}
    }
}
