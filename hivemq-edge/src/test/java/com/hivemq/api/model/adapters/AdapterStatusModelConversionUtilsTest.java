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
package com.hivemq.api.model.adapters;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.api.model.Status;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The API projection of {@link ProtocolAdapterState.ConnectionStatus}.
 *
 * <p>The interesting case is {@code CONNECTING}: the API enum has no such member, and an adapter that
 * is still establishing its connection must not be reported as {@code CONNECTED}. That is the defect
 * behind EDG-891 P1 — a consumer sampling adapter status could observe a healthy adapter for a server
 * whose certificate was about to be refused.
 */
class AdapterStatusModelConversionUtilsTest {

    @Test
    void connecting_isReportedAsDisconnected_notConnectedAndNotUnknown() {
        final Status.ConnectionEnum converted = AdapterStatusModelConversionUtils.convertConnectionStatus(
                ProtocolAdapterState.ConnectionStatus.CONNECTING);

        assertThat(converted)
                .as("an adapter that has not finished connecting is not connected")
                .isNotEqualTo(Status.ConnectionEnum.CONNECTED);
        assertThat(converted)
                .as("DISCONNECTED is accurate and actionable; UNKNOWN would claim less than is known")
                .isEqualTo(Status.ConnectionEnum.DISCONNECTED);
    }

    @Test
    void directStates_mapOneToOne() {
        assertThat(AdapterStatusModelConversionUtils.convertConnectionStatus(
                        ProtocolAdapterState.ConnectionStatus.CONNECTED))
                .isEqualTo(Status.ConnectionEnum.CONNECTED);
        assertThat(AdapterStatusModelConversionUtils.convertConnectionStatus(
                        ProtocolAdapterState.ConnectionStatus.DISCONNECTED))
                .isEqualTo(Status.ConnectionEnum.DISCONNECTED);
        assertThat(AdapterStatusModelConversionUtils.convertConnectionStatus(
                        ProtocolAdapterState.ConnectionStatus.ERROR))
                .isEqualTo(Status.ConnectionEnum.ERROR);
        assertThat(AdapterStatusModelConversionUtils.convertConnectionStatus(
                        ProtocolAdapterState.ConnectionStatus.STATELESS))
                .isEqualTo(Status.ConnectionEnum.STATELESS);
        assertThat(AdapterStatusModelConversionUtils.convertConnectionStatus(
                        ProtocolAdapterState.ConnectionStatus.UNKNOWN))
                .isEqualTo(Status.ConnectionEnum.UNKNOWN);
    }

    /**
     * Only a state that genuinely means "connected" may project to {@code CONNECTED}. Written over the
     * whole enum so that a value added to the SDK later cannot quietly acquire a connected projection.
     */
    @ParameterizedTest
    @EnumSource(ProtocolAdapterState.ConnectionStatus.class)
    void onlyConnected_projectsToConnected(final ProtocolAdapterState.ConnectionStatus status) {
        final Status.ConnectionEnum converted = AdapterStatusModelConversionUtils.convertConnectionStatus(status);

        if (status == ProtocolAdapterState.ConnectionStatus.CONNECTED) {
            assertThat(converted).isEqualTo(Status.ConnectionEnum.CONNECTED);
        } else {
            assertThat(converted)
                    .as("%s must not be reported to API consumers as CONNECTED", status)
                    .isNotEqualTo(Status.ConnectionEnum.CONNECTED);
        }
    }

    /**
     * A new SDK connection state falling through to {@code default} is safe (it becomes
     * {@code UNKNOWN}), but it is still a silent decision. This pins the set that is deliberately
     * mapped, so adding a state to the SDK fails here and forces the projection to be chosen.
     */
    @Test
    void everyKnownState_isMappedDeliberately() {
        final Set<ProtocolAdapterState.ConnectionStatus> deliberatelyMapped = EnumSet.of(
                ProtocolAdapterState.ConnectionStatus.CONNECTED,
                ProtocolAdapterState.ConnectionStatus.DISCONNECTED,
                ProtocolAdapterState.ConnectionStatus.ERROR,
                ProtocolAdapterState.ConnectionStatus.STATELESS,
                ProtocolAdapterState.ConnectionStatus.CONNECTING,
                ProtocolAdapterState.ConnectionStatus.UNKNOWN);

        assertThat(EnumSet.allOf(ProtocolAdapterState.ConnectionStatus.class))
                .as("a connection state added to the SDK needs an explicit API projection")
                .containsExactlyInAnyOrderElementsOf(deliberatelyMapped);
    }
}
