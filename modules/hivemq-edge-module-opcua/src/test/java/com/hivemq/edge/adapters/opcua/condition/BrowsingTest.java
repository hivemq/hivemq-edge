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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.eclipse.milo.opcua.stack.core.StatusCodes;
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
    void whenBrowseNextFails_thenTheFailureIsReportedRatherThanAPartialList() {
        // Reversed deliberately (review finding 8). This used to keep the pages already read and report
        // success, which makes an incomplete list indistinguishable from a complete one -- and every caller
        // asks "is X present?". A method on the unread page then reads as absent, and ConditionUpdateWriter
        // acts on that by falling back to a different call.
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(page(token("cp-1"), "Acknowledge")));
        when(client.browseNextAsync(eq(false), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("session closed")));
        when(client.browseNextAsync(eq(true), any()))
                .thenReturn(CompletableFuture.completedFuture(nextResponse(page(ByteString.NULL_VALUE))));

        assertThatThrownBy(() -> Browsing.browseAll(client, BROWSE).join())
                .hasCauseInstanceOf(Browsing.BrowseFailedException.class)
                .cause()
                .hasMessageContaining("incomplete")
                .hasMessageContaining("session closed");
    }

    @Test
    void whenBrowseNextFails_thenTheContinuationPointIsHandedBack() {
        // Review finding 15. The max-page branch released its token; this path did not, so a flaky server
        // accumulated continuation points until the session was torn down. OPC 10000-4 §5.9.3: a client
        // shall always use the continuation point to free the server's resources.
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(page(token("cp-1"), "Acknowledge")));
        when(client.browseNextAsync(eq(false), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("session closed")));
        when(client.browseNextAsync(eq(true), any()))
                .thenReturn(CompletableFuture.completedFuture(nextResponse(page(ByteString.NULL_VALUE))));

        assertThatThrownBy(() -> Browsing.browseAll(client, BROWSE).join())
                .hasCauseInstanceOf(Browsing.BrowseFailedException.class);

        // Released with the same token, and exactly once.
        verify(client, times(1)).browseNextAsync(true, List.of(token("cp-1")));
    }

    @Test
    void whenReleasingTheTokenAlsoFails_thenTheOriginalFailureIsWhatPropagates() {
        // A release that fails must not replace the reason. "BrowseNext failed" is the fact worth having;
        // "the release of the token for the BrowseNext that failed also failed" tells an operator nothing.
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(page(token("cp-1"), "Acknowledge")));
        when(client.browseNextAsync(eq(false), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("session closed")));
        when(client.browseNextAsync(eq(true), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("still closed")));

        assertThatThrownBy(() -> Browsing.browseAll(client, BROWSE).join())
                .cause()
                .hasMessageContaining("session closed");
    }

    @Test
    void whenTheServerKeepsPaging_thenTheLoopStopsAndReleasesTheToken() {
        // A server that always returns a continuation point would otherwise hang adapter start. Stopping is
        // not enough on its own: OPC 10000-4 §5.9.3 says a client shall always use the continuation point to
        // free the server's resources, including when it wants no more data.
        //
        // Reported as a failure rather than a truncated success, for the same reason as the BrowseNext case
        // above: the list is incomplete either way, and only the caller can decide what that means.
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(page(token("cp"), "First")));
        when(client.browseNextAsync(eq(false), any()))
                .thenReturn(CompletableFuture.completedFuture(nextResponse(page(token("cp"), "Another"))));
        when(client.browseNextAsync(eq(true), any()))
                .thenReturn(CompletableFuture.completedFuture(nextResponse(page(ByteString.NULL_VALUE))));

        assertThatThrownBy(() -> Browsing.browseAll(client, BROWSE).join())
                .cause()
                .hasMessageContaining("pages of references");

        verify(client, times(1)).browseNextAsync(eq(true), any());
    }

    @Test
    void whenTheBrowseItselfFails_thenTheFailureIsReported() {
        // Review finding 8. Swallowing this to an empty list made a transport error indistinguishable from a
        // node with no references -- so ConditionTypeVerifier's own exceptionally() handler was unreachable
        // and a disconnect was reported to the operator as "the node has no type definition".
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("not connected")));

        assertThatThrownBy(() -> Browsing.browseAll(client, BROWSE).join())
                .hasCauseInstanceOf(Browsing.BrowseFailedException.class)
                .cause()
                .hasMessageContaining("not connected");
    }

    @Test
    void whenTheBrowseResultCarriesABadStatus_thenTheFailureIsReported() {
        // The service call can succeed while the operation inside it fails. Reading getReferences() without
        // checking the result's own status takes Bad_NodeIdUnknown -- or a permissions refusal -- for a node
        // that simply has no references.
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        new BrowseResult(new StatusCode(StatusCodes.Bad_NodeIdUnknown), ByteString.NULL_VALUE, null)));

        assertThatThrownBy(() -> Browsing.browseAll(client, BROWSE).join())
                .hasCauseInstanceOf(Browsing.BrowseFailedException.class)
                .cause()
                .hasMessageContaining("refused");
    }

    @Test
    void whenBrowseNextReturnsABadStatus_thenTheFailureIsReported() {
        // Same rule one level down: BrowseNext answers with a result array, and each result carries its own
        // status. A bad one means the continuation was not honoured, so the list is incomplete.
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(page(token("cp-1"), "Acknowledge")));
        when(client.browseNextAsync(eq(false), any()))
                .thenReturn(CompletableFuture.completedFuture(nextResponse(new BrowseResult(
                        new StatusCode(StatusCodes.Bad_ContinuationPointInvalid), ByteString.NULL_VALUE, null))));
        when(client.browseNextAsync(eq(true), any()))
                .thenReturn(CompletableFuture.completedFuture(nextResponse(page(ByteString.NULL_VALUE))));

        assertThatThrownBy(() -> Browsing.browseAll(client, BROWSE).join())
                .cause()
                .hasMessageContaining("refused");
    }

    @Test
    void whenTheNodeGenuinelyHasNoReferences_thenTheAnswerIsAnEmptyListRatherThanAFailure() {
        // The distinction the whole reversal exists to preserve: empty is a fact about the address space and
        // must stay an ordinary answer, or every caller's "not found" branch becomes unreachable.
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(page(ByteString.NULL_VALUE)));

        assertThat(Browsing.browseAll(client, BROWSE).join()).isEmpty();
    }
}
