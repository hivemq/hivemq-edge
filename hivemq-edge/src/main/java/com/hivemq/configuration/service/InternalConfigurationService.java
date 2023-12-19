package com.hivemq.configuration.service;

import com.hivemq.extension.sdk.api.annotations.NotNull;

public interface InternalConfigurationService {

    @NotNull String get(@NotNull String key);

    void set(@NotNull String key, @NotNull String value);

    boolean getBoolean(@NotNull String key);

    double getDouble(@NotNull String key);

    int getInteger(@NotNull String key);

    long getLong(@NotNull String key);

    boolean isConfigSetByUser(@NotNull String key);

}
