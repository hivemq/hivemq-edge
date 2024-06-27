package com.hivemq.edge.adapters.file.convertion;

import org.jetbrains.annotations.NotNull;

public class MappingException extends RuntimeException {

    public MappingException(@NotNull final String message) {
        super(message);
    }


    @Override
    public synchronized @NotNull Throwable fillInStackTrace() {
        return this;
    }
}
