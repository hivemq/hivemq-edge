package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.HiveMQConfigEntity;

public interface Syncable<T> extends Configurator<T> {

    /**
     * Sync the provided entity with the current state in the configurator.
     *
     * @param entity the entity to witch the internal state of the configurator is applied to
     */
    void sync(HiveMQConfigEntity entity);
}
