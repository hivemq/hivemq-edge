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
import com.hivemq.configuration.service.BridgeConfigurationService;
import com.hivemq.configuration.service.DynamicConfigurationService;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.MqttsnConfigurationService;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.ProtocolAdapterConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.configuration.service.UnsConfigurationService;
import com.hivemq.configuration.service.UsageTrackingConfigurationService;
import com.hivemq.configuration.service.impl.ApiConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.BridgeConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.GatewayConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.InternalConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.MqttConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.MqttsnConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.PersistenceConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.ProtocolAdapterConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.RestrictionsConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.SecurityConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.UnsConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.UsageTrackingConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockitoAnnotations;

import java.io.File;

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
    BridgeConfigurationService bridgeConfigurationService;
    ApiConfigurationService apiConfigurationService;
    UnsConfigurationService unsConfigurationService;
    DynamicConfigurationService dynamicConfigurationService;
    UsageTrackingConfigurationService usageTrackingConfigurationService;
    ProtocolAdapterConfigurationService protocolAdapterConfigurationService;
    InternalConfigurationService internalConfigurationService = new InternalConfigurationServiceImpl();

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
        bridgeConfigurationService = new BridgeConfigurationServiceImpl();
        apiConfigurationService = new ApiConfigurationServiceImpl();
        unsConfigurationService = new UnsConfigurationServiceImpl();
        dynamicConfigurationService = new GatewayConfigurationServiceImpl();
        usageTrackingConfigurationService = new UsageTrackingConfigurationServiceImpl();
        protocolAdapterConfigurationService = new ProtocolAdapterConfigurationServiceImpl();

        final ConfigurationFile configurationFile = new ConfigurationFile(xmlFile);
        reader = new ConfigFileReaderWriter(
                configurationFile,
                new RestrictionConfigurator(restrictionsConfigurationService),
                new SecurityConfigurator(securityConfigurationService),
                new MqttConfigurator(mqttConfigurationService),
                new ListenerConfigurator(listenerConfigurationService, systemInformation),
                new PersistenceConfigurator(persistenceConfigurationService),
                new MqttsnConfigurator(mqttsnConfigurationService),
                new BridgeConfigurator(bridgeConfigurationService),
                new ApiConfigurator(apiConfigurationService),
                new UnsConfigurator(unsConfigurationService),
                new DynamicConfigConfigurator(dynamicConfigurationService),
                new UsageTrackingConfigurator(usageTrackingConfigurationService),
                new ProtocolAdapterConfigurator(protocolAdapterConfigurationService),
                new InternalConfigurator(internalConfigurationService));
    }

}
