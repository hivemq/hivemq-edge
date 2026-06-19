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

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The shared verification bookkeeping (design §7.6): a node is verified at most once while in flight (so a
 * read-and-write tag yields a single {@code verifyBatch} entry), a reported result clears it for the next time,
 * and {@code allReported} tracks the outstanding requests the gate counting consumes.
 */
class SharedNodeVerificationTest {

    private static final class CountingNode extends Node {
        private final @NotNull String id;

        private CountingNode(final @NotNull String id) {
            this.id = id;
        }

        @Override
        public @NotNull String nodeId() {
            return id;
        }

        @Override
        public @NotNull String nodeString() {
            return "{}";
        }

        @Override
        public @NotNull EnumSet<com.hivemq.adapter.sdk.api.v2.node.NodeProperty> properties() {
            return EnumSet.noneOf(com.hivemq.adapter.sdk.api.v2.node.NodeProperty.class);
        }
    }

    @Test
    void requestVerification_deduplicatesWhileInFlight() {
        final List<List<Node>> requests = new ArrayList<>();
        final SharedNodeVerification coordinator = new SharedNodeVerification(requests::add, node -> null);
        final Node node = new CountingNode("a");

        coordinator.requestVerification(node);
        coordinator.requestVerification(node); // already in flight — must not re-issue

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0)).containsExactly(node);
        assertThat(coordinator.needsVerify(node)).isFalse();
        assertThat(coordinator.allReported()).isFalse();
    }

    @Test
    void reportedResult_clearsTheNodeForTheNextRequest() {
        final List<List<Node>> requests = new ArrayList<>();
        final SharedNodeVerification coordinator = new SharedNodeVerification(requests::add, node -> null);
        final Node node = new CountingNode("a");

        coordinator.requestVerification(node);
        coordinator.onVerifyResult(node, new VerifyOutcome.Success());

        assertThat(coordinator.allReported()).isTrue();
        assertThat(coordinator.needsVerify(node)).isTrue();

        coordinator.requestVerification(node); // a fresh request issues again
        assertThat(requests).hasSize(2);
    }

    @Test
    void allReported_isFalseUntilEveryRequestedNodeReports() {
        final List<List<Node>> requests = new ArrayList<>();
        final SharedNodeVerification coordinator = new SharedNodeVerification(requests::add, node -> null);
        final Node first = new CountingNode("a");
        final Node second = new CountingNode("b");

        coordinator.requestVerification(first);
        coordinator.requestVerification(second);
        assertThat(coordinator.allReported()).isFalse();

        coordinator.onVerifyResult(first, new VerifyOutcome.Success());
        assertThat(coordinator.allReported()).isFalse();

        coordinator.onVerifyResult(second, new VerifyOutcome.TransientFailure("retry"));
        assertThat(coordinator.allReported()).isTrue();
    }
}
