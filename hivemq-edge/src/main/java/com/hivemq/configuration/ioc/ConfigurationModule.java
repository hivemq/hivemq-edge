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
package com.hivemq.configuration.ioc;

import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.configuration.reader.UnsExtractor;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.DynamicConfigurationService;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.MqttsnConfigurationService;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import org.jetbrains.annotations.NotNull;
import dagger.Module;
import dagger.Provides;

import jakarta.inject.Singleton;

@Module
public class ConfigurationModule {

    @Provides
    @Singleton
    static @NotNull ListenerConfigurationService listenerConfiguration(final @NotNull ConfigurationService configurationService) {
        return configurationService.listenerConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull MqttConfigurationService mqttConfigurationService(final @NotNull ConfigurationService configurationService) {
        return configurationService.mqttConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull MqttsnConfigurationService mqttsnConfigurationService(final @NotNull ConfigurationService configurationService) {
        return configurationService.mqttsnConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull RestrictionsConfigurationService restrictionsConfigurationService(final @NotNull ConfigurationService configurationService) {
        return configurationService.restrictionsConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull SecurityConfigurationService securityConfigurationService(final @NotNull ConfigurationService configurationService) {
        return configurationService.securityConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull BridgeExtractor bridgeConfiguration(final @NotNull ConfigurationService configurationService) {
        return configurationService.bridgeExtractor();
    }

    @Provides
    @Singleton
    static @NotNull UnsExtractor unsExtractor(final @NotNull ConfigurationService configurationService) {
        return configurationService.unsExtractor();
    }

    @Provides
    @Singleton
    static @NotNull ProtocolAdapterExtractor protocolAdapterExtractor(final @NotNull ConfigurationService configurationService) {
        return configurationService.protocolAdapterExtractor();
    }

    @Provides
    @Singleton
    static @NotNull DataCombiningExtractor dataCombiningExtractor(final @NotNull ConfigurationService configurationService) {
        return configurationService.dataCombiningExtractor();
    }

    @Provides
    @Singleton
    static @NotNull ApiConfigurationService apiConfigurationService(final @NotNull ConfigurationService configurationService) {
        return configurationService.apiConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull DynamicConfigurationService gatewayConfigurationService(final @NotNull ConfigurationService configurationService) {
        return configurationService.gatewayConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull PersistenceConfigurationService persistenceConfigurationService(final @NotNull ConfigurationService configurationService) {
        return configurationService.persistenceConfigurationService();
    }

    @Provides
    @Singleton
    static @NotNull InternalConfigurationService internalConfigurationService(final @NotNull ConfigurationService configurationService) {
        return configurationService.internalConfigurationService();
    }

}
