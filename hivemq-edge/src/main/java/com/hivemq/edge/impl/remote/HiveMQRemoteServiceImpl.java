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
import com.hivemq.edge.model.HiveMQEdgeRemoteEvent;
import com.hivemq.edge.model.HiveMQEdgeRemoteConfiguration;
import com.hivemq.edge.utils.HiveMQEdgeEnvironmentUtils;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * A service that (optionally) connects to remote endpoints to provision configuration
 * that helps/augments the runtime. Also provides endpoints for tracking usage to allow
 * HiveMQ to provide a better open-source product.
 *
 * @author Simon L Johnson
 */
public class HiveMQRemoteServiceImpl implements HiveMQEdgeRemoteService, HiveMQShutdownHook {

    static final String LOCAL_RESOURCE_CONFIGURATION = "/hivemq-edge-configuration.json";
    static final int TIMEOUT = 5000;
    static final int REFRESH = 60000;
    private static final Logger logger = LoggerFactory.getLogger(HiveMQRemoteServiceImpl.class);
    private @NotNull final ObjectMapper objectMapper;
    private @NotNull HiveMQEdgeHttpServiceImpl hiveMQEdgeHttpService;
    private @NotNull HiveMQEdgeRemoteConfiguration localConfiguration;
    private @NotNull SystemInformation systemInformation;

    private final Object lock = new Object();

    @Inject
    public HiveMQRemoteServiceImpl(
            @NotNull final SystemInformation systemInformation,
            @NotNull final ConfigurationService configurationService,
            @NotNull final ObjectMapper objectMapper,
            @NotNull final ShutdownHooks shutdownHooks) {
        this.objectMapper = objectMapper;
        this.systemInformation = systemInformation;

        //-- We should only init the remote service (and thus the overhead of the threads) when
        //-- remote tracking is enabled.
        if(configurationService.usageTrackingConfiguration().isUsageTrackingEnabled()){
            initHttpService();
            shutdownHooks.add(this);
        }
    }

    protected final void initHttpService() {
        try {
            hiveMQEdgeHttpService = new HiveMQEdgeHttpServiceImpl(systemInformation.getHiveMQVersion(),
                    objectMapper, HiveMQEdgeHttpServiceImpl.SERVICE_DISCOVERY_URL, TIMEOUT, TIMEOUT, REFRESH,
                    true);
            HiveMQEdgeRemoteEvent event = new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_STARTED);
            event.addAll(HiveMQEdgeEnvironmentUtils.generateEnvironmentMap());
            fireUsageEvent(event);
        } finally {
            if(logger.isTraceEnabled()){
                logger.trace("Initialized remote HTTP service(s), usage tracking enabled (this can be disabled in configuration)");
            }
        }
    }

    @Override
    public HiveMQEdgeRemoteConfiguration getConfiguration() {
        //-- If enabled (and available), load the configuration from a remote endpoint, else
        //-- load the config from the local classpath
        Optional<HiveMQEdgeRemoteConfiguration> optional = readConfigurationFromRemote();
        return optional.orElse(loadLocalConfiguration());
    }

    @Override
    public void fireUsageEvent(final HiveMQEdgeRemoteEvent event) {
        if(hiveMQEdgeHttpService != null){
            //only queue if its a startup event
            hiveMQEdgeHttpService.fireEvent(event, event.getEventType() == HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_STARTED);
        }
    }

    protected HiveMQEdgeRemoteConfiguration loadLocalConfiguration() {
        try {
            if (localConfiguration == null) {
                synchronized (lock) {
                    if (localConfiguration == null) {
                        try (final InputStream is = HiveMQRemoteServiceImpl.class.getResourceAsStream(LOCAL_RESOURCE_CONFIGURATION)) {
                            localConfiguration = objectMapper.readValue(is, HiveMQEdgeRemoteConfiguration.class);
                            if(logger.isTraceEnabled()){
                                logger.trace("Loaded HiveMQEdge Configuration From Local Classpath {}", localConfiguration);
                            }
                        }
                    }
                }
            }
            return localConfiguration;
        } catch (IOException e) {
            logger.error("Error Loading HiveMQEdge Configuration From Local Classpath", e);
            throw new RuntimeException("Error Loading HiveMQEdge Configuration from Classpath");
        }
    }

    protected @NotNull Optional<HiveMQEdgeRemoteConfiguration> readConfigurationFromRemote(){
        Optional<HiveMQEdgeRemoteConfiguration> remoteConfiguration = hiveMQEdgeHttpService != null ?
                hiveMQEdgeHttpService.getRemoteConfiguration() : Optional.empty();
        if(logger.isTraceEnabled()){
            logger.trace("Loaded HiveMQ Edge Configuration Remote Available ? {}",
                    hiveMQEdgeHttpService != null && remoteConfiguration.isPresent());
        }
        return remoteConfiguration;
    }

    @Override
    public String name() {
        return "HiveMQ Edge Remote Configuration Shutdown";
    }

    @Override
    public void run() {
        try {
            if(hiveMQEdgeHttpService != null) {
                hiveMQEdgeHttpService.stop();
            }
        } catch (Exception e) {
            logger.error("Error shutting down remote configuration service", e);
        }
    }
}
