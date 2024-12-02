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
import com.hivemq.api.model.status.Status;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ProtocolAdapterWrapper;

/**
 * @author Simon L Johnson
 */
public class AdapterStatusModelConversionUtils {


    public static @NotNull Status getAdapterStatus(final ProtocolAdapterWrapper protocolAdapterWrapper){
        Preconditions.checkNotNull(protocolAdapterWrapper);
        return new Status(
                convertRuntimeStatus(protocolAdapterWrapper.getRuntimeStatus()),
                convertConnectionStatus(protocolAdapterWrapper.getConnectionStatus()),
                protocolAdapterWrapper.getId(), ApiConstants.ADAPTER_TYPE,
                protocolAdapterWrapper.getTimeOfLastStartAttempt(), null,
                protocolAdapterWrapper.getErrorMessage());
    }

    public static @NotNull Status.CONNECTION_STATUS convertConnectionStatus(final @NotNull ProtocolAdapterState.ConnectionStatus connectionStatus){
        Preconditions.checkNotNull(connectionStatus);
        switch (connectionStatus){
            case DISCONNECTED:
                return Status.CONNECTION_STATUS.DISCONNECTED;
            case CONNECTED:
                return Status.CONNECTION_STATUS.CONNECTED;
            case ERROR:
                return Status.CONNECTION_STATUS.ERROR;
            case STATELESS:
                return Status.CONNECTION_STATUS.STATELESS;
            default:
            case UNKNOWN:
                return Status.CONNECTION_STATUS.UNKNOWN;
        }
    }

    public static @NotNull Status.RUNTIME_STATUS convertRuntimeStatus(final @NotNull ProtocolAdapterState.RuntimeStatus runtimeStatus){
        Preconditions.checkNotNull(runtimeStatus);
        switch (runtimeStatus){
            case STARTED:
                return Status.RUNTIME_STATUS.STARTED;
            default:
            case STOPPED:
                return Status.RUNTIME_STATUS.STOPPED;
        }
    }
}
