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
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.edge.modules.adapters.PollingPerSubscriptionProtocolAdapter;
import com.hivemq.edge.modules.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.adapters.config.AdapterSubscription;
import com.hivemq.edge.modules.adapters.data.DataPoint;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.factories.AdapterFactories;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.adapters.state.ProtocolAdapterState;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.spi.messages.DefaultPlcReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.hivemq.edge.modules.adapters.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;

/**
 * Abstract PLC4X implementation. Exposes core abstractions of the underlying framework so instances can be exposes
 * using the consistent
 * patterns.
 *
 * @author Simon L Johnson
 */
public abstract class AbstractPlc4xAdapter<T extends Plc4xAdapterConfig>
        implements PollingPerSubscriptionProtocolAdapter {

    protected static final String TAG_ADDRESS_TYPE_SEP = ":";
    private final Logger log = LoggerFactory.getLogger(getClass());
    protected final static @NotNull PlcDriverManager driverManager = PlcDriverManager.getDefault();
    private final @NotNull Object lock = new Object();
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    protected final @NotNull T adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    protected final @NotNull AdapterFactories adapterFactories;
    protected volatile @Nullable Plc4xConnection connection;
    private final @NotNull Map<String, ProtocolAdapterDataSample> lastSamples = new HashMap<>(1);

    public enum ReadType {
        Read,
        Subscribe
    }

    public AbstractPlc4xAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final ProtocolAdapterInput<T> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.adapterFactories = input.adapterFactories();
    }

    @Override
    public @NotNull CompletableFuture<? extends ProtocolAdapterDataSample> poll(@NotNull final AdapterSubscription adapterSubscription) {
        if (connection.isConnected()) {
            try {
                CompletableFuture<? extends PlcReadResponse> request =
                        connection.read((Plc4xAdapterConfig.AdapterSubscriptionImpl) adapterSubscription);
                return request.thenApply(response -> processReadResponse((Plc4xAdapterConfig.AdapterSubscriptionImpl) adapterSubscription,
                        response)).thenApply(this::captureDataSample);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        return CompletableFuture.completedFuture(new ProtocolAdapterDataSampleImpl<>(adapterSubscription,
                adapterFactories.dataPointFactory())).thenApply(this::captureDataSample);
    }

    @Override
    public @NotNull List<? extends AdapterSubscription> getSubscriptions() {
        if (getReadType() == ReadType.Read) {
            return adapterConfig.getSubscriptions();
        } else {
            return List.of();
        }
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public @NotNull CompletableFuture<ProtocolAdapterStartOutput> start(
            @NotNull final ProtocolAdapterStartInput input, @NotNull final ProtocolAdapterStartOutput output) {
        CompletableFuture<Plc4xConnection<T>> startFuture = CompletableFuture.supplyAsync(this::initConnection);
        startFuture.thenAccept(this::subscribeAllInternal);
        return startFuture.thenApply(connection -> output);
    }

    @Override
    public @NotNull CompletableFuture<Void> stop() {
        if (connection != null) {
            try {
                //-- Disconnect client
                connection.disconnect();
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Error disconnecting from PLC4X client", e);
                }
                protocolAdapterState.setErrorConnectionStatus(adapterConfig.getId(),
                        adapterInformation.getProtocolId(),
                        e,
                        null);
                return CompletableFuture.failedFuture(e);
            } finally {
                connection = null;
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    private @NotNull Plc4xConnection initConnection() {
        if (connection == null) {
            synchronized (lock) {
                if (connection == null) {
                    try {
                        if (log.isTraceEnabled()) {
                            log.trace("Creating new instance of Plc4x connector with {}.", adapterConfig);
                        }
                        connection = createConnection();
                        protocolAdapterState.setConnectionStatus(CONNECTED);
                    } catch (Plc4xException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return connection;
    }

    protected @NotNull Plc4xConnection<?> createConnection() throws Plc4xException {
        return new Plc4xConnection<>(driverManager,
                adapterConfig,
                plc4xAdapterConfig -> Plc4xDataUtils.createQueryString(createQueryStringParams(plc4xAdapterConfig),
                        true)) {
            @Override
            protected @NotNull String getProtocol() {
                return getProtocolHandler();
            }

            @Override
            protected @NotNull String createConnectionString(final T config) {
                return super.createConnectionString(config);
            }

            @Override
            protected @NotNull String getTagAddressForSubscription(final Plc4xAdapterConfig.@NotNull AdapterSubscriptionImpl subscription) {
                return createTagAddressForSubscription(subscription);
            }
        };
    }

    protected void subscribeAllInternal(@NotNull final Plc4xConnection<T> connection) throws RuntimeException {
        if (adapterConfig.getSubscriptions() != null) {
            for (Plc4xAdapterConfig.AdapterSubscriptionImpl subscription : adapterConfig.getSubscriptions()) {
                try {
                    subscribeInternal(connection, subscription);
                } catch (Plc4xException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected @NotNull CompletableFuture<?> subscribeInternal(
            final @NotNull Plc4xConnection<T> connection,
            final @NotNull Plc4xAdapterConfig.AdapterSubscriptionImpl subscription) throws Plc4xException {
        if (subscription != null) {
            switch (getReadType()) {
                case Subscribe:
                    if (log.isTraceEnabled()) {
                        log.trace("Subscribing to tag [{}] on connection.", subscription.getTagName());
                    }
                    return connection.subscribe(subscription,
                            plcSubscriptionEvent -> processReadResponse(subscription, plcSubscriptionEvent));
                case Read:
                    // NOOP, handled internally, see getSubscriptions();
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @NotNull
    protected ProtocolAdapterDataSample captureDataSample(final @NotNull ProtocolAdapterDataSample data) {
        boolean publishData = true;
        if (adapterConfig.getPublishChangedDataOnly()) {
            ProtocolAdapterDataSample previousSample =
                    lastSamples.put(((Plc4xAdapterConfig.AdapterSubscriptionImpl) data.getSubscription()).getTagAddress(),
                            data);
            if (previousSample != null) {
                List<DataPoint> dataPoints = previousSample.getDataPoints();
                publishData = !dataPoints.equals(data.getDataPoints());
            }
        }
        if (publishData) {
            return data;
        }
        return null;
    }

    /**
     * The protocol Handler is the prefix of the JNDI Connection URI used to instantiate the connection from the factory
     *
     * @return the prefix to use, for example "opcua"
     */
    protected abstract @NotNull String getProtocolHandler();

    /**
     * Whether to use read or subscription types
     *
     * @return Decides on the mode of reading data from the underlying connection
     */
    protected abstract @NotNull ReadType getReadType();

    /**
     * Use this hook method to modify the query generated used to read|subscribe to the devices,
     * for the most part this is simply the tagAddress field unchanged from the subscription
     * <p>
     * Default: tagAddress:expectedDataType eg. "0%20:BOOL"
     */
    protected @NotNull String createTagAddressForSubscription(@NotNull final Plc4xAdapterConfig.AdapterSubscriptionImpl subscription) {
        return String.format("%s%s%s", subscription.getTagAddress(), TAG_ADDRESS_TYPE_SEP, subscription.getDataType());
    }


    /**
     * Hook method used to populate the URL query parameters on the connection string from
     * the supplied configuration. For example;
     * ?remote-rack=0&remote-slot=3. Each value in the returned map will be encoded onto
     * the final connection string.
     */
    protected @NotNull Map<String, String> createQueryStringParams(@NotNull final T config) {
        return Collections.emptyMap();
    }

    /**
     * Hook method to format data from source tag
     */
    protected @NotNull Object convertTagValue(@NotNull final String tag, @NotNull final PlcValue plcValue) {
        return Plc4xDataUtils.convertObject(plcValue);
//        return Plc4xDataUtils.convertNative(plcValue);
    }

    public static @NotNull String nullSafe(final @Nullable Object o) {
        return Objects.toString(o, null);
    }

    protected @NotNull ProtocolAdapterDataSample processReadResponse(
            final @NotNull Plc4xAdapterConfig.AdapterSubscriptionImpl subscription,
            final @NotNull PlcReadResponse readEvent) {
        //it is possible that the read response does not contain any values at all, leading to unexpected error states, especially with EIP adapter
        if (!(readEvent instanceof DefaultPlcReadResponse) ||
                ((DefaultPlcReadResponse) readEvent).getValues().containsKey(subscription.getTagName())) {
            PlcResponseCode responseCode = readEvent.getResponseCode(subscription.getTagName());
            if (responseCode == PlcResponseCode.OK) {
                if (protocolAdapterState.getConnectionStatus() == ProtocolAdapterState.ConnectionStatus.ERROR) {
                    //Error was transient
                    protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
                }
                return processPlcFieldData(subscription, Plc4xDataUtils.readDataFromReadResponse(readEvent));
            }
        }
        return new ProtocolAdapterDataSampleImpl<>(subscription, adapterFactories.dataPointFactory());
    }

    protected @NotNull ProtocolAdapterDataSample processPlcFieldData(
            final @NotNull Plc4xAdapterConfig.AdapterSubscriptionImpl subscription,
            final @NotNull List<Pair<String, PlcValue>> l) {
        ProtocolAdapterDataSample data = new ProtocolAdapterDataSampleImpl<>(subscription,
                adapterFactories.dataPointFactory());
        //-- For every tag value associated with the sample, write a data point to be published
        if (!l.isEmpty()) {
            l.forEach(pair -> data.addDataPoint(pair.getLeft(), convertTagValue(pair.getLeft(), pair.getValue())));
        }
        return data;
    }
}
