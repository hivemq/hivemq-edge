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
package com.hivemq.edge.impl.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.model.HiveMQEdgeRemoteConfiguration;
import com.hivemq.edge.model.HiveMQEdgeRemoteEvent;
import com.hivemq.edge.utils.HiveMQEdgeEnvironmentUtils;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.requireNonNull;

/**
 * A service that (optionally) connects to remote endpoints to provision configuration
 * that helps/augments the runtime. Also provides endpoints for tracking usage to allow
 * HiveMQ to provide a better open-source product.
 */
public class HiveMQRemoteServiceImpl implements HiveMQEdgeRemoteService, HiveMQShutdownHook {

    private static final @NotNull Logger log = LoggerFactory.getLogger(HiveMQRemoteServiceImpl.class);
    private static final @NotNull String LOCAL_RESOURCE_CONFIGURATION = "/hivemq-edge-configuration.json";
    private static final int TIMEOUT = 5000;
    private static final int REFRESH = 60000;

    private final @NotNull ObjectMapper mapper;
    private final @NotNull Lock lock;
    private final @NotNull SystemInformation sysInfo;
    private @Nullable HiveMQEdgeHttpServiceImpl httpService;
    private @Nullable HiveMQEdgeRemoteConfiguration localConfig;

    @Inject
    public HiveMQRemoteServiceImpl(
            final @NotNull SystemInformation sysInfo,
            final @NotNull ConfigurationService configService,
            final @NotNull ObjectMapper mapper,
            final @NotNull ShutdownHooks shutdownHooks) {
        this.lock = new ReentrantLock();
        this.mapper = mapper;
        this.sysInfo = sysInfo;

        //-- We should only init the remote service (and thus the overhead of the threads) when
        //-- remote tracking is enabled.
        if (configService.usageTrackingConfiguration().isUsageTrackingEnabled()) {
            initHttpService();
            shutdownHooks.add(this);
        }
    }

    @Override
    public @NotNull HiveMQEdgeRemoteConfiguration getConfiguration() {
        //-- If enabled (and available), load the configuration from a remote endpoint, else
        //-- load the config from the local classpath
        return readConfigurationFromRemote().orElse(loadLocalConfiguration());
    }

    @Override
    public void fireUsageEvent(final @NotNull HiveMQEdgeRemoteEvent event) {
        if (httpService != null) {
            //only queue if its a startup event
            httpService.fireEvent(event, event.getEventType() == HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_STARTED);
        }
    }

    private void initHttpService() {
        try {
            httpService = new HiveMQEdgeHttpServiceImpl(sysInfo.getHiveMQVersion(),
                    mapper,
                    HiveMQEdgeHttpServiceImpl.SERVICE_DISCOVERY_URL,
                    TIMEOUT,
                    TIMEOUT,
                    REFRESH,
                    true);
            final HiveMQEdgeRemoteEvent event =
                    new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_STARTED);
            event.addAll(HiveMQEdgeEnvironmentUtils.generateEnvironmentMap());
            fireUsageEvent(event);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace(
                        "Initialized remote HTTP service(s), usage tracking enabled (this can be disabled in configuration)");
            }
        }
    }

    private @NotNull HiveMQEdgeRemoteConfiguration loadLocalConfiguration() {
        lock.lock();
        try {
            if (localConfig == null) {
                try (final InputStream is = HiveMQRemoteServiceImpl.class.getResourceAsStream(
                        LOCAL_RESOURCE_CONFIGURATION)) {
                    localConfig = mapper.readValue(is, HiveMQEdgeRemoteConfiguration.class);
                    if (log.isTraceEnabled()) {
                        log.trace("Loaded HiveMQEdge Configuration From Local Classpath {}", localConfig);
                    }
                }
            }
            return requireNonNull(localConfig);
        } catch (final IOException e) {
            log.error("Error Loading HiveMQEdge Configuration From Local Classpath", e);
            throw new RuntimeException("Error Loading HiveMQEdge Configuration from Classpath");
        } finally {
            lock.unlock();
        }
    }

    private @NotNull Optional<HiveMQEdgeRemoteConfiguration> readConfigurationFromRemote() {
        final Optional<HiveMQEdgeRemoteConfiguration> remote =
                httpService != null ? httpService.getRemoteConfiguration() : Optional.empty();
        if (log.isTraceEnabled()) {
            log.trace("Loaded HiveMQ Edge Configuration Remote Available ? {}",
                    httpService != null && remote.isPresent());
        }
        return remote;
    }

    @Override
    public @NotNull String name() {
        return "HiveMQ Edge Remote Configuration Shutdown";
    }

    @Override
    public void run() {
        try {
            if (httpService != null) {
                httpService.stop();
            }
        } catch (final Throwable e) {
            log.error("Error shutting down remote configuration service", e);
        }
    }
}
