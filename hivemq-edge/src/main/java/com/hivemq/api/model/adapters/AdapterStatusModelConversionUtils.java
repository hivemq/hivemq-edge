package com.hivemq.api.model.adapters;

import com.google.common.base.Preconditions;
import com.hivemq.api.model.ApiConstants;
import com.hivemq.api.model.status.Status;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
public class AdapterStatusModelConversionUtils {


    public static @NotNull Status getAdapterStatus(@NotNull final ProtocolAdapter protocolAdapter){
        Preconditions.checkNotNull(protocolAdapter);
        Status status = new Status(
                convertRuntimeStatus(protocolAdapter.getRuntimeStatus()),
                convertConnectionStatus(protocolAdapter.getConnectionStatus()),
                protocolAdapter.getId(), ApiConstants.ADAPTER_TYPE,
                protocolAdapter.getTimeOfLastStartAttempt(), null,
                protocolAdapter.getErrorMessage());
        return status;
    }

    public static @NotNull Status.CONNECTION_STATUS convertConnectionStatus(@NotNull final ProtocolAdapter.ConnectionStatus connectionStatus){
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

    public static @NotNull Status.RUNTIME_STATUS convertRuntimeStatus(@NotNull final ProtocolAdapter.RuntimeStatus runtimeStatus){
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
