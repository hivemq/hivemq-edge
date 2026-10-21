/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.plc4x.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7SpecificAdapterConfig;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcConnectionManager;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class Plc4xConnectionTest {

    @Test
    void test_startConnection_whenTheDeviceRefuses_carriesTheDriversCause() throws Exception {
        final PlcConnectionManager connectionManager = mock(PlcConnectionManager.class);
        when(connectionManager.getConnection(anyString()))
                .thenThrow(new PlcConnectionException("Connection refused: localhost/127.0.0.1:102"));
        final TestConnection connection = new TestConnection(driverManager(connectionManager));

        assertThatThrownBy(() -> connection.startConnection(mock(EventService.class), "testS7", "s7"))
                .isInstanceOf(Plc4xException.class)
                .hasMessageContaining("Connection refused")
                .hasCauseInstanceOf(PlcConnectionException.class);
        assertThat(connection.isConnected()).isFalse();
    }

    @Test
    void test_startConnection_whenTheDeviceAccepts_keepsTheConnection() throws Exception {
        final PlcConnection plcConnection = mock(PlcConnection.class);
        when(plcConnection.isConnected()).thenReturn(true);
        final PlcConnectionManager connectionManager = mock(PlcConnectionManager.class);
        when(connectionManager.getConnection(anyString())).thenReturn(plcConnection);
        final TestConnection connection = new TestConnection(driverManager(connectionManager));

        connection.startConnection(mock(EventService.class), "testS7", "s7");

        assertThat(connection.isConnected()).isTrue();
    }

    private static @NotNull PlcDriverManager driverManager(final @NotNull PlcConnectionManager connectionManager) {
        final PlcDriverManager driverManager = mock(PlcDriverManager.class);
        when(driverManager.getConnectionManager()).thenReturn(connectionManager);
        return driverManager;
    }

    private static final class TestConnection extends Plc4xConnection<S7SpecificAdapterConfig> {

        TestConnection(final @NotNull PlcDriverManager driverManager) throws Plc4xException {
            super(
                    driverManager,
                    new S7SpecificAdapterConfig(
                            102,
                            "localhost",
                            S7SpecificAdapterConfig.ControllerType.S7_1500,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null),
                    config -> "");
        }

        @Override
        protected @NotNull String getProtocol() {
            return "s7";
        }

        @Override
        protected @NotNull String getTagAddressForSubscription(final @NotNull Plc4xTag tag) {
            return tag.getDefinition().getTagAddress();
        }
    }
}
