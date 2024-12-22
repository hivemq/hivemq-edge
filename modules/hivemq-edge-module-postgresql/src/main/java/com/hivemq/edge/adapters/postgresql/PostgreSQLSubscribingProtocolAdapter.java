/*
 * Copyright 2024-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.postgresql;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.*;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.postgresql.config.PostgreSQLAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class PostgreSQLSubscribingProtocolAdapter implements ProtocolAdapter {
    private static final @NotNull Logger log = LoggerFactory.getLogger(PostgreSQLSubscribingProtocolAdapter.class);

    private final @NotNull PostgreSQLAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull AdapterFactories adapterFactories;
    private final @NotNull PostgreSQLHelpers postgreSQLHelpers;
    private final @NotNull String adapterId;
    private Connection databaseConnection;
    private final String compiledUri;
    private final String username;
    private final String password;

    public PostgreSQLSubscribingProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation, @NotNull PostgreSQLHelpers postgreSQLHelpers, final @NotNull ProtocolAdapterInput<PostgreSQLAdapterConfig> input) {
        this.postgreSQLHelpers = postgreSQLHelpers;
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.adapterFactories = input.adapterFactories();
        this.compiledUri = String.format("jdbc:postgresql://%s:%s/%s", adapterConfig.getServer(), adapterConfig.getPort(), adapterConfig.getDatabase());
        this.username = adapterConfig.getUsername();
        this.password = adapterConfig.getPassword();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(@NotNull ProtocolAdapterStartInput input, @NotNull ProtocolAdapterStartOutput output) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        /* Test connection to the database when starting the adapter. */
        try {
            databaseConnection = postgreSQLHelpers.connectDatabase(compiledUri, username, password);
            if(databaseConnection.isValid(0)){
                databaseConnection.close();
                output.startedSuccessfully();
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
            } else {
                output.failStart(new Throwable("Error connecting database, please check the configuration"), "Error connecting database, please check the configuration");
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            }
        } catch (final Exception e) {
            output.failStart(e, null);
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        }
    }

    @Override
    public void stop(@NotNull ProtocolAdapterStopInput protocolAdapterStopInput, @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {
        try {
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        } catch (final Exception e) {
            protocolAdapterStopOutput.failStop(e, null);
        }
        protocolAdapterStopOutput.stoppedSuccessfully();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

}
