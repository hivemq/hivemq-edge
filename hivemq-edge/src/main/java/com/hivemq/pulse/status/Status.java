package com.hivemq.pulse.status;


import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Status {

    public enum ActivationStatus {
        ACTIVATED,
        DEACTIVATED,
        ERROR
    }

    public enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED,
        ERROR
    }

    private final @NotNull ActivationStatus activationStatus;
    private final @NotNull ConnectionStatus connectionStatus;
    private final @NotNull List<String> errorMessages;

    public Status(
            final @NotNull ActivationStatus activationStatus,
            final @NotNull ConnectionStatus connectionStatus,
            final @NotNull List<String> errorMessages) {
        this.activationStatus = activationStatus;
        this.connectionStatus = connectionStatus;
        this.errorMessages = errorMessages;
    }

    public @NotNull ActivationStatus activationStatus() {
        return activationStatus;
    }

    public @NotNull ConnectionStatus connectionStatus() {
        return connectionStatus;
    }

    public @NotNull List<String> errorMessages() {
        return errorMessages;
    }
}
