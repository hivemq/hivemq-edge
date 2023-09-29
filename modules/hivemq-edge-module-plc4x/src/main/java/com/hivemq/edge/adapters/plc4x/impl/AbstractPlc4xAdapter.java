package com.hivemq.edge.adapters.plc4x.impl;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.impl.AbstractPollingPerSubscriptionAdapter;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.messages.PlcSubscriptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract PLC4X implementation. Exposes core abstractions of the underlying framework so instances can be exposes using the consistent
 * patterns.
 * @author Simon L Johnson
 */
public abstract class AbstractPlc4xAdapter<T extends Plc4xAdapterConfig>
        extends AbstractPollingPerSubscriptionAdapter<T, ProtocolAdapterDataSample> {

    private static final Logger log = LoggerFactory.getLogger(Plc4xAdapterConfig.class);
    private static final @NotNull PlcDriverManager driverManager = new PlcDriverManager();
    private final @NotNull Object lock = new Object();
    private volatile @Nullable Plc4xConnection connection;

    public enum ReadType {
        Read,
        Subscribe
    }

    public AbstractPlc4xAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation, final @NotNull T adapterConfig, final @NotNull MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    protected CompletableFuture<Void> startInternal(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        try {
            if (connection == null) {
                initConnection();
            }
            if (adapterConfig.getSubscriptions() != null) {
                for (T.Subscription subscription : adapterConfig.getSubscriptions()) {
                    subscribeInternal(subscription);
                }
            }
            output.startedSuccessfully("Successfully connected");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            setErrorConnectionStatus(e);
            output.failStart(e, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private void initConnection() throws Plc4xException {
        if (connection == null) {
            synchronized (lock) {
                if (connection == null) {
                    log.info("Creating new Instance Of Plc4x Connector with {}", adapterConfig);
                    connection = new Plc4xConnection<>(driverManager, adapterConfig,
                            plc4xAdapterConfig -> Plc4xDataUtils.createQueryString(
                                    createQueryStringParams(plc4xAdapterConfig), true), false) {
                        @Override
                        protected String getProtocol() {
                            return getProtocolHandler();
                        }
                    };
                    setConnectionStatus(ConnectionStatus.CONNECTED);
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
                setConnectionStatus(ConnectionStatus.DISCONNECTED);
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                log.error("Error disconnecting from Plc4x Client", e);
                setErrorConnectionStatus(e);
                return CompletableFuture.failedFuture(e);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    protected void subscribeInternal(final @NotNull T.Subscription subscription) throws Plc4xException {
        if (subscription != null) {
            switch(getReadType()) {
                case Subscribe:
                    if(log.isDebugEnabled()){
                        log.debug("Subscribing to tag [{}] on connection", subscription.getTagName());
                    }
                    connection.subscribe(subscription,
                            plcSubscriptionEvent ->
                                    processSubscriptionResponse(subscription, (PlcSubscriptionEvent) plcSubscriptionEvent));
                    break;
                case Read:
                    if(log.isDebugEnabled()){
                        log.debug("Scheduling read of tag [{}] on connection", subscription.getTagName());
                    }
                    startPolling(new SubscriptionSampler(this.adapterConfig, subscription));
                    break;
            }
        }
    }

    protected void processSubscriptionResponse(final @NotNull T.Subscription subscription,
                                               final @NotNull PlcSubscriptionEvent subscriptionEvent){
        processPlcFieldData(subscription,
                Plc4xDataUtils.readDataFromSubscriptionEvent(subscriptionEvent));
    }

    protected void processReadResponse(final @NotNull T.Subscription subscription,
                                       final @NotNull PlcReadResponse readEvent){
        processPlcFieldData(subscription,
                Plc4xDataUtils.readDataFromReadResponse(readEvent));
    }

    protected ProtocolAdapterDataSample processPlcFieldData(final @NotNull T.Subscription subscription, final @NotNull List<Pair<String, byte[]>> l){

        ProtocolAdapterDataSample data = new ProtocolAdapterDataSample(null,
                subscription.getDestination(),
                subscription.getQos());
        Object dataValue = null;
        if(!l.isEmpty()){
            if(l.size() > 1){
                Object[] arr = new Object[l.size()];
                for (int i = 0; i < l.size(); i++) {
                    Pair<String, byte[]> p = l.get(i);
                    if (p.getRight() != null && p.getRight().length > 0) {
                        try {
                            if(log.isDebugEnabled()){
                                log.info("Received field {} from plc4x-connection -> {}", p.getLeft(), Plc4xDataUtils.toHex(p.getRight()));
                            }
                            arr[i] = convertTagValue(p.getLeft(), p.getValue());
                        } catch (Exception e) {
                            if(log.isWarnEnabled()){
                                log.warn("Error receiving bytes from plc4x-connection -> field {}", p.getLeft(), e);
                            }
                        }
                    }
                }
            } else {
                //TODO format []?
                dataValue = convertTagValue(l.get(0).getLeft(), l.get(0).getValue());
            }
        }
        data.setData(dataValue);
        return data;
    }

    @Override
    protected ProtocolAdapterDataSample onSamplerInvoked(final T config, final AbstractProtocolAdapterConfig.Subscription subscription)
            throws Exception {
        if (connection.isConnected()) {
            connection.read((Plc4xAdapterConfig.Subscription) subscription,
                    plcResponse ->
                            processReadResponse((Plc4xAdapterConfig.Subscription) subscription, (PlcReadResponse) plcResponse));
        }
        return null;
    }

    /**
     * The protocol Handler is the prefix of the JNDI Connection URI used to instantiate the connection from the factory
     * @return the prefix to use, for example "opcua"
     */
    protected abstract String getProtocolHandler();

    /**
     * Whether to use read or subscription types
     * @return Decides on the mode of reading data from the underlying connection
     */
    protected abstract ReadType getReadType();


    /**
     * Hook method used to populate the URL query parameters on the connection string from
     * the supplied configuration. For example;
     * ?remote-rack=0&remote-slot=3. Each value in the returned map will be encoded onto
     * the final connection string.
     */
    protected Map<String, String> createQueryStringParams(@NotNull final T config){
        return Collections.emptyMap();
    }

    /**
     * Hook method to format data from source tag
     */
    protected byte[] convertTagValue(@NotNull final String tag, @NotNull final byte[] rawdata){
        return rawdata;
    }

    public static String nullSafe(final @Nullable Object o){
        return Objects.toString(o, null);
    }
}
