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
package com.hivemq.persistence.clientsession;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.impl.InternalConfigurationServiceImpl;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extensions.iteration.Chunker;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.connection.ConnectionPersistenceImpl;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.TestSingleWriterFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ClientSessionPersistenceImplTest {

    private ClientSessionLocalPersistence localPersistence;
    private ClientSessionSubscriptionPersistence subscriptionPersistence;
    private ClientQueuePersistence clientQueuePersistence;
    private PendingWillMessages pendingWillMessages;
    private MqttServerDisconnector mqttServerDisconnector;
    private ConnectionPersistenceImpl connectionPersistence;
    private ClientSessionPersistenceImpl clientSessionPersistence;
    private SingleWriterService singleWriterService;
    private final @NotNull InternalConfigurationService internalConfigurationService = new InternalConfigurationServiceImpl();

    @Before
    public void setUp() throws Exception {
        localPersistence = mock(ClientSessionLocalPersistence.class);
        subscriptionPersistence = mock(ClientSessionSubscriptionPersistence.class);
        clientQueuePersistence = mock(ClientQueuePersistence.class);
        pendingWillMessages = mock(PendingWillMessages.class);
        mqttServerDisconnector = mock(MqttServerDisconnector.class);
        connectionPersistence = mock(ConnectionPersistenceImpl.class);
        clientSessionPersistence = mock(ClientSessionPersistenceImpl.class);

        singleWriterService = TestSingleWriterFactory.defaultSingleWriter(internalConfigurationService);
        clientSessionPersistence = new ClientSessionPersistenceImpl(localPersistence, subscriptionPersistence,
                clientQueuePersistence, singleWriterService, connectionPersistence, mock(EventLog.class), pendingWillMessages,
                mqttServerDisconnector, new Chunker(internalConfigurationService));
    }

    @After
    public void tearDown() throws Exception {
        singleWriterService.stop();
        clientSessionPersistence.closeDB();
    }

    @Test
    public void test_is_existent() {
        when(localPersistence.getSession(eq("client1"), anyBoolean(), anyBoolean())).thenReturn(new ClientSession(true, 0));
        assertTrue(clientSessionPersistence.isExistent("client1"));
        when(localPersistence.getSession(eq("client2"), anyBoolean(), anyBoolean())).thenReturn(new ClientSession(false, 1));
        assertTrue(clientSessionPersistence.isExistent("client2"));
        when(localPersistence.getSession(eq("client3"), anyBoolean(), anyBoolean())).thenReturn(new ClientSession(false, 0));
        assertFalse(clientSessionPersistence.isExistent("client3"));
        assertFalse(clientSessionPersistence.isExistent("client4"));

        final Map<String, Boolean> existentClients = clientSessionPersistence.isExistent(ImmutableSet.of("client1", "client2", "client3", "client4"));
        assertTrue(existentClients.get("client1"));
        assertTrue(existentClients.get("client2"));
        assertFalse(existentClients.get("client3"));
        assertFalse(existentClients.get("client4"));
    }

    @Test
    public void test_client_disconnected() throws ExecutionException, InterruptedException {
        final ClientSession previousSession = new ClientSession(false, 0);
        when(clientQueuePersistence.removeAllQos0Messages("client", false)).thenReturn(Futures.immediateFuture(null));
        when(subscriptionPersistence.removeAll("client")).thenReturn(Futures.immediateFuture(null));
        when(localPersistence.disconnect(eq("client"), anyLong(), eq(true), anyInt(), eq(10L))).
                thenReturn(previousSession);
        clientSessionPersistence.clientDisconnected("client", true, 10).get();
        verify(pendingWillMessages).sendOrEnqueueWillIfAvailable("client", previousSession);
    }

    @Test
    public void test_client_connected() throws ExecutionException, InterruptedException {

        when(localPersistence.getTimestamp(eq("client"), anyInt())).thenReturn(123L);
        when(localPersistence.getSession(eq("client"), anyInt(), eq(false))).thenReturn(new ClientSession(false, 0));
        when(subscriptionPersistence.removeAll("client")).thenReturn(Futures.immediateFuture(null));
        when(clientQueuePersistence.clear("client", false)).thenReturn(Futures.immediateFuture(null));

        final MqttWillPublish willPublish = createWillPublish();
        clientSessionPersistence.clientConnected("client", true, 0, willPublish, 123L).get();

        verify(localPersistence).put(eq("client"), any(ClientSession.class), anyLong(), anyInt());
    }

    @Test
    public void force_client_disconnect_session_null() throws ExecutionException, InterruptedException {
        when(localPersistence.getSession("client", true)).thenReturn(null);
        final Boolean result = clientSessionPersistence.forceDisconnectClient("client", false, ClientSessionPersistenceImpl.DisconnectSource.EXTENSION).get();
        assertFalse(result);
    }

    @Test
    public void force_client_disconnect_not_connected() throws ExecutionException, InterruptedException {
        when(localPersistence.getSession(eq("client"), anyBoolean(), anyBoolean())).thenReturn(new ClientSession(true, 0));
        when(connectionPersistence.get("client")).thenReturn(null);
        final Boolean result = clientSessionPersistence.forceDisconnectClient("client", true, ClientSessionPersistenceImpl.DisconnectSource.EXTENSION).get();
        assertFalse(result);
        verify(pendingWillMessages).cancelWillIfPending("client");
    }

    @Test
    public void force_client_disconnect_connected() throws ExecutionException, InterruptedException {
        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        when(connectionPersistence.get("client")).thenReturn(clientConnection);
        when(localPersistence.getSession(eq("client"), anyBoolean(), anyBoolean())).thenReturn(new ClientSession(true, 0));
        final ListenableFuture<Boolean> future = clientSessionPersistence.forceDisconnectClient("client", true, ClientSessionPersistenceImpl.DisconnectSource.EXTENSION);
        channel.disconnect();
        final Boolean result = future.get();
        assertTrue(result);
        verify(pendingWillMessages).cancelWillIfPending("client");
        verify(mqttServerDisconnector).disconnect(any(Channel.class), anyString(), anyString(), eq(Mqtt5DisconnectReasonCode.ADMINISTRATIVE_ACTION), any());
    }

    @Test
    public void force_client_disconnect_connected_reason_code_string() throws ExecutionException, InterruptedException {
        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        when(connectionPersistence.get("client")).thenReturn(clientConnection);
        when(localPersistence.getSession(eq("client"), anyBoolean(), anyBoolean())).thenReturn(new ClientSession(true, 0));
        final ListenableFuture<Boolean> future = clientSessionPersistence.forceDisconnectClient("client", true, ClientSessionPersistenceImpl.DisconnectSource.EXTENSION, Mqtt5DisconnectReasonCode.SESSION_TAKEN_OVER, "reason-string");
        channel.disconnect();
        final Boolean result = future.get();
        assertTrue(result);
        verify(pendingWillMessages).cancelWillIfPending("client");
        verify(mqttServerDisconnector).disconnect(any(Channel.class), anyString(), anyString(), eq(Mqtt5DisconnectReasonCode.SESSION_TAKEN_OVER), eq("reason-string"));
    }

    @Test
    public void test_cleanup_client_date() throws ExecutionException, InterruptedException {
        when(subscriptionPersistence.removeAll("client")).thenReturn(Futures.immediateFuture(null));
        when(clientQueuePersistence.clear("client", false)).thenReturn(Futures.immediateFuture(null));

        clientSessionPersistence.cleanClientData("client").get();

        verify(subscriptionPersistence).removeAll("client");
        verify(clientQueuePersistence).clear("client", false);
    }

    @Test
    public void test_get_all_local_clients() throws ExecutionException, InterruptedException {
        when(localPersistence.getAllClients(0)).thenReturn(ImmutableSet.of("client1"));
        when(localPersistence.getAllClients(1)).thenReturn(ImmutableSet.of("client2"));
        when(localPersistence.getAllClients(2)).thenReturn(ImmutableSet.of("client3"));


        final Set<String> clients = clientSessionPersistence.getAllClients().get();
        assertEquals(3, clients.size());
        assertTrue(clients.contains("client1"));
        assertTrue(clients.contains("client2"));
        assertTrue(clients.contains("client3"));
    }

    @Test
    public void test_set_expiry_interval_no_session() throws ExecutionException, InterruptedException {
        when(subscriptionPersistence.removeAll("client")).thenReturn(Futures.immediateFuture(null));
        when(localPersistence.getSession("client", true)).thenReturn(null);
        final Boolean result = clientSessionPersistence.setSessionExpiryInterval("client", 0).get();
        assertFalse(result);
        verify(subscriptionPersistence).removeAll("client");
    }

    @Test
    public void test_set_expiry_interval() throws ExecutionException, InterruptedException {
        when(localPersistence.getSession("client")).thenReturn(new ClientSession(false, 100));
        when(subscriptionPersistence.removeAll("client")).thenReturn(Futures.immediateFuture(null));
        final Boolean result = clientSessionPersistence.setSessionExpiryInterval("client", 0).get();
        assertTrue(result);
        verify(subscriptionPersistence).removeAll("client");
    }

    @Test
    public void test_pending_wills() throws ExecutionException, InterruptedException {
        final ImmutableMap<String, PendingWillMessages.PendingWill> bucket1 = ImmutableMap.of("client1", new PendingWillMessages.PendingWill(1, 1), "client2",
                new PendingWillMessages.PendingWill(2, 2));
        final ImmutableMap<String, PendingWillMessages.PendingWill> bucket2 = ImmutableMap.of("client3", new PendingWillMessages.PendingWill(3, 3));

        when(localPersistence.getPendingWills(eq(0))).thenReturn(bucket1);
        when(localPersistence.getPendingWills(eq(1))).thenReturn(bucket2);
        final ListenableFuture<Map<String, PendingWillMessages.PendingWill>> listenableFuture = clientSessionPersistence.pendingWills();
        final Map<String, PendingWillMessages.PendingWill> willMap = listenableFuture.get();
        assertEquals(3, willMap.size());
    }

    private MqttWillPublish createWillPublish() {
        return new MqttWillPublish.Mqtt3Builder().withTopic("topic").withPayload("message".getBytes()).withHivemqId("hivemqId").withRetain(false).withQos(QoS.AT_LEAST_ONCE).build();
    }

}
