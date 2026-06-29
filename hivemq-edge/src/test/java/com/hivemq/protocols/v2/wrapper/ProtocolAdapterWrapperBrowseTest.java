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
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
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
    void browseWhenConnected_issuesBrowseAndCompletesFromTheResult() {
        final WrapperTestFixture fixture = connectedFixture();
        final CompletableFuture<List<BrowseResultEntry>> future = new CompletableFuture<>();

        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), future));

        assertThat(fixture.commands()).contains("browse");
        assertThat(future).isNotDone();

        fixture.output.browseResult(
                List.of(new BrowseResultEntry(fixture.nodeFor("temperature"), NodeType.VALUE, true)));
        fixture.drain();

        assertThat(future).isCompletedWithValueMatching(entries -> entries.size() == 1);
    }

    @Test
    void browseWhenNotConnected_failsWithNotConnected() {
        final WrapperTestFixture fixture =
                WrapperTestFixture.builder().skipVerification(true).build();
        final CompletableFuture<List<BrowseResultEntry>> future = new CompletableFuture<>();

        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), future));

        assertThat(reasonOf(future)).isEqualTo(BrowseRejectedException.Reason.NOT_CONNECTED);
        assertThat(fixture.commands()).doesNotContain("browse");
    }

    @Test
    void secondBrowseWhileOneIsInFlight_failsWithAlreadyInFlight() {
        final WrapperTestFixture fixture = connectedFixture();
        final CompletableFuture<List<BrowseResultEntry>> first = new CompletableFuture<>();
        final CompletableFuture<List<BrowseResultEntry>> second = new CompletableFuture<>();

        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), first));
        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), second));

        assertThat(first).isNotDone();
        assertThat(reasonOf(second)).isEqualTo(BrowseRejectedException.Reason.ALREADY_IN_FLIGHT);
    }

    @Test
    void browseThatNeverReturns_failsOnTheDeadline() {
        final WrapperTestFixture fixture = connectedFixture();
        final CompletableFuture<List<BrowseResultEntry>> future = new CompletableFuture<>();

        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), future));
        assertThat(future).isNotDone();

        fixture.advance(60_000);

        assertThat(reasonOf(future)).isEqualTo(BrowseRejectedException.Reason.TIMED_OUT);
    }

    @Test
    void browsePendingWhenConnectionLost_isFailed() {
        final WrapperTestFixture fixture = connectedFixture();
        final CompletableFuture<List<BrowseResultEntry>> future = new CompletableFuture<>();

        fixture.send(new ProtocolAdapterWrapperBrowseRequest(new BrowseFilter(fixture.nodeFor("temperature")), future));
        assertThat(future).isNotDone();

        fixture.output.disconnected();
        fixture.drain();

        assertThat(reasonOf(future)).isEqualTo(BrowseRejectedException.Reason.NOT_CONNECTED);
        assertThat(fixture.state()).isEqualTo(ProtocolAdapterWrapperState.WAITING_FOR_CONNECTION_RETRY);
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
