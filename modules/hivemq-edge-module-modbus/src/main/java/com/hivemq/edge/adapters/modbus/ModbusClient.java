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
package com.hivemq.edge.adapters.modbus;

import com.digitalpetri.modbus.Modbus;
import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.ReadCoilsRequest;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsRequest;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadInputRegistersRequest;
import com.digitalpetri.modbus.tcp.Netty;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;
import com.digitalpetri.modbus.tcp.client.NettyTimeoutScheduler;
import com.digitalpetri.netty.fsm.Event;
import com.digitalpetri.netty.fsm.State;
import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import com.hivemq.edge.adapters.modbus.config.ModbusSpecificAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTag;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTagDefinition;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ModbusClient {

    private static final int DEFAULT_MAX_INPUT_REGISTERS = 125;
    private static final int DEFAULT_MAX_DISCRETE_INPUTS = 2000;

    private final @NotNull ModbusTcpClient client;
    // Written by the client's state machine on each failed connect attempt and cleared when a connect succeeds; read
    // by the polling thread.
    private final @NotNull AtomicReference<Throwable> lastConnectFailure = new AtomicReference<>();
    // Set by the client's state machine when an established connection drops, taken by it when the first reconnect
    // attempt after the drop ends or the client disconnects; read by the polling thread.
    private final @NotNull AtomicReference<CompletableFuture<Void>> reconnectAfterDrop = new AtomicReference<>();

    ModbusClient(final @NotNull ModbusSpecificAdapterConfig adapterConfig) {
        final NettyTcpClientTransport transport = NettyTcpClientTransport.create(cfg -> {
            cfg.hostname = adapterConfig.getHost();
            cfg.port = adapterConfig.getPort();
            cfg.connectTimeout = Duration.ofMillis(adapterConfig.getTimeoutMillis());
        });
        transport.getChannelFsm().addTransitionListener(this::onStateTransition);
        client = ModbusTcpClient.create(
                transport, cfg -> cfg.timeoutScheduler = new NettyTimeoutScheduler(Netty.sharedWheelTimer()));
    }

    @NotNull
    CompletionStage<Void> connect() {
        return client.connectAsync().thenApply(unused -> null);
    }

    @NotNull
    CompletionStage<Void> disconnect() {
        // -- If the client is manually disconnected before connection established ensure we still call into the client
        // -- to shut it all down.
        return client.disconnectAsync().thenApply(unused -> null);
    }

    boolean isConnected() {
        return client.isConnected();
    }

    /**
     * The cause of the last failed connect attempt, or {@code null} once a connect succeeded or before the first
     * attempt ended.
     */
    @Nullable
    Throwable lastConnectFailure() {
        return lastConnectFailure.get();
    }

    /**
     * Completes when the first reconnect attempt after a dropped connection succeeds, and exceptionally with its cause
     * when it fails or the client disconnects; {@code null} when no established connection dropped since that
     * attempt, as for a device that was never reached.
     */
    @Nullable
    CompletableFuture<Void> reconnectAfterDrop() {
        return reconnectAfterDrop.get();
    }

    @NotNull
    CompletionStage<Object> readRegisters(final @NotNull ModbusTag modbusTag) {
        final ModbusTagDefinition def = modbusTag.getDefinition();
        final var dataType = def.getDataType();
        return (switch (def.readType) {
            case HOLDING_REGISTERS -> readHoldingRegisters(def.startIdx, dataType, def.unitId, def.flipRegisters);
            case INPUT_REGISTERS -> readInputRegisters(def.startIdx, dataType, def.unitId, def.flipRegisters);
            case COILS -> readCoils(def.startIdx, def.unitId);
            case DISCRETE_INPUTS -> readDiscreteInput(def.startIdx, def.unitId);
        });
    }

    /**
     * Coils are 1bit.
     */
    @NotNull
    CompletionStage<Object> readCoils(final int startIdx, final int unitId) {
        if (!client.isConnected()) {
            return notConnected();
        }

        return client.readCoilsAsync(unitId, new ReadCoilsRequest(startIdx, Math.min(1, DEFAULT_MAX_DISCRETE_INPUTS)))
                .thenApply(response -> ModbusDataType.BOOL.convert(response.coils(), false));
    }

    /**
     * Discrete registers are 1bit.
     */
    @NotNull
    CompletionStage<Object> readDiscreteInput(final int startIdx, final int unitId) {
        if (!client.isConnected()) {
            return notConnected();
        }

        return client.readDiscreteInputsAsync(
                        unitId, new ReadDiscreteInputsRequest(startIdx, Math.min(1, DEFAULT_MAX_DISCRETE_INPUTS)))
                .thenApply(response -> ModbusDataType.BOOL.convert(response.inputs(), false));
    }

    /**
     * Holding registers are 16bit.
     */
    @NotNull
    CompletionStage<Object> readHoldingRegisters(
            final int startIdx, final @NotNull ModbusDataType dataType, final int unitId, final boolean flipRegisters) {
        if (!client.isConnected()) {
            return notConnected();
        }

        return client.readHoldingRegistersAsync(
                        unitId,
                        new ReadHoldingRegistersRequest(
                                startIdx, Math.min(dataType.nrOfRegistersToRead, DEFAULT_MAX_INPUT_REGISTERS)))
                .thenApply(response -> dataType.convert(response.registers(), flipRegisters));
    }

    /**
     * Inout registers are 16bit.
     */
    @NotNull
    CompletionStage<Object> readInputRegisters(
            final int startIdx, final @NotNull ModbusDataType dataType, final int unitId, final boolean flipRegisters) {
        if (!client.isConnected()) {
            return notConnected();
        }

        return client.readInputRegistersAsync(
                        unitId,
                        new ReadInputRegistersRequest(
                                startIdx, Math.min(dataType.nrOfRegistersToRead, DEFAULT_MAX_INPUT_REGISTERS)))
                .thenApply(response -> dataType.convert(response.registers(), flipRegisters));
    }

    /**
     * A read on a connection that is gone fails, so the poll fails as a whole before any of its values reaches the
     * change cache; an answer of {@code null} would let the other tags of the same poll record values never published.
     */
    private static @NotNull CompletionStage<Object> notConnected() {
        return CompletableFuture.failedFuture(new IllegalStateException("The Modbus client is not connected"));
    }

    /**
     * Runs on the state machine's thread while it holds its lock, so a waiting poll resumes on the client's shared
     * executor instead of inside the transition.
     */
    private void onStateTransition(final @NotNull State from, final @NotNull State to, final @NotNull Event via) {
        if (via instanceof final Event.ConnectFailure failure) {
            lastConnectFailure.set(failure.failure);
        }
        if (to == State.Connected) {
            lastConnectFailure.set(null);
            final CompletableFuture<Void> reconnect = reconnectAfterDrop.getAndSet(null);
            if (reconnect != null) {
                Modbus.sharedExecutor().execute(() -> reconnect.complete(null));
            }
        } else if (from == State.Connected && to == State.ReconnectWait) {
            reconnectAfterDrop.set(new CompletableFuture<>());
        } else if ((from == State.Reconnecting && to == State.ReconnectWait) || to == State.NotConnected) {
            final CompletableFuture<Void> reconnect = reconnectAfterDrop.getAndSet(null);
            if (reconnect != null) {
                final Throwable cause = via instanceof final Event.ConnectFailure failure
                        ? failure.failure
                        : new IllegalStateException("Modbus client disconnected");
                Modbus.sharedExecutor().execute(() -> reconnect.completeExceptionally(cause));
            }
        }
    }
}
