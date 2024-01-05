package com.hivemq.edge.adapters.plc4x.impl;

import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.messages.PlcSubscriptionEvent;
import org.apache.plc4x.java.api.messages.PlcSubscriptionRequest;
import org.apache.plc4x.java.api.messages.PlcSubscriptionResponse;
import org.apache.plc4x.java.api.model.PlcSubscriptionHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class Plc4xConnection<T extends Plc4xAdapterConfig> {

    private static final Logger log = LoggerFactory.getLogger(Plc4xConnection.class);
    private final Object lock = new Object();
    protected final @NotNull PlcDriverManager plcDriverManager;
    protected final @NotNull T config;
    protected final @NotNull Plc4xConnectionQueryStringProvider connectionQueryStringProvider;
    protected volatile PlcConnection plcConnection;

    public Plc4xConnection(final @NotNull PlcDriverManager plcDriverManager,
                           final @NotNull T config,
                           final @NotNull Plc4xConnectionQueryStringProvider<T> connectionQueryStringProvider) throws Plc4xException {
        this.plcDriverManager = plcDriverManager;
        this.config = config;
        this.connectionQueryStringProvider = connectionQueryStringProvider;
        if(!validConfiguration(config)){
            if(log.isDebugEnabled()){
                log.debug("Configuration provided to Plc4X connection was considered invalid by implementation");
            }
            throw new Plc4xException("invalid connection configuration, unable to initialize");
        }
        initConnection();
    }

    protected String createConnectionString(final @NotNull T config){
        String queryString = connectionQueryStringProvider.getConnectionQueryString(config);
        if(queryString != null && !queryString.trim().equals("")){
            return String.format("%s://%s:%s?%s",
                    getProtocol().trim(),
                    config.getHost().trim(),
                    config.getPort(),
                    queryString.trim());
        } else {
            return String.format("%s://%s:%s",
                    getProtocol().trim(),
                    config.getHost().trim(),
                    config.getPort());
        }
    }

    protected void initConnection() throws Plc4xException {
        try {
            if(plcConnection == null){
                synchronized (lock){
                    if(plcConnection == null){
                        String connectionString = createConnectionString(config);
                        if(log.isInfoEnabled()){
                            log.info("Connecting via plx4j to {}", connectionString);
                        }
                        plcConnection = plcDriverManager.getConnectionManager().getConnection(connectionString);
                    }
                }
            }
        } catch(PlcConnectionException e){
            if(log.isWarnEnabled()){
                log.warn("Error encountered connecting to external device", e);
            }
            throw new Plc4xException("Error connecting", e);
        }
    }

    protected void lazyConnectionCheck() {
        if(!plcConnection.isConnected()){
            synchronized (lock){
                if(!plcConnection.isConnected()){
                    try {
                        plcConnection.connect();
                    } catch(PlcConnectionException e){
                        log.warn("Error attempting to connect to PLC device", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public void disconnect() throws Exception {
        synchronized (lock){
            try {
                if(plcConnection != null && plcConnection.isConnected()){
                    plcConnection.close();
                }
            } finally {
                plcConnection = null;
            }
        }
    }

    public boolean isConnected() {
        return plcConnection != null &&
                plcConnection.isConnected();
    }

    public CompletableFuture<? extends PlcReadResponse> read(final @NotNull T.Subscription subscription) {
        lazyConnectionCheck();
        if (!plcConnection.getMetadata().canRead()) {
            return CompletableFuture.failedFuture(new Plc4xException("connection type read-blocking"));
        }
        if(log.isDebugEnabled()){
            log.debug("Sending direct-read request to connection for {}", subscription.getTagName());
        }
        PlcReadRequest.Builder builder = plcConnection.readRequestBuilder();
        builder.addTagAddress(subscription.getTagName(), getTagAddressForSubscription(subscription));
        PlcReadRequest readRequest = builder.build();
        //Ok - seems the reads are not thread safe
        synchronized (lock){
            return readRequest.execute();
        }
    }



    public CompletableFuture<? extends PlcSubscriptionResponse> subscribe(final @NotNull T.Subscription subscription, final @NotNull Consumer<PlcSubscriptionEvent> consumer) {
        lazyConnectionCheck();
        if (!plcConnection.getMetadata().canSubscribe()) {
            return CompletableFuture.failedFuture(new Plc4xException("connection type cannot subscribe"));
        }
        if(log.isDebugEnabled()){
            log.debug("Sending subscribe request to connection for {}", subscription.getTagName());
        }
        final PlcSubscriptionRequest.Builder builder = plcConnection.subscriptionRequestBuilder();

        //TODO we're only registering for state change, could also register events
        builder.addChangeOfStateTagAddress(subscription.getTagName(), getTagAddressForSubscription(subscription));
        PlcSubscriptionRequest subscriptionRequest = builder.build();
        CompletableFuture<PlcSubscriptionResponse> future =
                (CompletableFuture<PlcSubscriptionResponse>) subscriptionRequest.execute();
        future.whenComplete((plcSubscriptionResponse, throwable) -> {
            if(throwable != null){
                log.warn("Connection subscription encountered an error;", throwable);
            } else {
                for (String subscriptionName : plcSubscriptionResponse.getTagNames()) {
                    final PlcSubscriptionHandle subscriptionHandle =
                            plcSubscriptionResponse.getSubscriptionHandle(subscriptionName);
                    subscriptionHandle.register(consumer);
                }
            }
        });
        return future;
    }



    protected boolean validConfiguration(@NotNull final T config){
        return config.getHost() != null && config.getPort() > 0 && config.getPort() <
                HiveMQEdgeConstants.MAX_UINT16;
    }

    /**
     * Concrete implementations should provide the protocol with which they are connecting
     */
    protected abstract String getProtocol();

    /**
     * Each adapter type will have its own address format. The implementation should provide the defaults
     */
    protected abstract String getTagAddressForSubscription(T.Subscription subscription);
}
