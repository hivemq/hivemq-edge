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

import static com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.adapter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.BrowseRequested;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.ConfigurationChanged;
import com.hivemq.protocols.v2.runtime.FakeClock;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import com.hivemq.protocols.v2.wrapper.BrowseRejectedException;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperBrowseRequest;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The manager half of the browse bridge: the manager checks the snapshot is {@code CONNECTED} and
 * forwards the request to the wrapper, fails it for a disconnected adapter, and fails it for an unknown adapter.
 * The {@link RecordingWrapperFactory} captures what is forwarded to the wrapper.
 */
class ProtocolAdapterManagerBrowseTest {

    private FakeClock clock;
    private ManualDispatcher dispatcher;
    private Mailbox<ProtocolAdapterManagerMessage> mailbox;
    private ProtocolAdapterHandleRegistry registry;
    private RecordingWrapperFactory wrapperFactory;
    private ProtocolAdapterManager manager;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
        dispatcher = new ManualDispatcher();
        mailbox = new DefaultMailbox<>();
        registry = new ProtocolAdapterHandleRegistry();
        wrapperFactory = new RecordingWrapperFactory();
        final ProtocolAdapterFactoryRegistry factories = new ProtocolAdapterFactoryRegistry(
                Set.of(new ProtocolAdapterManagerTestSupport.TestProtocolAdapterFactory(
                        ProtocolAdapterManagerTestSupport.TEST_PROTOCOL_ID)));
        manager = new ProtocolAdapterManager(factories, registry, wrapperFactory, clock);
        dispatcher.attach(mailbox, manager);
        manager.bindSelf(mailbox);
    }

    @Test
    void browseWhenConnected_forwardsToTheWrapper() {
        send(new ConfigurationChanged(List.of(adapter("a").build())));
        wrapperFactory.setMachineState("a", ProtocolAdapterWrapperState.CONNECTED);

        final CompletableFuture<List<BrowseResultEntry>> future = new CompletableFuture<>();
        send(new BrowseRequested("a", filter(), future));

        assertThat(wrapperFactory.commands("a")).hasAtLeastOneElementOfType(ProtocolAdapterWrapperBrowseRequest.class);
        assertThat(future).isNotDone();
    }

    @Test
    void browseWhenNotConnected_failsWithNotConnected() {
        send(new ConfigurationChanged(List.of(adapter("a").build())));
        // The recording wrapper stays STOPPED (it does not actually start), so the adapter is not connected.

        final CompletableFuture<List<BrowseResultEntry>> future = new CompletableFuture<>();
        send(new BrowseRequested("a", filter(), future));

        final Throwable thrown = catchThrowable(future::get);
        assertThat(thrown.getCause()).isInstanceOf(BrowseRejectedException.class);
        assertThat(((BrowseRejectedException) thrown.getCause()).reason())
                .isEqualTo(BrowseRejectedException.Reason.NOT_CONNECTED);
        assertThat(wrapperFactory.commands("a"))
                .doesNotHaveAnyElementsOfTypes(ProtocolAdapterWrapperBrowseRequest.class);
    }

    @Test
    void browseUnknownAdapter_failsWithIllegalArgument() {
        final CompletableFuture<List<BrowseResultEntry>> future = new CompletableFuture<>();
        send(new BrowseRequested("ghost", filter(), future));

        final Throwable thrown = catchThrowable(future::get);
        assertThat(thrown.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    private static @NotNull BrowseFilter filter() {
        return new BrowseFilter(new ProtocolAdapterManagerTestSupport.TestNode());
    }

    private void send(final @NotNull ProtocolAdapterManagerMessage message) {
        mailbox.tell(message);
        dispatcher.drainAll();
    }
}
