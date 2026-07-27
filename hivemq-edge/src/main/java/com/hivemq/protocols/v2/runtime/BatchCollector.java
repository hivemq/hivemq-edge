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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * The per-tick batch collector. Tag aspects post requests during a tick by appending to one of
 * five pending batches; after the timer drain the tick handler calls {@link #dispatch(ProtocolAdapter)}, which
 * sends each non-empty batch as one command to the adapter in a fixed order and then clears.
 * <p>
 * Poll and write batches are append-only lists — duplicates are kept and the adapter executes them in order.
 * Subscription requests are <b>reconciled per node</b>: {@code addSubscription} then {@code removeSubscription} for
 * one node in the same tick nets to a single remove; {@code removeSubscription} then {@code addSubscription} is a
 * <b>power cycle</b> and dispatches both — the fixed cross-type dispatch order ({@code removeSubscriptionBatch},
 * {@code verifyBatch}, {@code addSubscriptionBatch}, {@code pollBatch}, {@code writeBatch}) delivers the remove before
 * the add, so the cancel-then-resubscribe sequence (EDG-824 #16) survives same-tick reconciliation. Only orderings the
 * fixed dispatch order cannot express are netted.
 * <p>
 * The spontaneous-loss power cycle's re-verification is posted through {@link #verify(Node)} rather than issued
 * eagerly, so its {@code verifyBatch} dispatches after the same tick's {@code removeSubscriptionBatch} — the adapter
 * observes {@code cancel → verify → re-subscribe}, never a verify ahead of the cancel (EDG-824 #16). The connect-gate
 * and retry verifications stay eager and do not pass through here.
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
    private final @NotNull Set<Node> verifyBatch = new LinkedHashSet<>();
    private final @NotNull Map<Node, SubscriptionOperation> subscriptionOperations = new LinkedHashMap<>();

    private long writeDispatches;

    /**
     * @return how many write batches have been handed to the adapter. A write aspect samples this when it posts a
     *         write and compares it when a result arrives: while the count is unchanged the write is still sitting
     *         in the batch, so the result cannot be its own and must be a duplicate of an earlier one. The adapter
     *         SDK's write result carries no correlation of its own, and acting on a misattributed one deletes a
     *         durable command the device was never asked to execute.
     */
    public long writeDispatches() {
        return writeDispatches;
    }

    /**
     * Retract any not-yet-dispatched write for one node — the write was abandoned before the adapter ever saw it.
     * <p>
     * A write lives in this batch for up to a tick before {@link #dispatch} hands it over, and plenty can happen in
     * that window: the tag is deactivated, the connection drops, the write times out, or a reload re-points the tag
     * at a different node. Every one of those reports the write {@code ABORTED}, so its command is kept and
     * redelivered — but without this the entry would still be dispatched afterwards, writing to the device a value
     * that had already been abandoned, and in the reload case writing it to a node the configuration no longer
     * maps. The device's own acknowledgment then arrives for a write nobody is tracking and is dropped in silence.
     * <p>
     * At-least-once permits duplicates, so this is not a correctness fix so much as an honesty one: an abandoned
     * write should not reach the device, and a retargeted tag should not write to where it used to point.
     *
     * @param node the node whose pending write is retracted.
     * @return whether an entry was actually removed.
     */
    public boolean retractWrite(final @NotNull Node node) {
        return writeBatch.removeIf(entry -> entry.node() == node);
    }

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
     * Post a node for re-verification, de-duplicated per node. Unlike the eager connect-gate and retry
     * verifications, a re-verification posted here is dispatched by {@link #dispatch(ProtocolAdapter)} after that
     * tick's {@code removeSubscriptionBatch}, so the spontaneous-loss power cycle (EDG-824 #16) issues its
     * {@code verifyBatch} <b>after</b> the cancel — never before it. The caller keeps the in-flight bookkeeping and
     * gating (see {@code SharedNodeVerification#beginDeferredVerification}); this only orders the issue.
     *
     * @param node the node to re-verify.
     */
    public void verify(final @NotNull Node node) {
        verifyBatch.add(node);
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
     * Send each non-empty batch to the adapter in the fixed order remove, verify, add, poll, write, then clear all
     * five batches. Empty batches are not sent.
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
            // Between remove and add: a spontaneous-loss re-verify (EDG-824 #16) issues here, after the cancel.
            if (!verifyBatch.isEmpty()) {
                protocolAdapter.verifyBatch(new ArrayList<>(verifyBatch));
            }
            if (!toAdd.isEmpty()) {
                protocolAdapter.addSubscriptionBatch(toAdd);
            }
            if (!pollBatch.isEmpty()) {
                protocolAdapter.pollBatch(new ArrayList<>(pollBatch));
            }
            if (!writeBatch.isEmpty()) {
                // Counted BEFORE the call, so a result the adapter reports synchronously from inside writeBatch
                // still sees the dispatch that carried it. See writeDispatches().
                writeDispatches++;
                protocolAdapter.writeBatch(new ArrayList<>(writeBatch));
            }
        } finally {
            subscriptionOperations.clear();
            verifyBatch.clear();
            pollBatch.clear();
            writeBatch.clear();
        }
    }
}
