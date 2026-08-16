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
import static org.mockito.Mockito.mock;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Which configured fields each tag kind is actually judged on.
 * <p>
 * Review-02 finding 8. Verification branched on "everything that is not a CONDITION", which quietly made
 * VALUE and REFRESH share whatever EVENT_SUBSCRIPTION needed — including {@code sourceNode} and
 * {@code conditionNode}, two fields that exist only to become operands in a query tag's where clause. A stale
 * or hand-authored value in either one therefore dropped an otherwise healthy ordinary tag, over a field its
 * filter never consults. A migrated configuration is exactly where such a leftover lives.
 * <p>
 * The fix is an exhaustive switch on the kind, so a fifth kind cannot inherit another's checks by default.
 * These tests are what says the branches stayed distinct.
 * <p>
 * Driven through the private {@code verify} by reflection. It is the per-tag boundary and answers with an
 * {@code Optional}, so it is directly observable — and the alternative, an adapter against the embedded
 * server, would test the same one line through several hundred.
 */
class OpcUaTagVerificationPerKindTest {

    /** Not a node id in any spelling. Deliberately not "ns=2;s=", which Milo parses as an empty identifier. */
    private static final @NotNull String NOT_A_NODE_ID = "left-over-from-an-earlier-config";

    @Test
    void aValueTagIsNotJudgedOnFieldsOnlyAQueryTagReads() {
        // The finding. Both predicates are nonsense and neither is read by a value tag's monitored item,
        // which watches the Value attribute of one node and has no filter at all.
        final OpcuaTagDefinition definition = new OpcuaTagDefinition(
                "ns=2;s=Boiler1.Temperature", OpcuaTagKind.VALUE, null, null, NOT_A_NODE_ID, NOT_A_NODE_ID, null);

        assertThat(verify(new OpcuaTag("boiler-temperature", "", definition)))
                .as("an ordinary value tag must not be dropped over a query tag's fields")
                .isPresent();
    }

    @Test
    void andNeitherIsARefreshTag() {
        // A refresh tag reads even less of its configuration: its item goes on the Server object and its
        // filter admits nothing, so its own node is not parsed either.
        final OpcuaTagDefinition definition = new OpcuaTagDefinition(
                "ns=0;i=2253", OpcuaTagKind.REFRESH, null, null, NOT_A_NODE_ID, NOT_A_NODE_ID, null);

        assertThat(verify(new OpcuaTag("refresh", "", definition))).isPresent();
    }

    @Test
    void aQueryTagIsStillJudgedOnThem() {
        // The control, and the reason the fields are parsed at all: on this kind they become operands in the
        // where clause, so one that is not a node id is a filter the server cannot be asked for. Rejecting
        // the tag is the right answer here and only here.
        final OpcuaTagDefinition definition = new OpcuaTagDefinition(
                "ns=2;s=BoilerArea", OpcuaTagKind.EVENT_SUBSCRIPTION, null, null, NOT_A_NODE_ID, null, null);

        assertThat(verify(new OpcuaTag("area-alarms", "", definition)))
                .as("a malformed predicate is a filter the server cannot be asked for")
                .isEmpty();
    }

    @Test
    void aValueTagIsStillJudgedOnItsOwnNode() {
        // The other control. Narrowing which fields are read must not stop the one field that is read from
        // being read: a value tag's node is what its monitored item watches.
        final OpcuaTagDefinition definition = new OpcuaTagDefinition(NOT_A_NODE_ID);

        assertThat(verify(new OpcuaTag("boiler-temperature", "", definition)))
                .as("the node a value tag actually uses still has to be one")
                .isEmpty();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * Runs the handler's per-tag verification for one tag.
     * <p>
     * The client is a mock: none of these cases reaches a server call. A value or refresh tag never did, and
     * the two rejections happen while parsing configured strings, before anything is asked of the device.
     */
    @SuppressWarnings("unchecked")
    private static @NotNull Optional<?> verify(final @NotNull OpcuaTag tag) {
        try {
            final Method verify = OpcUaSubscriptionLifecycleHandler.class.getDeclaredMethod("verify", OpcuaTag.class);
            verify.setAccessible(true);
            return (Optional<?>) verify.invoke(handlerFor(tag), tag);
        } catch (final InvocationTargetException e) {
            throw new AssertionError("verification threw, and it is documented as total", e.getCause());
        } catch (final ReflectiveOperationException e) {
            // LinkageError rather than AssertionError: this fails only when the method has been renamed or
            // its signature changed, which is a broken assumption about the class rather than a failed
            // assertion about its behaviour.
            throw new LinkageError("the handler no longer verifies tags through 'verify(OpcuaTag)'", e);
        }
    }

    private static @NotNull OpcUaSubscriptionLifecycleHandler handlerFor(final @Nullable OpcuaTag tag) {
        return new OpcUaSubscriptionLifecycleHandler(
                mock(ProtocolAdapterMetricsService.class),
                mock(ProtocolAdapterTagStreamingService.class),
                new FakeEventService(),
                "test-adapter",
                tag == null ? List.of() : List.of(tag),
                mock(OpcUaClient.class),
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
