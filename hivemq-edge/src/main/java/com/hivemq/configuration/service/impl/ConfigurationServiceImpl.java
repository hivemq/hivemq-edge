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
package com.hivemq.configuration.service.impl;

import com.google.common.base.Preconditions;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.service.*;
import com.hivemq.configuration.service.DynamicConfigurationService;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * The implementation of the {@link ConfigurationService}
 *
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
public class ConfigurationServiceImpl implements ConfigurationService {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    private final @NotNull ListenerConfigurationService listenerConfigurationService;
    private final @NotNull MqttConfigurationService mqttConfigurationService;
    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;
    private final @NotNull SecurityConfigurationService securityConfigurationService;
    private final @NotNull PersistenceConfigurationService persistenceConfigurationService;
    private final @NotNull MqttsnConfigurationService mqttsnConfigurationService;
    private final @NotNull BridgeConfigurationService bridgeConfigurationService;
    private final @NotNull ApiConfigurationService apiConfigurationService;
    private final @NotNull UnsConfigurationService unsConfigurationService;
    private final @NotNull DynamicConfigurationService dynamicConfigurationService;
    private final @NotNull UsageTrackingConfigurationService usageTrackingConfigurationService;
    private final @NotNull ProtocolAdapterConfigurationService protocolAdapterConfigurationService;
    private final @NotNull InternalConfigurationService internalConfigurationService;
    private @Nullable ConfigFileReaderWriter configFileReaderWriter;
    private final @NotNull ReadWriteLock lock = new ReentrantReadWriteLock();
    private final @NotNull AtomicLong lastWrite = new AtomicLong(0L);


    public ConfigurationServiceImpl(
            final @NotNull ListenerConfigurationService listenerConfigurationService,
            final @NotNull MqttConfigurationService mqttConfigurationService,
            final @NotNull RestrictionsConfigurationService restrictionsConfigurationService,
            final @NotNull SecurityConfigurationService securityConfigurationService,
            final @NotNull PersistenceConfigurationService persistenceConfigurationService,
            final @NotNull MqttsnConfigurationService mqttsnConfigurationService,
            final @NotNull BridgeConfigurationService bridgeConfigurationService,
            final @NotNull ApiConfigurationService apiConfigurationService,
            final @NotNull UnsConfigurationService unsConfigurationService,
            final @NotNull DynamicConfigurationService dynamicConfigurationService,
            final @NotNull UsageTrackingConfigurationService usageTrackingConfigurationService,
            final @NotNull ProtocolAdapterConfigurationService protocolAdapterConfigurationService,
            final @NotNull InternalConfigurationService internalConfigurationService) {
        this.listenerConfigurationService = listenerConfigurationService;
        this.mqttConfigurationService = mqttConfigurationService;
        this.restrictionsConfigurationService = restrictionsConfigurationService;
        this.securityConfigurationService = securityConfigurationService;
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.mqttsnConfigurationService = mqttsnConfigurationService;
        this.bridgeConfigurationService = bridgeConfigurationService;
        this.apiConfigurationService = apiConfigurationService;
        this.unsConfigurationService = unsConfigurationService;
        this.dynamicConfigurationService = dynamicConfigurationService;
        this.usageTrackingConfigurationService = usageTrackingConfigurationService;
        this.protocolAdapterConfigurationService = protocolAdapterConfigurationService;
        this.internalConfigurationService = internalConfigurationService;
    }

    @Override
    public @NotNull ListenerConfigurationService listenerConfiguration() {
        return listenerConfigurationService;
    }

    @Override
    public @NotNull MqttConfigurationService mqttConfiguration() {
        return mqttConfigurationService;
    }

    @Override
    public @NotNull RestrictionsConfigurationService restrictionsConfiguration() {
        return restrictionsConfigurationService;
    }

    @Override
    public @NotNull SecurityConfigurationService securityConfiguration() {
        return securityConfigurationService;
    }

    @Override
    public @NotNull PersistenceConfigurationService persistenceConfigurationService() {
        return persistenceConfigurationService;
    }

    @Override
    public @NotNull MqttsnConfigurationService mqttsnConfiguration() {
        return mqttsnConfigurationService;
    }

    @Override
    public @NotNull ApiConfigurationService apiConfiguration() {
        return apiConfigurationService;
    }

    @Override
    public @NotNull DynamicConfigurationService gatewayConfiguration() {
        return dynamicConfigurationService;
    }

    @Override
    public UsageTrackingConfigurationService usageTrackingConfiguration() {
        return usageTrackingConfigurationService;
    }

    public @NotNull ProtocolAdapterConfigurationService protocolAdapterConfigurationService() {
        return proxy(ProtocolAdapterConfigurationService.class, protocolAdapterConfigurationService);
    }

    @Override
    public @NotNull InternalConfigurationService internalConfigurationService() {
        return internalConfigurationService;
    }

    @Override
    public @NotNull BridgeConfigurationService bridgeConfiguration() {
        return proxy(BridgeConfigurationService.class, bridgeConfigurationService);
    }

    public @NotNull UnsConfigurationService unsConfiguration() {
        return proxy(UnsConfigurationService.class, unsConfigurationService);
    }

    @Override
    public void setConfigFileReaderWriter(final @NotNull ConfigFileReaderWriter configFileReaderWriter) {
        Preconditions.checkNotNull(configFileReaderWriter);
        this.configFileReaderWriter = configFileReaderWriter;
    }

    public <T> @NotNull T proxy(final @NotNull Class<? extends T> type,
                                final @NotNull Object instance,
                                final @Nullable Class<?>...ifs) {

        if(configFileReaderWriter == null){
            //-- This is the initial loading phase from the XML through the configurators
            // --back to the config services, so do not proxy this else it will lead to a loop
            return (T) instance;
        } else {
            InvocationHandler handler = (proxyIn, method, args) -> {
                boolean mutator = method.getName().startsWith("set") ||
                        method.getName().startsWith("add") ||
                        method.getName().startsWith("remove");
                Lock presentLock = mutator ? lock.writeLock() : lock.readLock();
                try {
                    presentLock.lock();
                    return method.invoke(instance, args);
                } finally {
                    if(mutator){
                        flush();
                    }
                    presentLock.unlock();
                }
            };
            Class<?>[] allInterfaces = Stream.concat(Stream.of(type), Stream.of(ifs))
                    .distinct()
                    .toArray(Class<?>[]::new);
            return (T) Proxy.newProxyInstance(
                    type.getClassLoader(), allInterfaces, handler);
        }
    }

    private void flush() {
        if(log.isTraceEnabled()){
            log.trace("flushing configuration changes to entity layer");
        }
        try {
            configFileReaderWriter.syncConfiguration();
            if(gatewayConfiguration().isMutableConfigurationEnabled()){
                configFileReaderWriter.writeConfig();
            }
        } finally {
            lastWrite.set(System.currentTimeMillis());
        }
    }

    @Override
    public void writeConfiguration(final @NotNull Writer writer) {
        if(dynamicConfigurationService.isConfigurationExportEnabled()){
            configFileReaderWriter.writeConfigToXML(writer);
        } else {
            throw new SecurityException("xml export not allowed");
        }
    }

    public Optional<Long> getLastUpdateTime(){
        long l = lastWrite.get();
        return l == 0 ? Optional.empty() : Optional.of(l);
    }
}
