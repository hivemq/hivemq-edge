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
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * @author Simon L Johnson
 */
public class AdapterStatusModelConversionUtils {


    public static @NotNull Status getAdapterStatus(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        Preconditions.checkNotNull(protocolAdapterWrapper);
        return new Status().runtime(convertRuntimeStatus(protocolAdapterWrapper.getRuntimeStatus()))
                .connection(convertConnectionStatus(protocolAdapterWrapper.getConnectionStatus()))
                .id(protocolAdapterWrapper.getId())
                .type(ApiConstants.ADAPTER_TYPE)
                .startedAt(OffsetDateTime.ofInstant(Instant.ofEpochMilli(protocolAdapterWrapper.getTimeOfLastStartAttempt()),
                        ZoneOffset.UTC))
                .message(protocolAdapterWrapper.getErrorMessage());
    }

    public static @NotNull Status.ConnectionEnum convertConnectionStatus(final @NotNull ProtocolAdapterState.ConnectionStatus connectionStatus) {
        Preconditions.checkNotNull(connectionStatus);
        switch (connectionStatus) {
            case DISCONNECTED:
                return Status.ConnectionEnum.DISCONNECTED;
            case CONNECTED:
                return Status.ConnectionEnum.CONNECTED;
            case ERROR:
                return Status.ConnectionEnum.ERROR;
            case STATELESS:
                return Status.ConnectionEnum.STATELESS;
            default:
            case UNKNOWN:
                return Status.ConnectionEnum.UNKNOWN;
        }
    }

    public static @NotNull Status.RuntimeEnum convertRuntimeStatus(final @NotNull ProtocolAdapterState.RuntimeStatus runtimeStatus) {
        Preconditions.checkNotNull(runtimeStatus);
        switch (runtimeStatus) {
            case STARTED:
                return Status.RuntimeEnum.STARTED;
            default:
            case STOPPED:
                return Status.RuntimeEnum.STOPPED;
        }
    }
}
