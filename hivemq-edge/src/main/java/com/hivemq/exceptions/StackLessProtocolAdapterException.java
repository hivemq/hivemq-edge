package com.hivemq.exceptions;

import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class StackLessProtocolAdapterException extends ProtocolAdapterException {

    // makes this exception much cheaper, but we wont get a nice stack trace.
    @Override
    public synchronized @NotNull Throwable fillInStackTrace() {
        return this;
    }
}
