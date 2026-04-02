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

import com.hivemq.configuration.reader.AssetMappingExtractor;
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.configuration.reader.PulseExtractor;
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
import com.hivemq.edge.compiler.lib.serialization.CompiledConfigSerializer;
import com.hivemq.edge.knappogue.CompiledConfigApplier;
import com.hivemq.edge.knappogue.CompiledConfigSubscriber;
import com.hivemq.edge.knappogue.WorkspaceHolder;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Module
public class ConfigurationModule {

    @Provides
    @Singleton
    static @NotNull ListenerConfigurationService listenerConfiguration(
            final @NotNull ConfigurationService configurationService) {
        return configurationService.listenerConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull MqttConfigurationService mqttConfigurationService(
            final @NotNull ConfigurationService configurationService) {
        return configurationService.mqttConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull MqttsnConfigurationService mqttsnConfigurationService(
            final @NotNull ConfigurationService configurationService) {
        return configurationService.mqttsnConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull RestrictionsConfigurationService restrictionsConfigurationService(
            final @NotNull ConfigurationService configurationService) {
        return configurationService.restrictionsConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull SecurityConfigurationService securityConfigurationService(
            final @NotNull ConfigurationService configurationService) {
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
    static @NotNull ProtocolAdapterExtractor protocolAdapterExtractor(
            final @NotNull ConfigurationService configurationService) {
        return configurationService.protocolAdapterExtractor();
    }

    @Provides
    @Singleton
    static @NotNull DataCombiningExtractor dataCombiningExtractor(
            final @NotNull ConfigurationService configurationService) {
        return configurationService.dataCombiningExtractor();
    }

    @Provides
    @Singleton
    static @NotNull AssetMappingExtractor assetMappingExtractor(
            final @NotNull ConfigurationService configurationService) {
        return configurationService.assetMappingExtractor();
    }

    @Provides
    @Singleton
    static @NotNull PulseExtractor pulseExtractor(final @NotNull ConfigurationService configurationService) {
        return configurationService.pulseExtractor();
    }

    @Provides
    @Singleton
    static @NotNull ApiConfigurationService apiConfigurationService(
            final @NotNull ConfigurationService configurationService) {
        return configurationService.apiConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull DynamicConfigurationService gatewayConfigurationService(
            final @NotNull ConfigurationService configurationService) {
        return configurationService.gatewayConfiguration();
    }

    @Provides
    @Singleton
    static @NotNull PersistenceConfigurationService persistenceConfigurationService(
            final @NotNull ConfigurationService configurationService) {
        return configurationService.persistenceConfigurationService();
    }

    @Provides
    @Singleton
    static @NotNull InternalConfigurationService internalConfigurationService(
            final @NotNull ConfigurationService configurationService) {
        return configurationService.internalConfigurationService();
    }

    @Provides
    @Singleton
    static @NotNull ConfigFileReaderWriter configFileReaderWriter(
            final @NotNull ConfigurationService configurationService) {
        return configurationService.getConfigFileReaderWriter();
    }

    @Provides
    @Singleton
    static @NotNull CompiledConfigApplier compiledConfigApplier(
            final @NotNull ConfigFileReaderWriter configFileReaderWriter,
            final @NotNull WorkspaceHolder workspaceHolder) {
        return new CompiledConfigApplier(configFileReaderWriter, workspaceHolder);
    }

    @Provides
    @Singleton
    static @NotNull CompiledConfigSubscriber compiledConfigSubscriber(
            final @NotNull CompiledConfigApplier applier,
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService) {
        return new CompiledConfigSubscriber(
                applier, new CompiledConfigSerializer(), localTopicTree, clientQueuePersistence, singleWriterService);
    }
}
