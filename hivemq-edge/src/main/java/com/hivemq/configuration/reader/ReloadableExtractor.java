package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import org.jetbrains.annotations.NotNull;

public interface ReloadableExtractor<T,V> extends Extractor<T,V> {
    /**
     * This method will be incoked whenever a configuration stange was discovered in storage.
     * @param config
     * @return
     */
    Configurator.ConfigResult updateConfig(HiveMQConfigEntity config);

    void sync(final @NotNull HiveMQConfigEntity entity);
}
