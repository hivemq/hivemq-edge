package com.hivemq.edge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Defines endpoints to access for various remoteable interactions
 * @author Simon L Johnson
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HiveMQEdgeRemoteServices {

    private String usageEndpoint;
    private String configEndpoint;

    public HiveMQEdgeRemoteServices() {
    }

    public void setUsageEndpoint(final String usageEndpoint) {
        this.usageEndpoint = usageEndpoint;
    }

    public void setConfigEndpoint(final String configEndpoint) {
        this.configEndpoint = configEndpoint;
    }

    public String getUsageEndpoint() {
        return usageEndpoint;
    }

    public String getConfigEndpoint() {
        return configEndpoint;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HiveMQEdgeRemoteServices{");
        sb.append("usageEndpoint='").append(usageEndpoint).append('\'');
        sb.append(", configEndpoint='").append(configEndpoint).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
