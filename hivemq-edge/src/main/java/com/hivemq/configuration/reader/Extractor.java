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
import java.util.function.Consumer;

/**
 * Configurators are initialized before the actual dagger bootstrap.
 *
 * @param <T>
 */
public interface Extractor<T,V> {
    Logger log = LoggerFactory.getLogger(Extractor.class);

    /**
     * Indicate if the given config will require an edge restart to be applied
     * @param config
     * @return
     */
    boolean needsRestartWithConfig(HiveMQConfigEntity config);

    void registerConsumer(Consumer<V> consumer);

}
