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
package com.hivemq.protocols.v2.wiring;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.protocols.v2.manager.DefaultProtocolAdapterWrapperFactory;
import com.hivemq.protocols.v2.manager.ProtocolAdapterFactoryRegistry;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManager;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage;
import com.hivemq.protocols.v2.manager.ProtocolAdapterWrapperFactory;
import com.hivemq.protocols.v2.runtime.Clock;
import com.hivemq.protocols.v2.runtime.SystemClock;
import com.hivemq.protocols.v2.runtime.SystemDispatcher;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The single additive Dagger module that wires the v2 protocol-adapter subsystem into Edge <b>beside</b> the legacy
 * framework (touchpoint 4). It contributes only new bindings — it edits nothing the legacy path depends on. Its
 * factory registry is discovered from the loaded modules through the shared {@link ModuleLoader}, exactly as the
 * legacy framework discovers its own adapter types: each loaded module is scanned for the v2
 * {@link ProtocolAdapterFactory} service. A production distribution bundles no v2 adapter module, so the registry is
 * <b>empty</b> and {@code GET /api/v2/.../types} is empty until a real adapter type is ported; a build that bundles a
 * v2 adapter module — for example a test build carrying the chaos test simulator — has that type discovered here and
 * served like any other.
 * <p>
 * The module provides the actor-runtime singletons (one production {@link SystemClock} and one {@link SystemDispatcher}
 * shared by the manager and every wrapper), the two registries, the wrapper-assembly seam, the supervisor
 * {@link ProtocolAdapterManager}, and its mailbox — exposed as both a {@link Mailbox} (for
 * {@link ProtocolAdapterLifecycle} to attach and pump) and a {@link MailboxSender} (the tell-only handle the REST
 * resource and the config-extractor consumer hold). The actor lifecycle itself — attaching the manager to the
 * dispatcher, binding it to its own mailbox, scheduling its tick, and registering the configuration consumer — is
 * closed by {@link ProtocolAdapterLifecycle} at bootstrap, not at graph construction, so no thread starts until Edge
 * starts (touchpoint 5).
 */
@Module
public abstract class ProtocolAdapterModule {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdapterModule.class);

    /** The wrapper tick period (~50 ms): the cadence at which each wrapper fires its timers. */
    private static final long WRAPPER_TICK_PERIOD_MILLIS = 50L;

    @Provides
    @Singleton
    static @NotNull SystemClock systemClock() {
        return new SystemClock();
    }

    // The interface binding and the concrete binding resolve to the SAME singleton: the manager and the wrapper
    // factory schedule against it, and ProtocolAdapterLifecycle closes it on shutdown.
    @Binds
    @Singleton
    abstract @NotNull Clock clock(final @NotNull SystemClock clock);

    @Provides
    @Singleton
    static @NotNull MessageDispatcher messageDispatcher() {
        return new SystemDispatcher();
    }

    @Provides
    @Singleton
    static @NotNull ProtocolAdapterFactoryRegistry factoryRegistry(final @NotNull ModuleLoader moduleLoader) {
        // The v2 adapter types are discovered from the loaded modules through the shared module loader, the same way
        // the legacy framework discovers its own: each loaded module is scanned for the v2 ProtocolAdapterFactory
        // service and the implementations are instantiated through their no-argument constructor. A production
        // distribution bundles no v2 adapter module, so this set is empty and GET .../types is empty until a real
        // adapter type is ported; a build that bundles a v2 adapter module has its factory discovered here.
        final Set<ProtocolAdapterFactory> discovered = new LinkedHashSet<>();
        final Set<Class<? extends ProtocolAdapterFactory>> alreadySeen = new LinkedHashSet<>();
        for (final Class<? extends ProtocolAdapterFactory> factoryClass :
                moduleLoader.findImplementations(ProtocolAdapterFactory.class)) {
            // A module classloader delegates to its parent, so a factory on the application classpath is reported once
            // per loaded module — keep only the first sighting of each type, or the registry rejects the duplicate.
            if (!alreadySeen.add(factoryClass)) {
                continue;
            }
            try {
                discovered.add(factoryClass.getDeclaredConstructor().newInstance());
            } catch (final ReflectiveOperationException exception) {
                log.error(
                        "Skipping v2 protocol adapter factory '{}': it could not be instantiated through a "
                                + "no-argument constructor ({})",
                        factoryClass.getName(),
                        exception.getMessage());
            }
        }
        return new ProtocolAdapterFactoryRegistry(discovered);
    }

    @Provides
    @Singleton
    static @NotNull ProtocolAdapterHandleRegistry handleRegistry() {
        return new ProtocolAdapterHandleRegistry();
    }

    @Provides
    @Singleton
    static @NotNull ProtocolAdapterWrapperFactory wrapperFactory(
            final @NotNull Clock clock,
            final @NotNull MessageDispatcher dispatcher,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ObjectMapper objectMapper) {
        return new DefaultProtocolAdapterWrapperFactory(
                clock,
                dispatcher,
                metricRegistry,
                new DefaultDataPointFactory(),
                objectMapper,
                WRAPPER_TICK_PERIOD_MILLIS);
    }

    @Provides
    @Singleton
    static @NotNull ProtocolAdapterManager protocolAdapterManager(
            final @NotNull ProtocolAdapterFactoryRegistry factoryRegistry,
            final @NotNull ProtocolAdapterHandleRegistry handleRegistry,
            final @NotNull ProtocolAdapterWrapperFactory wrapperFactory,
            final @NotNull Clock clock) {
        return new ProtocolAdapterManager(factoryRegistry, handleRegistry, wrapperFactory, clock);
    }

    @Provides
    @Singleton
    static @NotNull Mailbox<ProtocolAdapterManagerMessage> managerMailbox() {
        return new DefaultMailbox<>();
    }

    // The tell-only handle the REST resource and the config-extractor consumer hold is the manager mailbox itself.
    @Binds
    @Singleton
    abstract @NotNull MailboxSender<ProtocolAdapterManagerMessage> managerSender(
            final @NotNull Mailbox<ProtocolAdapterManagerMessage> mailbox);
}
