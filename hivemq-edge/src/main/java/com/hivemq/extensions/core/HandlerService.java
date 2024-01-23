package com.hivemq.extensions.core;

import com.hivemq.bootstrap.factories.AdapterHandlingFactory;
import com.hivemq.bootstrap.factories.ClientSessionLocalPersistenceFactory;
import com.hivemq.bootstrap.factories.HandlerFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public class HandlerService {

    private @Nullable HandlerFactory handlerFactory;
    private @Nullable AdapterHandlingFactory adapterHandlingFactory;


    public void supplyHandlerFactory(final @NotNull HandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    public void supplyAdapterHandlingFactory(final @NotNull AdapterHandlingFactory adapterHandlingFactory) {
        this.adapterHandlingFactory = adapterHandlingFactory;
    }


    public @Nullable HandlerFactory getHandlerFactory() {
        return handlerFactory;
    }

    public @Nullable AdapterHandlingFactory getAdapterHandlerFactory() {
        return adapterHandlingFactory;
    }
}
