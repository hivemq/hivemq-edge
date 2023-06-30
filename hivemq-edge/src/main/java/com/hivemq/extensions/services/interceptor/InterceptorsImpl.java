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

package com.hivemq.extensions.services.interceptor;

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.bridge.BridgePublishInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.bridge.BridgePublishOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.protocoladapter.ProtocolAdapterPublishInboundInterceptorProvider;
import com.hivemq.extensions.ExtensionPriorityComparator;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Lukas Brandl
 * @author Florian Limpöck
 */
@Singleton
public class InterceptorsImpl implements Interceptors {

    @NotNull
    private final Map<@NotNull String, @NotNull ConnectInboundInterceptorProvider> connectInboundInterceptorProviderMap;

    @NotNull
    private final Map<@NotNull String, @NotNull ConnackOutboundInterceptorProvider>
            connackOutboundInterceptorProviderMap;

    @NotNull
    private final Map<@NotNull String, @NotNull BridgePublishInboundInterceptorProvider>
            bridgeInboundInterceptorProviderMap;

    @NotNull
    private final Map<@NotNull String, @NotNull BridgePublishOutboundInterceptorProvider>
            bridgeOutboundInterceptorProviderMap;

    @NotNull
    private final Map<@NotNull String, @NotNull ProtocolAdapterPublishInboundInterceptorProvider>
            protocolAdapterInboundInterceptorProviderMap;


    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final ReadWriteLock readWriteLock;

    @Inject
    public InterceptorsImpl(@NotNull final HiveMQExtensions hiveMQExtensions) {
        this.hiveMQExtensions = hiveMQExtensions;
        final ExtensionPriorityComparator extensionPriorityComparator =
                new ExtensionPriorityComparator(hiveMQExtensions);
        this.connectInboundInterceptorProviderMap = new TreeMap<>(extensionPriorityComparator);
        this.connackOutboundInterceptorProviderMap = new TreeMap<>(extensionPriorityComparator);
        this.bridgeInboundInterceptorProviderMap = new TreeMap<>(extensionPriorityComparator);
        this.bridgeOutboundInterceptorProviderMap = new TreeMap<>(extensionPriorityComparator);
        this.protocolAdapterInboundInterceptorProviderMap = new TreeMap<>(extensionPriorityComparator);
        this.readWriteLock = new ReentrantReadWriteLock();
        hiveMQExtensions.addAfterExtensionStopCallback(hiveMQExtension -> {
            final ClassLoader pluginClassloader = hiveMQExtension.getExtensionClassloader();
            if (pluginClassloader != null) {
                removeInterceptors(hiveMQExtension.getId());
            }
        });

    }

    @Override
    public void addConnectInboundInterceptorProvider(@NotNull final ConnectInboundInterceptorProvider provider) {
        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();

        try {

            final ClassLoader pluginClassloader = provider.getClass().getClassLoader();

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);

            if (plugin != null) {
                connectInboundInterceptorProviderMap.put(plugin.getId(), provider);
            }

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    @NotNull
    public ImmutableMap<String, ConnectInboundInterceptorProvider> connectInboundInterceptorProviders() {
        final Lock readLock = readWriteLock.readLock();

        readLock.lock();
        try {
            return ImmutableMap.copyOf(connectInboundInterceptorProviderMap);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void addConnackOutboundInterceptorProvider(final @NotNull ConnackOutboundInterceptorProvider provider) {
        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();

        try {

            final ClassLoader pluginClassloader = provider.getClass().getClassLoader();

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);

            if (plugin != null) {
                connackOutboundInterceptorProviderMap.put(plugin.getId(), provider);
            }

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public @NotNull ImmutableMap<String, ConnackOutboundInterceptorProvider> connackOutboundInterceptorProviders() {
        final Lock readLock = readWriteLock.readLock();

        readLock.lock();
        try {
            return ImmutableMap.copyOf(connackOutboundInterceptorProviderMap);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void addBridgeInboundInterceptorProvider(@NotNull final BridgePublishInboundInterceptorProvider provider) {
        final Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            final ClassLoader pluginClassloader = provider.getClass().getClassLoader();
            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);
            if (plugin != null) {
                bridgeInboundInterceptorProviderMap.put(plugin.getId(), provider);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public @NotNull ImmutableMap<String, BridgePublishInboundInterceptorProvider> bridgeInboundInterceptorProviders() {
        final Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            return ImmutableMap.copyOf(bridgeInboundInterceptorProviderMap);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void addBridgeOutboundInterceptorProvider(@NotNull final BridgePublishOutboundInterceptorProvider provider) {
        final Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            final ClassLoader pluginClassloader = provider.getClass().getClassLoader();
            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);
            if (plugin != null) {
                bridgeOutboundInterceptorProviderMap.put(plugin.getId(), provider);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public @NotNull ImmutableMap<String, BridgePublishOutboundInterceptorProvider> bridgeOutboundInterceptorProviders() {
        final Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            return ImmutableMap.copyOf(bridgeOutboundInterceptorProviderMap);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void addProtocolAdapterInboundInterceptorProvider(@NotNull final ProtocolAdapterPublishInboundInterceptorProvider provider) {
        final Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            final ClassLoader pluginClassloader = provider.getClass().getClassLoader();
            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);
            if (plugin != null) {
                protocolAdapterInboundInterceptorProviderMap.put(plugin.getId(), provider);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public @NotNull ImmutableMap<String, ProtocolAdapterPublishInboundInterceptorProvider> protocolAdapterOutboundInterceptorProviders() {
        final Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            return ImmutableMap.copyOf(protocolAdapterInboundInterceptorProviderMap);
        } finally {
            readLock.unlock();
        }
    }

    private void removeInterceptors(@NotNull final String pluginId) {

        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();
        try {
            connectInboundInterceptorProviderMap.remove(pluginId);
            connackOutboundInterceptorProviderMap.remove(pluginId);
            bridgeInboundInterceptorProviderMap.remove(pluginId);
            bridgeOutboundInterceptorProviderMap.remove(pluginId);
            protocolAdapterInboundInterceptorProviderMap.remove(pluginId);
        } finally {
            writeLock.unlock();
        }
    }
}
