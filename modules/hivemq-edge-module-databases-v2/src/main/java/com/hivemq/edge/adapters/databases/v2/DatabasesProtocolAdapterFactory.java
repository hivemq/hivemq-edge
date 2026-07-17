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
package com.hivemq.edge.adapters.databases.v2;

import com.hivemq.adapter.sdk.api.schema.ObjectSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * The factory for the v2 Databases adapter type. It is discovered by the module loader through the
 * {@code META-INF/services} declaration and instantiated with its no-argument constructor. It exposes the type
 * identity, constructs a {@link DatabasesProtocolAdapter} per configured instance, and advertises the reused v1
 * {@link Schema}s the framework validates the instance configuration and projects the node definition against.
 */
public final class DatabasesProtocolAdapterFactory implements ProtocolAdapterFactory {

    private static final @NotNull Schema ADAPTER_CONFIG_SCHEMA = buildAdapterConfigSchema();

    private static final @NotNull Schema NODE_DEFINITION_SCHEMA = buildNodeDefinitionSchema();

    @Override
    public @NotNull ProtocolAdapterInformation information() {
        return DatabasesProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull DatabasesProtocolAdapter createAdapter(
            final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
        return new DatabasesProtocolAdapter(input, output);
    }

    @Override
    public @NotNull Schema adapterConfigSchema() {
        return ADAPTER_CONFIG_SCHEMA;
    }

    @Override
    public @NotNull Schema nodeDefinitionSchema() {
        return NODE_DEFINITION_SCHEMA;
    }

    private static @NotNull Schema buildAdapterConfigSchema() {
        final Map<String, Schema> properties = new LinkedHashMap<>();
        // The reused v1 Schema has no enum constraint, so the engine projects as a plain string; the accepted values
        // are the names of the DatabaseType constants.
        properties.put(
                "type",
                new ScalarSchema(
                        ScalarType.STRING,
                        null,
                        null,
                        "Type",
                        "Database type (one of POSTGRESQL, MYSQL, MSSQL).",
                        false,
                        true,
                        false));
        properties.put(
                "server",
                new ScalarSchema(ScalarType.STRING, null, null, "Server", "Server address.", false, true, false));
        properties.put(
                "port",
                new ScalarSchema(
                        ScalarType.LONG,
                        1,
                        65536,
                        "Port",
                        "Server port (default: PostgreSQL 5432, MySQL 3306, MS SQL 1433).",
                        false,
                        true,
                        false));
        properties.put(
                "database",
                new ScalarSchema(ScalarType.STRING, null, null, "Database", "Database name.", false, true, false));
        properties.put(
                "username",
                new ScalarSchema(
                        ScalarType.STRING,
                        null,
                        null,
                        "Username",
                        "Username for the connection to the database.",
                        false,
                        true,
                        false));
        properties.put(
                "password",
                new ScalarSchema(
                        ScalarType.STRING,
                        null,
                        null,
                        "Password",
                        "Password for the connection to the database.",
                        false,
                        true,
                        false));
        properties.put(
                "encrypt",
                new ScalarSchema(
                        ScalarType.BOOLEAN,
                        null,
                        null,
                        "Encrypt",
                        "Use TLS to communicate with the remote database. Absent means false.",
                        false,
                        true,
                        false));
        properties.put(
                "trustCertificate",
                new ScalarSchema(
                        ScalarType.BOOLEAN,
                        null,
                        null,
                        "Trust Certificate",
                        "Trust the remote server certificate implicitly (MS SQL only). Absent means false.",
                        false,
                        true,
                        false));
        properties.put(
                "connectionTimeoutSeconds",
                new ScalarSchema(
                        ScalarType.LONG,
                        1,
                        181,
                        "Connection Timeout [s]",
                        "The timeout for connection establishment to the database. Absent means 30.",
                        false,
                        true,
                        false));
        return new ObjectSchema(
                properties,
                List.of("type", "server", "port", "database", "username", "password"),
                false,
                "Databases adapter configuration",
                null,
                false,
                true,
                false);
    }

    private static @NotNull Schema buildNodeDefinitionSchema() {
        final Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put(
                "query",
                new ScalarSchema(
                        ScalarType.STRING,
                        null,
                        null,
                        "Query",
                        "The SQL query to execute on the database.",
                        false,
                        true,
                        false));
        properties.put(
                "spiltLinesInIndividualMessages",
                new ScalarSchema(
                        ScalarType.BOOLEAN,
                        null,
                        null,
                        "Split lines into individual messages?",
                        "Select this option to create a single message per line returned by the query (by default all"
                                + " lines are sent in a single message as an array). Absent means false.",
                        false,
                        true,
                        false));
        return new ObjectSchema(
                properties, List.of("query"), false, "Database node definition", null, false, true, false);
    }
}
