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

import com.codahale.metrics.Counter;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.datagov.DataGovernanceService;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Brandl
 */
public class PendingWillMessagesTest {

    private final ListeningScheduledExecutorService executorService =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());

    private final @NotNull ClientSessionPersistence clientSessionPersistence = mock(ClientSessionPersistence.class);
    private final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence =
            mock(ClientSessionLocalPersistence.class);
    private @NotNull PendingWillMessages pendingWillMessages;
    private final @NotNull DataGovernanceService dataGovernanceService = mock(DataGovernanceService.class);

    @Before
    public void setUp() throws Exception {

        when(dataGovernanceService.applyAndPublish(any())).thenReturn(Futures.immediateFuture(PublishingResult.DELIVERED));

        final MetricsHolder metricsHolder = mock(MetricsHolder.class);
        when(metricsHolder.getPublishedWillMessagesCount()).thenReturn(mock(Counter.class));

        pendingWillMessages = new PendingWillMessages(executorService,
                () -> clientSessionPersistence,
                clientSessionLocalPersistence,
                metricsHolder,
                dataGovernanceService);
    }

    @After
    public void tearDown() throws Exception {
        executorService.shutdown();
    }

    @Test
    public void test_add() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE)
                .withTopic("topic")
                .withDelayInterval(5)
                .build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 10, sessionWill, 123L);
        pendingWillMessages.sendOrEnqueueWillIfAvailable("client", clientSession);

        final PendingWillMessages.PendingWill pendingWill = pendingWillMessages.getPendingWills().get("client");
        assertEquals(5, pendingWill.getDelayInterval());
    }

    @Test
    public void test_add_session_expiry() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE)
                .withTopic("topic")
                .withDelayInterval(10)
                .build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 5, sessionWill, 123L);
        pendingWillMessages.sendOrEnqueueWillIfAvailable("client", clientSession);

        final PendingWillMessages.PendingWill pendingWill = pendingWillMessages.getPendingWills().get("client");
        assertEquals(5, pendingWill.getDelayInterval());
    }

    @Test
    public void test_add_and_send() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE)
                .withTopic("topic")
                .withDelayInterval(0)
                .build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 10, sessionWill, 123L);
        pendingWillMessages.sendOrEnqueueWillIfAvailable("client", clientSession);

        verify(dataGovernanceService).applyAndPublish(any());
        verify(clientSessionPersistence).deleteWill(eq("client"));
    }

    @Test
    public void test_add_and_expiry() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE)
                .withTopic("topic")
                .withDelayInterval(10)
                .build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 0, sessionWill, 123L);
        pendingWillMessages.sendOrEnqueueWillIfAvailable("client", clientSession);

        verify(dataGovernanceService).applyAndPublish(any());
        verify(clientSessionPersistence).deleteWill(eq("client"));
    }

    @Test
    public void reset() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE)
                .withTopic("topic")
                .withDelayInterval(5)
                .build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 10, sessionWill, 123L);
        pendingWillMessages.sendOrEnqueueWillIfAvailable("client", clientSession);

        when(clientSessionPersistence.pendingWills()).thenReturn(Futures.immediateFuture(ImmutableMap.of("client1",
                new PendingWillMessages.PendingWill(3, System.currentTimeMillis()))));

        pendingWillMessages.reset();

        assertEquals(1, pendingWillMessages.getPendingWills().size());
        assertEquals(3, pendingWillMessages.getPendingWills().get("client1").getDelayInterval());
    }

    @Test
    public void test_check_dont_send() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE)
                .withTopic("topic")
                .withDelayInterval(5)
                .build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 10, sessionWill, 123L);
        pendingWillMessages.sendOrEnqueueWillIfAvailable("client", clientSession);

        final PendingWillMessages.CheckWillsTask checkWillsTask = pendingWillMessages.new CheckWillsTask();
        checkWillsTask.run();

        verify(dataGovernanceService, never()).applyAndPublish(any());
    }

    @Test
    public void test_check_send() {

        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE)
                .withTopic("topic")
                .withDelayInterval(5)
                .build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 10, sessionWill, 123L);
        when(clientSessionLocalPersistence.getSession("client", false)).thenReturn(clientSession);
        pendingWillMessages.getPendingWills()
                .put("client", new PendingWillMessages.PendingWill(3, System.currentTimeMillis() - 5000));

        final PendingWillMessages.CheckWillsTask checkWillsTask = pendingWillMessages.new CheckWillsTask();
        checkWillsTask.run();

        verify(dataGovernanceService).applyAndPublish(any());
    }

    @Test
    public void sendWillIfPending_sendsPendingWillIfAvailable_andRemovesIt() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE)
                .withTopic("topic")
                .withDelayInterval(1000)
                .build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 10, sessionWill, 123L);
        final String clientId = "client";
        when(clientSessionLocalPersistence.getSession(eq(clientId), anyBoolean())).thenReturn(clientSession);

        pendingWillMessages.sendWillIfPending("foobar");
        verify(dataGovernanceService, never()).applyAndPublish(any());
        pendingWillMessages.sendOrEnqueueWillIfAvailable(clientId, clientSession);
        verify(dataGovernanceService, never()).applyAndPublish(any());
        pendingWillMessages.sendWillIfPending(clientId);
        verify(dataGovernanceService).applyAndPublish(any());
        pendingWillMessages.sendWillIfPending(clientId);
        verify(dataGovernanceService).applyAndPublish(any());
    }
}
