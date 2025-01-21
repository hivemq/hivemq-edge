package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import org.apache.commons.lang3.builder.ReflectionDiffBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public interface Configurator <T>{
    enum ConfigResult {SUCCESS, NO_OP, NEEDS_RESTART}

    ConfigResult setConfig(HiveMQConfigEntity config);

    boolean needsRestartWithConfig(HiveMQConfigEntity config);

    default boolean hasChanged(final T originalConfig, final T newConfig) {
        if ((originalConfig != null && newConfig == null) || (originalConfig == null && newConfig != null)) {
            return true;
        }
        return new ReflectionDiffBuilder<>(originalConfig, newConfig, ToStringStyle.SHORT_PREFIX_STYLE)
                .build().getDiffs().isEmpty();
    }
}
