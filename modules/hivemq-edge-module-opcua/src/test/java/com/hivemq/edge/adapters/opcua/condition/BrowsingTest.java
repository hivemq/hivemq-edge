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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseNextResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Paging behaviour, tested against a stubbed client rather than the embedded server.
 * <p>
 * Milo's server never truncates a browse — it has no {@code maxReferencesPerNode} handling at all — so no
 * integration test in this module can produce a continuation point. Testing this against the real server
 * would assert only that the single-page case still works, which is the case that already worked. A stub is
 * the only way to exercise the behaviour the fix exists for.
 */
class BrowsingTest {

    private static final @NotNull BrowseDescription BROWSE = new BrowseDescription(
            NodeId.parse("ns=2;s=Alarm"),
            BrowseDirection.Forward,
            NodeIds.HasComponent,
            true,
            uint(NodeClass.Method.getValue()),
            uint(BrowseResultMask.All.getValue()));

    private static @NotNull ReferenceDescription reference(final @NotNull String browseName) {
        return new ReferenceDescription(
                NodeIds.HasComponent,
                true,
                ExpandedNodeId.of(browseName),
                new QualifiedName(0, browseName),
                LocalizedText.english(browseName),
                NodeClass.Method,
                ExpandedNodeId.NULL_VALUE);
    }

    private static @NotNull BrowseResult page(
            final @NotNull ByteString continuationPoint, final @NotNull String... browseNames) {
        return new BrowseResult(
                StatusCode.GOOD,
                continuationPoint,
                java.util.Arrays.stream(browseNames)
                        .map(BrowsingTest::reference)
                        .toArray(ReferenceDescription[]::new));
    }

    private static @NotNull ByteString token(final @NotNull String value) {
        return new ByteString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static @NotNull BrowseNextResponse nextResponse(final @NotNull BrowseResult result) {
        return new BrowseNextResponse(
                new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null),
                new BrowseResult[] {result},
                null);
    }

    @Test
    void whenTheServerPagesTheAnswer_thenEveryPageIsRead() throws Exception {
        // The defect this exists for: a method sitting on page two was reported as absent, and the caller
        // turned that into Bad_MethodInvalid -- blaming the device for something the client did.
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(page(token("cp-1"), "Acknowledge", "Confirm")));
        when(client.browseNextAsync(eq(false), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        nextResponse(page(ByteString.NULL_VALUE, "Suppress", "Suppress2"))));

        final List<ReferenceDescription> references =
                Browsing.browseAll(client, BROWSE).get();

        assertThat(references).as("both pages must be read, not only the first").hasSize(4);
        assertThat(references.stream().map(r -> r.getBrowseName().getName()).toList())
                .containsExactly("Acknowledge", "Confirm", "Suppress", "Suppress2");
    }

    @Test
    void whenThereIsNoContinuationPoint_thenNoBrowseNextIsIssued() {
        // The overwhelmingly common case. An extra round trip per browse would cost every adapter start.
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(page(ByteString.NULL_VALUE, "Acknowledge")));

        Browsing.browseAll(client, BROWSE).join();

        verify(client, never()).browseNextAsync(anyBoolean(), any());
    }

    @Test
    void whenBrowseNextFails_thenThePagesAlreadyReadAreKept() {
        // A partial list is closer to the truth than an exception, and every caller already treats "not
        // found" as an answer. Failing adapter start over a paging hiccup would be the worse outcome.
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(page(token("cp-1"), "Acknowledge")));
        when(client.browseNextAsync(eq(false), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("session closed")));

        final List<ReferenceDescription> references =
                Browsing.browseAll(client, BROWSE).join();

        assertThat(references).hasSize(1);
        assertThat(references.get(0).getBrowseName().getName()).isEqualTo("Acknowledge");
    }

    @Test
    void whenTheServerKeepsPaging_thenTheLoopStopsAndReleasesTheToken() {
        // A server that always returns a continuation point would otherwise hang adapter start. Stopping is
        // not enough on its own: OPC 10000-4 §5.9.3 says a client shall always use the continuation point to
        // free the server's resources, including when it wants no more data.
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(page(token("cp"), "First")));
        when(client.browseNextAsync(eq(false), any()))
                .thenReturn(CompletableFuture.completedFuture(nextResponse(page(token("cp"), "Another"))));
        when(client.browseNextAsync(eq(true), any()))
                .thenReturn(CompletableFuture.completedFuture(nextResponse(page(ByteString.NULL_VALUE))));

        final List<ReferenceDescription> references =
                Browsing.browseAll(client, BROWSE).join();

        assertThat(references).as("the loop must terminate").isNotEmpty();
        verify(client, times(1)).browseNextAsync(eq(true), any());
    }

    @Test
    void whenTheBrowseItselfFails_thenTheAnswerIsNoReferences() {
        // Callers ask "is X present?" and handle absence. An exception here would propagate into adapter
        // start, where a single unreadable node would abort a whole tag sequence.
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("not connected")));

        assertThat(Browsing.browseAll(client, BROWSE).join()).isEmpty();
    }
}
