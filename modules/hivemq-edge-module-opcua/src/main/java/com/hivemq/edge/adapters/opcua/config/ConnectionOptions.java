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
        @ModuleConfigField(title = "Retry Interval (milliseconds)",
                   description = "Interval between connection retry attempts in milliseconds",
                   numberMin = 5 * 1000,
                   numberMax = 300 * 1000,
                   defaultValue = ""+DEFAULT_RETRY_INTERVAL)
        Long retryIntervalMs,

        @JsonProperty("autoReconnect")
        @ModuleConfigField(title = "Automatic Reconnection",
                   description = "Enable automatic reconnection when health check detects connection issues",
                   defaultValue = ""+DEFAULT_AUTO_RECONNECT)
        Boolean autoReconnect) {

    public static final long DEFAULT_SESSION_TIMEOUT = 120L * 1000;
    public static final long DEFAULT_KEEP_ALIVE_INTERVAL = 10L * 1000;
    public static final int DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED = 3;
    public static final boolean DEFAULT_AUTO_RECONNECT = true;
    public static final long DEFAULT_REQUEST_TIMEOUT = 30 * 1000;
    public static final long DEFAULT_CONNECTION_TIMEOUT = 30 * 1000;
    public static final long DEFAULT_HEALTHCHECK_INTERVAL = 30 * 1000;
    public static final long DEFAULT_RETRY_INTERVAL = 30 * 1000;

    public ConnectionOptions {
        // Timeout configurations with sensible defaults
        sessionTimeoutMs = requireNonNullElse(sessionTimeoutMs, DEFAULT_SESSION_TIMEOUT);
        requestTimeoutMs = requireNonNullElse(requestTimeoutMs, DEFAULT_REQUEST_TIMEOUT);
        keepAliveIntervalMs = requireNonNullElse(keepAliveIntervalMs, DEFAULT_KEEP_ALIVE_INTERVAL);
        keepAliveFailuresAllowed = requireNonNullElse(keepAliveFailuresAllowed, DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED);
        connectionTimeoutMs = requireNonNullElse(connectionTimeoutMs, DEFAULT_CONNECTION_TIMEOUT);
        healthCheckIntervalMs = requireNonNullElse(healthCheckIntervalMs, DEFAULT_HEALTHCHECK_INTERVAL);
        retryIntervalMs = requireNonNullElse(retryIntervalMs, DEFAULT_RETRY_INTERVAL);
        autoReconnect = requireNonNullElse(autoReconnect, DEFAULT_AUTO_RECONNECT);
    }

    public static ConnectionOptions defaultConnectionOptions() {
        return new ConnectionOptions(DEFAULT_SESSION_TIMEOUT, DEFAULT_REQUEST_TIMEOUT, DEFAULT_KEEP_ALIVE_INTERVAL,
                DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_HEALTHCHECK_INTERVAL, DEFAULT_RETRY_INTERVAL, DEFAULT_AUTO_RECONNECT);
    }
}
