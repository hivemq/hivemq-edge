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
package com.hivemq.edge.adapters.opcua;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaServiceFaultListener;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaSubscriptionLifecycleHandler;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Review-07 finding 5: stopping during condition verification must wait for only the phase already running.
 * <p>
 * A condition tag has as many as three sequential ten-second phases: type verification, notifier resolution,
 * and the preflight of an explicitly declared notifier. The teardown flag used to be read only between tags,
 * so a stop arriving in the first phase still allowed the next two to start and made the documented one-call
 * bound untrue.
 * <p>
 * Each test holds one phase's future, calls the real {@link OpcUaClientConnection#stop()}, and then releases
 * that answer. The handler is attached to the connection through the same early reference production uses;
 * only the server services are mocked so the exact phase boundary can be controlled without sleeping.
 */
class OpcUaConditionVerificationShutdownTest {

    private static final @NotNull String CONDITION = "ns=2;s=Boiler1.HighTemp";
    private static final @NotNull String NOTIFIER = "ns=2;s=BoilerArea";

    private @NotNull OpcUaClient client;
    private @NotNull FakeEventService events;
    private @NotNull OpcUaSpecificAdapterConfig config;

    @BeforeEach
    void setUp() {
        client = mock(OpcUaClient.class);
        events = new FakeEventService();
        config = new OpcUaSpecificAdapterConfig(
                "opc.tcp://localhost:4840",
                false,
                null,
                null,
                null,
                OpcUaToMqttConfig.defaultOpcUaToMqttConfig(),
                null,
                ConnectionOptions.defaultConnectionOptions());
        when(client.getNamespaceTable()).thenReturn(new NamespaceTable());
    }

    @Test
    @Timeout(30)
    void stopDuringTypeVerificationDoesNotStartNotifierResolution() throws Exception {
        final CountDownLatch typeVerificationStarted = new CountDownLatch(1);
        final CompletableFuture<BrowseResult> typeAnswer = new CompletableFuture<>();
        when(client.browseAsync(any(BrowseDescription.class))).thenAnswer(ignored -> {
            typeVerificationStarted.countDown();
            return typeAnswer;
        });

        final Verification verification = startVerification(conditionTag(null));
        assertThat(typeVerificationStarted.await(10, SECONDS))
                .as("precondition: verification is waiting for the condition type")
                .isTrue();

        verification.connection().stop();
        typeAnswer.complete(typeDefinitionAnswer());

        assertThat(verification.result().get(10, SECONDS))
                .as("a stopped connection must not carry the tag into its next phase")
                .isEmpty();
        verify(client, times(1)).browseAsync(any(BrowseDescription.class));
        verify(client, never()).readAsync(anyDouble(), any(), any());
        assertThat(events.readEvents(null, null))
                .as("teardown is not a configuration failure against the tag")
                .isEmpty();
    }

    @Test
    @Timeout(30)
    void stopDuringNotifierResolutionDoesNotStartAnotherVerificationPhase() throws Exception {
        final CountDownLatch notifierResolutionStarted = new CountDownLatch(1);
        final CompletableFuture<BrowseResult> notifierAnswer = new CompletableFuture<>();
        final AtomicInteger browseNumber = new AtomicInteger();
        when(client.browseAsync(any(BrowseDescription.class))).thenAnswer(ignored -> {
            if (browseNumber.getAndIncrement() == 0) {
                return CompletableFuture.completedFuture(typeDefinitionAnswer());
            }
            notifierResolutionStarted.countDown();
            return notifierAnswer;
        });

        final Verification verification = startVerification(conditionTag(null));
        assertThat(notifierResolutionStarted.await(10, SECONDS))
                .as("precondition: the type passed and notifier resolution is waiting")
                .isTrue();

        verification.connection().stop();
        notifierAnswer.completeExceptionally(new IllegalStateException("released after stop"));

        assertThat(verification.result().get(10, SECONDS)).isEmpty();
        verify(client, times(2)).browseAsync(any(BrowseDescription.class));
        verify(client, never()).readAsync(anyDouble(), any(), any());
        assertThat(events.readEvents(null, null)).isEmpty();
    }

    @Test
    @Timeout(30)
    void stopDuringDeclaredNotifierPreflightDoesNotAdmitTheVerifiedTag() throws Exception {
        final CountDownLatch notifierPreflightStarted = new CountDownLatch(1);
        final CompletableFuture<ReadResponse> notifierAnswer = new CompletableFuture<>();
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(typeDefinitionAnswer()));
        when(client.readAsync(anyDouble(), any(), any())).thenAnswer(ignored -> {
            notifierPreflightStarted.countDown();
            return notifierAnswer;
        });

