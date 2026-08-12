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
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Which notifier the walk arrives at, over address spaces with more than one way up.
 * <p>
 * Review-04 finding 4. The resolver promises the <em>nearest</em> notifier — the narrowest node that can see
 * the condition — and the walk was depth-first, so it returned the first notifier on whichever branch the
 * server happened to list first. A server's reference order is not a distance ordering, so "first" and
 * "nearest" are only the same thing when there is one way up.
 * <p>
 * Nothing about that failure is visible in production: both answers are notifiers that deliver the
 * condition's events, and the {@code ConditionId} filter means no unrelated alarm is published either way.
 * What it costs is a subscription one level broader than it needed to be, decided by the server's reference
 * order — including, on a server with per-node permissions, a broad notifier the session may not subscribe to
 * while a narrow one it may was available.
 * <p>
 * Every test here runs each topology in <b>both server orders</b>, because a wrong answer that depends on
 * reference order passes half the time otherwise, and which half is the fixture author's coincidence.
 */
class NotifierResolverWalkTest {

    private static final @NotNull NodeId CONDITION = NodeId.parse("ns=2;s=Boiler1.HighTemp");

    private @NotNull FakeAddressSpace server;
    private @NotNull OpcUaClient client;

    @BeforeEach
    void setUp() {
        server = new FakeAddressSpace();
        client = server.client();
    }

    @Test
    void aSourceThatIsItselfANotifierBeatsAnotherSourcesAncestor() {
        // The finding's topology. Source A is not a notifier and its area above it is; source B is a notifier
        // itself, one hop nearer the condition. Depth-first with A listed first walks A's whole ancestry,
        // finds area A, and never asks whether B is a notifier at all.
        for (final List<String> order : bothOrders("ns=2;s=SourceA", "ns=2;s=SourceB")) {
            setUp();
            server.conditionSourcesOf(CONDITION, order);
            server.parentOf("ns=2;s=SourceA", "ns=2;s=AreaA");
            server.notifiers("ns=2;s=AreaA", "ns=2;s=SourceB");

            assertThat(resolve())
                    .as("with the server listing %s first", order.get(0))
                    .isEqualTo(new NotifierResolver.Result.Found(
                            NodeId.parse("ns=2;s=SourceB"), "found by walking up from the condition"));
        }
    }

    @Test
    void aDirectParentThatIsANotifierBeatsAnotherParentsGrandparent() {
        // The same ordering defect on the compatibility path, for servers that attach HasEventSource to the
        // condition directly. Two direct parents; the first leads to a notifier two hops up, the second is
        // one hop up and a notifier itself.
        for (final List<String> order : bothOrders("ns=2;s=ParentA", "ns=2;s=ParentB")) {
            setUp();
            server.parentsOf(CONDITION, order);
            server.parentOf("ns=2;s=ParentA", "ns=2;s=GrandparentA");
            server.notifiers("ns=2;s=GrandparentA", "ns=2;s=ParentB");

            assertThat(found())
                    .as("with the server listing %s first", order.get(0))
                    .isEqualTo(NodeId.parse("ns=2;s=ParentB"));
        }
    }

    @Test
    void theNearestIsTakenAcrossLevelsNotJustAcrossBranches() {
        // Three levels, so that "breadth-first" is doing something a two-level fixture cannot distinguish
        // from "test all the sources first". The only notifier reachable through A is three hops up; through
        // B it is two. Neither source is a notifier, so the answer comes from the second level either way.
        for (final List<String> order : bothOrders("ns=2;s=SourceA", "ns=2;s=SourceB")) {
            setUp();
            server.conditionSourcesOf(CONDITION, order);
            server.parentOf("ns=2;s=SourceA", "ns=2;s=MidA");
            server.parentOf("ns=2;s=MidA", "ns=2;s=TopA");
            server.parentOf("ns=2;s=SourceB", "ns=2;s=MidB");
            server.notifiers("ns=2;s=TopA", "ns=2;s=MidB");

            assertThat(found())
                    .as("with the server listing %s first", order.get(0))
                    .isEqualTo(NodeId.parse("ns=2;s=MidB"));
        }
    }

