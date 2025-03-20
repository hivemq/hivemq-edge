/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Configurators are initialized before the actual dagger bootstrap.
 *
 * @param <T>
 */
public interface Configurator <T> {
    Logger log = LoggerFactory.getLogger(Configurator.class);

    enum ConfigResult {SUCCESS, NO_OP, NEEDS_RESTART}

    /**
     * Called for initial setup.
     * Not to be invoked more than once.
     *
     * @param config
     * @return indicator on whether the config was correctly applied
     */
    ConfigResult applyConfig(HiveMQConfigEntity config);


    /**
     * Indicate if the given config will require an edge restart to be applied
     * @param config
     * @return
     */
    boolean needsRestartWithConfig(HiveMQConfigEntity config);

    /**
     * Check whether the config has changed.
     *
     * @param originalConfig
     * @param newConfig
     * @return
     */
    default boolean hasChanged(final T originalConfig, final T newConfig) {
        return !Objects.equals(originalConfig, newConfig);
    }
}
