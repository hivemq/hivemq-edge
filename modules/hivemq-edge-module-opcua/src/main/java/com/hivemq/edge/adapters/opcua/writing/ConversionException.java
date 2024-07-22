package com.hivemq.edge.adapters.opcua.writing;

import org.jetbrains.annotations.NotNull;

public class ConversionException extends Exception {

    ConversionException(final @NotNull String message) {
        super(message);
    }

    @Override
    public synchronized @NotNull Throwable fillInStackTrace() {
        return this;
    }
}
