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
package com.hivemq.configuration;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.ioc.ConfigurationFileProvider;
import com.hivemq.configuration.reader.ApiConfigurator;
import com.hivemq.configuration.reader.BridgeConfigurator;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.configuration.reader.DynamicConfigConfigurator;
import com.hivemq.configuration.reader.InternalConfigurator;
import com.hivemq.configuration.reader.ListenerConfigurator;
import com.hivemq.configuration.reader.ModuleConfigurator;
import com.hivemq.configuration.reader.MqttConfigurator;
import com.hivemq.configuration.reader.MqttsnConfigurator;
import com.hivemq.configuration.reader.PersistenceConfigurator;
import com.hivemq.configuration.reader.ProtocolAdapterConfigurator;
import com.hivemq.configuration.reader.RestrictionConfigurator;
import com.hivemq.configuration.reader.SecurityConfigurator;
import com.hivemq.configuration.reader.UnsConfigurator;
import com.hivemq.configuration.reader.UsageTrackingConfigurator;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.impl.ApiConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.BridgeConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.ConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.GatewayConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.InternalConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.ModuleConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.MqttConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.MqttsnConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.PersistenceConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.ProtocolAdapterConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.RestrictionsConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.SecurityConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.UnsConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.UsageTrackingConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Christoph Schäbel
 */
public class ConfigurationBootstrap {

    public static @NotNull ConfigurationService bootstrapConfig(final @NotNull SystemInformation systemInformation) {

        final ConfigurationServiceImpl configurationService =
                new ConfigurationServiceImpl(new ListenerConfigurationServiceImpl(),
                        new MqttConfigurationServiceImpl(),
                        new RestrictionsConfigurationServiceImpl(),
                        new SecurityConfigurationServiceImpl(),
                        new PersistenceConfigurationServiceImpl(),
                        new MqttsnConfigurationServiceImpl(),
                        new BridgeConfigurationServiceImpl(),
                        new ApiConfigurationServiceImpl(),
                        new UnsConfigurationServiceImpl(),
                        new GatewayConfigurationServiceImpl(),
                        new UsageTrackingConfigurationServiceImpl(),
                        new ProtocolAdapterConfigurationServiceImpl(),
                        new ModuleConfigurationServiceImpl(),
                        new InternalConfigurationServiceImpl());

        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);

        final ConfigFileReaderWriter configFileReader = new ConfigFileReaderWriter(configurationFile,
                new RestrictionConfigurator(configurationService.restrictionsConfiguration()),
                new SecurityConfigurator(configurationService.securityConfiguration()),
                new MqttConfigurator(configurationService.mqttConfiguration()),
                new ListenerConfigurator(configurationService.listenerConfiguration(), systemInformation),
                new PersistenceConfigurator(configurationService.persistenceConfigurationService()),
                new MqttsnConfigurator(configurationService.mqttsnConfiguration()),
                new BridgeConfigurator(configurationService.bridgeConfiguration()),
                new ApiConfigurator(configurationService.apiConfiguration()),
                new UnsConfigurator(configurationService.unsConfiguration()),
                new DynamicConfigConfigurator(configurationService.gatewayConfiguration()),
                new UsageTrackingConfigurator(configurationService.usageTrackingConfiguration()),
                new ProtocolAdapterConfigurator(configurationService.protocolAdapterConfigurationService()),
                new ModuleConfigurator(configurationService.commercialModuleConfigurationService()),
                new InternalConfigurator(configurationService.internalConfigurationService()));

        configFileReader.applyConfig();
        configurationService.setConfigFileReaderWriter(configFileReader);
        return configurationService;
    }

}
