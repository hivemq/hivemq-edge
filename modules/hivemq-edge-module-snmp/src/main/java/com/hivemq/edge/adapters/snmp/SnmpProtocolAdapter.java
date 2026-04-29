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
package com.hivemq.edge.adapters.snmp;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.discovery.NodeTree;
import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.snmp.config.SnmpDataType;
import com.hivemq.edge.adapters.snmp.config.SnmpSpecificAdapterConfig;
import com.hivemq.edge.adapters.snmp.config.SnmpToMqttConfig;
import com.hivemq.edge.adapters.snmp.config.SnmpVersion;
import com.hivemq.edge.adapters.snmp.config.tag.SnmpTag;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.VariableBinding;

/**
 * SNMP Protocol Adapter for HiveMQ Edge.
 * Polls SNMP agents and publishes values to MQTT topics.
 */
public class SnmpProtocolAdapter implements BatchPollingProtocolAdapter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SnmpProtocolAdapter.class);

    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull SnmpSpecificAdapterConfig config;
    private final @NotNull ProtocolAdapterState state;
    private final @NotNull List<SnmpTag> tags;
    private final @NotNull SnmpClientFactory clientFactory;
    private final @NotNull AtomicBoolean started = new AtomicBoolean(false);
    // One-way latch: once stop() is called this adapter instance is permanently retired.
    private final @NotNull AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final @NotNull ConcurrentHashMap<String, Optional<Object>> lastValues = new ConcurrentHashMap<>();

    private @Nullable SnmpClient client;

    public SnmpProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<SnmpSpecificAdapterConfig> input) {
        this(adapterInformation, input, SnmpClient::new);
    }

    public SnmpProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<SnmpSpecificAdapterConfig> input,
            final @NotNull SnmpClientFactory clientFactory) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.config = input.getConfig();
        this.state = input.getProtocolAdapterState();
        this.tags = input.getTags().stream().map(t -> (SnmpTag) t).toList();
        this.clientFactory = clientFactory;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        if (stopRequested.get()) {
            output.failStart(new IllegalStateException("Stop requested"), "Adapter is stopping");
            return;
        }

        if (!started.compareAndSet(false, true)) {
            output.failStart(new IllegalStateException("Already started"), "Adapter already started");
            return;
        }

        try {
            final SnmpClient newClient = clientFactory.create(config);
            newClient.open();
            client = newClient;

            // Guard against a concurrent stop() that arrived before we assigned client.
            if (stopRequested.get()) {
                closeClientQuietly(client);
                client = null;
                started.set(false);
                state.setConnectionStatus(DISCONNECTED);
                output.failStart(new IllegalStateException("Stop requested"), "Adapter is stopping");
                return;
            }

            if (client.testConnection()) {
                state.setConnectionStatus(CONNECTED);
                output.startedSuccessfully();
                log.info("SNMP adapter {} started - connected to {}:{}", adapterId, config.getHost(), config.getPort());
            } else {
                closeClientQuietly(client);
                client = null;
                started.set(false);
                state.setConnectionStatus(ERROR);
                output.failStart(
                        new RuntimeException("Connection test failed"),
                        "Failed to connect to SNMP agent at " + config.getHost() + ":" + config.getPort());
            }

        } catch (final Exception e) {
            state.setErrorConnectionStatus(e, "Failed to initialize SNMP client");
            output.failStart(e, "Failed to start SNMP adapter: " + e.getMessage());
            started.set(false);
            log.error("Failed to start SNMP adapter {}", adapterId, e);
        }
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        if (!stopRequested.compareAndSet(false, true)) {
            output.stoppedSuccessfully();
            return;
        }

        log.info("Stopping SNMP adapter {}", adapterId);
        lastValues.clear();

        try {
            if (client != null) {
                client.close();
                client = null;
            }
            started.set(false);
            state.setConnectionStatus(DISCONNECTED);
            output.stoppedSuccessfully();
            log.info("SNMP adapter {} stopped successfully", adapterId);
        } catch (final Exception e) {
            log.error("Error stopping SNMP adapter {}", adapterId, e);
            output.failStop(e, "Failed to stop SNMP client: " + e.getMessage());
        }
    }

    @Override
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        if (!started.get() || client == null || stopRequested.get()) {
            pollingOutput.fail(new IllegalStateException("Adapter not running"), "Adapter is not started");
            return;
        }

        if (tags.isEmpty()) {
            pollingOutput.dataPointListPublisher().publish();
            return;
        }

        // Fan out all SNMP GETs on virtual threads, then collect results sequentially.
        final List<Future<TagReadResult>> futures;
        try (final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            futures = tags.stream()
                    .map(tag -> executor.submit(() -> readTag(tag)))
                    .toList();
        } // close() blocks until all submitted tasks complete

        final boolean publishChangedDataOnly = config.getSnmpToMqttConfig() != null
                && config.getSnmpToMqttConfig().getPublishChangedDataOnly();

        final var publisher = pollingOutput.dataPointListPublisher();
        int published = 0;
        int failed = 0;
        for (int i = 0; i < futures.size(); i++) {
            final SnmpTag tag = tags.get(i);
            try {
                final TagReadResult result = futures.get(i).get();
                if (!publishChangedDataOnly || hasValueChanged(tag.getName(), result.result().value())) {
                    publishTagResult(publisher, result);
                    published++;
                }
            } catch (final ExecutionException e) {
                final Throwable cause = e.getCause();
                log.warn(
                        "[{}] Failed to read OID {} for tag {}: {}",
                        adapterId,
                        tag.getDefinition().getOid(),
                        tag.getName(),
                        cause != null ? cause.getMessage() : e.getMessage());
                failed++;
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                state.setConnectionStatus(ERROR);
                pollingOutput.fail(e, "Polling interrupted");
                return;
            }
        }
        log.debug("[{}] Poll complete: {} published, {} failed", adapterId, published, failed);
        // Only mark ERROR if every attempted tag failed; partial failures keep CONNECTED.
        state.setConnectionStatus(published == 0 && failed > 0 ? ERROR : CONNECTED);
        publisher.publish();
    }

    private @NotNull TagReadResult readTag(final @NotNull SnmpTag tag) throws IOException {
        final SnmpClient snmpClient = client;
        if (snmpClient == null) {
            throw new IOException("SNMP client is not initialized");
        }
        final String oid = tag.getDefinition().getOid();
        final SnmpReadResult raw = snmpClient.get(oid);
        final SnmpDataType hint = tag.getDefinition().getDataType();
        final Object coerced = hint == SnmpDataType.AUTO ? raw.value() : applyHint(raw.value(), hint);
        return new TagReadResult(tag, oid, new SnmpReadResult(coerced, raw.rawType()));
    }

    private @Nullable Object applyHint(final @Nullable Object value, final @NotNull SnmpDataType hint) {
        if (value == null) {
            return null;
        }
        return switch (hint) {
            case INTEGER -> value instanceof Number n ? n.intValue() : value;
            case STRING, IP_ADDRESS, OID, OPAQUE -> value.toString();
            case COUNTER32, COUNTER64, GAUGE -> value instanceof Number n ? n.longValue() : value;
            case TIMETICKS -> value instanceof Number n ? n.doubleValue() : value;
            case AUTO -> value;
        };
    }

    private boolean hasValueChanged(final @NotNull String tagName, final @Nullable Object newValue) {
        final Optional<Object> wrapped = Optional.ofNullable(newValue);
        return !wrapped.equals(lastValues.put(tagName, wrapped));
    }

    private void publishTagResult(
            final @NotNull DataPointListBuilder publisher, final @NotNull TagReadResult tagResult) {
        final var metaBuilder = publisher
                .addDataPoint(tagResult.tag())
                .startObjectMetadata()
                .put("oid", tagResult.oid())
                .put("dataType", tagResult.tag().getDefinition().getDataType().name())
                .put("rawType", tagResult.result().rawType())
                .put("snmpVersion", config.getSnmpVersion().name());

        if (config.getSnmpVersion() == SnmpVersion.V3) {
            if (config.getSecurityName() != null) {
                metaBuilder.put("securityName", config.getSecurityName());
            }
        } else {
            metaBuilder.put("community", config.getCommunity());
        }

        setTypedValue(metaBuilder.endObject(), tagResult.result().value());
    }

    private void setTypedValue(
            final @NotNull DataPointBuilder<DataPointListBuilder> builder, final @Nullable Object value) {
        switch (value) {
            case Integer v -> builder.value(v);
            case Long v -> builder.value(v);
            case Double v -> builder.value(v);
            case null -> builder.valueNull();
            default -> builder.value(value.toString());
        }
        builder.endDataPoint();
    }

    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {
        final NodeTree tree = output.getNodeTree();

        if (input.getRootNode() == null) {
            tree.addNode(
                    "mib2-system",
                    "System",
                    "1.3.6.1.2.1.1",
                    "System MIB - sysDescr, sysObjectID, sysUpTime, sysContact, sysName, sysLocation, sysServices",
                    null,
                    NodeType.FOLDER,
                    false);
            tree.addNode(
                    "mib2-interfaces",
                    "Interfaces",
                    "1.3.6.1.2.1.2",
                    "Network interface statistics - ifNumber, ifTable",
                    null,
                    NodeType.FOLDER,
                    false);
            tree.addNode(
                    "mib2-ip",
                    "IP",
                    "1.3.6.1.2.1.4",
                    "IP statistics - forwarding, routing, addresses",
                    null,
                    NodeType.FOLDER,
                    false);
            tree.addNode(
                    "mib2-tcp",
                    "TCP",
                    "1.3.6.1.2.1.6",
                    "TCP statistics - connections, segments",
                    null,
                    NodeType.FOLDER,
                    false);
            tree.addNode(
                    "mib2-udp", "UDP", "1.3.6.1.2.1.7", "UDP statistics - datagrams", null, NodeType.FOLDER, false);
            tree.addNode(
                    "mib2-host",
                    "Host Resources",
                    "1.3.6.1.2.1.25",
                    "Host resources MIB - storage, processors, software, devices",
                    null,
                    NodeType.FOLDER,
                    false);
            output.finish();
            return;
        }

        final String rootOid = input.getRootNode();
        final SnmpClient snmpClient = client;
        if (snmpClient != null) {
            try {
                final List<VariableBinding> results = snmpClient.walk(rootOid);
                for (final VariableBinding vb : results) {
                    final String oid = vb.getOid().toString();
                    final String value = vb.getVariable().toString();
                    final String displayValue = value.length() > 50 ? value.substring(0, 47) + "..." : value;
                    tree.addNode(
                            oid,
                            getLastOidSegment(oid),
                            oid,
                            "Value: " + displayValue,
                            rootOid,
                            NodeType.VALUE,
                            true);
                }
                log.debug("[{}] Discovery walk from {} found {} OIDs", adapterId, rootOid, results.size());
            } catch (final IOException e) {
                log.warn("[{}] SNMP WALK failed for {}: {}", adapterId, rootOid, e.getMessage());
                state.reportErrorMessage(e, "Discovery failed: " + e.getMessage(), false);
            }
        }

        output.finish();
    }

    private @NotNull String getLastOidSegment(final @NotNull String oid) {
        final int lastDot = oid.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < oid.length() - 1) {
            return oid.substring(lastDot + 1);
        }
        return oid;
    }

    private void closeClientQuietly(final @Nullable SnmpClient snmpClient) {
        if (snmpClient == null) {
            return;
        }
        try {
            snmpClient.close();
        } catch (final Exception e) {
            log.warn("[{}] Error closing SNMP client: {}", adapterId, e.getMessage());
        }
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public int getPollingIntervalMillis() {
        final SnmpToMqttConfig mqttConfig = config.getSnmpToMqttConfig();
        return mqttConfig != null
                ? mqttConfig.getPollingIntervalMillis()
                : SnmpToMqttConfig.DEFAULT_POLLING_INTERVAL_MILLIS;
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        final SnmpToMqttConfig mqttConfig = config.getSnmpToMqttConfig();
        return mqttConfig != null
                ? mqttConfig.getMaxPollingErrorsBeforeRemoval()
                : SnmpToMqttConfig.DEFAULT_MAX_POLLING_ERRORS_BEFORE_REMOVAL;
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    private record TagReadResult(
            @NotNull SnmpTag tag,
            @NotNull String oid,
            @NotNull SnmpReadResult result) {}
}
