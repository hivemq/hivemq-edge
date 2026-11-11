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
package com.hivemq.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.api.resources.impl.BridgeResourceImpl;
import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.mqtt.BridgeMqttClient;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.api.model.TlsConfiguration;
import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// A running bridge is replaced on reload only by a configuration whose TLS material can be read.
///
/// Stopping a connected bridge for a replacement that cannot connect turns a typo in a truststore
/// path into an outage of that bridge; the running one is kept instead. At start-up there is nothing
/// running to keep, so a bridge with unreadable material is still started and refused when it
/// connects, and the other bridges and Edge come up.
class BridgeServiceTlsRefusalTest {

    private static final @NotNull String BRIDGE_ID = "tls-refusal-bridge";

    private @NotNull BridgeService bridgeService;
    private @NotNull BridgeMqttClientFactory clientFactory;

    @TempDir
    private @NotNull File temporaryFolder;

    @BeforeEach
    void setUp() {
        clientFactory = mock(BridgeMqttClientFactory.class);
        bridgeService = new BridgeService(
                mock(BridgeExtractor.class),
                mock(MessageForwarder.class),
                clientFactory,
                MoreExecutors.newDirectExecutorService(),
                mock(HiveMQEdgeRemoteService.class),
                new ShutdownHooks(),
                new MetricRegistry());
    }

    /// A reload that would replace a running bridge with one whose truststore cannot be read keeps
    /// the running bridge: its client is not stopped and no new client is created.
    @Test
    void givenARunningBridge_whenReloadedWithATruststoreThatCannotBeRead_thenTheRunningBridgeIsKept() {
        final MqttBridge running = bridge(systemAuthorities(10));
        final MqttBridge broken = bridge(missingTruststore());
        final BridgeMqttClient runningClient = client(running);
        final BridgeMqttClient brokenClient = client(broken);
        when(clientFactory.createRemoteClient(any())).thenReturn(runningClient).thenReturn(brokenClient);
        bridgeService.updateBridges(List.of(running));

        bridgeService.updateBridges(List.of(broken));

        verify(runningClient, never()).stop();
        verify(clientFactory, times(1)).createRemoteClient(any());
        assertThat(bridgeService.isRunning(BRIDGE_ID)).isTrue();
    }

    /// A reload with a readable change stops the running bridge and starts its replacement.
    @Test
    void givenARunningBridge_whenReloadedWithAReadableChange_thenItIsRestarted() {
        final MqttBridge running = bridge(systemAuthorities(10));
        final MqttBridge changed = bridge(systemAuthorities(30));
        final BridgeMqttClient runningClient = client(running);
        final BridgeMqttClient changedClient = client(changed);
        when(clientFactory.createRemoteClient(any())).thenReturn(runningClient).thenReturn(changedClient);
        bridgeService.updateBridges(List.of(running));

        bridgeService.updateBridges(List.of(changed));

        verify(runningClient).stop();
        verify(clientFactory, times(2)).createRemoteClient(any());
        assertThat(bridgeService.isRunning(BRIDGE_ID)).isTrue();
    }

    /// At start-up nothing is running to keep, so a bridge whose truststore cannot be read is still
    /// started and is refused when it connects.
    @Test
    void givenATruststoreThatCannotBeRead_whenEdgeStarts_thenTheBridgeIsStillStarted() {
        final MqttBridge broken = bridge(missingTruststore());
        final BridgeMqttClient brokenClient = client(broken);
        when(clientFactory.createRemoteClient(any())).thenReturn(brokenClient);

        bridgeService.updateBridges(List.of(broken));

        verify(clientFactory, times(1)).createRemoteClient(any());
        verify(brokenClient).start();
    }

    private static @NotNull MqttBridge bridge(final @NotNull BridgeTls tls) {
        return new MqttBridge.Builder()
                .withId(BRIDGE_ID)
                .withHost("remote.example.com")
                .withPort(8883)
                .withClientId(BRIDGE_ID + "-client")
                .withBridgeTls(tls)
                .withLocalSubscriptions(List.of())
                .withRemoteSubscriptions(List.of())
                .build();
    }

    /// TLS trusting the runtime's own authorities, which every runtime can read.
    private static @NotNull BridgeTls systemAuthorities(final int handshakeTimeoutSeconds) {
        final BridgeTls tls = BridgeResourceImpl.convertTls(
                BRIDGE_ID, new TlsConfiguration().enabled(true).handshakeTimeout(handshakeTimeoutSeconds));
        assertThat(tls).isNotNull();
        return tls;
    }

    private static @NotNull BridgeMqttClient client(final @NotNull MqttBridge bridge) {
        final BridgeMqttClient client = mock(BridgeMqttClient.class);
        when(client.getBridge()).thenReturn(bridge);
        when(client.start()).thenReturn(Futures.immediateFuture(null));
        when(client.stop()).thenReturn(Futures.immediateFuture(null));
        return client;
    }

    /// TLS trusting a truststore that does not exist.
    private @NotNull BridgeTls missingTruststore() {
        final BridgeTls tls = BridgeResourceImpl.convertTls(
                BRIDGE_ID,
                new TlsConfiguration()
                        .enabled(true)
                        .truststorePath(new File(temporaryFolder, "missing.jks").getAbsolutePath())
                        .truststorePassword("trust-pw"));
        assertThat(tls).isNotNull();
        return tls;
    }
}
