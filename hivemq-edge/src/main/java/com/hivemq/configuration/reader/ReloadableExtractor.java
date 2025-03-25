package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import org.jetbrains.annotations.NotNull;

public interface ReloadableExtractor<T,V> extends Extractor<T,V> {
    /**
     * This method will be incoked whenever a configuration change was discovered in storage.
     * @param config
     * @return
     */
    Configurator.ConfigResult updateConfig(HiveMQConfigEntity config);

    /**
     * When a sync to disk was triggered this method will be invoked with the object being synced to disk.
     * The object must be filled with changed objects from this extractor.
     * @param entity the config entity to be used
     */
    void sync(final @NotNull HiveMQConfigEntity entity);
}
