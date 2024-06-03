package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class PollingInputImpl<T extends  PollingContext> implements PollingInput<T> {

    private final @NotNull T pollingContext;

    public PollingInputImpl(final @NotNull T pollingContext) {
        this.pollingContext = pollingContext;
    }

    @Override
    public @NotNull T getPollingContext() {
        return pollingContext;
    }
}
