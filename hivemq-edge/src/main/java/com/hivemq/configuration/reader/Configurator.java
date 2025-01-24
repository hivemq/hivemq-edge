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

public interface Configurator <T>{
    Logger log = LoggerFactory.getLogger(Configurator.class);

    enum ConfigResult {SUCCESS, NO_OP, NEEDS_RESTART}

    ConfigResult setConfig(HiveMQConfigEntity config);

    boolean needsRestartWithConfig(HiveMQConfigEntity config);

    default boolean hasChanged(final T originalConfig, final T newConfig) {
        if ((originalConfig != null && newConfig == null) || (originalConfig == null && newConfig != null)) {
            log.error( "{} has changed {} {}", originalConfig.getClass().getSimpleName(), originalConfig == null, newConfig == null);
            return true;
        }

        return !Objects.equals(originalConfig, newConfig);
    }
}
