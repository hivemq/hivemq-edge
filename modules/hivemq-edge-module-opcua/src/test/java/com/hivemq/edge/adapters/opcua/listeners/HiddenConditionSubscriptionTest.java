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
package com.hivemq.edge.adapters.opcua.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Subscribing a Condition on a server that does not expose its Condition instances.
 * <p>
 * Review-03 finding 1, at the half the writer cannot reach. OPC 10000-9 §4.3 permits a server to keep its
 * Condition instances out of the AddressSpace and deliver them through events alone, and review 02 added the
 * standard-nodeset MethodId so commands work against such a server. None of that was reachable: verification
 * browses the condition for its {@code HasTypeDefinition}, the server answers {@code Bad_NodeIdUnknown}
 * because there is no node there, and the tag was dropped before it was ever subscribed.
 * <p>
 * <b>The waiver is gated on {@code notifierNode}, and these tests are mostly about the gate.</b> A typo in
 * {@code node} produces the identical status, so accepting every unverifiable condition would trade a clear
 * rejection at start for a tag that subscribes and stays silent forever — indistinguishable from an alarm that
 * has not fired. Naming the notifier is how an operator says they meant it, and it is a field this server
 * model forces anyway: the notifier walk starts at the condition, so it cannot run when the condition is not
 * there to walk from.
 * <p>
 * Driven through the handler's private {@code verify} by reflection, as {@link OpcUaTagVerificationPerKindTest}
 * is and for the same reason: it is the per-tag boundary, it answers with an {@code Optional}, and the
 * alternative would test this one decision through several hundred lines of adapter start.
 */
class HiddenConditionSubscriptionTest {

    private static final @NotNull String CONDITION = "ns=2;s=Boiler1.HighTemp";
    private static final @NotNull String NOTIFIER = "ns=2;s=BoilerArea";

    private @NotNull OpcUaClient client;
    private @NotNull FakeEventService events;

    @BeforeEach
    void setUp() {
        client = mock(OpcUaClient.class);
        events = new FakeEventService();
        when(client.getNamespaceTable()).thenReturn(new NamespaceTable());
        // The server model under test: no such node, so no references and no type definition to compare.
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        new BrowseResult(new StatusCode(StatusCodes.Bad_NodeIdUnknown), ByteString.NULL_VALUE, null)));
        // The notifier, by contrast, is an ordinary object that does deliver events.
        when(client.readAsync(anyDouble(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new ReadResponse(
                        new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null),
                        new DataValue[] {
                            new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null),
                            new DataValue(new Variant(uint(1)), StatusCode.GOOD, null)
                        },
                        null)));
    }

    @Test
    void aHiddenConditionThatNamesItsNotifierIsSubscribed() {
        // The finding. Every other piece of the hidden-instance workflow was in place; this rejection is what
        // made it unreachable end to end.
        assertThat(verify(tag(NOTIFIER)))
                .as("OPC 10000-9 §4.3 permits this server, and Edge now supports the rest of it")
                .isPresent();
    }

    @Test
    void andNothingIsReportedAgainstIt() {
        // A warning in the log, not an adapter event. The tag is working as configured; the operator is told
        // what could not be checked, but there is no failure to report against the adapter.
        verify(tag(NOTIFIER));

        assertThat(events.readEvents(null, null)).isEmpty();
    }

    @Test
    void aHiddenConditionWithNoNotifierIsStillDropped() {
        // The gate. Without it a mistyped node id -- which draws the same Bad_NodeIdUnknown -- would be
        // waved through into a tag that subscribes cleanly and never publishes.
        assertThat(verify(tag(null)))
                .as("an unverifiable condition is only accepted when the operator has said they meant it")
                .isEmpty();
    }

    @Test
    void andTheRejectionNamesBothWaysOut() {
        // The two things the operator could have meant, because from here they are indistinguishable: either
        // the server is one of those and the notifier has to be named, or the node id is wrong.
        verify(tag(null));

        assertThat(events.readEvents(null, null)).singleElement().satisfies(event -> assertThat(event.getMessage())
                .contains("notifierNode")
                .contains("§4.3")
                .contains("wrong node id is refused with the same status"));
    }

    @Test
    void aRefusalThatIsNotAboutTheNodesExistenceIsStillARejection() {
        // The boundary of the waiver. A server that will not let this session browse the node has said
        // nothing about whether the node is there, so naming a notifier does not license subscribing to it.
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(new BrowseResult(
                        new StatusCode(StatusCodes.Bad_UserAccessDenied), ByteString.NULL_VALUE, null)));

        assertThat(verify(tag(NOTIFIER))).isEmpty();
        assertThat(events.readEvents(null, null)).singleElement().satisfies(event -> assertThat(event.getMessage())
                .contains("could not read the type"));
    }

    @Test
    void aNamedNotifierThatCannotDeliverEventsIsStillRejected() {
        // The waiver covers the condition, not the notifier. The one node this tag names that the server does
        // expose is still checked -- otherwise the gate would admit exactly the typo it exists to catch.
        when(client.readAsync(anyDouble(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new ReadResponse(
                        new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null),
                        new DataValue[] {
                            new DataValue(new Variant(NodeClass.Variable), StatusCode.GOOD, null),
                            new DataValue(Variant.NULL_VALUE, new StatusCode(StatusCodes.Bad_NodeIdUnknown), null)
                        },
                        null)));

        assertThat(verify(tag(NOTIFIER))).isEmpty();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    private static @NotNull OpcuaTag tag(final @Nullable String notifierNode) {
        return new OpcuaTag(
                "boiler-high-temp",
                "",
                new OpcuaTagDefinition(
                        CONDITION, OpcuaTagKind.CONDITION, OpcuaConditionType.ALARM_CONDITION, notifierNode));
    }

    @SuppressWarnings("unchecked")
    private @NotNull Optional<?> verify(final @NotNull OpcuaTag tag) {
        try {
            final Method verify = OpcUaSubscriptionLifecycleHandler.class.getDeclaredMethod("verify", OpcuaTag.class);
            verify.setAccessible(true);
            return (Optional<?>) verify.invoke(handlerFor(tag), tag);
        } catch (final InvocationTargetException e) {
            throw new AssertionError("verification threw, and it is documented as total", e.getCause());
        } catch (final ReflectiveOperationException e) {
            throw new LinkageError("the handler no longer verifies tags through 'verify(OpcuaTag)'", e);
        }
    }

    private @NotNull OpcUaSubscriptionLifecycleHandler handlerFor(final @NotNull OpcuaTag tag) {
        return new OpcUaSubscriptionLifecycleHandler(
                mock(ProtocolAdapterMetricsService.class),
                mock(ProtocolAdapterTagStreamingService.class),
                events,
                "test-adapter",
                List.of(tag),
                client,
                new OpcUaSpecificAdapterConfig(
                        "opc.tcp://localhost:4840",
                        false,
                        null,
                        null,
                        null,
                        OpcUaToMqttConfig.defaultOpcUaToMqttConfig(),
                        null,
                        ConnectionOptions.defaultConnectionOptions()));
    }
}
