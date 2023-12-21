package com.hivemq.configuration.service.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class InternalConfigurationServiceImpl implements InternalConfigurationService {

    private static final @NotNull Logger log = LoggerFactory.getLogger(InternalConfigurationServiceImpl.class);

    private final @NotNull Map<String, String> values = Maps.newConcurrentMap();



    @Override
    public @NotNull String get(final @NotNull String key) {
        final String value = values.get(key);
        if (value != null) {
            return value;
        }
        return InternalConfigurations.DEFAULT_VALUES.get(key);
    }

    @Override
    public void set(final @NotNull String key, final @NotNull String value) {
        log.debug("Setting internal configuration '{}' to '{}'", key, value);
        values.put(key, value);
    }

    @Override
    public boolean getBoolean(final @NotNull String key) {
        final String value = get(key);
        return Boolean.parseBoolean(value);
    }

    @Override
    public double getDouble(final @NotNull String key) {
        try {
            final String value = get(key);
            return Double.parseDouble(value);
        } catch (final NumberFormatException ex) {
            final String defaultValue = InternalConfigurations.DEFAULT_VALUES.get(key);
            Preconditions.checkState(defaultValue != null,
                    "Illegal format for internal configuration " + key + " and no default value available");
            log.debug("Illegal format for internal configuration {} using default value {}", key, defaultValue);
            set(key, defaultValue);
            return Double.parseDouble(defaultValue);
        }
    }

    @Override
    public int getInteger(final @NotNull String key) {
        try {
            final String value = get(key);
            return Integer.parseInt(value);
        } catch (final NumberFormatException ex) {
            final String defaultValue = InternalConfigurations.DEFAULT_VALUES.get(key);
            Preconditions.checkState(defaultValue != null,
                    "Illegal format for internal configuration '" +
                            key +
                            "' with set value '" +
                            get(key) +
                            "' and no default value available");
            log.debug("Illegal format for internal configuration {} using default value {}", key, defaultValue);
            set(key, defaultValue);
            return Integer.parseInt(defaultValue);
        }
    }

    @Override
    public long getLong(final @NotNull String key) {
        try {
            final String value = get(key);
            return Long.parseLong(value);
        } catch (final NumberFormatException ex) {
            final String defaultValue = InternalConfigurations.DEFAULT_VALUES.get(key);
            Preconditions.checkState(defaultValue != null,
                    "Illegal format for internal configuration " + key + " and no default value available");
            log.debug("Illegal format for internal configuration {} using default value {}", key, defaultValue);
            set(key, defaultValue);
            return Long.parseLong(defaultValue);
        }
    }

    @Override
    public boolean isConfigSetByUser(final @NotNull String key) {
        return values.containsKey(key);
    }

    @Override
    public @NotNull Map<String, String> getConfigsSetByUser() {
        return values;
    }

    /**
     * Resets the Internal Configuration to its default values,
     * this is needed for Tests and Integration Tests to prevent pollution between tests
     */
    @VisibleForTesting
    public void resetToDefaults() {
        values.clear();
    }
}
