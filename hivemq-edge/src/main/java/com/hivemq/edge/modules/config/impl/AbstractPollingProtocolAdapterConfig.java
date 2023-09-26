package com.hivemq.edge.modules.config.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;

/**
 * @author Simon L Johnson
 */
public class AbstractPollingProtocolAdapterConfig extends AbstractProtocolAdapterConfig {

    @JsonProperty("publishingInterval")
    @ModuleConfigField(title = "Polling interval [ms]",
                       description = "Time in millisecond that this URL will be called",
                       numberMin = 1,
                       required = true,
                       defaultValue = "1000")
    private int publishingInterval = DEFAULT_PUBLISHING_INTERVAL; //1 second

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped",
                       numberMin = 3,
                       defaultValue = "10")
    private int maxPollingErrorsBeforeRemoval = DEFAULT_MAX_POLLING_ERROR_BEFORE_REMOVAL;

    public void setPublishingInterval(final int publishingInterval) {
        this.publishingInterval = publishingInterval;
    }

    public void setMaxPollingErrorsBeforeRemoval(final int maxPollingErrorsBeforeRemoval) {
        this.maxPollingErrorsBeforeRemoval = maxPollingErrorsBeforeRemoval;
    }

    public int getPublishingInterval() {
        return publishingInterval;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }
}
