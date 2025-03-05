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
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.config.Plc4XSpecificAdapterConfig;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
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

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;

/**
 * Abstract PLC4X implementation. Exposes core abstractions of the underlying framework so instances can be exposes
 * using the consistent
 * patterns.
 *
 * @author Simon L Johnson
 */
public abstract class AbstractPlc4xAdapter<T extends Plc4XSpecificAdapterConfig<?>, C extends Plc4xToMqttMapping>
        implements BatchPollingProtocolAdapter {

    protected static final String TAG_ADDRESS_TYPE_SEP = ":";
    private final Logger log = LoggerFactory.getLogger(getClass());
    protected static final @NotNull PlcDriverManager driverManager = PlcDriverManager.getDefault();
    private final @NotNull Object lock = new Object();
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    protected final @NotNull T adapterConfig;
    protected final @NotNull List<Plc4xTag> tags;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    protected final @NotNull AdapterFactories adapterFactories;
    private final @NotNull String adapterId;
    protected volatile @Nullable Plc4xConnection<T> connection;
    private final @NotNull Map<String, Plc4xDataSample> lastSamples = new HashMap<>(1);

    public enum ReadType {
        Read,
        Subscribe
    }

    public AbstractPlc4xAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation, final ProtocolAdapterInput<T> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.adapterFactories = input.adapterFactories();
        this.tags = input.getTags().stream().map(tag -> (Plc4xTag)tag).toList();
    }

    @Override
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        final Plc4xConnection<T> tempConnection = connection;
        if (tempConnection != null && tempConnection.isConnected()) {
                tempConnection.read(tags)
                    .thenApply(response -> processReadResponse(tags, response))
                    .whenComplete((sample, t) -> {
                        if (t != null) {
                            pollingOutput.fail(t, null);
                        } else {
                            sample.getDataPoints().forEach(pollingOutput::addDataPoint);
                            pollingOutput.finish();
                        }
                    });
        } else {
            pollingOutput.fail("Polling failed for adapter '" + adapterId + "' because the connection was null.");
        }
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        try {
            // we do not subscribe anymore as no current adapter type supports it anyway
            initConnection();
            output.startedSuccessfully();
        } catch (final Exception e) {
            output.failStart(e, null);
        }
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
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
            protected @NotNull String getTagAddressForSubscription(final Plc4xTag tag) {
                return createTagAddressForSubscription(tag);
            }
        };
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
    protected @NotNull String createTagAddressForSubscription(final @NotNull Plc4xTag tag) {
        final String tagAddress = tag.getDefinition().getTagAddress();
        return String.format("%s%s%s", tagAddress, TAG_ADDRESS_TYPE_SEP, tag.getDefinition().getDataType());
    }


    /**
     * Hook method used to populate the URL query parameters on the connection string from
     * the supplied configuration. For example;
     * ?remote-rack=0&remote-slot=3. Each value in the returned map will be encoded onto
     * the final connection string.
     */
    protected @NotNull Map<String, String> createQueryStringParams(final @NotNull T config) {
        return Collections.emptyMap();
    }

    /**
     * Hook method to format data from source tag
     */
    protected @NotNull Object convertTagValue(final @NotNull String tag, final @NotNull PlcValue plcValue) {
        return Plc4xDataUtils.convertObject(plcValue);
//        return Plc4xDataUtils.convertNative(plcValue);
    }

    public static @NotNull String nullSafe(final @Nullable Object o) {
        return Objects.toString(o, null);
    }

    protected @NotNull Plc4xDataSample processReadResponse(
            final @NotNull List<Plc4xTag> tags, final @NotNull PlcReadResponse readEvent) {
        //it is possible that the read response does not contain any values at all, leading to unexpected error states
        if (readEvent instanceof DefaultPlcReadResponse event) {
            if (tags.stream().allMatch(tag -> event.getResponseCode(tag.getName()) == PlcResponseCode.OK)) {
                if (protocolAdapterState.getConnectionStatus() == ProtocolAdapterState.ConnectionStatus.ERROR) {
                    //Error was transient
                    protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
                }
                return processPlcFieldData(Plc4xDataUtils.readDataFromReadResponse(event));
            }
        }
        return new Plc4xDataSample(adapterFactories.dataPointFactory());
    }

    protected @NotNull Plc4xDataSample processPlcFieldData(
            final @NotNull List<Pair<String, PlcValue>> l) {
        final Plc4xDataSample data = new Plc4xDataSample(adapterFactories.dataPointFactory());
        //-- For every tag value associated with the sample, write a data point to be published
        if (!l.isEmpty()) {
            l.forEach(pair -> data.addDataPoint(pair.getLeft(), convertTagValue(pair.getLeft(), pair.getValue())));
        }
        return data;
    }

    @Override
    public int getPollingIntervalMillis() {
        if (adapterConfig.getPlc4xToMqttConfig() != null) {
            return adapterConfig.getPlc4xToMqttConfig().getPollingIntervalMillis();
        } else {
            return 1000;
        }
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        if (adapterConfig.getPlc4xToMqttConfig() != null) {
            return adapterConfig.getPlc4xToMqttConfig().getMaxPollingErrorsBeforeRemoval();
        } else {
            return 5;
        }
    }
}
