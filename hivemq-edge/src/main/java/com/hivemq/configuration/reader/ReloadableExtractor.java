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
