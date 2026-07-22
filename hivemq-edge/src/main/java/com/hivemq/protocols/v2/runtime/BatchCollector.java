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

import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * The per-tick batch collector. Tag aspects post requests during a tick by appending to one of
 * four pending batches; after the timer drain the tick handler calls {@link #dispatch(ProtocolAdapter)}, which
 * sends each non-empty batch as one command to the adapter in a fixed order and then clears.
 * <p>
 * Poll and write batches are append-only lists — duplicates are kept and the adapter executes them in order.
 * Subscription requests are <b>reconciled per node</b>: {@code addSubscription} then {@code removeSubscription} for
 * one node in the same tick nets to a single remove; {@code removeSubscription} then {@code addSubscription} is a
 * <b>power cycle</b> and dispatches both — the fixed cross-type dispatch order ({@code removeSubscriptionBatch},
 * {@code addSubscriptionBatch}, {@code pollBatch}, {@code writeBatch}) delivers the remove before the add, so the
 * cancel-then-resubscribe sequence (EDG-824 #16) survives same-tick reconciliation. Only orderings the fixed
 * dispatch order cannot express are netted.
 * <p>
 * Owned by one actor and used only on its dispatch thread; it holds no locks.
 */
public final class BatchCollector {

    private enum SubscriptionOperation {
        ADD,
        REMOVE,
        /** A same-tick cancel-then-resubscribe: the node goes into BOTH batches, remove dispatched first. */
        REMOVE_THEN_ADD
    }

    private final @NotNull List<Node> pollBatch = new ArrayList<>();
    private final @NotNull List<WriteEntry> writeBatch = new ArrayList<>();
    private final @NotNull Map<Node, SubscriptionOperation> subscriptionOperations = new LinkedHashMap<>();

    /**
     * Append a node to the poll batch. Duplicates are kept and polled in order.
     *
     * @param node the node to poll.
     */
    public void poll(final @NotNull Node node) {
        pollBatch.add(node);
    }

    /**
     * Append a write to the write batch. Duplicates are kept and written in order.
     *
     * @param entry the node/value pair to write.
     */
    public void write(final @NotNull WriteEntry entry) {
        writeBatch.add(entry);
    }

    /**
     * Request a subscription for a node. Reconciled per node: an add after a pending remove becomes a
     * power cycle (remove dispatched first, then add); an add after an add stays a single add.
     *
     * @param node the node to subscribe to.
     */
    public void addSubscription(final @NotNull Node node) {
        subscriptionOperations.merge(
                node,
                SubscriptionOperation.ADD,
                (pending, add) -> pending == SubscriptionOperation.ADD
                        ? SubscriptionOperation.ADD
                        : SubscriptionOperation.REMOVE_THEN_ADD);
    }

    /**
     * Request a subscription removal for a node. Reconciled per node: a remove supersedes any pending add
     * (the fixed dispatch order cannot express add-then-remove, so it nets to the remove) and collapses a pending
     * power cycle back to a plain remove.
     *
     * @param node the node to unsubscribe from.
     */
    public void removeSubscription(final @NotNull Node node) {
        subscriptionOperations.put(node, SubscriptionOperation.REMOVE);
    }

    /**
     * Send each non-empty batch to the adapter in the fixed order remove, add, poll, write, then clear all
     * four batches. Empty batches are not sent.
     *
     * @param protocolAdapter the adapter to dispatch the batches to.
     */
    public void dispatch(final @NotNull ProtocolAdapter protocolAdapter) {
        final List<Node> toRemove = new ArrayList<>();
        final List<Node> toAdd = new ArrayList<>();
        for (final Map.Entry<Node, SubscriptionOperation> operation : subscriptionOperations.entrySet()) {
            switch (operation.getValue()) {
                case REMOVE -> toRemove.add(operation.getKey());
                case ADD -> toAdd.add(operation.getKey());
                case REMOVE_THEN_ADD -> {
                    toRemove.add(operation.getKey());
                    toAdd.add(operation.getKey());
                }
            }
        }
        // Clear in finally: a batch is delivered at most once. If the adapter throws mid-dispatch the remaining
        // batches are dropped with it — never redelivered on the next tick to an adapter that was just stopped
        // (that redelivery caused a per-tick stop/error storm; QA round on EDG-824 #7).
        try {
            if (!toRemove.isEmpty()) {
                protocolAdapter.removeSubscriptionBatch(toRemove);
            }
            if (!toAdd.isEmpty()) {
                protocolAdapter.addSubscriptionBatch(toAdd);
            }
            if (!pollBatch.isEmpty()) {
                protocolAdapter.pollBatch(new ArrayList<>(pollBatch));
            }
            if (!writeBatch.isEmpty()) {
                protocolAdapter.writeBatch(new ArrayList<>(writeBatch));
            }
        } finally {
            subscriptionOperations.clear();
            pollBatch.clear();
            writeBatch.clear();
        }
    }
}
