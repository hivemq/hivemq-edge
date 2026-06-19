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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared verification bookkeeping for a single adapter's tags (design §7.6). One node's verification serves all of
 * that node's aspects: if both the read and write aspect of a tag need to verify, the coordinator issues
 * <b>one</b> {@code verifyBatch} entry and fans the single {@link VerifyOutcome} out to every aspect of the node.
 * <p>
 * In this task only read aspects exist, so the fan-out reaches the read aspect; the write aspect joins in a later
 * task with no change here. The coordinator owns the de-duplication: a node already in flight is not re-requested
 * ({@link #needsVerify(Node)}), which is what keeps a read-and-write tag to a single verify.
 * <p>
 * The initial connect verification is issued by the wrapper's adapter gate (design §6.3), which also counts for
 * the {@code CONNECTED} transition; those results flow through {@link #onVerifyResult(Node, VerifyOutcome)} for
 * fan-out but are not tracked here. This coordinator issues only the post-connect re-verifications an aspect asks
 * for — a verification retry or a tag retry (design §7.6). {@link #allReported()} reports whether the
 * coordinator-issued requests have all come back; a later task wires it to the gate counting.
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
     * of the node's aspects (design §7.6).
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
     * Fan one node's verification outcome out to its aspects and clear it from the in-flight set (design §7.6). A
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
     * @return {@code true} when every coordinator-issued verification has reported — the signal a later task
     *         feeds into the adapter gate (design §6.3).
     */
    public boolean allReported() {
        return inFlight.isEmpty();
    }
}
