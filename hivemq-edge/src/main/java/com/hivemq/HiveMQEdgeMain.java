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
package com.hivemq;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.hivemq.bootstrap.LoggingBootstrap;
import com.hivemq.bootstrap.ioc.Injector;
import com.hivemq.bootstrap.ioc.Persistences;
import com.hivemq.bootstrap.services.AfterHiveMQStartBootstrapServiceImpl;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.exceptions.HiveMQEdgeStartupException;
import com.hivemq.http.JaxrsHttpServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class HiveMQEdgeMain {
    private static final @NotNull Logger log = LoggerFactory.getLogger(HiveMQEdgeMain.class);

    private final @NotNull ModuleLoader moduleLoader;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull SystemInformation systemInformation;
    private @Nullable ConfigurationService configService;
    private @Nullable JaxrsHttpServer jaxrsServer;
    private @Nullable Injector injector;
    private @Nullable Thread shutdownThread;

    public HiveMQEdgeMain(
            final @NotNull SystemInformation systemInformation,
            final @NotNull MetricRegistry metricRegistry,
            final @Nullable ConfigurationService configService,
            final @NotNull ModuleLoader moduleLoader) {
        this.metricRegistry = metricRegistry;
        this.systemInformation = systemInformation;
        this.configService = configService;
        this.moduleLoader = moduleLoader;
    }

    public static void main(final String @NotNull [] args) throws Exception {
        log.info("Starting HiveMQ Edge...");
        final long startTime = System.nanoTime();
        final SystemInformationImpl systemInformation = new SystemInformationImpl(true);
        final ModuleLoader moduleLoader = new ModuleLoader(systemInformation);
        final HiveMQEdgeMain server = new HiveMQEdgeMain(systemInformation, new MetricRegistry(), null, moduleLoader);
        try {
            server.start(null);
            log.info("Started HiveMQ Edge in {}ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
        } catch (final HiveMQEdgeStartupException e) {
            log.error("HiveMQ Edge start was aborted with error.", e);
        }
    }

    public void bootstrap() throws HiveMQEdgeStartupException {
        if (injector == null) {
            injector =
                    new HiveMQEdgeBootstrap(metricRegistry, systemInformation, moduleLoader, configService).bootstrap();
            if (configService == null) {
                configService = injector.configurationService();
            }
        }
    }

    protected void startGateway(final @Nullable EmbeddedExtension embeddedExtension) throws HiveMQEdgeStartupException {
        if (injector == null) {
            throw new HiveMQEdgeStartupException("invalid startup state");
        }

        final ShutdownHooks shutdownHooks = injector.shutdownHooks();
        if (shutdownHooks.isShuttingDown()) {
            throw new HiveMQEdgeStartupException("User aborted.");
        }

        injector.edgeGateway().start(embeddedExtension);
        initializeApiServer(injector);
        startApiServer();
    }

    protected void stopGateway() {
        if (injector == null) {
            return;
        }
        final ShutdownHooks shutdownHooks = injector.shutdownHooks();
        if (shutdownHooks.isShuttingDown()) {
            return;
        }
        shutdownHooks.runShutdownHooks();

        //clear metrics
        metricRegistry.removeMatching(MetricFilter.ALL);

        //Stop the API Webserver
        stopApiServer();
        LoggingBootstrap.resetLogging();
    }

    protected void initializeApiServer(final @NotNull Injector injector) {
        if (jaxrsServer == null && requireNonNull(configService).apiConfiguration().isEnabled()) {
            jaxrsServer = injector.apiServer();
        } else {
            log.info("API is DISABLED by configuration");
        }
    }

    protected void startApiServer() {
        //-- This will only have initialized if the config is enabled
        if (jaxrsServer != null && configService != null && configService.apiConfiguration().isEnabled()) {
            jaxrsServer.startServer();
        }
    }

    protected void stopApiServer() {
        if (jaxrsServer != null) {
            jaxrsServer.stopServer();
        }
    }

    protected void afterStart() {
        afterHiveMQStartBootstrap();
    }

    private void afterHiveMQStartBootstrap() {
        Preconditions.checkNotNull(injector);
        final Persistences persistences = injector.persistences();
        Preconditions.checkNotNull(persistences);
        Preconditions.checkNotNull(configService);
        try {
            injector.commercialModuleLoaderDiscovery()
                    .afterHiveMQStart(AfterHiveMQStartBootstrapServiceImpl.decorate(injector.completeBootstrapService(),
                            injector.protocolAdapterManager(),
                            injector.services().modulesAndExtensionsService()));
        } catch (final Exception e) {
            log.warn("Error on bootstrapping modules:", e);
            throw new HiveMQEdgeStartupException(e);
        }
    }

    public void start(final @Nullable EmbeddedExtension embeddedExtension)
            throws InterruptedException, HiveMQEdgeStartupException {
        shutdownThread = new Thread(this::stop, "shutdown-thread");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        bootstrap();
        startGateway(embeddedExtension);
        afterStart();
    }

    public void stop() {
        stopGateway();
        if (shutdownThread != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownThread);
            } catch (final IllegalStateException ignored) {
                //ignore
            }
        }
    }

    public @Nullable Injector getInjector() {
        return injector;
    }
}
