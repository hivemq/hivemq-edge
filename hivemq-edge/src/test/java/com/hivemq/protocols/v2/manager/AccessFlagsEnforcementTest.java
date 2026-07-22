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
package com.hivemq.protocols.v2.manager;

import static com.hivemq.adapter.sdk.api.v2.node.AccessTriState.NO;
import static com.hivemq.adapter.sdk.api.v2.node.AccessTriState.WILL_NOT_USE;
import static com.hivemq.adapter.sdk.api.v2.node.AccessTriState.YES;
import static com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.adapter;
import static com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.tag;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.config.AccessFlagsEntity;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.TestDataPointFactory;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.TestProtocolAdapterFactory;
import com.hivemq.protocols.v2.runtime.FakeClock;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.view.TagStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperCommand;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEventListener;
import com.hivemq.protocols.v2.wrapper.TagAspectActivationPreference;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The per-tag {@code <access>} model is enforced, not decorative (EDG-824 #14): a tag whose access
 * flags forbid reading is never polled, never subscribed, and never delivered northbound; a tag whose flags forbid
 * writing accepts no southbound write. {@code readable} is the master read gate, {@code pollable} /
 * {@code subscribable} select the transports under it, and only {@code YES} permits — {@code NO} and
 * {@code WILL_NOT_USE} both forbid.
 */
class AccessFlagsEnforcementTest {

    private static final @NotNull AccessFlagsEntity ALL_YES = new AccessFlagsEntity(YES, YES, YES, YES);

    private static @NotNull AccessFlagsEntity access(
            final @NotNull com.hivemq.adapter.sdk.api.v2.node.AccessTriState readable,
            final @NotNull com.hivemq.adapter.sdk.api.v2.node.AccessTriState writable,
            final @NotNull com.hivemq.adapter.sdk.api.v2.node.AccessTriState pollable,
            final @NotNull com.hivemq.adapter.sdk.api.v2.node.AccessTriState subscribable) {
        return new AccessFlagsEntity(readable, writable, pollable, subscribable);
    }

    // ── activation derivation ───────────────────────────────────────────────────────────────

    @Test
    void readableNo_deactivatesTheReadAspect_regardlessOfTransports() {
        final ProtocolAdapterEntity entity = adapter("a")
                .tags(tag("t").access(access(NO, YES, YES, YES)).build())
                .build();

        final TagAspectActivationPreference preference =
                ProtocolAdapterConfigSupport.activationOf(entity).get("t");
        assertThat(preference.readActivated()).isFalse();
        assertThat(preference.writeActivated()).isTrue();
    }

    @Test
    void noPermittedReadTransport_deactivatesTheReadAspect() {
        // readable=YES but neither poll nor subscribe is permitted — there is no way to read the point.
        final ProtocolAdapterEntity entity = adapter("a")
                .tags(tag("t").access(access(YES, YES, NO, NO)).build())
                .build();

        assertThat(ProtocolAdapterConfigSupport.activationOf(entity).get("t").readActivated())
                .isFalse();
    }

    @Test
    void doublyForbidden_deactivatesTheReadAspect() {
        final ProtocolAdapterEntity entity = adapter("a")
                .tags(tag("t").access(access(NO, YES, NO, NO)).build())
                .build();

        assertThat(ProtocolAdapterConfigSupport.activationOf(entity).get("t").readActivated())
                .isFalse();
    }

    @Test
    void permittedRead_keepsTheReadAspectActivated() {
        final ProtocolAdapterEntity entity =
                adapter("a").tags(tag("t").access(ALL_YES).build()).build();

        assertThat(ProtocolAdapterConfigSupport.activationOf(entity).get("t").readActivated())
                .isTrue();
    }

    @Test
    void writableNoOrWillNotUse_deactivatesTheWriteAspect() {
        final ProtocolAdapterEntity noEntity = adapter("a")
                .tags(tag("t").access(access(YES, NO, YES, YES)).build())
                .build();
        final ProtocolAdapterEntity willNotUseEntity = adapter("a")
                .tags(tag("t").access(access(YES, WILL_NOT_USE, YES, YES)).build())
                .build();

        assertThat(ProtocolAdapterConfigSupport.activationOf(noEntity).get("t").writeActivated())
                .isFalse();
        assertThat(ProtocolAdapterConfigSupport.activationOf(willNotUseEntity)
                        .get("t")
                        .writeActivated())
                .isFalse();
    }

    @Test
    void willNotUse_forbidsReadingLikeNo() {
        final ProtocolAdapterEntity entity = adapter("a")
                .tags(tag("t").access(access(WILL_NOT_USE, YES, YES, YES)).build())
                .build();

        assertThat(ProtocolAdapterConfigSupport.activationOf(entity).get("t").readActivated())
                .isFalse();
    }

    // ── effective transports carried into the node/tag pair ─────────────────────────────────

    @Test
    void translateNodes_carriesOnlyThePermittedTransports() {
        final FakeClock clock = new FakeClock();
        final ManualDispatcher dispatcher = new ManualDispatcher();
        final DefaultProtocolAdapterWrapperFactory factory = new DefaultProtocolAdapterWrapperFactory(
                clock, dispatcher, new MetricRegistry(), new TestDataPointFactory(), new ObjectMapper(), 100);
        final TestProtocolAdapterFactory sdkFactory =
                new TestProtocolAdapterFactory(ProtocolAdapterManagerTestSupport.TEST_PROTOCOL_ID);
        final ProtocolAdapterEntity entity = adapter("a")
                .tags(
                        tag("allowed").access(ALL_YES).build(),
                        tag("no-poll").access(access(YES, YES, NO, YES)).build(),
                        tag("no-read").access(access(NO, YES, YES, YES)).build())
                .build();

        final List<NodeTagPair> nodes = factory.translateNodes(entity, sdkFactory);

        assertThat(pairOf(nodes, "allowed").tag().pollable()).isTrue();
        assertThat(pairOf(nodes, "no-poll").tag().pollable()).isFalse();
        // readable=NO forbids every read transport, whatever the transport flags say
        assertThat(pairOf(nodes, "no-read").tag().pollable()).isFalse();
        assertThat(pairOf(nodes, "no-read").tag().subscribable()).isFalse();
    }

    // ── end to end: the forbidden tag never operates while its sibling produces ─────────────

    private FakeClock clock;
    private ManualDispatcher dispatcher;
    private DefaultProtocolAdapterWrapperFactory factory;
    private TestProtocolAdapterFactory sdkFactory;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
        dispatcher = new ManualDispatcher();
        factory = new DefaultProtocolAdapterWrapperFactory(
                clock, dispatcher, new MetricRegistry(), new TestDataPointFactory(), new ObjectMapper(), 100);
        sdkFactory = new TestProtocolAdapterFactory(ProtocolAdapterManagerTestSupport.TEST_PROTOCOL_ID);
    }

    @Test
    void nonReadableTag_staysDeactivatedWhileItsSiblingProduces() {
        final ProtocolAdapterEntity entity = adapter("a")
                .northboundActivated(true)
                .tags(
                        tag("open").access(ALL_YES).build(),
                        tag("forbidden").access(access(NO, NO, YES, YES)).build())
                .northboundMapping("open", "plant/a/open")
                .northboundMapping("forbidden", "plant/a/forbidden")
                .build();

        final ProtocolAdapterContainer managed =
                factory.create(entity, sdkFactory, ProtocolAdapterWrapperEventListener.NONE);
        managed.handle()
                .wrapperSender()
                .tell(new ProtocolAdapterWrapperCommand.ApplyActivation(
                        ProtocolAdapterConfigSupport.goalOf(entity),
                        ProtocolAdapterConfigSupport.activationOf(entity)));
        dispatcher.drainAll();

        final AdapterStatusSnapshot snapshot = managed.handle().snapshot().get();
        assertThat(snapshot).isNotNull();
        final Map<String, TagStatusSnapshot> byName = Map.of(
                snapshot.tags().get(0).tagName(), snapshot.tags().get(0),
                snapshot.tags().get(1).tagName(), snapshot.tags().get(1));
        // The permitted sibling operates on the poll cadence; the forbidden tag is DEACTIVATED — never verified,
        // never polled, and a routed value would be rejected, so nothing of it ever reaches MQTT.
        assertThat(byName.get("open").readAspectStateName()).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(byName.get("forbidden").readAspectStateName()).isEqualTo("DEACTIVATED");
        assertThat(byName.get("forbidden").writeAspectStateName()).isEqualTo("DEACTIVATED");

        managed.close();
    }

    private static @NotNull NodeTagPair pairOf(final @NotNull List<NodeTagPair> nodes, final @NotNull String tagName) {
        return nodes.stream()
                .filter(pair -> pair.tag().name().equals(tagName))
                .findFirst()
                .orElseThrow();
    }
}
