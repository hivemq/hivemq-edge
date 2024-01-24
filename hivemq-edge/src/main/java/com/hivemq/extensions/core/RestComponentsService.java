package com.hivemq.extensions.core;

import com.hivemq.extension.sdk.api.annotations.NotNull;

public interface RestComponentsService {

    /**
     * Must be called before the REST Api gets bootstrapped
     * @param component the component that will be registered when the REST Api gets bootstrapped.
     */
    void add(final @NotNull Object component);
}
