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
package com.hivemq.configuration.service;

import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.configuration.reader.UnsExtractor;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import org.jetbrains.annotations.NotNull;

import java.io.Writer;
import java.util.Optional;

/**
 * The Configuration Service interface which allows to change HiveMQ configuration programmatically.
 *
 * @author Dominik Obermaier
 * @since 3.0
 */

public interface ConfigurationService {

    /**
     * Returns the configuration service which allows to add and inspect listeners add runtime.
     *
     * @return the listener configuration service
     */
    @NotNull ListenerConfigurationService listenerConfiguration();

    /**
     * Returns the configuration service for MQTT configuration
     *
     * @return the mqtt configuration service
     */
    @NotNull MqttConfigurationService mqttConfiguration();

    /**
     * Returns the throttling configuration service for global throttling
     *
     * @return the global throttling service
     */
    @NotNull RestrictionsConfigurationService restrictionsConfiguration();

    /**
     * Returns the configuration service for MQTTSN configuration
     *
     * @return the mqttsn configuration service
     */
    @NotNull MqttsnConfigurationService mqttsnConfiguration();

    /**
     * Returns the configuration service for Api configuration
     *
     * @return the api configuration service
     */
    @NotNull ApiConfigurationService apiConfiguration();

    @NotNull SecurityConfigurationService securityConfiguration();

    @NotNull PersistenceConfigurationService persistenceConfigurationService();

    @NotNull DynamicConfigurationService gatewayConfiguration();

    @NotNull UsageTrackingConfigurationService usageTrackingConfiguration();

    @NotNull ModuleConfigurationService commercialModuleConfigurationService();

    @NotNull InternalConfigurationService internalConfigurationService();

    @NotNull BridgeExtractor bridgeExtractor();

    @NotNull ProtocolAdapterExtractor protocolAdapterExtractor();

    @NotNull DataCombiningExtractor dataCombiningExtractor();

    @NotNull UnsExtractor unsExtractor();

    void setConfigFileReaderWriter(@NotNull ConfigFileReaderWriter configFileReaderWriter);

    void writeConfiguration(final @NotNull Writer writer);

    @NotNull Optional<Long> getLastUpdateTime();

}


