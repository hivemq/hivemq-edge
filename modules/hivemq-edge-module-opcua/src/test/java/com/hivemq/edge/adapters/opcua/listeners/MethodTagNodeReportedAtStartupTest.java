/*
 * Copyright 2019-present HiveMQ GmbH
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

import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import java.util.List;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// That a METHOD tag whose configured node is not a node id is reported when the adapter starts.
///
/// A METHOD tag names an object whose methods are called southbound. It is excluded from the observable tags
/// before per-tag verification runs, correctly -- an object has no Value attribute to sample and emits no
/// events, so there is no monitored item it could yield, and the `case METHOD` arm in `verify` is unreachable.
/// The side effect was that **nothing** looked at such a tag during startup, not even to parse its node: an
/// operator who mistyped it learned of the mistake from the first request, which may be hours later and is
/// reported to whoever sent that request rather than to whoever wrote the configuration.
///
/// Only the parse is checked at startup. Whether the node exists, and whether it is an object with methods,
/// stays lazy on purpose: the browse-metadata warm-up may fail without failing the connection, so browsing here
/// would leave an adapter that is connected and not browse-ready reporting every method that exists as absent.
class MethodTagNodeReportedAtStartupTest {

    private static final @NotNull String ADAPTER_ID = "test-adapter";

    private final @NotNull FakeEventService events = new FakeEventService();

    private static @NotNull OpcuaTag tag(
            final @NotNull String name, final @NotNull String node, final @NotNull OpcuaTagKind kind) {
        return new OpcuaTag(name, "", new OpcuaTagDefinition(node, kind));
    }

    /// Calls subscribe, which performs the configuration check before it asks the client for anything. The
    /// client is a mock, so creating the subscription fails and subscribe answers empty -- which is irrelevant
    /// here: the check has already run.
    private void startWith(final @NotNull List<OpcuaTag> tags) {
        final var handler = new OpcUaSubscriptionLifecycleHandler(
                mock(ProtocolAdapterMetricsService.class),
                mock(ProtocolAdapterTagStreamingService.class),
                events,
                ADAPTER_ID,
                tags,
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
        try {
            handler.subscribe(mock(OpcUaClient.class));
        } catch (final RuntimeException ignored) {
            // The mocked client cannot create a subscription. The check under test runs before that.
        }
    }

    private @NotNull List<String> warnings() {
        return events.readEvents(null, null).stream().map(Event::getMessage).toList();
    }

    @Test
    void aMethodTagWithAnUnparseableNode_isReportedWhenTheAdapterStarts() {
        // The decisive test. Before this, startup said nothing at all about such a tag.
        startWith(List.of(tag("boiler-methods", "not-a-node-id", OpcuaTagKind.METHOD)));

        assertThat(warnings())
                .anySatisfy(message ->
                        assertThat(message).contains("boiler-methods").contains("not-a-node-id"));
    }

    @Test
    void aMethodTagWithAGoodNode_isNotReported() {
        // The counterpart: the check must not warn about every method tag, which would make the warning
        // worthless. Whether `ns=2;s=Boiler1` is really an object with methods is not asked here.
        startWith(List.of(tag("boiler-methods", "ns=2;s=Boiler1", OpcuaTagKind.METHOD)));

        assertThat(warnings()).noneSatisfy(message -> assertThat(message).contains("boiler-methods"));
    }

    @Test
    void everyBadMethodTagIsReported_notJustTheFirst() {
        // One unusable tag must not hide another: an operator fixing the configuration needs the whole list,
        // not one entry per restart.
        startWith(List.of(
                tag("first", "nonsense-one", OpcuaTagKind.METHOD), tag("second", "nonsense-two", OpcuaTagKind.METHOD)));

        assertThat(warnings()).anySatisfy(message -> assertThat(message).contains("first"));
        assertThat(warnings()).anySatisfy(message -> assertThat(message).contains("second"));
    }

    @Test
    void theMessageSaysTheTagCannotServeARequest_notThatItWasNotSubscribed() {
        // "did not subscribe tag" is true of every method tag by design, so it would say nothing about this
        // one. What the operator can act on is that the tag is unusable.
        startWith(List.of(tag("boiler-methods", "not-a-node-id", OpcuaTagKind.METHOD)));

        assertThat(warnings()).anySatisfy(message -> assertThat(message).contains("cannot serve a request"));
    }
}
