/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;

import static java.util.Objects.requireNonNullElse;

public record ConnectionOptions(
        @JsonProperty("sessionTimeoutMs")
        @ModuleConfigField(title = "Session Timeout (milliseconds)",
                           description = "OPC UA session timeout in milliseconds. Session will be renewed at this interval.",
                           numberMin = 10 * 1000,
                           numberMax = 3600 * 1000,
                           defaultValue = ""+DEFAULT_SESSION_TIMEOUT)
        Long sessionTimeoutMs,

        @JsonProperty("requestTimeoutMs")
        @ModuleConfigField(title = "Request Timeout (milliseconds)",
                   description = "Timeout for OPC UA requests in milliseconds",
                   numberMin = 5 * 1000,
                   numberMax = 300 * 1000,
                   defaultValue = ""+DEFAULT_REQUEST_TIMEOUT)
        Long requestTimeoutMs,

        @JsonProperty("keepAliveIntervalMs")
        @ModuleConfigField(title = "Keep-Alive Interval (milliseconds)",
                   description = "Interval between OPC UA keep-alive pings in milliseconds",
                   numberMin = 1000,
                   numberMax = 60 * 1000,
                   defaultValue = ""+DEFAULT_KEEP_ALIVE_INTERVAL)
        Long keepAliveIntervalMs,

        @JsonProperty("keepAliveFailuresAllowed")
        @ModuleConfigField(title = "Keep-Alive Failures Allowed",
                   description = "Number of consecutive keep-alive failures before connection is considered dead",
                   numberMin = 1,
                   numberMax = 10,
                   defaultValue = ""+DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED)
        Integer keepAliveFailuresAllowed,

        @JsonProperty("connectionTimeoutMs")
        @ModuleConfigField(title = "Connection Timeout (milliseconds)",
                   description = "Timeout for establishing connection to OPC UA server in milliseconds",
                   numberMin = 2 * 1000,
                   numberMax = 300 * 1000,
                   defaultValue = ""+DEFAULT_CONNECTION_TIMEOUT)
        Long connectionTimeoutMs,

        @JsonProperty("healthCheckIntervalMs")
        @ModuleConfigField(title = "Health Check Interval (milliseconds)",
                   description = "Interval between connection health checks in milliseconds",
                   numberMin = 10 * 1000,
                   numberMax = 300 * 1000,
                   defaultValue = ""+DEFAULT_HEALTHCHECK_INTERVAL)
        Long healthCheckIntervalMs,

        @JsonProperty("retryIntervalMs")
        @ModuleConfigField(title = "Connection Retry Intervals (milliseconds)",
                   description = "Comma-separated list of backoff delays in milliseconds for connection retry attempts. The adapter will use these delays sequentially for each retry attempt, repeating the last value if attempts exceed the list length.",
                   defaultValue = DEFAULT_RETRY_INTERVALS)
        String retryIntervalMs,

        @JsonProperty("autoReconnect")
        @ModuleConfigField(title = "Automatic Reconnection",
                   description = "Enable automatic reconnection when health check detects connection issues",
                   defaultValue = ""+DEFAULT_AUTO_RECONNECT)
        Boolean autoReconnect,

        @JsonProperty("reconnectOnServiceFault")
        @ModuleConfigField(title = "Reconnect on Service Fault",
                   description = "Enable automatic reconnection when critical OPC UA service faults occur (e.g., session invalid, subscription lost). Recommended to keep enabled.",
                   defaultValue = ""+DEFAULT_RECONNECT_ON_SERVICE_FAULT)
        Boolean reconnectOnServiceFault) {

    public static final long DEFAULT_SESSION_TIMEOUT = 120L * 1000;
    public static final long DEFAULT_KEEP_ALIVE_INTERVAL = 10L * 1000;
    public static final int DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED = 3;
    public static final boolean DEFAULT_AUTO_RECONNECT = true;
    public static final boolean DEFAULT_RECONNECT_ON_SERVICE_FAULT = true;
    public static final long DEFAULT_REQUEST_TIMEOUT = 30 * 1000;
    public static final long DEFAULT_CONNECTION_TIMEOUT = 30 * 1000;
    public static final long DEFAULT_HEALTHCHECK_INTERVAL = 30 * 1000;
    // Exponential backoff delays: 1s, 2s, 4s, 8s, 16s, 32s, 64s, 128s, 256s, 300s (capped at 5 minutes)
    public static final String DEFAULT_RETRY_INTERVALS = "1000,2000,4000,8000,16000,32000,64000,128000,256000,300000";

    public ConnectionOptions {
        // Timeout configurations with sensible defaults
        sessionTimeoutMs = requireNonNullElse(sessionTimeoutMs, DEFAULT_SESSION_TIMEOUT);
        requestTimeoutMs = requireNonNullElse(requestTimeoutMs, DEFAULT_REQUEST_TIMEOUT);
        keepAliveIntervalMs = requireNonNullElse(keepAliveIntervalMs, DEFAULT_KEEP_ALIVE_INTERVAL);
        keepAliveFailuresAllowed = requireNonNullElse(keepAliveFailuresAllowed, DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED);
        connectionTimeoutMs = requireNonNullElse(connectionTimeoutMs, DEFAULT_CONNECTION_TIMEOUT);
        healthCheckIntervalMs = requireNonNullElse(healthCheckIntervalMs, DEFAULT_HEALTHCHECK_INTERVAL);
        retryIntervalMs = requireNonNullElse(retryIntervalMs, DEFAULT_RETRY_INTERVALS);
        autoReconnect = requireNonNullElse(autoReconnect, DEFAULT_AUTO_RECONNECT);
        reconnectOnServiceFault = requireNonNullElse(reconnectOnServiceFault, DEFAULT_RECONNECT_ON_SERVICE_FAULT);
    }

    public static ConnectionOptions defaultConnectionOptions() {
        return new ConnectionOptions(DEFAULT_SESSION_TIMEOUT, DEFAULT_REQUEST_TIMEOUT, DEFAULT_KEEP_ALIVE_INTERVAL,
                DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_HEALTHCHECK_INTERVAL,
                DEFAULT_RETRY_INTERVALS, DEFAULT_AUTO_RECONNECT, DEFAULT_RECONNECT_ON_SERVICE_FAULT);
    }
}
