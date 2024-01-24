package com.hivemq.extensions.core;

import com.hivemq.api.resources.GenericAPIHolder;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class RestComponentsServiceImpl implements RestComponentsService{

    private final @NotNull GenericAPIHolder genericAPIHolder;

    public RestComponentsServiceImpl(final @NotNull GenericAPIHolder genericAPIHolder) {
        this.genericAPIHolder = genericAPIHolder;
    }

    public void add(final @NotNull Object component) {
        genericAPIHolder.addComponent(component);
    }

}