    @Test
    void aSourceThatIsANotifierIsStillPreferredToAnythingAboveIt() {
        // The property the old code did get right, kept: HasNotifier is a subtype of HasEventSource, so a
        // node can be both a ConditionSource and a notifier (§6.2), and one that is needs no walking past.
        server.conditionSourcesOf(CONDITION, List.of("ns=2;s=Source"));
        server.parentOf("ns=2;s=Source", "ns=2;s=Area");
        server.notifiers("ns=2;s=Source", "ns=2;s=Area");

        assertThat(found()).isEqualTo(NodeId.parse("ns=2;s=Source"));
    }

    @Test
    void aCycleTerminatesWithoutRebrowsingItsWayRound() {
        // The visited set, which is what lets the depth bound be a backstop rather than the only thing
        // between this and a loop. A -> B -> A, with no notifier anywhere: the walk must answer NotFound,
        // and must not browse either node more than once to get there.
        server.conditionSourcesOf(CONDITION, List.of("ns=2;s=A"));
        server.parentOf("ns=2;s=A", "ns=2;s=B");
        server.parentOf("ns=2;s=B", "ns=2;s=A");

        assertThat(resolve()).isInstanceOf(NotifierResolver.Result.NotFound.class);
        assertThat(server.browseCountOf("ns=2;s=A"))
                .as("a node already queued must not be walked into a second time")
                .isEqualTo(1);
        assertThat(server.browseCountOf("ns=2;s=B")).isEqualTo(1);
    }

    @Test
    void aDiamondBrowsesItsSharedAncestorOnce() {
        // Two paths into one ancestor. Without a visited set it is browsed once per path, which on a wide
        // hierarchy is the difference between a linear walk and an exponential one.
        server.conditionSourcesOf(CONDITION, List.of("ns=2;s=Left", "ns=2;s=Right"));
        server.parentOf("ns=2;s=Left", "ns=2;s=Shared");
        server.parentOf("ns=2;s=Right", "ns=2;s=Shared");
        server.parentOf("ns=2;s=Shared", "ns=2;s=Root");
        server.notifiers("ns=2;s=Root");

        assertThat(found()).isEqualTo(NodeId.parse("ns=2;s=Root"));
        assertThat(server.browseCountOf("ns=2;s=Shared")).isEqualTo(1);
    }

    @Test
    void aHierarchyWithNoNotifierAnywhereIsReportedRatherThanGuessed() {
        // There is deliberately no fallback to the Server object: it would almost always work, which is
        // exactly the objection -- it would silently widen the tag from one area to the whole plant.
        server.conditionSourcesOf(CONDITION, List.of("ns=2;s=Source"));
        server.parentOf("ns=2;s=Source", "ns=2;s=Area");

        assertThat(resolve())
                .asInstanceOf(
                        org.assertj.core.api.InstanceOfAssertFactories.type(NotifierResolver.Result.NotFound.class))
                .satisfies(notFound -> assertThat(notFound.reason()).contains("notifierNode"));
    }

