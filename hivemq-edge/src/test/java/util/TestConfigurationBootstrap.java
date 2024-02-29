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
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.ProtocolAdapterConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.configuration.service.UnsConfigurationService;
import com.hivemq.configuration.service.UsageTrackingConfigurationService;
import com.hivemq.configuration.service.impl.ApiConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.BridgeConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.ConfigurationServiceImpl;
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
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Christoph Schäbel
 */
public class TestConfigurationBootstrap {

    private @NotNull ListenerConfigurationServiceImpl listenerConfigurationService;
    private @NotNull MqttConfigurationServiceImpl mqttConfigurationService;
    private @NotNull MqttsnConfigurationServiceImpl mqttsnConfigurationService;
    private @NotNull RestrictionsConfigurationServiceImpl restrictionsConfigurationService;
    private final @NotNull SecurityConfigurationServiceImpl securityConfigurationService;
    private @NotNull ConfigurationServiceImpl configurationService;
    private final @NotNull PersistenceConfigurationService persistenceConfigurationService;
    private final @NotNull BridgeConfigurationService bridgeConfigurationService;
    private final @NotNull ApiConfigurationService apiConfigurationService;
    private final @NotNull UnsConfigurationService unsConfigurationService;
    private final @NotNull DynamicConfigurationService dynamicConfigurationService;
    private final @NotNull UsageTrackingConfigurationService usageTrackingConfigurationService;
    private final @NotNull ProtocolAdapterConfigurationService protocolAdapterConfigurationService;
    private final @NotNull InternalConfigurationService internalConfigurationService = new InternalConfigurationServiceImpl();

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
                protocolAdapterConfigurationService,
                internalConfigurationService);
    }

    public @NotNull SecurityConfigurationService getSecurityConfigurationService() {
        return securityConfigurationService;
    }

    public @NotNull ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public @NotNull ListenerConfigurationServiceImpl getListenerConfigurationService() {
        return listenerConfigurationService;
    }

    public void setListenerConfigurationService(final @NotNull ListenerConfigurationServiceImpl listenerConfigurationService) {
        this.listenerConfigurationService = listenerConfigurationService;
    }

    public @NotNull MqttConfigurationServiceImpl getMqttConfigurationService() {
        return mqttConfigurationService;
    }

    public void setMqttConfigurationService(final @NotNull MqttConfigurationServiceImpl mqttConfigurationService) {
        this.mqttConfigurationService = mqttConfigurationService;
    }

    public @NotNull RestrictionsConfigurationServiceImpl getRestrictionsConfigurationService() {
        return restrictionsConfigurationService;
    }

    public void setRestrictionsConfigurationService(final @NotNull RestrictionsConfigurationServiceImpl restrictionsConfigurationService) {
        this.restrictionsConfigurationService = restrictionsConfigurationService;
    }

    public void setConfigurationService(final @NotNull ConfigurationServiceImpl configurationService) {
        this.configurationService = configurationService;
    }

    public @NotNull PersistenceConfigurationService getPersistenceConfigurationService() {
        return persistenceConfigurationService;
    }

    public @NotNull MqttsnConfigurationServiceImpl getMqttsnConfigurationService() {
        return mqttsnConfigurationService;
    }

    public void setMqttsnConfigurationService(final @NotNull MqttsnConfigurationServiceImpl mqttsnConfigurationService) {
        this.mqttsnConfigurationService = mqttsnConfigurationService;
    }

    public @NotNull BridgeConfigurationService getBridgeConfigurationService() {
        return bridgeConfigurationService;
    }

    public @NotNull ApiConfigurationService getApiConfigurationService() {
        return apiConfigurationService;
    }

    public @NotNull DynamicConfigurationService getGatewayConfigurationService() {
        return dynamicConfigurationService;
    }

    public @NotNull UsageTrackingConfigurationService getUsageTrackingConfigurationService() {
        return usageTrackingConfigurationService;
    }
}
