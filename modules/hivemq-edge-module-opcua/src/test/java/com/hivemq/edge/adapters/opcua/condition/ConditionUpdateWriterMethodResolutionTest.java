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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Which of a method's two forms is called, against a stubbed server that exposes only some of them.
 * <p>
 * Review finding 6. OPC UA grew these in two passes: the original {@code Suppress()} takes no arguments, so
 * the specification later added {@code Suppress2(Comment)}. Table 40 lists them separately and both are
 * Optional, so <b>which of the two a server exposes is a per-device question and they are independent</b> —
 * a server may have either, or both.
 * <p>
 * Resolution used to be one-directional: it looked for {@code Suppress2} when the user supplied a comment and
 * for {@code Suppress} otherwise. On a server exposing only the newer form, a command without a comment
 * therefore failed with {@code Bad_NotSupported} — so whether an alarm could be suppressed depended on
 * whether anyone had written a note about it.
 * <p>
 * Stubbed rather than run against the embedded server because the case is a server that <em>lacks</em> a
 * standard method, which Milo's namespace always provides.
 */
class ConditionUpdateWriterMethodResolutionTest {

    private static final @NotNull NodeId CONDITION = NodeId.parse("ns=2;s=Boiler1.HighTemp");

    private @NotNull OpcUaClient client;

