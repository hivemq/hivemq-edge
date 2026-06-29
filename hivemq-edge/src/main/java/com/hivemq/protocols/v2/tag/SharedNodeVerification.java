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
package com.hivemq.protocols.v2.tag;

import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared verification bookkeeping for a single adapter's tags — <b>the single verification
 * authority</b> that both the connect-time adapter gate and the per-aspect re-verifications run
 * through. One node's verification serves all of that node's aspects: if both the read and write aspect of a tag
 * need to verify, exactly <b>one</b> {@code verifyBatch} entry is issued and the single {@link VerifyOutcome} is
 * fanned out to every aspect of the node.
 * <p>
 * It owns the de-duplication and the counting: a node already in flight is not re-requested
 * ({@link #needsVerify(Node)}), which is what keeps a read-and-write tag to a single verify, and
 * {@link #allReported()} is the signal the wrapper uses to leave {@code WAITING_FOR_VERIFICATION} for
 * {@code CONNECTED} once every node in the connect batch has reported some outcome.
 * <p>
 * Two entry points feed it: {@link #beginConnectVerification(List)} — the connect gate issues the aspects' nodes
 * as <b>one</b> batch — and {@link #requestVerification(Node)} — a single post-connect re-verification an aspect
 * asks for after a verification-retry or a tag retry. {@link #reset()} drops any outstanding
 * in-flight nodes when verification is abandoned (a disconnect or stop). Every reported outcome flows through
 * {@link #onVerifyResult(Node, VerifyOutcome)}, which both clears the count and fans out to the aspects.
 * <p>
 * Owned by one adapter wrapper and used only on its single dispatch thread; it holds no locks.
 */
public final class SharedNodeVerification {

    private final @NotNull NodeVerifier nodeVerifier;
    private final @NotNull Function<Node, @Nullable TagRuntime> tagRuntimeByNode;
    private final @NotNull Set<Node> inFlight = new HashSet<>();

    /**
     * @param nodeVerifier  the seam that verifies nodes against the device (the adapter's {@code verifyBatch}).
     * @param tagRuntimeByNode the current node-to-tag-runtime lookup; consulted on every result so it always
     *                         reflects the latest tag set after a tags-only transition.
     */
    public SharedNodeVerification(
            final @NotNull NodeVerifier nodeVerifier,
            final @NotNull Function<Node, @Nullable TagRuntime> tagRuntimeByNode) {
        this.nodeVerifier = nodeVerifier;
        this.tagRuntimeByNode = tagRuntimeByNode;
    }

    /**
     * @param node the node an aspect wants to verify.
     * @return {@code true} when the node is not already in flight — i.e. a fresh request is needed.
     */
    public boolean needsVerify(final @NotNull Node node) {
        return !inFlight.contains(node);
    }

    /**
     * Request a re-verification of the node, de-duplicated: if the node is already in flight (another aspect of
     * the same tag asked first, or a request is outstanding) nothing is issued, so the single result serves all
     * of the node's aspects.
     *
     * @param node the node to verify.
     */
    public void requestVerification(final @NotNull Node node) {
        if (needsVerify(node)) {
            inFlight.add(node);
            nodeVerifier.verify(List.of(node));
        }
    }

    /**
     * Begin the connect-time verification: register every given node as in flight (de-duplicated)
     * and issue them as <b>one</b> {@code verifyBatch} — a read-and-write tag therefore yields a single entry.
     * {@link #allReported()} then gates the {@code CONNECTED} transition. An empty list issues nothing and leaves
     * {@link #allReported()} {@code true}, so an adapter with no aspect needing verification connects immediately.
     *
     * @param nodes the nodes whose aspects need verifying on connect.
     */
    public void beginConnectVerification(final @NotNull List<Node> nodes) {
        final List<Node> toVerify = new ArrayList<>(nodes.size());
        for (final Node node : nodes) {
            if (inFlight.add(node)) {
                toVerify.add(node);
            }
        }
        if (!toVerify.isEmpty()) {
            nodeVerifier.verify(toVerify);
        }
    }

    /**
     * Drop every outstanding in-flight node — called when verification is abandoned (the adapter disconnected,
     * errored, or stopped), so a stale request can never linger into the next connect gate.
     */
    public void reset() {
        inFlight.clear();
    }

    /**
     * Fan one node's verification outcome out to its aspects and clear it from the in-flight set. A
     * result for a node this coordinator did not request — an initial connect verification issued by the adapter
     * gate — is fanned out all the same; the in-flight removal is then a no-op.
     *
     * @param node    the verified node.
     * @param outcome the verification outcome.
     */
    public void onVerifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
        inFlight.remove(node);
        final TagRuntime tagRuntime = tagRuntimeByNode.apply(node);
        if (tagRuntime != null) {
            tagRuntime.onVerifyResult(outcome);
        }
    }

    /**
     * @return {@code true} when no verification is outstanding — the signal the wrapper uses to leave
     *         {@code WAITING_FOR_VERIFICATION} for {@code CONNECTED} (the adapter gate).
     */
    public boolean allReported() {
        return inFlight.isEmpty();
    }
}
