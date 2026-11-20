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
package com.hivemq.api.resources.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.bridge.BridgeService;
import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.api.model.Bridge;
import com.hivemq.edge.api.model.TlsConfiguration;
import com.hivemq.security.tls.FileSet;
import com.hivemq.security.tls.MaterialRef;
import com.hivemq.security.tls.TLSConfig;
import com.hivemq.security.tls.TLSConstants;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * The two branches {@code updateBridge} grew on this branch, asserted deterministically (EDG-882 review
 * v02, R2-21).
 * <p>
 * {@code BridgeResourceImplConcurrencyTest} is the only other test of this class, and it races threads
 * against each other to prove they serialise — which cannot pin what a single call does. Both branches
 * here decide something the ticket is about: whether an update is one transition or a removal, and
 * whether it leaves the bridge running.
 */
class BridgeResourceImplTest {

    private static final @NotNull String BRIDGE_ID = "edg-882-rest-bridge";

    private final @NotNull BridgeExtractor bridgeExtractor = mock(BridgeExtractor.class);
    private final @NotNull BridgeService bridgeService = mock(BridgeService.class);

    private @NotNull BridgeResourceImpl bridgeResource;

    private static @NotNull Bridge apiBridge() {
        return new Bridge().id(BRIDGE_ID).host("remote.example.com").port(1883).clientId(BRIDGE_ID + "-client");
    }

    private static @NotNull MqttBridge configured() {
        return new MqttBridge.Builder()
                .withId(BRIDGE_ID)
                .withHost("remote.example.com")
                .withPort(1883)
                .withClientId(BRIDGE_ID + "-client")
                .withLocalSubscriptions(List.of())
                .withRemoteSubscriptions(List.of())
                .build();
    }

    @BeforeEach
    void setUp() {
        final ConfigurationService configurationService = mock(ConfigurationService.class);
        final SystemInformation systemInformation = mock(SystemInformation.class);
        when(systemInformation.isConfigWriteable()).thenReturn(true);
        when(configurationService.bridgeExtractor()).thenReturn(bridgeExtractor);
        when(bridgeExtractor.getBridges()).thenReturn(List.of(configured()));
        bridgeResource = new BridgeResourceImpl(configurationService, bridgeService, systemInformation);
    }

    /**
     * A bridge that vanished between the existence check and the replacement is a 404, not a silent 200.
     * {@code replaceBridge} returning {@code false} is the only signal the resource gets, and treating it
     * as success would answer OK for a configuration that was never written.
     */
    @Test
    @Timeout(5)
    void updateBridge_whenTheReplacementFindsNoSuchBridge_thenNotFound() {
        when(bridgeExtractor.replaceBridge(anyString(), any(MqttBridge.class))).thenReturn(false);

        final Response response = bridgeResource.updateBridge(BRIDGE_ID, apiBridge());

        assertEquals(404, response.getStatus());
        verify(bridgeService, never()).startBridge(anyString());
    }

    /**
     * A bridge the operator had stopped is started again by an update, which is what this endpoint has
     * always done: an update leaves the bridge running.
     */
    @Test
    @Timeout(5)
    void updateBridge_whenTheBridgeIsNotRunning_thenItIsStarted() {
        when(bridgeExtractor.replaceBridge(anyString(), any(MqttBridge.class))).thenReturn(true);
        when(bridgeService.isRunning(BRIDGE_ID)).thenReturn(false);

        final Response response = bridgeResource.updateBridge(BRIDGE_ID, apiBridge());

        assertEquals(200, response.getStatus());
        verify(bridgeService).startBridge(BRIDGE_ID);
    }

    /**
     * And a running one is left alone: {@code updateBridges} has already restarted it as part of the
     * configuration transition, and starting it again here would be a second restart — which is another
     * hand-over of its queues for no reason.
     */
    @Test
    @Timeout(5)
    void updateBridge_whenTheBridgeIsRunning_thenItIsNotStartedAgain() {
        when(bridgeExtractor.replaceBridge(anyString(), any(MqttBridge.class))).thenReturn(true);
        when(bridgeService.isRunning(BRIDGE_ID)).thenReturn(true);

        final Response response = bridgeResource.updateBridge(BRIDGE_ID, apiBridge());

        assertEquals(200, response.getStatus());
        verify(bridgeService, never()).startBridge(anyString());
    }

