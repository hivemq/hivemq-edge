/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.plc4x.impl;

import com.hivemq.edge.adapters.plc4x.config.Plc4XSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
public interface Plc4xConnectionQueryStringProvider<T extends Plc4XSpecificAdapterConfig> {

    String getConnectionQueryString(@NotNull final T plc4xAdapterConfig);

}
