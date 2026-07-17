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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.protocols.v2.view.TagStatus;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.containers.GenericContainer;

/**
 * Drives the real {@link DatabasesProtocolAdapter} through a real {@code ProtocolAdapterWrapper} — with the real
 * polled read-aspect machines — against real PostgreSQL, MySQL (via the MariaDB driver), and MS SQL containers.
 * Cadence is deterministic ({@code FakeClock} + a shared {@code ManualDispatcher}; the real query executes
 * synchronously inside the drain), so no sleeping or awaiting is needed beyond container startup.
 * <p>
 * The containers are plain {@link GenericContainer}s: the engine-specific Testcontainers modules live only in the
 * {@code hivemq-edge-test} catalog, not in {@code hivemq-edge}, and their JDBC readiness checks would need vendor
 * drivers this module deliberately does not depend on. Readiness is gated instead by connecting through the same
 * drivers the adapter bundles — MySQL through the MariaDB driver, exactly as the adapter connects.
 */
class DatabasesProtocolAdapterWrapperTest {

    private static final @NotNull String USERNAME = "testuser";
    private static final @NotNull String PASSWORD = "testPassword_1";
    private static final @NotNull String DATABASE = "testdatabase";
    private static final long POLL_INTERVAL_MILLIS = 1000;
    private static final @NotNull Duration STARTUP_AT_MOST = Duration.ofSeconds(180);

    private static final @NotNull Map<DatabaseType, GenericContainer<?>> CONTAINERS = new EnumMap<>(DatabaseType.class);

    private static synchronized @NotNull GenericContainer<?> containerFor(final @NotNull DatabaseType type) {
        return CONTAINERS.computeIfAbsent(type, ignored -> {
            final GenericContainer<?> container =
                    switch (type) {
                        case POSTGRESQL ->
                            new GenericContainer<>("postgres:15")
                                    .withEnv("POSTGRES_DB", DATABASE)
                                    .withEnv("POSTGRES_USER", USERNAME)
                                    .withEnv("POSTGRES_PASSWORD", PASSWORD)
                                    .withExposedPorts(5432);
                        case MYSQL ->
                            new GenericContainer<>("mysql:lts")
                                    .withEnv("MYSQL_DATABASE", DATABASE)
                                    .withEnv("MYSQL_USER", USERNAME)
                                    .withEnv("MYSQL_PASSWORD", PASSWORD)
                                    .withEnv("MYSQL_ROOT_PASSWORD", PASSWORD)
                                    .withExposedPorts(3306);
                        case MSSQL ->
                            new GenericContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                                    .withEnv("ACCEPT_EULA", "Y")
                                    .withEnv("MSSQL_SA_PASSWORD", PASSWORD)
                                    .withExposedPorts(1433);
                    };
            container.start();
            // The generic wait strategy only proves the port is listening; the engines finish initializing after
            // that (MySQL even restarts once during first-boot init), so gate on a real connection through the same
            // driver the adapter uses before seeding.
            await().atMost(STARTUP_AT_MOST).ignoreExceptions().untilAsserted(() -> {
                try (final Connection connection = openConnection(type, container)) {
                    assertThat(connection.isValid(5)).isTrue();
                }
            });
            seed(type, container);
            return container;
        });
    }

    private static int internalPortFor(final @NotNull DatabaseType type) {
        return switch (type) {
            case POSTGRESQL -> 5432;
            case MYSQL -> 3306;
            case MSSQL -> 1433;
        };
    }

    private static @NotNull String usernameFor(final @NotNull DatabaseType type) {
        // The MSSQL image has no per-test database or user; queries run in its default catalog as the sa user.
        return type == DatabaseType.MSSQL ? "sa" : USERNAME;
    }

    private static @NotNull String databaseFor(final @NotNull DatabaseType type) {
        return type == DatabaseType.MSSQL ? "master" : DATABASE;
    }

    /**
     * A JDBC URL over the same drivers the adapter bundles — MySQL through the MariaDB driver, so no MySQL connector
     * is needed on the test classpath.
     */
    private static @NotNull String jdbcUrlFor(
            final @NotNull DatabaseType type, final @NotNull GenericContainer<?> container) {
        final String host = container.getHost();
        final int port = container.getMappedPort(internalPortFor(type));
        return switch (type) {
            case POSTGRESQL -> "jdbc:postgresql://" + host + ":" + port + "/" + DATABASE;
            case MYSQL ->
                "jdbc:mariadb://" + host + ":" + port + "/" + DATABASE + "?allowPublicKeyRetrieval=true&useSSL=false";
            case MSSQL -> "jdbc:sqlserver://" + host + ":" + port + ";databaseName=master;encrypt=false";
        };
    }

    private static @NotNull Connection openConnection(
            final @NotNull DatabaseType type, final @NotNull GenericContainer<?> container) throws Exception {
        return DriverManager.getConnection(jdbcUrlFor(type, container), usernameFor(type), PASSWORD);
    }

    private static void seed(final @NotNull DatabaseType type, final @NotNull GenericContainer<?> container) {
        execute(type, container, "CREATE TABLE products (product_no INT, name VARCHAR(100))");
        execute(type, container, "INSERT INTO products VALUES (1, 'apple')");
        execute(type, container, "INSERT INTO products VALUES (2, 'banana')");
        execute(type, container, "INSERT INTO products VALUES (3, 'cherry')");
    }

