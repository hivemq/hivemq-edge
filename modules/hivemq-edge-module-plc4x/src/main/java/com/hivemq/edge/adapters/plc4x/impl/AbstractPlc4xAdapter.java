package com.hivemq.edge.adapters.plc4x.impl;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.impl.AbstractPollingPerSubscriptionAdapter;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.api.value.PlcValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract PLC4X implementation. Exposes core abstractions of the underlying framework so instances can be exposes
 * using the consistent
 * patterns.
 *
 * @author Simon L Johnson
 */
public abstract class AbstractPlc4xAdapter<T extends Plc4xAdapterConfig>
        extends AbstractPollingPerSubscriptionAdapter<T, ProtocolAdapterDataSample<T>> {

    protected static final String TAG_ADDRESS_TYPE_SEP = ":";
    private static final Logger log = LoggerFactory.getLogger(Plc4xAdapterConfig.class);
    protected final static @NotNull PlcDriverManager driverManager = PlcDriverManager.getDefault();
    private final @NotNull Object lock = new Object();
    protected volatile @Nullable Plc4xConnection connection;
    private final @NotNull Map<String, ProtocolAdapterDataSample<T>> lastSamples = new HashMap<>(1);

    public enum ReadType {
        Read,
        Subscribe
    }

    public AbstractPlc4xAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull T adapterConfig,
            final @NotNull MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }


    private Plc4xConnection initConnection() {
        if (connection == null) {
            synchronized (lock) {
                if (connection == null) {
                    try {
                        log.info("Creating new Instance Of Plc4x Connector with {}", adapterConfig);
                        connection = createConnection();
                        setConnectionStatus(ConnectionStatus.CONNECTED);
                    } catch (Plc4xException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return connection;
    }

    protected Plc4xConnection<?> createConnection() throws Plc4xException {
        return new Plc4xConnection<>(driverManager,
                adapterConfig,
                plc4xAdapterConfig -> Plc4xDataUtils.createQueryString(createQueryStringParams(
                        plc4xAdapterConfig), true)) {
            @Override
            protected String getProtocol() {
                return getProtocolHandler();
            }

            @Override
            protected String createConnectionString(final T config) {
                return super.createConnectionString(config);
            }

            @Override
            protected String getTagAddressForSubscription(final Plc4xAdapterConfig.Subscription subscription) {
                return createTagAddressForSubscription(subscription);
            }
        };
    }

    @Override
    protected CompletableFuture<ProtocolAdapterStartOutput> startInternal(final @NotNull ProtocolAdapterStartOutput output) {
        CompletableFuture<Plc4xConnection<T>> startFuture = CompletableFuture.supplyAsync(() -> initConnection());
        startFuture.thenAccept(this::subscribeAllInternal);
        return startFuture.thenApply(connection -> output);
    }

    protected void subscribeAllInternal(@NotNull final Plc4xConnection<T> connection) throws RuntimeException {
        if (adapterConfig.getSubscriptions() != null) {
            for (T.Subscription subscription : adapterConfig.getSubscriptions()) {
                try {
                    subscribeInternal(connection, subscription);
                } catch (Plc4xException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    protected CompletableFuture<Void> stopInternal() {
        if (connection != null) {
            try {
                //-- Disconnect client
                connection.disconnect();
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                log.error("Error disconnecting from Plc4x Client", e);
                setErrorConnectionStatus(e, null);
                return CompletableFuture.failedFuture(e);
            } finally {
                connection = null;
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    protected CompletableFuture<?> subscribeInternal(
            final @NotNull Plc4xConnection<T> connection, final @NotNull T.Subscription subscription)
            throws Plc4xException {
        if (subscription != null) {
            switch (getReadType()) {
                case Subscribe:
                    if (log.isDebugEnabled()) {
                        log.debug("Subscribing to tag [{}] on connection", subscription.getTagName());
                    }
                    return connection.subscribe(subscription,
                            plcSubscriptionEvent -> processReadResponse(subscription, plcSubscriptionEvent));
                case Read:
                    if (log.isDebugEnabled()) {
                        log.debug("Scheduling read of tag [{}] on connection", subscription.getTagName());
                    }
                    startPolling(new SubscriptionSampler(this.adapterConfig, subscription));
                    break;
            }
        }
        return CompletableFuture.completedFuture(null);
    }


    @Override
    protected CompletableFuture<ProtocolAdapterDataSample<T>> onSamplerInvoked(
            final T config, final AbstractProtocolAdapterConfig.Subscription subscription) {
        if (connection.isConnected()) {
            try {
                CompletableFuture<? extends PlcReadResponse> request =
                        connection.read((Plc4xAdapterConfig.Subscription) subscription);
                return request.thenApply(response -> processReadResponse((Plc4xAdapterConfig.Subscription) subscription,
                        response));
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        return CompletableFuture.completedFuture(null);
    }


    @Override
    protected CompletableFuture<?> captureDataSample(final @NotNull ProtocolAdapterDataSample<T> data) {
        boolean publishData = true;
        if (adapterConfig.getPublishChangedDataOnly()) {
            ProtocolAdapterDataSample<T> previousSample =
                    lastSamples.put(((Plc4xAdapterConfig.Subscription) data.getSubscription()).getTagAddress(), data);
            if (previousSample != null) {
                List<ProtocolAdapterDataSample.DataPoint> dataPoints = previousSample.getDataPoints();
                publishData = !dataPoints.equals(data.getDataPoints());
            }
        }
        if (publishData) {
            return super.captureDataSample(data);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * The protocol Handler is the prefix of the JNDI Connection URI used to instantiate the connection from the factory
     *
     * @return the prefix to use, for example "opcua"
     */
    protected abstract String getProtocolHandler();

    /**
     * Whether to use read or subscription types
     *
     * @return Decides on the mode of reading data from the underlying connection
     */
    protected abstract ReadType getReadType();

    /**
     * Use this hook method to modify the query generated used to read|subscribe to the devices,
     * for the most part this is simply the tagAddress field unchanged from the subscription
     * <p>
     * Default: tagAddress:expectedDataType eg. "0%20:BOOL"
     */
    protected String createTagAddressForSubscription(@NotNull final T.Subscription subscription) {
        return String.format("%s%s%s", subscription.getTagAddress(), TAG_ADDRESS_TYPE_SEP, subscription.getDataType());
    }


    /**
     * Hook method used to populate the URL query parameters on the connection string from
     * the supplied configuration. For example;
     * ?remote-rack=0&remote-slot=3. Each value in the returned map will be encoded onto
     * the final connection string.
     */
    protected Map<String, String> createQueryStringParams(@NotNull final T config) {
        return Collections.emptyMap();
    }

    /**
     * Hook method to format data from source tag
     */
    protected Object convertTagValue(@NotNull final String tag, @NotNull final PlcValue plcValue) {
        return Plc4xDataUtils.convertObject(plcValue);
//        return Plc4xDataUtils.convertNative(plcValue);
    }

    public static String nullSafe(final @Nullable Object o) {
        return Objects.toString(o, null);
    }

    protected ProtocolAdapterDataSample processReadResponse(
            final @NotNull T.Subscription subscription, final @NotNull PlcReadResponse readEvent) {
        PlcResponseCode responseCode = readEvent.getResponseCode(subscription.getTagName());
        if (responseCode == PlcResponseCode.OK) {
            if (getConnectionStatus() == ConnectionStatus.ERROR) {
                //Error was transient
                setConnectionStatus(ConnectionStatus.CONNECTED);
            }
            return processPlcFieldData(subscription, Plc4xDataUtils.readDataFromReadResponse(readEvent));
        } else {
            return null;
        }
    }

    protected ProtocolAdapterDataSample processPlcFieldData(
            final @NotNull T.Subscription subscription, final @NotNull List<Pair<String, PlcValue>> l) {
        ProtocolAdapterDataSample data = new ProtocolAdapterDataSample(subscription);
        //-- For every tag value associated with the sample, write a data point to be published
        if (!l.isEmpty()) {
            l.forEach(pair -> data.addDataPoint(pair.getLeft(), convertTagValue(pair.getLeft(), pair.getValue())));
        }
        return data;
    }
}
