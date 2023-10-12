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
package com.hivemq.edge.modules.api.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterInput;
import com.hivemq.edge.modules.api.adapters.model.ProtocolAdapterValidationFailure;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ProtocolAdapterSchemaManager;

import java.util.List;
import java.util.Map;

/**
 * The factory is responsible for constructing and managing the lifecycle of the various aspects of the
 * adapter sub-systems. We bind this to the configuration types to we can provide tightly coupled
 * implementation instances responsible for adapter management.
 */
public interface ProtocolAdapterFactory<E extends CustomConfig> {

    /**
     * Returns Metadata related to the protocol adapter instance, including descriptions, iconography,
     * categorisation et al
     * @return the instance that provides the adapter information
     */
    @NotNull ProtocolAdapterInformation getInformation();

    @NotNull ProtocolAdapter createAdapter(@NotNull ProtocolAdapterInformation adapterInformation, @NotNull ProtocolAdapterInput<E> input);

    @NotNull E convertConfigObject(final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config);

    @NotNull Map<String, Object> unconvertConfigObject(final @NotNull ObjectMapper objectMapper, final CustomConfig config);

    /**
     * A bean class that will be reflected upon by the framework to determine the structural requirements of the configuration associated with
     * an adapter instance. It is expected that the bean class supplied, be marked up with @ModuleConfigField annotations.
     * @return The class that represents (and will encapsulate) the configuration requirements of the adapter
     */
    @NotNull Class<E> getConfigClass();

    default @NotNull ProtocolAdapterSchemaManager getConfigSchemaManager(final @NotNull ObjectMapper objectMapper){
        return new ProtocolAdapterSchemaManager(objectMapper, getConfigClass());
    }

    default @NotNull ProtocolAdapterValidator getValidator() {
        return (objectMapper, config) -> getConfigSchemaManager(objectMapper).validateObject(config);
    }
}
