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
package com.hivemq.edge.adapters.databases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.databases.config.DatabaseType;
import com.hivemq.edge.adapters.databases.config.DatabasesAdapterConfig;
import com.hivemq.edge.adapters.databases.config.DatabasesAdapterTag;
import com.hivemq.edge.adapters.databases.config.DatabasesAdapterTagDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.STATELESS;


public class DatabasesPollingProtocolAdapter implements BatchPollingProtocolAdapter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DatabasesPollingProtocolAdapter.class);
    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final int TIMEOUT = 30;

    private final @NotNull DatabasesAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private final @NotNull List<Tag> tags;
    private final @NotNull DatabaseConnection databaseConnection;
    private final @NotNull AdapterFactories adapterFactories;

    public DatabasesPollingProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<DatabasesAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tags = input.getTags();
        this.adapterFactories = input.adapterFactories();

        log.debug("Building connection string");

        this.databaseConnection = new DatabaseConnection(adapterConfig.getType(),
                adapterConfig.getServer(),
                adapterConfig.getPort(),
                adapterConfig.getDatabase(),
                adapterConfig.getUsername(),
                adapterConfig.getPassword(),
                adapterConfig.getConnectionTimeout(),
                adapterConfig.getEncrypt());
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        log.debug("Loading PostgreSQL Driver");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (final ClassNotFoundException e) {
            output.failStart(e, null);
            return;
        }

        log.debug("Loading MariaDB Driver (for MySQL)");
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (final ClassNotFoundException e) {
            output.failStart(e, null);
            return;
        }

        log.debug("Loading MS SQL Driver");
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
        } catch (final ClassNotFoundException e) {
            output.failStart(e, null);
            return;
        }



        databaseConnection.connect();

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
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        /* Connect to the database and execute the query */
        log.debug("Handling tags for the adapter");
        tags.forEach(tag -> loadDataFromDB(pollingOutput, (DatabasesAdapterTag) tag));

        protocolAdapterState.setConnectionStatus(STATELESS);
        pollingOutput.finish();
    }

    private void loadDataFromDB(final @NotNull BatchPollingOutput output, final @NotNull DatabasesAdapterTag tag) {
        // ARM to ensure the connection is closed afterward
        try (final Connection connection = databaseConnection.getConnection()) {
            log.debug("Getting tag definition");
            /* Get the tag definition (Query, RowLimit and Split Lines)*/
            final DatabasesAdapterTagDefinition definition = tag.getDefinition();
            log.debug("Executing query : {}", definition.getQuery());
            /* Execute query and handle result */
            final PreparedStatement preparedStatement = connection.prepareStatement(definition.getQuery());
            final ResultSet result = preparedStatement.executeQuery();
            assert result != null;
            final ArrayList<ObjectNode> resultObject = new ArrayList<>();
            final ResultSetMetaData resultSetMD = result.getMetaData();
            final DataPointFactory dataPointFactory = adapterFactories.dataPointFactory();
            while (result.next()) {
                final int numColumns = resultSetMD.getColumnCount();
                final ObjectNode node = OBJECT_MAPPER.createObjectNode();
                for (int i = 1; i <= numColumns; i++) {
                    parseAndAddValue(i, result, resultSetMD, node);
                }

                /* Publish datapoint with a single line if split is required */
                if (definition.getSpiltLinesInIndividualMessages()) {
                    log.debug("Creating unique message");
                    log.debug("Value : {}", node);
                    output.addDataPoint(dataPointFactory.create(tag.getName(), node));
                } else {
                    resultObject.add(node);
                }
            }

            /* Publish datapoint with all lines if no split is required */
            if (!definition.getSpiltLinesInIndividualMessages()) {
                log.debug("Publishing all lines in a single message");
                log.debug("Value : {}", resultObject);
                output.addDataPoint(dataPointFactory.create(tag.getName(), resultObject));
            }
        } catch (final Exception e) {
            output.fail(e, null);
        }
    }

    // according to https://www.ibm.com/docs/en/db2/11.1?topic=djr-sql-data-type-representation
    private void parseAndAddValue(
            final int index,
            final @NotNull ResultSet result,
            final @NotNull ResultSetMetaData resultSetMD,
            final @NotNull ObjectNode node) throws SQLException {
        final String columnName = resultSetMD.getColumnName(index);
        final int columnType = resultSetMD.getColumnType(index);
        switch (columnType) {
            case Types.BIT:
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                node.put(columnName, result.getInt(index));
                return;
            case Types.BIGINT:
                node.put(columnName, result.getLong(index));
                return;
            case Types.DECIMAL:
                node.put(columnName, result.getBigDecimal(index));
                return;
            case Types.REAL:
            case Types.FLOAT:
            case Types.DOUBLE:
            case Types.NUMERIC:
                node.put(columnName, result.getDouble(index));
                return;
            default:
                node.put(columnName, result.getString(index));
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
