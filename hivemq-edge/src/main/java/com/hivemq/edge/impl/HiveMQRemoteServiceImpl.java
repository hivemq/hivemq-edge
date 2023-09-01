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
package com.hivemq.edge.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.model.HiveMQEdgeEvent;
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
 * @author Simon L Johnson
 */
public class HiveMQRemoteServiceImpl implements HiveMQEdgeRemoteService, HiveMQShutdownHook {
    static final int TIMEOUT = 5000;
    static final int REFRESH = 60000;
    private static final Logger logger = LoggerFactory.getLogger(HiveMQRemoteServiceImpl.class);
    private @NotNull final ObjectMapper objectMapper;
    private @NotNull final HiveMQEdgeHttpServiceImpl hiveMQEdgeHttpService;
    private @NotNull HiveMQEdgeRemoteConfiguration localConfiguration;
    private @NotNull ConfigurationService configurationService;
    private @NotNull SystemInformation systemInformation;

    private final Object lock = new Object();

    @Inject
    public HiveMQRemoteServiceImpl(
            @NotNull final SystemInformation systemInformation,
            @NotNull final ConfigurationService configurationService,
            @NotNull final ObjectMapper objectMapper,
            @NotNull final ShutdownHooks shutdownHooks) {
        final long start = System.currentTimeMillis();
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
        this.systemInformation = systemInformation;
        this.hiveMQEdgeHttpService = initHttpService();
        shutdownHooks.add(this);
        HiveMQEdgeEvent event = new HiveMQEdgeEvent(HiveMQEdgeEvent.EVENT_TYPE.EDGE_STARTED);
        event.addAll(HiveMQEdgeEnvironmentUtils.generateEnvironmentMap());
        fireUsageEvent(event);
        logger.trace("Initialized remote service(s) in {}ms", (System.currentTimeMillis() - start));
    }

    protected final @NotNull HiveMQEdgeHttpServiceImpl initHttpService() {
        return new HiveMQEdgeHttpServiceImpl(systemInformation.getHiveMQVersion(),
                objectMapper, HiveMQEdgeHttpServiceImpl.SERVICE_DISCOVERY_URL, TIMEOUT, TIMEOUT, REFRESH,
                configurationService.usageTrackingConfiguration().isUsageTrackingEnabled());
    }

    @Override
    public HiveMQEdgeRemoteConfiguration getConfiguration() {
        Optional<HiveMQEdgeRemoteConfiguration> optional = hiveMQEdgeHttpService.getRemoteConfiguration();
        logger.debug("Loaded HiveMQ Edge Frontend Configuration Remote Available ? {}", optional.isPresent());
        if (optional.isPresent()) {
            return optional.get();
        } else {
            loadLocalConfiguration();
            if (localConfiguration == null) {
                throw new RuntimeException("Unable to load remote or local configuration, this is an unexpected error");
            }
            return localConfiguration;
        }
    }

    @Override
    public void fireUsageEvent(final HiveMQEdgeEvent event) {
        if(configurationService.usageTrackingConfiguration().isUsageTrackingEnabled()){
            //only queue if its a startup event
            hiveMQEdgeHttpService.fireEvent(event, event.getEventType() == HiveMQEdgeEvent.EVENT_TYPE.EDGE_STARTED);
        }
    }

    protected void loadLocalConfiguration() {
        try {
            if (localConfiguration == null) {
                synchronized (lock) {
                    if (localConfiguration == null) {
                        try (final InputStream is = HiveMQRemoteServiceImpl.class.getResourceAsStream(
                                "/hivemq-edge-configuration.json")) {
                            localConfiguration = objectMapper.readValue(is, HiveMQEdgeRemoteConfiguration.class);
                            logger.trace("Loaded HiveMQEdge Configuration From Local Classpath {}", localConfiguration);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error Loading HiveMQEdge Configuration From Local Classpath", e);
        }
    }

    @Override
    public String name() {
        return "HiveMQ Edge Remote Configuration Shutdown";
    }

    @Override
    public void run() {
        try {
            hiveMQEdgeHttpService.stop();
        } catch (Exception e) {
            logger.error("Error shutting down remote configuration service", e);
        }
    }
}
