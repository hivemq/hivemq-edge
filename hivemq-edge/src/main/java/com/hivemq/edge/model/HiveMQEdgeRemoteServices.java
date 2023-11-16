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
