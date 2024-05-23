package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class PollingInputImpl implements PollingInput {

    private final @NotNull PollingContext pollingContext;

    public PollingInputImpl(final @NotNull PollingContext pollingContext) {
        this.pollingContext = pollingContext;
    }

    @Override
    public @NotNull PollingContext getPollingContext() {
        return pollingContext;
    }
}
