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
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;


public class PostgreSQLPollingProtocolAdapter implements PollingProtocolAdapter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(PostgreSQLPollingProtocolAdapter.class);
    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final int TIMEOUT = 30;

    private final @NotNull PostgreSQLAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private final @NotNull List<Tag> tags;
    private final @NotNull String compiledUri;
    private final @NotNull String username;
    private final @NotNull String password;
    private final @NotNull DatabaseConnection databaseConnection = new DatabaseConnection();
    private final @NotNull List<PollingContext> pollingContexts;
    private final @NotNull HashMap<String, PreparedStatement> tagNameToPreparedStatement = new HashMap<>();

    public PostgreSQLPollingProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<PostgreSQLAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tags = input.getTags();
        this.compiledUri = String.format("jdbc:postgresql://%s:%s/%s",
                adapterConfig.getServer(),
                adapterConfig.getPort(),
                adapterConfig.getDatabase());
        this.username = adapterConfig.getUsername();
        this.password = adapterConfig.getPassword();
        this.pollingContexts = input.getPollingContexts();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (final ClassNotFoundException e) {
            output.failStart(e, null);
            return;
        }
        databaseConnection.connect(compiledUri, username, password);

        try {
            preparePreparedStatements(output);
        } catch (final SQLException e) {
            output.failStart(e, null);
            return;
        }

        try {
            log.debug("Starting connection to the database instance");
            if (databaseConnection.getConnection().isValid(TIMEOUT)) {
                output.startedSuccessfully();
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
            } else {
                output.failStart(new Throwable("Error connecting database, please check the configuration"),
                        "Error connecting database, please check the configuration");
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            }
        } catch (final Exception e) {
            output.failStart(e, null);
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        }
    }

    private void preparePreparedStatements(final @NotNull ProtocolAdapterStartOutput output) throws SQLException {
        for (final PollingContext pollingContext : pollingContexts) {
            final Optional<Tag> optDomainTag =
                    tags.stream().filter(tag -> tag.getName().equals(pollingContext.getTagName())).findFirst();
            if (optDomainTag.isPresent()) {
                final PostgreSQLAdapterTagDefinition definition =
                        (PostgreSQLAdapterTagDefinition) optDomainTag.get().getDefinition();
                final PreparedStatement preparedStatement =
                        databaseConnection.getConnection().prepareStatement(definition.getQuery());
                tagNameToPreparedStatement.put(pollingContext.getTagName(), preparedStatement);
            } else {
                output.failStart(new IllegalStateException(
                                "Polling for PostgreSQL protocol adapter failed because the used tag '" +
                                        pollingContext.getTagName() +
                                        "' was not found. For the polling to work the tag must be created via REST API or the UI."),
                        null);
            }
        }
    }

    @Override
    public void stop(
            final @NotNull ProtocolAdapterStopInput protocolAdapterStopInput,
            final @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {
        databaseConnection.close();
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
        log.debug("Checking database connection state");
        log.debug("Handling tags for the adapter");
        tags.stream()
                .filter(tag -> tag.getName().equals(pollingContext.getTagName()))
                .findFirst()
                .ifPresentOrElse(def -> loadDataFromDB(pollingOutput, def),
                        () -> pollingOutput.fail("Polling for PostgreSQL protocol adapter failed because the used tag '" +
                                pollingInput.getPollingContext().getTagName() +
                                "' was not found. For the polling to work the tag must be created via REST API or the UI."));
        pollingOutput.finish();
    }

    private void loadDataFromDB(final @NotNull PollingOutput pollingOutput, final @NotNull Tag tag) {
        try {
            log.debug("Getting tag definition");
            /* Get the tag definition (Query, RowLimit and Split Lines)*/
            final PostgreSQLAdapterTagDefinition definition = (PostgreSQLAdapterTagDefinition) tag.getDefinition();

            /* Execute query and handle result */
            final PreparedStatement preparedStatement = tagNameToPreparedStatement.get(tag.getName());
            final ResultSet result = preparedStatement.executeQuery();
            assert result != null;
            final ArrayList<ObjectNode> resultObject = new ArrayList<>();
            final ResultSetMetaData resultSetMD = result.getMetaData();
            while (result.next()) {
                final int numColumns = resultSetMD.getColumnCount();
                final ObjectNode node = OBJECT_MAPPER.createObjectNode();
                for (int i = 1; i <= numColumns; i++) {
                    final String column_name = resultSetMD.getColumnName(i);
                    node.put(column_name, result.getString(column_name));
                }

                /* Publish datapoint with a single line if split is required */
                if (definition.getSpiltLinesInIndividualMessages()) {
                    log.debug("Splitting lines in multiple messages");
                    pollingOutput.addDataPoint("queryResult", node);
                } else {
                    resultObject.add(node);
                }
            }

            /* Publish datapoint with all lines if no split is required */
            if (!definition.getSpiltLinesInIndividualMessages()) {
                log.debug("Publishing all lines in a single message");
                pollingOutput.addDataPoint("queryResult", resultObject);
            }
        } catch (final Exception e) {
            pollingOutput.fail(e, null);
        }
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
