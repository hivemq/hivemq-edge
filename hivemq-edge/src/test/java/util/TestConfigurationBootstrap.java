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
package util;

import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.configuration.service.BridgeConfigurationService;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.DynamicConfigurationService;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.ProtocolAdapterConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.configuration.service.UnsConfigurationService;
import com.hivemq.configuration.service.UsageTrackingConfigurationService;
import com.hivemq.configuration.service.impl.ApiConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.BridgeConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.ConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.GatewayConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.MqttConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.MqttsnConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.PersistenceConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.ProtocolAdapterConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.RestrictionsConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.SecurityConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.UnsConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.UsageTrackingConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;

/**
 * @author Christoph Schäbel
 */
public class TestConfigurationBootstrap {

    private ListenerConfigurationServiceImpl listenerConfigurationService;
    private MqttConfigurationServiceImpl mqttConfigurationService;
    private MqttsnConfigurationServiceImpl mqttsnConfigurationService;
    private RestrictionsConfigurationServiceImpl restrictionsConfigurationService;
    private final SecurityConfigurationServiceImpl securityConfigurationService;
    private ConfigurationServiceImpl configurationService;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final BridgeConfigurationService bridgeConfigurationService;
    private final ApiConfigurationService apiConfigurationService;
    private final UnsConfigurationService unsConfigurationService;
    private final DynamicConfigurationService dynamicConfigurationService;
    private final UsageTrackingConfigurationService usageTrackingConfigurationService;
    private final ProtocolAdapterConfigurationService protocolAdapterConfigurationService;

    public TestConfigurationBootstrap() {
        listenerConfigurationService = new ListenerConfigurationServiceImpl();
        mqttConfigurationService = new MqttConfigurationServiceImpl();
        mqttsnConfigurationService = new MqttsnConfigurationServiceImpl();
        restrictionsConfigurationService = new RestrictionsConfigurationServiceImpl();
        securityConfigurationService = new SecurityConfigurationServiceImpl();
        persistenceConfigurationService = new PersistenceConfigurationServiceImpl();
        bridgeConfigurationService = new BridgeConfigurationServiceImpl();
        apiConfigurationService = new ApiConfigurationServiceImpl();
        unsConfigurationService = new UnsConfigurationServiceImpl();
        dynamicConfigurationService = new GatewayConfigurationServiceImpl();

        //-- Ensure usage reporting is disabled during tests
        usageTrackingConfigurationService = new UsageTrackingConfigurationServiceImpl() {
            @Override
            public boolean isUsageTrackingEnabled() {
                return false;
            }
        };
        protocolAdapterConfigurationService = new ProtocolAdapterConfigurationServiceImpl();

        configurationService = new ConfigurationServiceImpl(listenerConfigurationService,
                mqttConfigurationService,
                restrictionsConfigurationService,
                securityConfigurationService,
                persistenceConfigurationService,
                mqttsnConfigurationService,
                bridgeConfigurationService,
                apiConfigurationService,
                unsConfigurationService,
                dynamicConfigurationService,
                usageTrackingConfigurationService,
                protocolAdapterConfigurationService);
    }

    public SecurityConfigurationService getSecurityConfigurationService() {
        return securityConfigurationService;
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public ListenerConfigurationServiceImpl getListenerConfigurationService() {
        return listenerConfigurationService;
    }

    public void setListenerConfigurationService(final ListenerConfigurationServiceImpl listenerConfigurationService) {
        this.listenerConfigurationService = listenerConfigurationService;
    }

    public MqttConfigurationServiceImpl getMqttConfigurationService() {
        return mqttConfigurationService;
    }

    public void setMqttConfigurationService(final MqttConfigurationServiceImpl mqttConfigurationService) {
        this.mqttConfigurationService = mqttConfigurationService;
    }

    public RestrictionsConfigurationServiceImpl getRestrictionsConfigurationService() {
        return restrictionsConfigurationService;
    }

    public void setRestrictionsConfigurationService(final RestrictionsConfigurationServiceImpl restrictionsConfigurationService) {
        this.restrictionsConfigurationService = restrictionsConfigurationService;
    }

    public void setConfigurationService(final ConfigurationServiceImpl configurationService) {
        this.configurationService = configurationService;
    }

    public PersistenceConfigurationService getPersistenceConfigurationService() {
        return persistenceConfigurationService;
    }

    public MqttsnConfigurationServiceImpl getMqttsnConfigurationService() {
        return mqttsnConfigurationService;
    }

    public void setMqttsnConfigurationService(final MqttsnConfigurationServiceImpl mqttsnConfigurationService) {
        this.mqttsnConfigurationService = mqttsnConfigurationService;
    }

    public BridgeConfigurationService getBridgeConfigurationService() {
        return bridgeConfigurationService;
    }

    public ApiConfigurationService getApiConfigurationService() {
        return apiConfigurationService;
    }

    public DynamicConfigurationService getGatewayConfigurationService() {
        return dynamicConfigurationService;
    }

    public UsageTrackingConfigurationService getUsageTrackingConfigurationService() {
        return usageTrackingConfigurationService;
    }
}
