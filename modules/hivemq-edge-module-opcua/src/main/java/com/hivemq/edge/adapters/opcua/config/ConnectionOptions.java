package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;

import static java.util.Objects.requireNonNullElse;

public record ConnectionOptions(
        @JsonProperty("sessionTimeout")
        @ModuleConfigField(title = "Session Timeout (seconds)",
                           description = "OPC UA session timeout in seconds. Session will be renewed at this interval.",
                           numberMin = 10,
                           numberMax = 3600,
                           defaultValue = ""+DEFAULT_SESSION_TIMEOUT)
        Long sessionTimeout,

        @JsonProperty("requestTimeout")
        @ModuleConfigField(title = "Request Timeout (seconds)",
                   description = "Timeout for OPC UA requests in seconds",
                   numberMin = 5,
                   numberMax = 300,
                   defaultValue = ""+DEFAULT_REQUEST_TIMEOUT)
        Long requestTimeout,

        @JsonProperty("keepAliveInterval")
        @ModuleConfigField(title = "Keep-Alive Interval (seconds)",
                   description = "Interval between OPC UA keep-alive pings in seconds",
                   numberMin = 1,
                   numberMax = 60,
                   defaultValue = ""+DEFAULT_KEEP_ALIVE_INTERVAL)
        Long keepAliveInterval,

        @JsonProperty("keepAliveFailuresAllowed")
        @ModuleConfigField(title = "Keep-Alive Failures Allowed",
                   description = "Number of consecutive keep-alive failures before connection is considered dead",
                   numberMin = 1,
                   numberMax = 10,
                   defaultValue = ""+DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED)
        Integer keepAliveFailuresAllowed,

        @JsonProperty("connectionTimeout")
        @ModuleConfigField(title = "Connection Timeout (seconds)",
                   description = "Timeout for establishing connection to OPC UA server in seconds",
                   numberMin = 2,
                   numberMax = 300,
                   defaultValue = ""+DEFAULT_CONNECTION_TIMEOUT)
        Long connectionTimeout,

        @JsonProperty("healthCheckInterval")
        @ModuleConfigField(title = "Health Check Interval (seconds)",
                   description = "Interval between connection health checks in seconds",
                   numberMin = 10,
                   numberMax = 300,
                   defaultValue = ""+DEFAULT_HEALTHCHECK_INTERVAL)
        Long healthCheckInterval,

        @JsonProperty("retryInterval")
        @ModuleConfigField(title = "Retry Interval (seconds)",
                   description = "Interval between connection retry attempts in seconds",
                   numberMin = 5,
                   numberMax = 300,
                   defaultValue = ""+DEFAULT_RETRY_INTERVAL)
        Long retryInterval,

        @JsonProperty("autoReconnect")
        @ModuleConfigField(title = "Automatic Reconnection",
                   description = "Enable automatic reconnection when health check detects connection issues",
                   defaultValue = ""+DEFAULT_AUTO_RECONNECT)
        Boolean autoReconnect) {

    public static final long DEFAULT_SESSION_TIMEOUT = 120L;
    public static final long DEFAULT_KEEP_ALIVE_INTERVAL = 10L;
    public static final int DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED = 3;
    public static final boolean DEFAULT_AUTO_RECONNECT = true;
    public static final long DEFAULT_REQUEST_TIMEOUT = 30;
    public static final long DEFAULT_CONNECTION_TIMEOUT = 30;
    public static final long DEFAULT_HEALTHCHECK_INTERVAL = 30;
    public static final long DEFAULT_RETRY_INTERVAL = 30;

    public ConnectionOptions {
        // Timeout configurations with sensible defaults
        sessionTimeout = requireNonNullElse(sessionTimeout, DEFAULT_SESSION_TIMEOUT);
        requestTimeout = requireNonNullElse(requestTimeout, DEFAULT_REQUEST_TIMEOUT);
        keepAliveInterval = requireNonNullElse(keepAliveInterval, DEFAULT_KEEP_ALIVE_INTERVAL);
        keepAliveFailuresAllowed = requireNonNullElse(keepAliveFailuresAllowed, DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED);
        connectionTimeout = requireNonNullElse(connectionTimeout, DEFAULT_CONNECTION_TIMEOUT);
        healthCheckInterval = requireNonNullElse(healthCheckInterval, DEFAULT_HEALTHCHECK_INTERVAL);
        retryInterval = requireNonNullElse(retryInterval, DEFAULT_RETRY_INTERVAL);
        autoReconnect = requireNonNullElse(autoReconnect, DEFAULT_AUTO_RECONNECT);
    }

    public static ConnectionOptions defaultConnectionOptions() {
        return new ConnectionOptions(DEFAULT_SESSION_TIMEOUT, DEFAULT_REQUEST_TIMEOUT, DEFAULT_KEEP_ALIVE_INTERVAL,
                DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_HEALTHCHECK_INTERVAL, DEFAULT_RETRY_INTERVAL, DEFAULT_AUTO_RECONNECT);
    }
}