    @Test
    void aDeclaredNotifierSkipsTheWalkEntirely() {
        // The escape hatch exists because the device could not be relied on to answer, so consulting it
        // would defeat the purpose.
        server.conditionSourcesOf(CONDITION, List.of("ns=2;s=Source"));
        server.notifiers("ns=2;s=Source");

        assertThat(NotifierResolver.resolve(client, CONDITION, "ns=2;s=DeclaredArea", "boiler-high-temp")
                        .join())
                .isEqualTo(
                        new NotifierResolver.Result.Found(NodeId.parse("ns=2;s=DeclaredArea"), "declared on the tag"));
        assertThat(server.totalBrowses()).isZero();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    /** The same two nodes in both the orders a server might list them in. */
    private static @NotNull List<List<String>> bothOrders(final @NotNull String first, final @NotNull String second) {
        return List.of(List.of(first, second), List.of(second, first));
    }

    private @NotNull NotifierResolver.Result resolve() {
        return NotifierResolver.resolve(client, CONDITION, null, "boiler-high-temp")
                .join();
    }

    private @NotNull NodeId found() {
        final NotifierResolver.Result result = resolve();
        assertThat(result).isInstanceOf(NotifierResolver.Result.Found.class);
        return ((NotifierResolver.Result.Found) result).notifier();
    }

    /**
     * A server's address space, answered through the two services the resolver uses.
     * <p>
     * A fake rather than per-call Mockito stubs: the walk's shape is the thing under test, so the fixture has
     * to be able to answer a browse the test did not anticipate — an unstubbed node returning null would make
     * a walk that went somewhere wrong look like one that stopped.
     */
    private static final class FakeAddressSpace {

        private final @NotNull Map<NodeId, List<NodeId>> conditionSources = new LinkedHashMap<>();
        private final @NotNull Map<NodeId, List<NodeId>> parents = new LinkedHashMap<>();
        private final @NotNull Set<NodeId> notifiers = new HashSet<>();
        private final @NotNull Map<NodeId, AtomicInteger> browses = new HashMap<>();

        void conditionSourcesOf(final @NotNull NodeId condition, final @NotNull List<String> sources) {
            conditionSources.put(condition, sources.stream().map(NodeId::parse).toList());
        }

        void parentsOf(final @NotNull NodeId child, final @NotNull List<String> above) {
            parents.put(child, above.stream().map(NodeId::parse).toList());
        }

        void parentOf(final @NotNull String child, final @NotNull String above) {
            parentsOf(NodeId.parse(child), List.of(above));
        }

        void notifiers(final @NotNull String... nodes) {
            for (final String node : nodes) {
                notifiers.add(NodeId.parse(node));
            }
        }

        int browseCountOf(final @NotNull String node) {
            final AtomicInteger count = browses.get(NodeId.parse(node));
            return count == null ? 0 : count.get();
        }

        int totalBrowses() {
            return browses.values().stream().mapToInt(AtomicInteger::get).sum();
        }

        @NotNull
        OpcUaClient client() {
            final OpcUaClient client = mock(OpcUaClient.class);
            when(client.getNamespaceTable()).thenReturn(new NamespaceTable());
            when(client.browseAsync(any(BrowseDescription.class))).thenAnswer(invocation -> {
                final BrowseDescription browse = invocation.getArgument(0);
                final NodeId node = browse.getNodeId();
                final boolean upward = NodeIds.HasEventSource.equals(browse.getReferenceTypeId());
                if (upward) {
                    // Only the upward leg is counted: it is the one a cycle or a diamond repeats.
                    browses.computeIfAbsent(node, ignored -> new AtomicInteger())
                            .incrementAndGet();
                }
                final List<NodeId> targets = (upward ? parents : conditionSources).getOrDefault(node, List.of());
                return CompletableFuture.completedFuture(page(targets));
            });
            when(client.readAsync(anyDouble(), any(), any())).thenAnswer(invocation -> {
                final List<ReadValueId> reads = invocation.getArgument(2);
                final DataValue[] results = new DataValue[reads.size()];
                for (int i = 0; i < reads.size(); i++) {
                    final ReadValueId read = reads.get(i);
                    results[i] = AttributeId.EventNotifier.uid().equals(read.getAttributeId())
                            ? eventNotifier(notifiers.contains(read.getNodeId()))
                            : new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null);
                }
                return CompletableFuture.completedFuture(new ReadResponse(
                        new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null), results, null));
            });
            return client;
        }

        /** {@code SubscribeToEvents} set or clear — the only bit that makes a node a valid event target. */
        private static @NotNull DataValue eventNotifier(final boolean subscribable) {
            return new DataValue(new Variant(uint(subscribable ? 1 : 0)), StatusCode.GOOD, null);
        }

        private static @NotNull BrowseResult page(final @NotNull List<NodeId> targets) {
            final List<ReferenceDescription> references = new ArrayList<>(targets.size());
            for (final NodeId target : targets) {
                references.add(new ReferenceDescription(
                        NodeIds.HasEventSource,
                        false, // inverse: these are the nodes above
                        ExpandedNodeId.of(target.getNamespaceIndex(), String.valueOf(target.getIdentifier())),
                        new QualifiedName(0, String.valueOf(target.getIdentifier())),
                        LocalizedText.english(String.valueOf(target.getIdentifier())),
                        NodeClass.Object,
                        ExpandedNodeId.NULL_VALUE));
            }
            return new BrowseResult(
                    StatusCode.GOOD, ByteString.NULL_VALUE, references.toArray(new ReferenceDescription[0]));
        }
    }
}
