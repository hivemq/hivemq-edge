package com.hivemq.protocols.writing;

import com.hivemq.adapter.sdk.api.writing.WriteInput;
import com.hivemq.adapter.sdk.api.writing.WritePayload;
import org.jetbrains.annotations.NotNull;

public class WriteInputImpl<T extends WritePayload> implements WriteInput<T> {

    private final @NotNull T payload;

    WriteInputImpl(final @NotNull WritePayload payload) {
        // we can not cast it in the calling method as it does not know T
        //noinspection unchecked
        this.payload = (T) payload;
    }

    @Override
    public @NotNull T getWritePayload() {
        return payload;
    }
}