        final Verification verification = startVerification(conditionTag(NOTIFIER));
        assertThat(notifierPreflightStarted.await(10, SECONDS))
                .as("precondition: the declared notifier is in its final preflight")
                .isTrue();

        verification.connection().stop();
        notifierAnswer.complete(subscribableNotifierAnswer());

        assertThat(verification.result().get(10, SECONDS))
                .as("an answer from the closing connection must not produce a monitored-item candidate")
                .isEmpty();
        verify(client, times(1)).browseAsync(any(BrowseDescription.class));
        verify(client, times(1)).readAsync(anyDouble(), any(), any());
        assertThat(events.readEvents(null, null)).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────────────────

    private @NotNull Verification startVerification(final @NotNull OpcuaTag tag) {
        final ProtocolAdapterMetricsService metrics = mock(ProtocolAdapterMetricsService.class);
        final var handler = new OpcUaSubscriptionLifecycleHandler(
                metrics,
                mock(ProtocolAdapterTagStreamingService.class),
                events,
                "test-adapter",
                List.of(tag),
                client,
                config);
        final var connection = new OpcUaClientConnection(
                "test-adapter",
                List.of(tag),
                mock(ProtocolAdapterState.class),
                mock(ProtocolAdapterTagStreamingService.class),
                events,
                metrics,
                config,
                new OpcUaServiceFaultListener(metrics, events, "test-adapter", () -> {}, true),
                ConnectionOwnership.alwaysCurrent());
        attachHandler(connection, handler);
        return new Verification(connection, CompletableFuture.supplyAsync(() -> verifyTag(handler, tag)));
    }

    @SuppressWarnings("unchecked")
    private static void attachHandler(
            final @NotNull OpcUaClientConnection connection, final @NotNull OpcUaSubscriptionLifecycleHandler handler) {
        try {
            final Field field = OpcUaClientConnection.class.getDeclaredField("subscriptionHandler");
            field.setAccessible(true);
            ((AtomicReference<OpcUaSubscriptionLifecycleHandler>) field.get(connection)).set(handler);
        } catch (final ReflectiveOperationException e) {
            throw new LinkageError("the connection no longer exposes its early subscription-handler reference", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Optional<?> verifyTag(
            final @NotNull OpcUaSubscriptionLifecycleHandler handler, final @NotNull OpcuaTag tag) {
        try {
            final Method verify = OpcUaSubscriptionLifecycleHandler.class.getDeclaredMethod("verify", OpcuaTag.class);
            verify.setAccessible(true);
            return (Optional<?>) verify.invoke(handler, tag);
        } catch (final InvocationTargetException e) {
            throw new AssertionError("verification threw, and it is documented as total", e.getCause());
        } catch (final ReflectiveOperationException e) {
            throw new LinkageError("the handler no longer verifies tags through 'verify(OpcuaTag)'", e);
        }
    }

    private static @NotNull OpcuaTag conditionTag(final @Nullable String notifierNode) {
        return new OpcuaTag(
                "boiler-high-temp",
                "",
                new OpcuaTagDefinition(
                        CONDITION, OpcuaTagKind.CONDITION, OpcuaConditionType.ALARM_CONDITION, notifierNode));
    }

    private static @NotNull BrowseResult typeDefinitionAnswer() {
        final ReferenceDescription type = new ReferenceDescription(
                NodeIds.HasTypeDefinition,
                true,
                ExpandedNodeId.parse(NodeIds.AlarmConditionType.toParseableString()),
                new QualifiedName(0, "AlarmConditionType"),
                LocalizedText.english("AlarmConditionType"),
                NodeClass.ObjectType,
                ExpandedNodeId.NULL_VALUE);
        return new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, new ReferenceDescription[] {type});
    }

    private static @NotNull ReadResponse subscribableNotifierAnswer() {
        return new ReadResponse(
                new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null),
                new DataValue[] {
                    new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null),
                    new DataValue(new Variant(uint(1)), StatusCode.GOOD, null)
                },
                null);
    }

    private record Verification(
            @NotNull OpcUaClientConnection connection,
            @NotNull CompletableFuture<Optional<?>> result) {}
}
