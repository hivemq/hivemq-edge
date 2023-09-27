package com.hivemq.edge.modules.config.impl;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;

/**
 * @author Simon L Johnson
 */
public class AbstractPollingProtocolAdapterConfig extends AbstractProtocolAdapterConfig {

    @JsonProperty("pollingIntervalMillis")
    @JsonAlias(value = "publishingInterval") //-- Ensure we cater for properties created with legacy configuration
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       required = true,
                       defaultValue = "1000")
    private int pollingIntervalMillis = DEFAULT_POLLING_INTERVAL; //1 second

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped",
                       numberMin = 3,
                       defaultValue = "10")
    private int maxPollingErrorsBeforeRemoval = DEFAULT_MAX_POLLING_ERROR_BEFORE_REMOVAL;

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }
}
