package com.hivemq.protocols.writing;

import com.hivemq.adapter.sdk.api.config.WriteContext;
import com.hivemq.adapter.sdk.api.writing.WriteInput;
import com.hivemq.adapter.sdk.api.writing.WritePayload;
import org.jetbrains.annotations.NotNull;

public class WriteInputImpl<P extends WritePayload, C extends WriteContext> implements WriteInput<P, C> {

    private final @NotNull P payload;
    private final @NotNull C writeContext;

    WriteInputImpl(final @NotNull WritePayload payload, final @NotNull WriteContext writeContext) {
        // we can not cast it in the calling method as it does not know T
        //noinspection unchecked
        this.payload = (P) payload;
        //noinspection unchecked
        this.writeContext = (C) writeContext;
    }

    @Override
    public @NotNull P getWritePayload() {
        return payload;
    }

    @Override
    public @NotNull C getWriteContext() {
        return writeContext;
    }
}
