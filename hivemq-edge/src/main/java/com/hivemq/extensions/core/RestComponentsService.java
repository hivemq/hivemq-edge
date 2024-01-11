package com.hivemq.extensions.core;

import com.hivemq.api.resources.GenericAPIHolder;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class RestComponentsService {

    private final @NotNull GenericAPIHolder genericAPIHolder;

    public RestComponentsService(final @NotNull GenericAPIHolder genericAPIHolder) {
        this.genericAPIHolder = genericAPIHolder;
    }

    public void add(final @NotNull Object component) {
        genericAPIHolder.addComponent(component);
    }
}
