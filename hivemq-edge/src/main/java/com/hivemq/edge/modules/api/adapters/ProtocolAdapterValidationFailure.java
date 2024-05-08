package com.hivemq.edge.modules.api.adapters;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public interface ProtocolAdapterValidationFailure {
    @NotNull String getMessage();

    @Nullable Class getOrigin();

    @Nullable Throwable getCause();

    @Nullable String getFieldName();
}