    /**
     * The update goes through {@code replaceBridge} and never through remove-then-add. Each half of that
     * pair notifies the bridge subsystem separately, and the removal half is read as "this bridge is gone
     * from the configuration" — answered by clearing every queue it owns, including the subscriptions the
     * request never mentioned (EDG-882 QA round 1).
     */
    @Test
    @Timeout(5)
    void updateBridge_isOneTransition_notARemovalFollowedByAnAddition() {
        when(bridgeExtractor.replaceBridge(anyString(), any(MqttBridge.class))).thenReturn(true);

        bridgeResource.updateBridge(BRIDGE_ID, apiBridge());

        verify(bridgeExtractor).replaceBridge(eq(BRIDGE_ID), any(MqttBridge.class));
        verify(bridgeExtractor, never()).removeBridge(anyString());
        verify(bridgeExtractor, never()).addBridge(any(MqttBridge.class));
    }

    @Test
    void aBridgeCreatedThroughTheApiCarriesItsCertificateValidationPolicy() {
        // The bridge this produces is handed to the code that starts bridges before the
        // configuration is written back, so without a policy here it would connect without going
        // through the certificate-validation chain at all - and nothing would say so.
        final BridgeTls tls = BridgeResourceImpl.convertTls(
                BRIDGE_ID,
                new TlsConfiguration()
                        .enabled(true)
                        .truststorePath("/etc/edge/trust.jks")
                        .truststorePassword("pw")
                        .protocols(List.of())
                        .cipherSuites(List.of())
                        .verifyHostname(true));

        assertThat(tls).isNotNull();
        final TLSConfig effective = tls.getEffectiveTls();
        assertThat(effective).isNotNull();
        // The checks a bridge has always performed, which is what this spelling can express.
        assertThat(effective.trustMode()).isEqualTo(TLSConstants.TrustMode.CHAIN);
        assertThat(effective.hostname()).isEqualTo(TLSConstants.HostnameCheck.HOSTNAME);
        assertThat(effective.validity()).isEqualTo(TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER);
        assertThat(effective.revocation()).isEqualTo(TLSConstants.RevocationCheck.NONE);
        // The container format is guessed from the file name, as it always was.
        assertThat(((MaterialRef.Inline) effective.truststore().orElseThrow()).format())
                .isEqualTo("jks");
    }

    @Test
    void whenTheApiTurnsHostnameVerificationOff_thenOnlyThatCheckChanges() {
        final BridgeTls tls = BridgeResourceImpl.convertTls(
                BRIDGE_ID,
                new TlsConfiguration()
                        .enabled(true)
                        .truststorePath("/etc/edge/trust.p12")
                        .truststorePassword("pw")
                        .protocols(List.of())
                        .cipherSuites(List.of())
                        .verifyHostname(false));

        assertThat(tls).isNotNull();
        final TLSConfig effective = tls.getEffectiveTls();
        assertThat(effective).isNotNull();
        assertThat(effective.hostname()).isEqualTo(TLSConstants.HostnameCheck.NONE);
        assertThat(effective.validity()).isEqualTo(TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER);
        assertThat(((MaterialRef.Inline) effective.truststore().orElseThrow()).format())
                .isEqualTo("p12");
    }

    @Test
    void whenTheApiNamesNoTruststore_thenTheRuntimesOwnAuthoritiesAreUsed() {
        // What a bridge to a cloud broker relies on, and what the API could always express by
        // leaving the truststore out.
        final BridgeTls tls = BridgeResourceImpl.convertTls(
                BRIDGE_ID,
                new TlsConfiguration()
                        .enabled(true)
                        .protocols(List.of())
                        .cipherSuites(List.of())
                        .verifyHostname(true));

        assertThat(tls).isNotNull();
        final TLSConfig effective = tls.getEffectiveTls();
        assertThat(effective).isNotNull();
        assertThat(((MaterialRef.Inline) effective.truststore().orElseThrow())
                        .files()
                        .builtins())
                .containsExactly(FileSet.JVM_CACERTS);
    }
}
