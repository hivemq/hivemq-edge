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

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.config.Plc4xAdapterConfig;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTagDefinition;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.spi.messages.DefaultPlcReadResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;

/**
 * Abstract PLC4X implementation. Exposes core abstractions of the underlying framework so instances can be exposes
 * using the consistent
 * patterns.
 *
 * @author Simon L Johnson
 */
public abstract class AbstractPlc4xAdapter<T extends Plc4xAdapterConfig<?>, C extends Plc4xToMqttMapping>
        implements PollingProtocolAdapter<C> {

    protected static final String TAG_ADDRESS_TYPE_SEP = ":";
    private final Logger log = LoggerFactory.getLogger(getClass());
    protected final static @NotNull PlcDriverManager driverManager = PlcDriverManager.getDefault();
    private final @NotNull Object lock = new Object();
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    protected final @NotNull T adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    protected final @NotNull AdapterFactories adapterFactories;
    protected final @NotNull ProtocolAdapterTagService protocolAdapterTagService;
    protected volatile @Nullable Plc4xConnection<T> connection;
    private final @NotNull Map<String, ProtocolAdapterDataSample> lastSamples = new HashMap<>(1);

    public enum ReadType {
        Read,
        Subscribe
    }

    public AbstractPlc4xAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation, final ProtocolAdapterInput<T> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.adapterFactories = input.adapterFactories();
        this.protocolAdapterTagService = input.moduleServices().protocolAdapterTagService();
    }

    @Override
    public void poll(final @NotNull PollingInput pollingInput, @NotNull final PollingOutput pollingOutput) {
        final Plc4xConnection<T> tempConnection = connection;
        if (tempConnection != null && tempConnection.isConnected()) {
            final Plc4xToMqttMapping plc4xToMqttMapping = (Plc4xToMqttMapping) pollingInput.getPollingContext();
            final String tagName = plc4xToMqttMapping.getTagName();

            findTag(tagName)
                    .ifPresentOrElse(
                            def -> tempConnection.read(plc4xToMqttMapping)
                                    .thenApply(response -> processReadResponse((Plc4xToMqttMapping) pollingInput.getPollingContext(),
                                            response))
                                    .thenApply(data -> captureDataSample(data, def))
                                    .whenComplete((sample, t) -> handleDataAndExceptions(sample, t, pollingOutput)),
                            () -> pollingOutput.fail("Polling for protocol adapter failed because the used tag '" +
                                    tagName +
                                    "' was not found. For the polling to work the tag must be created via REST API or the UI.")
                    );
        }
    }

    private @NotNull Optional<? extends Tag> findTag(String tagName) {
        return adapterConfig.getTags().stream().filter(tag -> tag.getName().equals(tagName)).findFirst();
    }

    protected void handleDataAndExceptions(
            final @NotNull ProtocolAdapterDataSample dataSample,
            final @Nullable Throwable throwable,
            final @NotNull PollingOutput pollingOutput) {
        if (throwable != null) {
            pollingOutput.fail(throwable, null);
        } else {
            for (final DataPoint dataPoint : dataSample.getDataPoints()) {
                pollingOutput.addDataPoint(dataPoint);
            }
            pollingOutput.finish();
        }
    }

    @Override
    public @NotNull List<C> getPollingContexts() {
        if (getReadType() == ReadType.Read) {
            return (List<C>) adapterConfig.getPlc4xToMqttConfig().getMappings();
        } else {
            return List.of();
        }
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public void start(
            @NotNull final ProtocolAdapterStartInput input, @NotNull final ProtocolAdapterStartOutput output) {
        try {
            subscribeAllInternal(initConnection());
            output.startedSuccessfully();
        } catch (final Exception e) {
            output.failStart(e, null);
        }
    }

    @Override
    public void stop(@NotNull final ProtocolAdapterStopInput input, @NotNull final ProtocolAdapterStopOutput output) {
        final Plc4xConnection<T> tempConnection = connection;
        connection = null;
        if (tempConnection != null) {
            try {
                //-- Disconnect client
                tempConnection.disconnect();
            } catch (final Exception e) {
                protocolAdapterState.setErrorConnectionStatus(e, null);
                output.failStop(e, "Error disconnecting from PLC4X client");
            }
        }
        output.stoppedSuccessfully();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    private Plc4xConnection<T> initConnection() {
        if (connection == null) {
            synchronized (lock) {
                if (connection == null) {
                    try {
                        if (log.isTraceEnabled()) {
                            log.trace("Creating new instance of Plc4x connector with {}.", adapterConfig);
                        }
                        connection = createConnection();
                        protocolAdapterState.setConnectionStatus(CONNECTED);
                        return connection;
                    } catch (final Plc4xException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return connection;
    }

    protected @NotNull Plc4xConnection<T> createConnection() throws Plc4xException {
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
            protected @NotNull String getTagAddressForSubscription(final Plc4xToMqttMapping context) {
                return findTag(context.getTagName())
                        .map(tag -> createTagAddressForSubscription(context, tag))
                        .orElseThrow(); //TODO this sucks
            }
        };
    }

    protected void subscribeAllInternal(@NotNull final Plc4xConnection<T> connection) throws RuntimeException {
        for (final Plc4xToMqttMapping mapping : adapterConfig.getPlc4xToMqttConfig().getMappings()) {
            try {
                subscribeInternal(connection, mapping);
            } catch (final Plc4xException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected @NotNull CompletableFuture<?> subscribeInternal(
            final @NotNull Plc4xConnection<T> connection, final @NotNull Plc4xToMqttMapping subscription)
            throws Plc4xException {
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
        return CompletableFuture.completedFuture(null);
    }


    protected @Nullable ProtocolAdapterDataSample captureDataSample(
            final @NotNull ProtocolAdapterDataSample data, final @NotNull Tag<Plc4xTagDefinition> plc4xTag) {
        boolean publishData = true;
        final String tagAddress = plc4xTag.getDefinition().getTagAddress();

        if (adapterConfig.getPlc4xToMqttConfig().getPublishChangedDataOnly()) {
            final ProtocolAdapterDataSample previousSample = lastSamples.put(tagAddress, data);
            if (previousSample != null) {
                final List<DataPoint> dataPoints = previousSample.getDataPoints();
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
    protected @NotNull String createTagAddressForSubscription(final @NotNull Plc4xToMqttMapping subscription, final @NotNull Tag<Plc4xTagDefinition> tag) {
        final String tagAddress = tag.getDefinition().getTagAddress();
        return String.format("%s%s%s", tagAddress, TAG_ADDRESS_TYPE_SEP, subscription.getDataType());
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
            final @NotNull Plc4xToMqttMapping subscription, final @NotNull PlcReadResponse readEvent) {
        //it is possible that the read response does not contain any values at all, leading to unexpected error states, especially with EIP adapter
        if (!(readEvent instanceof DefaultPlcReadResponse) ||
                ((DefaultPlcReadResponse) readEvent).getValues().containsKey(subscription.getTagName())) {
            final PlcResponseCode responseCode = readEvent.getResponseCode(subscription.getTagName());
            if (responseCode == PlcResponseCode.OK) {
                if (protocolAdapterState.getConnectionStatus() == ProtocolAdapterState.ConnectionStatus.ERROR) {
                    //Error was transient
                    protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
                }
                return processPlcFieldData(subscription, Plc4xDataUtils.readDataFromReadResponse(readEvent));
            }
        }
        return new Plc4xDataSample<>(adapterFactories.dataPointFactory());
    }

    protected @NotNull ProtocolAdapterDataSample processPlcFieldData(
            final @NotNull Plc4xToMqttMapping subscription, final @NotNull List<Pair<String, PlcValue>> l) {
        final ProtocolAdapterDataSample data = new Plc4xDataSample<>(adapterFactories.dataPointFactory());
        //-- For every tag value associated with the sample, write a data point to be published
        if (!l.isEmpty()) {
            l.forEach(pair -> data.addDataPoint(pair.getLeft(), convertTagValue(pair.getLeft(), pair.getValue())));
        }
        return data;
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getPlc4xToMqttConfig().getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getPlc4xToMqttConfig().getMaxPollingErrorsBeforeRemoval();
    }
}
