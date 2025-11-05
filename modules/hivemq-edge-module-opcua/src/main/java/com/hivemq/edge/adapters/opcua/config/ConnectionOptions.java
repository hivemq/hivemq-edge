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
                           defaultValue = "120")
        Integer sessionTimeout,

        @JsonProperty("requestTimeout")
        @ModuleConfigField(title = "Request Timeout (seconds)",
                   description = "Timeout for OPC UA requests in seconds",
                   numberMin = 5,
                   numberMax = 300,
                   defaultValue = "30")
        Integer requestTimeout,

        @JsonProperty("keepAliveInterval")
        @ModuleConfigField(title = "Keep-Alive Interval (seconds)",
                   description = "Interval between OPC UA keep-alive pings in seconds",
                   numberMin = 1,
                   numberMax = 60,
                   defaultValue = "10")
        Integer keepAliveInterval,

        @JsonProperty("keepAliveFailuresAllowed")
        @ModuleConfigField(title = "Keep-Alive Failures Allowed",
                   description = "Number of consecutive keep-alive failures before connection is considered dead",
                   numberMin = 1,
                   numberMax = 10,
                   defaultValue = "3")
        Integer keepAliveFailuresAllowed,

        @JsonProperty("connectionTimeout")
        @ModuleConfigField(title = "Connection Timeout (seconds)",
                   description = "Timeout for establishing connection to OPC UA server in seconds",
                   numberMin = 2,
                   numberMax = 300,
                   defaultValue = "30")
        Integer connectionTimeout,

        @JsonProperty("healthCheckInterval")
        @ModuleConfigField(title = "Health Check Interval (seconds)",
                   description = "Interval between connection health checks in seconds",
                   numberMin = 10,
                   numberMax = 300,
                   defaultValue = "30")
        Integer healthCheckInterval,

        @JsonProperty("retryInterval")
        @ModuleConfigField(title = "Retry Interval (seconds)",
                   description = "Interval between connection retry attempts in seconds",
                   numberMin = 5,
                   numberMax = 300,
                   defaultValue = "30")
        Integer retryInterval,

        @JsonProperty("autoReconnect")
        @ModuleConfigField(title = "Automatic Reconnection",
                   description = "Enable automatic reconnection when health check detects connection issues",
                   defaultValue = "true")
        Boolean autoReconnect) {

    public ConnectionOptions {
        // Timeout configurations with sensible defaults
        sessionTimeout = requireNonNullElse(sessionTimeout, 120);
        requestTimeout = requireNonNullElse(requestTimeout, 30);
        keepAliveInterval = requireNonNullElse(keepAliveInterval, 10);
        keepAliveFailuresAllowed = requireNonNullElse(keepAliveFailuresAllowed, 3);
        connectionTimeout = requireNonNullElse(connectionTimeout, 30);
        healthCheckInterval = requireNonNullElse(healthCheckInterval, 30);
        retryInterval = requireNonNullElse(retryInterval, 30);
        autoReconnect = requireNonNullElse(autoReconnect, true);
    }

    public static ConnectionOptions defaultConnectionOptions() {
        return new ConnectionOptions(120, 30, 10, 3, 30, 30, 30, true);
    }
}