    private static void execute(
            final @NotNull DatabaseType type, final @NotNull GenericContainer<?> container, final @NotNull String sql) {
        try (final Connection connection = openConnection(type, container);
                final Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (final Exception e) {
            throw new IllegalStateException("could not execute: " + sql, e);
        }
    }

    @AfterAll
    static void stopContainers() {
        CONTAINERS.values().forEach(GenericContainer::stop);
        CONTAINERS.clear();
    }

    private static @NotNull Map<String, Object> configurationFor(final @NotNull DatabaseType type) {
        final GenericContainer<?> container = containerFor(type);
        return Map.of(
                "type",
                type.name(),
                "server",
                container.getHost(),
                "port",
                container.getMappedPort(internalPortFor(type)),
                "database",
                databaseFor(type),
                "username",
                usernameFor(type),
                "password",
                PASSWORD);
    }

    @ParameterizedTest
    @EnumSource(DatabaseType.class)
    void pollCadence_deliversTheQueryResultAndFoldsNorthboundOnly(final @NotNull DatabaseType type) throws Exception {
        try (final DatabasesWrapperTestFixture fixture = new DatabasesWrapperTestFixture(
                "databases-v2-" + type,
                configurationFor(type),
                List.of(DatabasesAdapterTestFixtures.queryTag(
                        "products", "SELECT product_no, name FROM products ORDER BY product_no", false)),
                POLL_INTERVAL_MILLIS)) {

            fixture.activateNorthbound();

            // The real pool opened and validated; verification passed; the tag rests at its poll interval.
            assertThat(fixture.state()).isEqualTo(ProtocolAdapterWrapperState.CONNECTED);
            assertThat(fixture.readState("products")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
            assertThat(fixture.tagStatus("products")).isEqualTo(TagStatus.NORTHBOUND_ONLY);

            fixture.advanceOnePollInterval();

            // Array mode: one data point carrying all three rows.
            assertThat(fixture.northboundDataPoints).hasSize(1);
            final ArrayNode rows =
                    (ArrayNode) fixture.northboundDataPoints.get(0).getTagValue();
            assertThat(rows).hasSize(3);
            assertThat(rows.get(0).get("name").asText()).isEqualTo("apple");
            assertThat(fixture.northboundDataPoints.get(0).getTagName()).isEqualTo("products");
            assertThat(fixture.readState("products")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
            assertThat(fixture.tag("products").failureCount()).isZero();

            // The cadence continues.
            fixture.advanceOnePollInterval();
            assertThat(fixture.northboundDataPoints).hasSize(2);
        }
    }

    @ParameterizedTest
    @EnumSource(DatabaseType.class)
    void splitLines_publishesOneDataPointPerRowThroughTheRealWrapper(final @NotNull DatabaseType type)
            throws Exception {
        try (final DatabasesWrapperTestFixture fixture = new DatabasesWrapperTestFixture(
                "databases-v2-" + type,
                configurationFor(type),
                List.of(DatabasesAdapterTestFixtures.queryTag(
                        "products", "SELECT product_no, name FROM products ORDER BY product_no", true)),
                POLL_INTERVAL_MILLIS)) {

            fixture.activateNorthbound();
            fixture.advanceOnePollInterval();

            // Split-lines mode: one data point per row — the poll-completion boundary's payoff.
            assertThat(fixture.northboundDataPoints).hasSize(3);
            assertThat(((ObjectNode) fixture.northboundDataPoints.get(0).getTagValue())
                            .get("name")
                            .asText())
                    .isEqualTo("apple");
            assertThat(((ObjectNode) fixture.northboundDataPoints.get(2).getTagValue())
                            .get("name")
                            .asText())
                    .isEqualTo("cherry");
            assertThat(fixture.readState("products")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
            assertThat(fixture.tag("products").failureCount()).isZero();
        }
    }

    @ParameterizedTest
    @EnumSource(DatabaseType.class)
    void aFailingQuery_countsTheFailureAndRecoversWhenTheTableReturns(final @NotNull DatabaseType type)
            throws Exception {
        final GenericContainer<?> container = containerFor(type);
        final String tableName = "recovery_" + type.name().toLowerCase();
        try (final DatabasesWrapperTestFixture fixture = new DatabasesWrapperTestFixture(
                "databases-v2-" + type,
                configurationFor(type),
                List.of(DatabasesAdapterTestFixtures.queryTag("recovery", "SELECT name FROM " + tableName, false)),
                POLL_INTERVAL_MILLIS)) {

            fixture.activateNorthbound();
            fixture.advanceOnePollInterval();

            // The table does not exist: the poll fails, the failure counts, and the tag stays active — the next
            // scheduled poll is the retry (no auto-removal).
            assertThat(fixture.northboundDataPoints).isEmpty();
            assertThat(fixture.tag("recovery").failureCount()).isEqualTo(1);
            assertThat(fixture.readState("recovery")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
            assertThat(fixture.tagStatus("recovery")).isEqualTo(TagStatus.NORTHBOUND_ONLY);

            execute(type, container, "CREATE TABLE " + tableName + " (name VARCHAR(100))");
            execute(type, container, "INSERT INTO " + tableName + " VALUES ('recovered')");
            try {
                fixture.advanceOnePollInterval();

                assertThat(fixture.northboundDataPoints).hasSize(1);
                final ArrayNode rows =
                        (ArrayNode) fixture.northboundDataPoints.get(0).getTagValue();
                assertThat(rows.get(0).get("name").asText()).isEqualTo("recovered");
            } finally {
                execute(type, container, "DROP TABLE " + tableName);
            }
        }
    }
}
