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
package com.hivemq.extensions.services.session;

import static com.hivemq.persistence.clientsession.ClientSessionPersistenceImpl.DisconnectSource.EXTENSION;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.services.exception.RateLimitExceededException;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import com.hivemq.extension.sdk.api.services.session.SessionInformation;
import com.hivemq.extensions.iteration.*;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.extensions.services.executor.GlobalManagedExtensionExecutorService;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionWill;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestException;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class ClientServiceImplTest {

    private ClientService clientService;

    @Mock
    private ClientSessionPersistence clientSessionPersistence;

    @Mock
    private PluginServiceRateLimitService pluginServiceRateLimitService;

    @Mock
    AsyncIteratorFactory asyncIteratorFactory;

    private final String clientId = "clientID";
    private final long sessionExpiry = 123546L;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        clientService = new ClientServiceImpl(
                pluginServiceRateLimitService,
                clientSessionPersistence,
                getManagedExtensionExecutorService(),
                asyncIteratorFactory);
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(false);
    }

    @Test
    public void test_get_session_limit_exceeded() throws Throwable {
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        assertThatThrownBy(() -> clientService.getSession(clientId).get())
                .hasCauseInstanceOf(RateLimitExceededException.class);
    }

    @Test
    public void test_client_connected_limit_exceeded() {
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        assertThatThrownBy(() -> clientService.isClientConnected(clientId).get())
                .hasCauseInstanceOf(RateLimitExceededException.class);
    }

    @Test
    public void test_disconnect_client_do_not_prevent_lwt_limit_exceeded() {
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        assertThatThrownBy(() -> clientService.disconnectClient(clientId).get())
                .hasCauseInstanceOf(RateLimitExceededException.class);
    }

    @Test
    public void test_disconnect_client_prevent_lwt_limit_exceeded() {
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        assertThatThrownBy(() -> clientService.disconnectClient(clientId, true).get())
                .hasCauseInstanceOf(RateLimitExceededException.class);
    }

    @Test
    public void test_invalidate_session_limit_exceeded() {
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        assertThatThrownBy(() -> clientService.invalidateSession(clientId).get())
                .hasCauseInstanceOf(RateLimitExceededException.class);
    }

    @Test
    public void test_disconnect_prevent_lwt_failed() {

        when(clientSessionPersistence.forceDisconnectClient(clientId, true, EXTENSION, null, null))
                .thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));
        assertThatThrownBy(() -> clientService.disconnectClient(clientId, true).get())
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    public void test_disconnect_do_not_prevent_lwt_failed() {

        when(clientSessionPersistence.forceDisconnectClient(clientId, false, EXTENSION, null, null))
                .thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));
        assertThatThrownBy(() -> clientService.disconnectClient(clientId).get()).isInstanceOf(ExecutionException.class);
    }

    @Test
    public void test_invalidate_session_request_failed() {

        when(clientSessionPersistence.invalidateSession(clientId, EXTENSION))
                .thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));
        assertThatThrownBy(() -> clientService.invalidateSession(clientId).get())
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    public void test_isClientConnected_client_id_null() {

        assertThrows(NullPointerException.class, () -> clientService.isClientConnected(null));
    }

    @Test
    public void test_getSession_client_id_null() {

        assertThrows(NullPointerException.class, () -> clientService.getSession(null));
    }

    @Test
    public void test_disconnectClient_client_id_null() {

        assertThrows(NullPointerException.class, () -> clientService.disconnectClient(null));
    }

    @Test
    public void test_disconnectClient_prevent_client_id_null() {

        assertThrows(NullPointerException.class, () -> clientService.disconnectClient(null, false));
    }

    @Test
    public void test_invalidateSession_client_id_null() {

        assertThrows(NullPointerException.class, () -> clientService.invalidateSession(null));
    }

    @Test
    @Timeout(20)
    public void test_client_connected_null_success() throws Throwable {

        when(clientSessionPersistence.getSession(clientId, false)).thenReturn(null);
        assertFalse(clientService.isClientConnected(clientId).get());
    }

    @Test
    @Timeout(20)
    public void test_get_session_null_success() throws Throwable {
        when(clientSessionPersistence.getSession(clientId, false)).thenReturn(null);
        assertEquals(Optional.empty(), clientService.getSession(clientId).get());
    }

    @Test
    @Timeout(20)
    public void test_get_session_success() throws Throwable {

        final ClientSession session = getSession(true);

        when(clientSessionPersistence.getSession(clientId, false)).thenReturn(session);
        final Optional<SessionInformation> sessionInformation =
                clientService.getSession(clientId).get();

        assertTrue(sessionInformation.isPresent());
        assertTrue(sessionInformation.get().isConnected());
        assertEquals(clientId, sessionInformation.get().getClientIdentifier());
        assertEquals(sessionExpiry, sessionInformation.get().getSessionExpiryInterval());
    }

    @Test
    @Timeout(20)
    public void test_disconnect_client_do_not_prevent_lwt_null_success() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(clientId, false, EXTENSION, null, null))
                .thenReturn(Futures.immediateFuture(null));
        assertNull(clientService.disconnectClient(clientId).get());
    }

    @Test
    @Timeout(20)
    public void test_disconnect_client_do_not_prevent_lwt_true_success() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(clientId, false, EXTENSION, null, null))
                .thenReturn(Futures.immediateFuture(true));
        assertTrue(clientService.disconnectClient(clientId).get());
    }

    @Test
    @Timeout(20)
    public void test_disconnect_client_do_not_prevent_lwt_false_success() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(clientId, false, EXTENSION, null, null))
                .thenReturn(Futures.immediateFuture(false));
        assertFalse(clientService.disconnectClient(clientId).get());
    }

    @Test
    @Timeout(20)
    public void test_disconnect_client_prevent_lwt_null_success() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(clientId, true, EXTENSION, null, null))
                .thenReturn(Futures.immediateFuture(null));
        assertNull(clientService.disconnectClient(clientId, true).get());
    }

    @Test
    @Timeout(20)
    public void test_disconnect_client_prevent_lwt_true_success() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(clientId, true, EXTENSION, null, null))
                .thenReturn(Futures.immediateFuture(true));
        assertTrue(clientService.disconnectClient(clientId, true).get());
    }

    @Test
    @Timeout(20)
    public void test_disconnect_with_reason_Code() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(
                        clientId,
                        true,
                        EXTENSION,
                        Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION,
                        "Disconnecting Normally"))
                .thenReturn(Futures.immediateFuture(true));
        assertEquals(
                true,
                clientService
                        .disconnectClient(
                                clientId, true, DisconnectReasonCode.NORMAL_DISCONNECTION, "Disconnecting Normally")
                        .get());
    }

    @Test
    public void test_disconnect_with_deprecated_reason_code() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        when(clientSessionPersistence.getSession(eq("client"), anyBoolean())).thenReturn(new ClientSession(true, 0));
        assertThatThrownBy(() -> clientService.disconnectClient(
                        "client", true, DisconnectReasonCode.CLIENT_IDENTIFIER_NOT_VALID, "reason-string"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void test_disconnect_with_client_reason_code() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        when(clientSessionPersistence.getSession(eq("client"), anyBoolean())).thenReturn(new ClientSession(true, 0));
        assertThatThrownBy(() -> clientService.disconnectClient(
                        "client", true, DisconnectReasonCode.DISCONNECT_WITH_WILL_MESSAGE, "reason-string"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Timeout(20)
    public void test_disconnect_client_prevent_lwt_false_success() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(clientId, true, EXTENSION, null, null))
                .thenReturn(Futures.immediateFuture(false));
        assertFalse(clientService.disconnectClient(clientId, true).get());
    }

    @Test
    @Timeout(20)
    public void test_invalidate_session_null_failed() {
        when(clientSessionPersistence.invalidateSession(clientId, EXTENSION)).thenReturn(Futures.immediateFuture(null));
        assertThatThrownBy(() -> clientService.invalidateSession(clientId).get())
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    @Timeout(20)
    public void test_invalidate_session_true_success() throws Throwable {
        when(clientSessionPersistence.invalidateSession(clientId, EXTENSION)).thenReturn(Futures.immediateFuture(true));
        assertTrue(clientService.invalidateSession(clientId).get());
    }

    @Test
    @Timeout(20)
    public void test_invalidate_session_false_success() throws Throwable {
        when(clientSessionPersistence.invalidateSession(clientId, EXTENSION))
                .thenReturn(Futures.immediateFuture(false));
        assertFalse(clientService.invalidateSession(clientId).get());
    }

    @Test
    @Timeout(20)
    public void test_client_connected_true_success() throws Throwable {

        when(clientSessionPersistence.getSession(clientId, false)).thenReturn(getSession(true));
        assertTrue(clientService.isClientConnected(clientId).get());
    }

    @Test
    @Timeout(20)
    public void test_client_connected_false_success() throws Throwable {

        when(clientSessionPersistence.getSession(clientId, false)).thenReturn(getSession(false));
        assertFalse(clientService.isClientConnected(clientId).get());
    }

    @NotNull
    private ClientSession getSession(final boolean connected) {
        return new ClientSession(
                connected, sessionExpiry, new ClientSessionWill(mock(MqttWillPublish.class), 12345L), 23456L);
    }

    @Test
    @Timeout(10)
    public void test_iterate_all_rate_limit_exceeded() {
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);

        assertThatThrownBy(() ->
                        clientService.iterateAllClients((context, value) -> {}).get())
                .hasCauseInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_all_callback_null() {
        assertThatThrownBy(() -> clientService.iterateAllClients(null).get()).isInstanceOf(NullPointerException.class);
    }

    @Test
    @Timeout(10)
    public void test_iterate_all_callback_executor_null() {
        assertThatThrownBy(() -> clientService
                        .iterateAllClients((context, value) -> {}, null)
                        .get())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Timeout(10)
    public void test_item_callback() throws Exception {
        final ArrayList<SessionInformation> items = Lists.newArrayList();

        final CountDownLatch latch = new CountDownLatch(1);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final AllItemsItemCallback<SessionInformation> itemCallback =
                new AllItemsItemCallback<>(executor, (context, value) -> {
                    items.add(value);
                    latch.countDown();
                });

        final ListenableFuture<Boolean> onItems = itemCallback.onItems(List.of(
                new SessionInformationImpl("client", 1, true),
                new SessionInformationImpl("client2", 2, true),
                new SessionInformationImpl("client3", 3, false)));

        assertTrue(onItems.get());

        assertEquals(3, items.size());

        executor.shutdownNow();
    }

    @Test
    @Timeout(10)
    public void test_item_callback_abort() throws Exception {

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final AllItemsItemCallback<SessionInformation> itemCallback =
                new AllItemsItemCallback<>(executor, (context, value) -> {
                    context.abortIteration();
                });

        final ListenableFuture<Boolean> onItems = itemCallback.onItems(List.of(
                new SessionInformationImpl("client", 1, true),
                new SessionInformationImpl("client2", 2, true),
                new SessionInformationImpl("client3", 3, false)));

        assertFalse(onItems.get());

        executor.shutdownNow();
    }

    @Test
    @Timeout(10)
    public void test_item_callback_exception() throws Throwable {

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final AllItemsItemCallback<SessionInformation> itemCallback =
                new AllItemsItemCallback<>(executor, (context, value) -> {
                    throw new RuntimeException("test-exception");
                });

        final ListenableFuture<Boolean> onItems = itemCallback.onItems(List.of(
                new SessionInformationImpl("client", 1, true),
                new SessionInformationImpl("client2", 2, true),
                new SessionInformationImpl("client3", 3, false)));

        assertThatThrownBy(() -> onItems.get()).hasCauseInstanceOf(RuntimeException.class);

        executor.shutdownNow();
    }

    @Test
    @Timeout(10)
    public void test_iteration_started() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        //noinspection unchecked
        when(asyncIteratorFactory.createIterator(any(FetchCallback.class), any(AsyncIterator.ItemCallback.class)))
                .thenReturn(new AsyncIterator() {
                    @Override
                    public void fetchAndIterate() {
                        latch.countDown();
                    }

                    @Override
                    public @NotNull CompletableFuture<Void> getFinishedFuture() {
                        return resultFuture;
                    }
                });

        clientService.iterateAllClients((context, value) -> {});

        resultFuture.complete(null);

        latch.await();
    }

    @Test
    public void test_test_fetch_callback_conversion() {

        final ClientServiceImpl.AllClientsFetchCallback fetchCallback =
                new ClientServiceImpl.AllClientsFetchCallback(null);

        final ChunkResult<SessionInformation> chunkResult =
                fetchCallback.convertToChunkResult(new MultipleChunkResult<Map<String, ClientSession>>(Map.of(
                        1, new BucketChunkResult<>(Map.of("client1", new ClientSession(true, 10)), true, "client1", 1),
                        2,
                                new BucketChunkResult<>(
                                        Map.of(
                                                "client2", new ClientSession(true, 10),
                                                "client3", new ClientSession(true, 10)),
                                        false,
                                        "client3",
                                        2))));

        assertTrue(chunkResult.getCursor().getFinishedBuckets().contains(1));
        assertFalse(chunkResult.getCursor().getFinishedBuckets().contains(2));
        assertEquals(3, chunkResult.getResults().size());
    }

    @NotNull
    private GlobalManagedExtensionExecutorService getManagedExtensionExecutorService() {
        final GlobalManagedExtensionExecutorService globalManagedPluginExecutorService =
                new GlobalManagedExtensionExecutorService(mock(ShutdownHooks.class));
        globalManagedPluginExecutorService.postConstruct();
        return globalManagedPluginExecutorService;
    }
}
