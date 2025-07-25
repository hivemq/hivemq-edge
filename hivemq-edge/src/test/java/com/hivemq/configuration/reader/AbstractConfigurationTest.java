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

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.configuration.service.DynamicConfigurationService;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.ModuleConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.MqttsnConfigurationService;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.configuration.service.UsageTrackingConfigurationService;
import com.hivemq.configuration.service.impl.ApiConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.GatewayConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.InternalConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.ModuleConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.MqttConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.MqttsnConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.PersistenceConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.RestrictionsConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.SecurityConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.UsageTrackingConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.List;

public class AbstractConfigurationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    ListenerConfigurationService listenerConfigurationService;
    File xmlFile;
    ConfigFileReaderWriter reader;
    MqttConfigurationService mqttConfigurationService;
    MqttsnConfigurationService mqttsnConfigurationService;
    RestrictionsConfigurationService restrictionsConfigurationService;
    SecurityConfigurationService securityConfigurationService;
    SystemInformation systemInformation;
    PersistenceConfigurationService persistenceConfigurationService;
    ApiConfigurationService apiConfigurationService;
    DynamicConfigurationService dynamicConfigurationService;
    UsageTrackingConfigurationService usageTrackingConfigurationService;
    ModuleConfigurationService moduleConfigurationService;
    InternalConfigurationService internalConfigurationService = new InternalConfigurationServiceImpl();

    BridgeExtractor bridgeConfiguration;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        listenerConfigurationService = new ListenerConfigurationServiceImpl();

        xmlFile = temporaryFolder.newFile();
        securityConfigurationService = new SecurityConfigurationServiceImpl();
        mqttConfigurationService = new MqttConfigurationServiceImpl();
        mqttsnConfigurationService = new MqttsnConfigurationServiceImpl();
        restrictionsConfigurationService = new RestrictionsConfigurationServiceImpl();
        systemInformation = new SystemInformationImpl(false);
        persistenceConfigurationService = new PersistenceConfigurationServiceImpl();
        apiConfigurationService = new ApiConfigurationServiceImpl();
        dynamicConfigurationService = new GatewayConfigurationServiceImpl();
        usageTrackingConfigurationService = new UsageTrackingConfigurationServiceImpl();
        moduleConfigurationService = new ModuleConfigurationServiceImpl();

        final ConfigurationFile configurationFile = new ConfigurationFile(xmlFile);
        reader = new ConfigFileReaderWriter(
                systemInformation,
                configurationFile,
                List.of(
                        new RestrictionConfigurator(restrictionsConfigurationService),
                        new SecurityConfigurator(securityConfigurationService),
                        new MqttConfigurator(mqttConfigurationService),
                        new ListenerConfigurator(listenerConfigurationService, systemInformation),
                        new PersistenceConfigurator(persistenceConfigurationService),
                        new MqttsnConfigurator(mqttsnConfigurationService),
                        new ApiConfigurator(apiConfigurationService),
                        new DynamicConfigConfigurator(dynamicConfigurationService),
                        new UsageTrackingConfigurator(usageTrackingConfigurationService),
                        new ModuleConfigurator(moduleConfigurationService),
                        new InternalConfigurator(internalConfigurationService)));
        bridgeConfiguration = reader.getBridgeExtractor();
    }

}
