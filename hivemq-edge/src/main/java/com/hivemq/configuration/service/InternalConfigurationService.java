package com.hivemq.configuration.service;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Map;

public interface InternalConfigurationService {

    @Nullable String get(@NotNull String key);

    void set(@NotNull String key, @NotNull String value);

    boolean getBoolean(@NotNull String key);

    double getDouble(@NotNull String key);

    int getInteger(@NotNull String key);

    long getLong(@NotNull String key);

    boolean isConfigSetByUser(@NotNull String key);

    Map<String,String> getConfigsSetByUser();

}