    @BeforeEach
    void setUp() {
        client = mock(OpcUaClient.class);
        when(client.getNamespaceTable()).thenReturn(new NamespaceTable());
        when(client.callAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(new CallResponse(
                        new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null),
                        new CallMethodResult[] {new CallMethodResult(StatusCode.GOOD, null, null, null)},
                        null)));
    }

    @Test
    void aServerExposingOnlyTheCommentedFormAcceptsACommandWithoutAComment() {
        // The defect. Suppress2 is there, Suppress is not, and the command carries no comment -- which used
        // to look for Suppress alone and give up.
        exposes("Suppress2");

        final StatusCode status = suppress(null);

        assertThat(status.isGood())
                .as("the alarm must still be suppressible when only the newer form exists")
                .isTrue();
        assertThat(calledMethod()).isEqualTo(methodNode("Suppress2"));
    }

    @Test
    void andItSendsTheSpecificationsNullCommentRatherThanInventingOne() {
        // Nothing is invented to fill the argument: OPC 10000-9 §5.7.3 defines a NULL LocalizedText -- both
        // locale and text empty -- as "ignored and any existing comments will remain unchanged", which is
        // exactly what "the user said nothing about the comment" means. An empty *string* would be the
        // opposite instruction: §5.7.3's reset form, which erases the existing comment.
        exposes("Suppress2");

        suppress(null);

        final Variant[] arguments = calledArguments();
        assertThat(arguments).hasSize(1);
        assertThat(arguments[0].value())
                .as("a null LocalizedText leaves the existing comment alone")
                .isEqualTo(LocalizedText.NULL_VALUE);
    }

    @Test
    void aServerExposingOnlyTheCommentedFormStillCarriesAComment() {
        // The case that already worked, pinned so the reordering did not cost it.
        exposes("Suppress2");

        suppress("checked the burner");

        assertThat(calledMethod()).isEqualTo(methodNode("Suppress2"));
        assertThat(calledArguments()[0].value()).isEqualTo(LocalizedText.english("checked the burner"));
    }

    @Test
    void aServerExposingBothPrefersTheBaseFormWhenThereIsNoComment() {
        // With nothing to carry, the original form is the one to call: it is the older and more widely
        // implemented of the two, and reaching for the newer one would be gratuitous.
        exposes("Suppress", "Suppress2");

        suppress(null);

        assertThat(calledMethod()).isEqualTo(methodNode("Suppress"));
    }

    @Test
    void aServerExposingBothPrefersTheCommentedFormWhenThereIsAComment() {
        exposes("Suppress", "Suppress2");

        suppress("checked the burner");

        assertThat(calledMethod()).isEqualTo(methodNode("Suppress2"));
    }

    @Test
    void aServerExposingOnlyTheBaseFormDropsTheCommentButStillActs() {
        // The deliberate trade: someone writing {"method":"SUPPRESS","comment":"..."} wants the alarm
        // suppressed first and foremost, so a server without Suppress2 gets the plain Suppress and the
        // comment is dropped with a warning. Refusing would leave an alarm unsuppressed over a note.
        exposes("Suppress");

        final StatusCode status = suppress("checked the burner");

        assertThat(status.isGood()).isTrue();
        assertThat(calledMethod()).isEqualTo(methodNode("Suppress"));
        assertThat(calledArguments()).as("the base form takes no arguments").isEmpty();
    }

    @Test
    void aServerExposingNeitherFormFallsBackToTheStandardTypesMethodId() {
        // Review-02 finding 1. This used to assert the opposite -- Bad_NotSupported and no Call at all -- on
        // the reading that Part 9 prescribes a type-level MethodId for Enable and Disable alone. OPC 10000-4
        // §5.12.2.2 Table 59 states the rule once for every method, so a missing instance node is the end of
        // what browsing can answer rather than the end of the road, and whether the server supports the
        // method is the server's answer to give.
        exposes();

        final StatusCode status = suppress(null);

        assertThat(status.isGood()).isTrue();
        assertThat(calledMethod()).isEqualTo(NodeIds.AlarmConditionType_Suppress);
        verify(client, times(1)).callAsync(any());
    }

    @Test
    void theCommentedFormIsNotBrowsedTwiceWhenItIsAlreadyKnownAbsent() {
        // A command with a comment browses for Suppress2 first. If that misses, the base-preferred path must
        // not ask again -- the answer has not changed, and each browse is a server round trip.
        exposes("Suppress");

        suppress("checked the burner");

        final ArgumentCaptor<BrowseDescription> browses = ArgumentCaptor.forClass(BrowseDescription.class);
        verify(client, org.mockito.Mockito.atLeastOnce()).browseAsync(browses.capture());
        assertThat(browses.getAllValues())
                .as("one browse for the commented form, one for the base form, and no more")
                .hasSize(2);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    /** Stubs the server so the condition exposes exactly these methods as components. */
    private void exposes(final @NotNull String... methodNames) {
        final ReferenceDescription[] references = Arrays.stream(methodNames)
                .map(ConditionUpdateWriterMethodResolutionTest::methodReference)
                .toArray(ReferenceDescription[]::new);
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, references)));
    }

    private @NotNull StatusCode suppress(final String comment) {
        return ConditionUpdateWriter.requestTransition(
                        client, CONDITION, new ConditionUpdate(ConditionUpdate.Method.SUPPRESS, null, comment, null))
                .join();
    }

    private @NotNull NodeId calledMethod() {
        return capturedRequest().getMethodId();
    }

    private @NotNull Variant[] calledArguments() {
        final Variant[] arguments = capturedRequest().getInputArguments();
        return arguments == null ? new Variant[0] : arguments;
    }

    @SuppressWarnings("unchecked")
    private @NotNull CallMethodRequest capturedRequest() {
        final ArgumentCaptor<List<CallMethodRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(client).callAsync(captor.capture());
        return captor.getValue().get(0);
    }

    /** The node id this stub gives a method of that name, so a call can be checked against it. */
    private static @NotNull NodeId methodNode(final @NotNull String browseName) {
        return NodeId.parse("ns=2;s=" + browseName);
    }

    private static @NotNull ReferenceDescription methodReference(final @NotNull String browseName) {
        return new ReferenceDescription(
                NodeIds.HasComponent,
                true,
                ExpandedNodeId.parse("ns=2;s=" + browseName),
                // Namespace 0: these are specification-defined names, and Browsing.isStandardName requires it.
                new QualifiedName(0, browseName),
                LocalizedText.english(browseName),
                NodeClass.Method,
                ExpandedNodeId.NULL_VALUE);
    }
}
