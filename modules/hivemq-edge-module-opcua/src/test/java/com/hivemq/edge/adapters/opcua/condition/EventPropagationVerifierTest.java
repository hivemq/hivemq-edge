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
import org.eclipse.milo.opcua.stack.core.StatusCodes;
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

    /**
     * Review-09 finding 4: the Server object is not a notifier the walk gets an opinion about.
     * <p>
     * OPC 10000-5 §8.3.2 makes every event of the server accessible there, so {@code OutsideHierarchy} is
     * unreachable by definition — but the walk cannot know that, because it reasons from modelled inverse
     * references and a server may deliver to Server while modelling none of the chain back up. On a sparse
     * or permission-filtered address space it would browse a clean zero-parent answer, conclude the source
     * is outside the hierarchy, and drop a tag that would have published.
     * <p>
     * The narrowing node here exists and has no modelled ancestry at all — precisely the shape that used to
     * produce a confident {@code OutsideHierarchy}.
     */
    @Test
    void theServerObjectCarriesEverythingSoNothingIsOutsideIt() {
        final OpcUaClient client = client(Map.of(NODE, List.of()), Map.of(), Set.of(), Set.of());

        assertThat(EventPropagationVerifier.sourceCanEmitThrough(client, NODE, NodeIds.Server)
                        .join())
                .isEqualTo(new EventPropagationVerifier.Result.Reachable());
        assertThat(EventPropagationVerifier.conditionCanEmitThrough(client, NODE, NodeIds.Server)
                        .join())
                .isEqualTo(new EventPropagationVerifier.Result.Reachable());
    }

    /** A narrowing node that is the Server object itself needs no browse to settle either question. */
    @Test
    void andTheServerObjectNarrowedByItselfIsSettledWithoutAskingTheServer() {
        final OpcUaClient client = mock(OpcUaClient.class);

        assertThat(EventPropagationVerifier.sourceCanEmitThrough(client, NodeIds.Server, NodeIds.Server)
                        .join())
                .isEqualTo(new EventPropagationVerifier.Result.Reachable());
        assertThat(EventPropagationVerifier.conditionCanEmitThrough(client, NodeIds.Server, NodeIds.Server)
                        .join())
                .isEqualTo(new EventPropagationVerifier.Result.Reachable());
        verifyNoInteractions(client);
    }

    /**
     * EDG-894 P6: exempting the Server object from the <em>hierarchy</em> question must not exempt the
     * narrowing node from existing.
     * <p>
     * §8.3.2 promises that every event of the server is accessible at the Server object. It promises nothing
     * about a node id the operator typed, so a query tag rooted at Server narrowed by a node the server does
     * not hold used to be accepted with nothing looked at — subscribing cleanly to a where clause that can
     * never match, silent for the life of the adapter. The answer is {@code Unverified} rather than
     * {@code OutsideHierarchy} because OPC 10000-9 §4.3 lets a server hide its condition instances, so the
     * tag must still subscribe; what it must not do is subscribe without saying so.
     */
    @Test
    void butANarrowingNodeTheServerDoesNotHoldIsNamedRatherThanSilentlyAccepted() {
        final OpcUaClient client = client(Map.of(), Map.of(), Set.of(), Set.of(NODE));

        assertThat(EventPropagationVerifier.sourceCanEmitThrough(client, NODE, NodeIds.Server)
                        .join())
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(
                        EventPropagationVerifier.Result.Unverified.class))
                .satisfies(unverified -> assertThat(unverified.reason())
                        .contains("does not expose")
                        .contains("ns=2;s=Node")
                        .contains("nothing can be confirmed to match this predicate"));
        assertThat(EventPropagationVerifier.conditionCanEmitThrough(client, NODE, NodeIds.Server)
                        .join())
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(
                        EventPropagationVerifier.Result.Unverified.class))
                .satisfies(unverified -> assertThat(unverified.reason())
                        .contains("does not expose")
                        .contains("ns=2;s=Node"));
    }

    /** A browse that fails for any other reason under the root notifier is uncertainty, not a clean answer. */
    @Test
    void andABrowseThatCouldNotSettleItUnderTheRootNotifierIsUncertaintyToo() {
        final OpcUaClient client = client(Map.of(), Map.of(), Set.of(NODE), Set.of());

        assertThat(EventPropagationVerifier.sourceCanEmitThrough(client, NODE, NodeIds.Server)
                        .join())
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(
                        EventPropagationVerifier.Result.Unverified.class))
                .satisfies(unverified ->
                        assertThat(unverified.reason()).contains("ns=2;s=Node").contains("browse denied"));
    }

    @Test
    void butAnOrdinaryNotifierWithNoAncestryIsStillARejection() {
        // The other half of the rule, and the reason it is scoped to Server alone. A named area notifier
        // carries no such guarantee, so a complete browse proving no path to it still rejects the tag --
        // which is the sibling-mismatch behaviour review 08 added and this must not weaken.
        final OpcUaClient client = client(Map.of(NODE, List.of()), Map.of(), Set.of());

        assertThat(EventPropagationVerifier.sourceCanEmitThrough(client, NODE, NodeId.parse("ns=2;s=SiblingArea"))
                        .join())
                .isEqualTo(new EventPropagationVerifier.Result.OutsideHierarchy());
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
        return client(parents, conditionSources, failedEventSourceBrowses, Set.of());
    }

    /**
     * @param absentNodes nodes the server answers {@code Bad_NodeIdUnknown} about — the one browse outcome
     *     that is a statement about the address space rather than about the connection.
     */
    private static @NotNull OpcUaClient client(
            final @NotNull Map<NodeId, List<NodeId>> parents,
            final @NotNull Map<NodeId, List<NodeId>> conditionSources,
            final @NotNull Set<NodeId> failedEventSourceBrowses,
            final @NotNull Set<NodeId> absentNodes) {
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.getNamespaceTable()).thenReturn(new NamespaceTable());
        when(client.browseAsync(any(BrowseDescription.class))).thenAnswer(invocation -> {
            final BrowseDescription browse = invocation.getArgument(0);
            final NodeId node = browse.getNodeId();
            if (absentNodes.contains(node)) {
                return CompletableFuture.completedFuture(new BrowseResult(
                        new StatusCode(StatusCodes.Bad_NodeIdUnknown),
                        ByteString.NULL_VALUE,
                        new ReferenceDescription[0]));
            }
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
