package com.hivemq.api.resources;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GenericAPIHolder {

    private final @NotNull List<Object> components = new ArrayList<>();


    public void addComponent(final @NotNull Object component) {
        components.add(component);
    }

    public @NotNull List<Object> getComponents() {
        return components;
    }
}
