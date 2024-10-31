/*
 * Copyright 2023-present HiveMQ GmbH
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.model.*;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.postgresql.config.PostgreSQLAdapterConfig;
import com.hivemq.edge.adapters.postgresql.config.PostgreSQLPollingContext;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PostgreSQLPollingProtocolAdapter implements PollingProtocolAdapter<PostgreSQLPollingContext> {
    private final @NotNull PostgreSQLAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull List<PostgreSQLPollingContext> pollingContext;
    private Connection databaseConnection;
    private String compiledUri;
    private String username;
    private String password;

    public PostgreSQLPollingProtocolAdapter(final @NotNull ProtocolAdapterInformation adapterInformation, final @NotNull ProtocolAdapterInput<PostgreSQLAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.pollingContext = adapterConfig.getPollingContexts();
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public void start(final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        /* Test connection to the database when starting the adapter. */
        try {
            compiledUri = String.format("jdbc:postgresql://%s:%s/%s", adapterConfig.getServer(), adapterConfig.getPort(), adapterConfig.getDatabase());
            username = adapterConfig.getUsername();
            password = adapterConfig.getPassword();
            databaseConnection = connectDatabase();

            if(databaseConnection.isValid(0)){
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
    public void stop(final @NotNull ProtocolAdapterStopInput protocolAdapterStopInput, final @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {
        protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        protocolAdapterStopOutput.stoppedSuccessfully();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(final @NotNull PollingInput<PostgreSQLPollingContext> pollingInput, final @NotNull PollingOutput pollingOutput) {
        ResultSet result;
        ObjectMapper om = new ObjectMapper();

        /* Rework the query to protect against big data volumes (basically removing possible LIMIT XX in the query and replacing with defined limit in the sub setting. */
        String query = removeLimitFromQuery(Objects.requireNonNull(pollingInput.getPollingContext().getQuery()), "LIMIT") + " LIMIT " + pollingInput.getPollingContext().getRowLimit() + ";";

        /* Connect to the database and execute the query */
        try {
            if(!databaseConnection.isValid(0)){
                databaseConnection = connectDatabase();
            }
            result = (databaseConnection.createStatement()).executeQuery(query);
            ArrayList<ObjectNode> resultObject = new ArrayList<>();
            ResultSetMetaData resultSetMD = result.getMetaData();
            while(result.next()) {
                int numColumns = resultSetMD.getColumnCount();
                ObjectNode node = om.createObjectNode();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = resultSetMD.getColumnName(i);
                    node.put(column_name, result.getString(column_name));
                }
                /* Publish datapoint with a single line if split is required */
                if(pollingInput.getPollingContext().getSpiltLinesInIndividualMessages()){
                    pollingOutput.addDataPoint("queryResult", node);
                } else {
                    resultObject.add(node);
                }
            }

            /* Publish datapoint with all lines if no split is required */
            if(!pollingInput.getPollingContext().getSpiltLinesInIndividualMessages()) {
                pollingOutput.addDataPoint("queryResult", resultObject);
            }
            databaseConnection.close();

        } catch (SQLException e) {
            try {
                databaseConnection.close();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }

        pollingOutput.finish();
    }

    @Override
    public @NotNull List<PostgreSQLPollingContext> getPollingContexts() {
        return pollingContext;
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getMaxPollingErrorsBeforeRemoval();
    }

    // Database connection method
    public Connection connectDatabase() throws SQLException {
        return DriverManager.getConnection(compiledUri, username, password);
    }

    // Query cleaning method
    public String removeLimitFromQuery(final @NotNull String query, final @NotNull String toRemove) {
        var words = query.split(" ");
        StringBuilder newStr = new StringBuilder();
        var wasPreviousWord = false;
        for (String word : words) {
            if (!Objects.equals(word, toRemove) && !wasPreviousWord) {
                newStr.append(word).append(" ");
            } else {
                wasPreviousWord = !wasPreviousWord;
            }
        }
        return newStr.toString();
    }
}
