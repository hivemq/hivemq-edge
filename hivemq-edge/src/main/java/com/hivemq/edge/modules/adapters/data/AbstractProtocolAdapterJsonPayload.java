package com.hivemq.edge.modules.adapters.data;

import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * @author Simon L Johnson
 */
public abstract class AbstractProtocolAdapterJsonPayload {

    private final Long timestamp;

    public AbstractProtocolAdapterJsonPayload(final @Nullable Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

}
