/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class EventPropagationVerifierTest {

    private static final @NotNull NodeId NODE = NodeId.parse("ns=2;s=Node");

    @Test
    void theSelectedNotifierMayBeTheNarrowingNodeItself() {
        final OpcUaClient client = mock(OpcUaClient.class);

        assertThat(EventPropagationVerifier.conditionCanEmitThrough(client, NODE, NODE)
                        .join())
                .isEqualTo(new EventPropagationVerifier.Result.Reachable());
        assertThat(EventPropagationVerifier.sourceCanEmitThrough(client, NODE, NODE)
                        .join())
                .isEqualTo(new EventPropagationVerifier.Result.Reachable());
        verifyNoInteractions(client);
    }

    @Test
    void aNotifierAtTheBoundIsReachableAndOneBeyondItIsUnverified() {
        final Map<NodeId, List<NodeId>> parents = new LinkedHashMap<>();
        NodeId below = NODE;
        for (int hop = 1; hop <= 11; hop++) {
            final NodeId above = NodeId.parse("ns=2;s=Up" + hop);
            parents.put(below, List.of(above));
            below = above;
        }
        final OpcUaClient client = client(parents, Map.of(), Set.of());

        assertThat(EventPropagationVerifier.sourceCanEmitThrough(client, NODE, NodeId.parse("ns=2;s=Up10"))
                        .join())
                .isEqualTo(new EventPropagationVerifier.Result.Reachable());
        assertThat(EventPropagationVerifier.sourceCanEmitThrough(client, NODE, NodeId.parse("ns=2;s=Up11"))
                        .join())
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(
                        EventPropagationVerifier.Result.Unverified.class))
                .satisfies(unverified -> assertThat(unverified.reason()).contains("10-hop verification bound"));
    }

    @Test
    void anUnknownCompatibilityPathPreventsAFalseOutsideAnswer() {
        final OpcUaClient client = client(Map.of(), Map.of(NODE, List.of()), Set.of(NODE));

        assertThat(EventPropagationVerifier.conditionCanEmitThrough(
                                client, NODE, NodeId.parse("ns=2;s=SelectedNotifier"))
                        .join())
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(
                        EventPropagationVerifier.Result.Unverified.class))
                .satisfies(unverified -> assertThat(unverified.reason()).contains("browse denied"));
    }

    @Test
    void failedAndCyclicBranchesRemainUnverifiedWithoutReplacingTheFirstFailure() {
        final NodeId firstFailed = NodeId.parse("ns=2;s=FirstFailedBranch");
        final NodeId secondFailed = NodeId.parse("ns=2;s=SecondFailedBranch");
        final NodeId cyclic = NodeId.parse("ns=2;s=CyclicBranch");
        final OpcUaClient client = client(
                Map.of(NODE, List.of(firstFailed, secondFailed, cyclic), cyclic, List.of(NODE)),
                Map.of(),
                Set.of(firstFailed, secondFailed));

        assertThat(EventPropagationVerifier.sourceCanEmitThrough(client, NODE, NodeId.parse("ns=2;s=UnrelatedNotifier"))
                        .join())
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(
                        EventPropagationVerifier.Result.Unverified.class))
                .satisfies(unverified -> assertThat(unverified.reason())
                        .contains("FirstFailedBranch")
                        .doesNotContain("SecondFailedBranch")
                        .contains("browse denied"));
    }

    private static @NotNull OpcUaClient client(
            final @NotNull Map<NodeId, List<NodeId>> parents,
            final @NotNull Map<NodeId, List<NodeId>> conditionSources,
            final @NotNull Set<NodeId> failedEventSourceBrowses) {
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.getNamespaceTable()).thenReturn(new NamespaceTable());
        when(client.browseAsync(any(BrowseDescription.class))).thenAnswer(invocation -> {
            final BrowseDescription browse = invocation.getArgument(0);
            final NodeId node = browse.getNodeId();
            final boolean eventSource = NodeIds.HasEventSource.equals(browse.getReferenceTypeId());
            if (eventSource && failedEventSourceBrowses.contains(node)) {
                return CompletableFuture.failedFuture(new IllegalStateException("browse denied"));
            }
            final List<NodeId> targets = (eventSource ? parents : conditionSources).getOrDefault(node, List.of());
            return CompletableFuture.completedFuture(page(browse.getReferenceTypeId(), targets));
        });
        return client;
    }

    private static @NotNull BrowseResult page(
            final @NotNull NodeId referenceType, final @NotNull List<NodeId> targets) {
        final List<ReferenceDescription> references = new ArrayList<>(targets.size());
        for (final NodeId target : targets) {
            references.add(new ReferenceDescription(
                    referenceType,
                    false,
                    target.expanded(),
                    new QualifiedName(0, String.valueOf(target.getIdentifier())),
                    LocalizedText.english(String.valueOf(target.getIdentifier())),
                    NodeClass.Object,
                    NodeId.NULL_VALUE.expanded()));
        }
        return new BrowseResult(
                StatusCode.GOOD, ByteString.NULL_VALUE, references.toArray(new ReferenceDescription[0]));
    }
}
