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
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.service.ApiConfigurationService;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.DataCombiningConfigurationService;
import com.hivemq.configuration.service.DynamicConfigurationService;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.ModuleConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.MqttsnConfigurationService;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.ProtocolAdapterConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.configuration.service.UnsConfigurationService;
import com.hivemq.configuration.service.UsageTrackingConfigurationService;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    private final @NotNull ApiConfigurationService apiConfigurationService;
    private final @NotNull UnsConfigurationService unsConfigurationService;
    private final @NotNull DynamicConfigurationService dynamicConfigurationService;
    private final @NotNull UsageTrackingConfigurationService usageTrackingConfigurationService;
    private final @NotNull ProtocolAdapterConfigurationService protocolAdapterConfigurationService;
    private final @NotNull DataCombiningConfigurationServiceImpl dataCombiningConfigurationService;
    private final @NotNull ModuleConfigurationService moduleConfigurationService;
    private final @NotNull InternalConfigurationService internalConfigurationService;
    private @Nullable ConfigFileReaderWriter configFileReaderWriter;
    private final @NotNull ReadWriteLock lock = new ReentrantReadWriteLock();


    public ConfigurationServiceImpl(
            final @NotNull ListenerConfigurationService listenerConfigurationService,
            final @NotNull MqttConfigurationService mqttConfigurationService,
            final @NotNull RestrictionsConfigurationService restrictionsConfigurationService,
            final @NotNull SecurityConfigurationService securityConfigurationService,
            final @NotNull PersistenceConfigurationService persistenceConfigurationService,
            final @NotNull MqttsnConfigurationService mqttsnConfigurationService,
            final @NotNull ApiConfigurationService apiConfigurationService,
            final @NotNull UnsConfigurationService unsConfigurationService,
            final @NotNull DynamicConfigurationService dynamicConfigurationService,
            final @NotNull UsageTrackingConfigurationService usageTrackingConfigurationService,
            final @NotNull ProtocolAdapterConfigurationService protocolAdapterConfigurationService,
            final @NotNull DataCombiningConfigurationServiceImpl dataCombiningConfigurationService,
            final @NotNull ModuleConfigurationService moduleConfigurationService,
            final @NotNull InternalConfigurationService internalConfigurationService) {
        this.listenerConfigurationService = listenerConfigurationService;
        this.mqttConfigurationService = mqttConfigurationService;
        this.restrictionsConfigurationService = restrictionsConfigurationService;
        this.securityConfigurationService = securityConfigurationService;
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.mqttsnConfigurationService = mqttsnConfigurationService;
        this.apiConfigurationService = apiConfigurationService;
        this.unsConfigurationService = unsConfigurationService;
        this.dynamicConfigurationService = dynamicConfigurationService;
        this.usageTrackingConfigurationService = usageTrackingConfigurationService;
        this.protocolAdapterConfigurationService = protocolAdapterConfigurationService;
        this.dataCombiningConfigurationService = dataCombiningConfigurationService;
        this.moduleConfigurationService = moduleConfigurationService;
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
    public @NotNull UsageTrackingConfigurationService usageTrackingConfiguration() {
        return usageTrackingConfigurationService;
    }

    public @NotNull ProtocolAdapterConfigurationService protocolAdapterConfigurationService() {
        return proxy(ProtocolAdapterConfigurationService.class, protocolAdapterConfigurationService);
    }

    @Override
    public @NotNull DataCombiningConfigurationService dataCombiningConfigurationService() {
        return proxy(DataCombiningConfigurationService.class, dataCombiningConfigurationService);
    }

    public @NotNull ModuleConfigurationService commercialModuleConfigurationService() {
        return proxy(ModuleConfigurationService.class, moduleConfigurationService);
    }

    @Override
    public @NotNull InternalConfigurationService internalConfigurationService() {
        return internalConfigurationService;
    }

    public @NotNull UnsConfigurationService unsConfiguration() {
        return proxy(UnsConfigurationService.class, unsConfigurationService);
    }

    @Override
    public void setConfigFileReaderWriter(final @NotNull ConfigFileReaderWriter configFileReaderWriter) {
        Preconditions.checkNotNull(configFileReaderWriter);
        this.configFileReaderWriter = configFileReaderWriter;
    }

    @Override
    public @NotNull BridgeExtractor bridgeExtractor() {
        return configFileReaderWriter.getBridgeExtractor();
    }

    public <T> @NotNull T proxy(
            final @NotNull Class<T> type, final @NotNull T instance, final @Nullable Class<?>... ifs) {

        if (configFileReaderWriter == null) {
            //-- This is the initial loading phase from the XML through the configurators
            // --back to the config services, so do not proxy this else it will lead to a loop
            return instance;
        } else {
            final InvocationHandler handler = (proxyIn, method, args) -> {
                final boolean mutator = method.getName().startsWith("set") ||
                        method.getName().startsWith("add") ||
                        method.getName().startsWith("remove");
                final Lock presentLock = mutator ? lock.writeLock() : lock.readLock();
                try {
                    presentLock.lock();
                    return method.invoke(instance, args);
                } finally {
                    if (mutator) {
                        flush(configFileReaderWriter);
                    }
                    presentLock.unlock();
                }
            };
            final Class<?>[] allInterfaces =
                    Stream.concat(Stream.of(type), Stream.of(ifs)).distinct().toArray(Class<?>[]::new);
            //noinspection unchecked
            return (T) Proxy.newProxyInstance(type.getClassLoader(), allInterfaces, handler);
        }
    }

    private void flush(final @NotNull ConfigFileReaderWriter configFileReaderWriter) {
        configFileReaderWriter.writeConfigWithSync();
    }

    @Override
    public void writeConfiguration(final @NotNull Writer writer) {
        if (dynamicConfigurationService.isConfigurationExportEnabled() && configFileReaderWriter != null) {
            configFileReaderWriter.writeConfigToXML(writer);
        } else {
            throw new SecurityException("xml export not allowed");
        }
    }

    public @NotNull Optional<Long> getLastUpdateTime() {
        final long l = configFileReaderWriter.getLastWrite();
        return l == 0 ? Optional.empty() : Optional.of(l);
    }
}
