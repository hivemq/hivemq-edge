/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.api.model.adapters;

import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.api.model.ApiConstants;
import com.hivemq.edge.api.model.Status;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.jetbrains.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
public class AdapterStatusModelConversionUtils {

    public static @NotNull Status getAdapterStatus(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        Preconditions.checkNotNull(protocolAdapterWrapper);
        final OffsetDateTime offsetDateTime = protocolAdapterWrapper.getTimeOfLastStartAttempt() == null
                ? null
                : OffsetDateTime.ofInstant(
                        Instant.ofEpochMilli(protocolAdapterWrapper.getTimeOfLastStartAttempt()), ZoneOffset.UTC);
        return new Status()
                .runtime(convertRuntimeStatus(protocolAdapterWrapper.getRuntimeStatus()))
                .connection(convertConnectionStatus(protocolAdapterWrapper.getConnectionStatus()))
                .id(protocolAdapterWrapper.getId())
                .type(ApiConstants.ADAPTER_TYPE)
                .startedAt(offsetDateTime)
                .message(protocolAdapterWrapper.getErrorMessage());
    }

    /**
     * Translates an SDK connection status into the one the API publishes.
     *
     * <p>Every member is named rather than left to the default, and the default is kept only for a value the
     * SDK might add after this is compiled. {@code CONNECTING} was previously unreachable in the product —
     * nothing published it — so falling through cost nothing; once an adapter did publish it, the fall-through
     * answered {@code UNKNOWN} for the whole connect window. That is not a smaller truth, it is a different
     * one: {@code UNKNOWN} means the server cannot say, and consumers reasonably treat it as a fault, whereas
     * an adapter that is connecting is doing exactly what it should. Enumerating the cases also makes the next
     * addition a compile-time question instead of a silent downgrade discovered in the UI.
     */
    public static @NotNull Status.ConnectionEnum convertConnectionStatus(
            final @NotNull ProtocolAdapterState.ConnectionStatus connectionStatus) {
        Preconditions.checkNotNull(connectionStatus);
        return switch (connectionStatus) {
            case DISCONNECTED -> Status.ConnectionEnum.DISCONNECTED;
            case CONNECTED -> Status.ConnectionEnum.CONNECTED;
            case CONNECTING -> Status.ConnectionEnum.CONNECTING;
            case ERROR -> Status.ConnectionEnum.ERROR;
            case STATELESS -> Status.ConnectionEnum.STATELESS;
            case UNKNOWN -> Status.ConnectionEnum.UNKNOWN;
        };
    }

    public static @NotNull Status.RuntimeEnum convertRuntimeStatus(
            final @NotNull ProtocolAdapterState.RuntimeStatus runtimeStatus) {
        Preconditions.checkNotNull(runtimeStatus);
        if (ProtocolAdapterState.RuntimeStatus.STARTED.equals(runtimeStatus)) {
            return Status.RuntimeEnum.STARTED;
        }
        return Status.RuntimeEnum.STOPPED;
    }
}
