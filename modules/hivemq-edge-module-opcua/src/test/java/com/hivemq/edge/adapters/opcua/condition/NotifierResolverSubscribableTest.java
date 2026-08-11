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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Validation of an event target a tag names directly, rather than one Edge walked to.
 * <p>
 * Review finding 9. Two kinds of tag name their target themselves: an {@code EVENT_SUBSCRIPTION} tag, whose
 * {@code node} <em>is</em> the notifier, and a {@code CONDITION} tag carrying an explicit
 * {@code notifierNode}. Neither was checked against the device. The Linear end-state asks for NodeClass,
 * {@code HasTypeDefinition} and {@code EventNotifier} verification; only condition <em>types</em> were being
 * verified.
 * <p>
 * The failure mode that matters is the quiet one. Depending on the server, an invalid target either fails the
 * whole monitored-item batch — taking healthy tags with it — or is accepted into a tag that subscribes
 * cleanly and then never produces anything, which is indistinguishable from an alarm that has not fired.
 * <p>
 * <b>Only a definite answer rejects.</b> A server that will not say leaves the tag alone with a warning:
 * refusing on silence would break servers that restrict attribute reads on nodes that are perfectly good.
 */
class NotifierResolverSubscribableTest {

    private static final @NotNull NodeId TARGET = NodeId.parse("ns=2;s=BoilerArea");

    private @NotNull OpcUaClient client;

    @BeforeEach
    void setUp() {
        client = mock(OpcUaClient.class);
    }

    @Test
    void aRealNotifierIsAccepted() {
        answers(good(NodeClass.Object), good(uint(1))); // SubscribeToEvents set

        assertThat(check()).isEmpty();
    }

    @Test
    void aViewIsAcceptedToo() {
        // OPC 10000-3 §7.17: the source of a HasEventSource "shall be an Object or View". Masking to Objects
        // would drop a View-organised area.
        answers(good(NodeClass.View), good(uint(1)));

        assertThat(check()).isEmpty();
    }

    @Test
    void aVariableIsRejected() {
        // The commonest form of the mistake: a value tag's node id pasted into an event tag. A Variable has
        // no EventNotifier attribute at all, so it can never deliver events.
        answers(good(NodeClass.Variable), good(uint(0)));

        assertThat(check()).hasValueSatisfying(reason -> assertThat(reason)
                .contains("Variable")
                .contains("only an Object or a View can deliver events"));
    }

    @Test
    void anObjectWithTheSubscribeToEventsBitClearIsRejected() {
        // An ordinary object, not a notifier. The bit is the only thing that makes a node a valid target for
        // an event monitored item, and a server with it clear will simply never send anything.
        answers(good(NodeClass.Object), good(uint(0)));

        assertThat(check())
                .hasValueSatisfying(reason ->
                        assertThat(reason).contains("SubscribeToEvents").contains("will not deliver events"));
    }

    @Test
    void theHistoryReadBitAloneIsNotEnough() {
        // EventNotifier is a bit mask: bit 0 is SubscribeToEvents, bit 2 HistoryRead. A node offering only
        // history is not subscribable, and reading the attribute as a boolean would accept it.
        answers(good(NodeClass.Object), good(uint(4)));

        assertThat(check()).isPresent();
    }

    @Test
    void aServerThatWillNotReportTheNodeClassIsGivenTheBenefitOfTheDoubt() {
        // Not a statement that the node is unusable -- only that this server does not answer attribute reads
        // on it. Refusing here would break a working configuration over a permissions setting.
        answers(bad(), good(uint(1)));

        assertThat(check()).isEmpty();
    }

    @Test
    void aServerThatWillNotReportTheEventNotifierIsGivenTheBenefitOfTheDoubt() {
        answers(good(NodeClass.Object), bad());

        assertThat(check()).isEmpty();
    }

    @Test
    void aFailedReadIsGivenTheBenefitOfTheDoubt() {
        when(client.readAsync(anyDouble(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("not connected")));

        assertThat(check())
                .as("a transport failure says nothing about the node, so it must not reject the tag")
                .isEmpty();
    }

    @Test
    void aTruncatedResponseIsGivenTheBenefitOfTheDoubt() {
        when(client.readAsync(anyDouble(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(response(good(NodeClass.Object))));

        assertThat(check()).isEmpty();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    private @NotNull Optional<String> check() {
        return NotifierResolver.checkSubscribable(client, TARGET, "area-alarms").join();
    }

    private void answers(final @NotNull DataValue nodeClass, final @NotNull DataValue eventNotifier) {
        when(client.readAsync(anyDouble(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(response(nodeClass, eventNotifier)));
    }

    private static @NotNull ReadResponse response(final @NotNull DataValue... results) {
        return new ReadResponse(new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null), results, null);
    }

    private static @NotNull DataValue good(final @NotNull Object value) {
        return new DataValue(new Variant(value), StatusCode.GOOD, null);
    }

    private static @NotNull DataValue bad() {
        return new DataValue(Variant.NULL_VALUE, new StatusCode(StatusCodes.Bad_UserAccessDenied), null);
    }

    /** Milo may hand back the NodeClass either as the enum or as its integer encoding. */
    @Test
    void aNodeClassEncodedAsAnIntegerIsUnderstood() {
        answers(good(NodeClass.Variable.getValue()), good(uint(0)));

        assertThat(check()).isPresent();
    }
}
