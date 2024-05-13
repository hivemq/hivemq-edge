package com.hivemq.edge.modules.config.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.adapters.config.UserProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public class UserPropertyImpl implements UserProperty {
    @JsonProperty("propertyName")
    @ModuleConfigField(title = "Property Name", description = "Name of the associated property")
    private @Nullable String propertyName = null;

    @JsonProperty("propertyValue")
    @ModuleConfigField(title = "Property Value", description = "Value of the associated property")
    private @Nullable String propertyValue = null;

    public UserPropertyImpl() {
    }

    public UserPropertyImpl(@Nullable final String propertyName, @Nullable final String propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    @Override
    public @NotNull String getName() {
        return propertyName;
    }

    @Override
    public void setName(final @NotNull String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public @NotNull String getValue() {
        return propertyValue;
    }

    @Override
    public void setValue(final @NotNull String propertyValue) {
        this.propertyValue = propertyValue;
    }
}
