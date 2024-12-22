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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.model.*;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.postgresql.config.PostgreSQLAdapterConfig;
import com.hivemq.edge.adapters.postgresql.config.PostgreSQLAdapterTagDefinition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class PostgreSQLPollingProtocolAdapter implements PollingProtocolAdapter{
    private static final @NotNull Logger log = LoggerFactory.getLogger(PostgreSQLPollingProtocolAdapter.class);
    private final @NotNull PostgreSQLAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull PostgreSQLHelpers postgreSQLHelpers = new PostgreSQLHelpers();
    private final @NotNull String adapterId;
    private final @NotNull List<Tag> tags;
    private Connection databaseConnection;
    private final String compiledUri;
    private final String username;
    private final String password;

    public PostgreSQLPollingProtocolAdapter(final @NotNull ProtocolAdapterInformation adapterInformation, final @NotNull ProtocolAdapterInput<PostgreSQLAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tags = input.getTags();
        this.compiledUri = String.format("jdbc:postgresql://%s:%s/%s", adapterConfig.getServer(), adapterConfig.getPort(), adapterConfig.getDatabase());
        this.username = adapterConfig.getUsername();
        this.password = adapterConfig.getPassword();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
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
            log.debug("Starting connection to the database instance");
            databaseConnection = postgreSQLHelpers.connectDatabase(compiledUri, username, password);
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
        try {
            log.debug("Closing database connection");
            databaseConnection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        protocolAdapterStopOutput.stoppedSuccessfully();
    }


    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(final @NotNull PollingInput pollingInput, final @NotNull PollingOutput pollingOutput) {
        log.debug("Getting polling context");
        final PollingContext pollingContext = pollingInput.getPollingContext();

        /* Connect to the database and execute the query */
        try {
            log.debug("Checking database connection state");
            if(!databaseConnection.isValid(0)){
                log.debug("Connecting to the database");
                databaseConnection = postgreSQLHelpers.connectDatabase(compiledUri, username, password);
            }

            log.debug("Handling tags for the adapter");
            tags.stream()
                    .filter(tag -> tag.getName().equals(pollingContext.getTagName()))
                    .findFirst()
                    .ifPresentOrElse(
                            def -> {
                                try {
                                    ResultSet result;
                                    ObjectMapper om = new ObjectMapper();
                                    log.debug("Getting tag definition");
                                    /* Get the tag definition (Query, RowLimit and Split Lines)*/
                                    PostgreSQLAdapterTagDefinition definition = (PostgreSQLAdapterTagDefinition) def.getDefinition();

                                    log.debug("Cleaning query");
                                    /* Rework the query to protect against big data volumes (basically removing possible LIMIT XX in the query and replacing with defined limit in the sub setting). */
                                    String query = postgreSQLHelpers.removeLimitFromQuery(Objects.requireNonNull(definition.getQuery()), "LIMIT") + " LIMIT " + definition.getRowLimit() + ";";
                                    log.debug("Cleaned Tag Query : {}", query);

                                    /* Execute query and handle result */
                                    result = (databaseConnection.createStatement()).executeQuery(query);
                                    assert result != null;
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
                                        if(definition.getSpiltLinesInIndividualMessages()){
                                            log.debug("Splitting lines in multiple messages");
                                            pollingOutput.addDataPoint("queryResult", node);
                                        } else {
                                            resultObject.add(node);
                                        }
                                    }

                                    /* Publish datapoint with all lines if no split is required */
                                    if(!definition.getSpiltLinesInIndividualMessages()) {
                                        log.debug("Publishing all lines in a single message");
                                        pollingOutput.addDataPoint("queryResult", resultObject);
                                    }
                                } catch (final Exception e) {
                                    pollingOutput.fail(e, null);
                                }
                            },
                            () -> pollingOutput.fail("Polling for PostgreSQL protocol adapter failed because the used tag '" +
                                    pollingInput.getPollingContext().getTagName() +
                                    "' was not found. For the polling to work the tag must be created via REST API or the UI.")
                    );
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException(e);
        }
        pollingOutput.finish();
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getMaxPollingErrorsBeforeRemoval();
    }

}
