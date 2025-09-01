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

import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.config.Plc4XSpecificAdapterConfig;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.messages.PlcSubscriptionEvent;
import org.apache.plc4x.java.api.messages.PlcSubscriptionRequest;
import org.apache.plc4x.java.api.messages.PlcSubscriptionResponse;
import org.apache.plc4x.java.api.model.PlcSubscriptionHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class Plc4xConnection<T extends Plc4XSpecificAdapterConfig<?>> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(Plc4xConnection.class);
    private static final int MAX_UINT16 = 65535;
    protected final @NotNull PlcDriverManager plcDriverManager;
    protected final @NotNull T config;
    protected final @NotNull Plc4xConnectionQueryStringProvider<T> connectionQueryStringProvider;
    private final Object lock = new Object();
    protected volatile @Nullable PlcConnection plcConnection;

    public Plc4xConnection(
            final @NotNull PlcDriverManager plcDriverManager,
            final @NotNull T config,
            final @NotNull Plc4xConnectionQueryStringProvider<T> connectionQueryStringProvider)
            throws Plc4xException {
        this.plcDriverManager = plcDriverManager;
        this.config = config;
        this.connectionQueryStringProvider = connectionQueryStringProvider;
        if (!validConfiguration(config)) {
            if (log.isTraceEnabled()) {
                log.trace("Configuration provided to PLC4X, connection was considered invalid by implementation");
            }
            throw new Plc4xException("invalid connection configuration, unable to initialize");
        }
    }

    protected @NotNull String createConnectionString(final @NotNull T config) {
        final String queryString = connectionQueryStringProvider.getConnectionQueryString(config);
        if (queryString != null && !queryString.trim().isEmpty()) {
            return String.format("%s://%s:%s?%s",
                    getProtocol().trim(),
                    config.getHost().trim(),
                    config.getPort(),
                    queryString.trim());
        } else {
            return String.format("%s://%s:%s", getProtocol().trim(), config.getHost().trim(), config.getPort());
        }
    }

    void startConnection() throws Plc4xException {
        if (plcConnection == null) {
            synchronized (lock) {
                if (plcConnection == null) {
                    final String connectionString = createConnectionString(config);
                    if (log.isTraceEnabled()) {
                        log.trace("Connecting via PLC4X to {}.", connectionString);
                    }
                    try {
                        plcConnection = CompletableFuture.supplyAsync(() -> {
                                    try {
                                        return Optional.of(plcDriverManager.getConnectionManager()
                                                .getConnection(connectionString));
                                    } catch (final Throwable e) {
                                        log.info("Error encountered connecting to external device", e);
                                    }
                                    return Optional.<PlcConnection>empty();
                                })
                                .get(2_000, TimeUnit.MILLISECONDS)
                                .orElseThrow(() -> new Plc4xException("Error encountered connecting to external device"));
                    } catch (final Throwable e) {
                        log.error("Error encountered connecting to external device", e);
                        throw new Plc4xException("Error encountered connecting to external device");
                    }
                }
            }
        }
    }

    protected void lazyConnectionCheck() {
        if (!plcConnection.isConnected()) {
            synchronized (lock) {
                if (!plcConnection.isConnected()) {
                    try {
                        plcConnection.connect();
                    } catch (PlcConnectionException e) {
                        log.warn("Error attempting to connect to PLC device.", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public void disconnect() throws Exception {
        synchronized (lock) {
            try {
                if (plcConnection != null && plcConnection.isConnected()) {
                    plcConnection.close();
                }
            } finally {
                plcConnection = null;
            }
        }
    }

    public boolean isConnected() {
        return plcConnection != null && plcConnection.isConnected();
    }

    public @NotNull CompletableFuture<? extends PlcReadResponse> read(final @NotNull List<Plc4xTag> tags) {
        lazyConnectionCheck();
        if (!plcConnection.getMetadata().canRead()) {
            return CompletableFuture.failedFuture(new Plc4xException("connection type read-blocking"));
        }
        if (log.isTraceEnabled()) {
            log.trace("Sending direct-read request to connection for {}.", tags);
        }
        final PlcReadRequest.Builder builder = plcConnection.readRequestBuilder();
        tags.forEach(tag -> builder.addTagAddress(tag.getName(), getTagAddressForSubscription(tag)));
        final PlcReadRequest readRequest = builder.build();
        //Ok - seems the reads are not thread safe
        synchronized (lock) {
            return readRequest.execute();
        }
    }


    public @NotNull CompletableFuture<? extends PlcSubscriptionResponse> subscribe(
            final @NotNull Plc4xTag tag,
            final @NotNull Consumer<PlcSubscriptionEvent> consumer) {
        lazyConnectionCheck();
        if (!plcConnection.getMetadata().canSubscribe()) {
            return CompletableFuture.failedFuture(new Plc4xException("connection type cannot subscribe"));
        }
        if (log.isTraceEnabled()) {
            log.trace("Sending subscribe request to connection for {}.", tag.getName());
        }
        final PlcSubscriptionRequest.Builder builder = plcConnection.subscriptionRequestBuilder();

        //TODO we're only registering for state change, could also register events
        builder.addChangeOfStateTagAddress(tag.getName(), getTagAddressForSubscription(tag));
        PlcSubscriptionRequest subscriptionRequest = builder.build();
        CompletableFuture<PlcSubscriptionResponse> future =
                (CompletableFuture<PlcSubscriptionResponse>) subscriptionRequest.execute();
        future.whenComplete((plcSubscriptionResponse, throwable) -> {
            if (throwable != null) {
                log.warn("Connection subscription encountered an error;", throwable);
            } else {
                for (final String subscriptionName : plcSubscriptionResponse.getTagNames()) {
                    final PlcSubscriptionHandle subscriptionHandle =
                            plcSubscriptionResponse.getSubscriptionHandle(subscriptionName);
                    subscriptionHandle.register(consumer);
                }
            }
        });
        return future;
    }

    protected boolean validConfiguration(final @NotNull T config) {
        return config.getHost() != null && config.getPort() > 0 && config.getPort() < MAX_UINT16;
    }

    /**
     * Concrete implementations should provide the protocol with which they are connecting
     */
    protected abstract @NotNull String getProtocol();

    /**
     * Each adapter type will have its own address format. The implementation should provide the defaults
     */
    protected abstract @NotNull String getTagAddressForSubscription(final @NotNull Plc4xTag tag);
}
