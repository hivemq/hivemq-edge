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
package com.hivemq.protocols.v2.wrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseNode;
import com.hivemq.adapter.sdk.api.v2.model.ResolvedAttributes;
import com.hivemq.adapter.sdk.api.v2.node.AccessFlags;
import com.hivemq.adapter.sdk.api.v2.node.AccessTriState;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The wrapper half of the browse bridge on a {@link WrapperTestFixture}: a browse runs only when
 * the adapter is {@code CONNECTED} with no browse in flight; it is completed from the protocol adapter's browse
 * result, failed on its deadline, and failed when the connection is lost.
 */
class ProtocolAdapterWrapperBrowseTest {

    @Test
    void browseWhenConnected_walksDiscoverThenResolveAndCompletesWithSelectableVariables() {
        final WrapperTestFixture fixture = connectedFixture();
        final CompletableFuture<List<BrowseNode>> future = new CompletableFuture<>();

        // Browse below "root"; the engine issues browse(requestId=1, filter, 0) to the adapter.
        fixture.send(
                new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(WrapperTestSupport.node("root")), future));
        assertThat(fixture.commands()).contains("browse");
        assertThat(future).isNotDone();

        // DISCOVER: one page carrying a selectable variable, no continuation ⇒ DISCOVER complete.
        fixture.output.browsePage(
                1,
                List.of(new BrowseNode(WrapperTestSupport.node("temperature"), NodeType.VALUE, true, "temperature")),
                null);
        fixture.drain();
        // The engine has moved to RESOLVE and issued readNodeAttributes for the discovered variable.
        assertThat(fixture.commands()).contains("readNodeAttributes");
        assertThat(future).isNotDone();

        // RESOLVE: the variable's attributes ⇒ the walk completes with the resolved selectable variable.
        fixture.output.readAttributesResult(
                1,
                List.of(new ResolvedAttributes(
                        WrapperTestSupport.node("temperature"),
                        "double",
                        AccessFlags.builder()
                                .readable(AccessTriState.YES)
                                .pollable(AccessTriState.YES)
                                .build(),
                        "")));
        fixture.drain();

        assertThat(future).isCompletedWithValueMatching(entries -> entries.size() == 1);
    }

    @Test
    void browseWhenNotConnected_failsWithNotConnected() {
        final WrapperTestFixture fixture =
                WrapperTestFixture.builder().skipVerification(true).build();
        final CompletableFuture<List<BrowseNode>> future = new CompletableFuture<>();

        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), future));

        assertThat(reasonOf(future)).isEqualTo(BrowseRejectedException.Reason.NOT_CONNECTED);
        assertThat(fixture.commands()).doesNotContain("browse");
    }

    @Test
    void secondBrowseWhileOneIsInFlight_failsWithAlreadyInFlight() {
        final WrapperTestFixture fixture = connectedFixture();
        final CompletableFuture<List<BrowseNode>> first = new CompletableFuture<>();
        final CompletableFuture<List<BrowseNode>> second = new CompletableFuture<>();

        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), first));
        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), second));

        assertThat(first).isNotDone();
        assertThat(reasonOf(second)).isEqualTo(BrowseRejectedException.Reason.ALREADY_IN_FLIGHT);
    }

    @Test
    void browseThatNeverReturns_failsOnTheDeadline() {
        final WrapperTestFixture fixture = connectedFixture();
        final CompletableFuture<List<BrowseNode>> future = new CompletableFuture<>();

        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), future));
        assertThat(future).isNotDone();

        fixture.advance(60_000);

        assertThat(reasonOf(future)).isEqualTo(BrowseRejectedException.Reason.TIMED_OUT);
    }

    @Test
    void browsePendingWhenConnectionLost_isFailed() {
        final WrapperTestFixture fixture = connectedFixture();
        final CompletableFuture<List<BrowseNode>> future = new CompletableFuture<>();

        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), future));
        assertThat(future).isNotDone();

        fixture.output.disconnected();
        fixture.drain();

        assertThat(reasonOf(future)).isEqualTo(BrowseRejectedException.Reason.NOT_CONNECTED);
        assertThat(fixture.state()).isEqualTo(ProtocolAdapterWrapperState.WAITING_FOR_CONNECTION_RETRY);
    }

    @Test
    void browseDeadlineWithAThrowingBrowseCancel_stillTimesOutAndReleasesTheSlot() {
        // EDG-785: the deadline aborts the engine, which issues browseCancel; a raw adapter that throws there must
        // not skip the caller's TIMED_OUT rejection nor wedge the in-flight slot on a permanent 409.
        final WrapperTestFixture fixture = connectedFixture();
        fixture.adapter.browseCancelThrows = true;
        final CompletableFuture<List<BrowseNode>> future = new CompletableFuture<>();

        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), future));
        assertThat(future).isNotDone();

        fixture.advance(60_000);

        assertThat(fixture.commands()).contains("browseCancel");
        assertThat(reasonOf(future)).isEqualTo(BrowseRejectedException.Reason.TIMED_OUT);
        assertThat(fixture.state()).isEqualTo(ProtocolAdapterWrapperState.CONNECTED);

        // the slot was genuinely released: the next browse is accepted, not rejected as already in flight.
        final CompletableFuture<List<BrowseNode>> next = new CompletableFuture<>();
        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), next));
        assertThat(next).isNotDone();
    }

    @Test
    void connectionLossWithAThrowingBrowseCancel_stillFailsNotConnectedAndDoesNotWedgeTheFsm() {
        // EDG-785: connection loss aborts the pending browse from an FSM transition action; a throwing browseCancel
        // must be swallowed (never escaping into the tick/FSM loop) and the caller still rejected NOT_CONNECTED.
        final WrapperTestFixture fixture = connectedFixture();
        fixture.adapter.browseCancelThrows = true;
        final CompletableFuture<List<BrowseNode>> future = new CompletableFuture<>();

        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), future));
        assertThat(future).isNotDone();

        fixture.output.disconnected();
        fixture.drain();

        assertThat(fixture.commands()).contains("browseCancel");
        assertThat(reasonOf(future)).isEqualTo(BrowseRejectedException.Reason.NOT_CONNECTED);
        // the FSM advanced normally rather than tripping a defensive reset from an escaped exception.
        assertThat(fixture.state()).isEqualTo(ProtocolAdapterWrapperState.WAITING_FOR_CONNECTION_RETRY);
        assertThat(fixture.defensiveResets()).isZero();
    }

    private static @NotNull WrapperTestFixture connectedFixture() {
        // skip-verification so the adapter reaches CONNECTED straight away; a wide tick period so the browse
        // deadline test fires exactly one tick.
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .skipVerification(true)
                .tickPeriodMillis(60_000)
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(ProtocolAdapterWrapperState.CONNECTED);
        return fixture;
    }

    private static BrowseRejectedException.@NotNull Reason reasonOf(final @NotNull CompletableFuture<?> future) {
        final Throwable thrown = catchThrowable(future::get);
        assertThat(thrown).isNotNull();
        assertThat(thrown.getCause()).isInstanceOf(BrowseRejectedException.class);
        return ((BrowseRejectedException) thrown.getCause()).reason();
    }
}
