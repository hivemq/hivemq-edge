package com.hivemq.extensions.core;

import com.hivemq.bootstrap.factories.ClientSessionLocalPersistenceFactory;
import com.hivemq.bootstrap.factories.HandlerFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public class HandlerService {

    private @Nullable HandlerFactory handlerFactory;

    public void supplyHandlerFactory(final @NotNull HandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }


    public @Nullable HandlerFactory getHandlerFactory() {
        return handlerFactory;
    }
}
