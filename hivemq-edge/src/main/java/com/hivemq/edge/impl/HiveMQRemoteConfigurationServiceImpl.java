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
import com.hivemq.edge.HiveMQEdgeRemoteConfigurationService;
import com.hivemq.edge.model.HiveMQEdgeRemoteConfiguration;
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
public class HiveMQRemoteConfigurationServiceImpl implements HiveMQEdgeRemoteConfigurationService, HiveMQShutdownHook {

    static final String URL = "https://www.hivemq.com/downloads/hivemq-edge-configuration.json";
    static final int TIMEOUT = 5000;
    static final int REFRESH = 60000;
    private static final Logger logger = LoggerFactory.getLogger(HiveMQRemoteConfigurationServiceImpl.class);
    private @NotNull
    final ObjectMapper objectMapper;
    private @NotNull
    final HiveMQEdgeHttpServiceImpl hiveMQEdgeHttpService;
    private @NotNull HiveMQEdgeRemoteConfiguration localConfiguration;
    private final Object lock = new Object();

    @Inject
    public HiveMQRemoteConfigurationServiceImpl(
            @NotNull final ObjectMapper objectMapper, @NotNull final ShutdownHooks shutdownHooks) {
        logger.trace("Initializing HTTP remote service");
        final long start = System.currentTimeMillis();
        this.objectMapper = objectMapper;
        this.hiveMQEdgeHttpService = initHttpService();
        shutdownHooks.add(this);
        logger.trace("Initializing HTTP remote service in {}ms", (System.currentTimeMillis() - start));
    }

    protected final @NotNull HiveMQEdgeHttpServiceImpl initHttpService() {
        return new HiveMQEdgeHttpServiceImpl(objectMapper, URL, TIMEOUT, TIMEOUT, REFRESH);
    }

    @Override
    public HiveMQEdgeRemoteConfiguration getConfiguration() {
        Optional<HiveMQEdgeRemoteConfiguration> optional = hiveMQEdgeHttpService.getRemoteConfiguration();
        if (optional.isPresent()) {
            logger.info("Loaded HiveMQ Edge Configuration From Remote");
            return optional.get();
        } else {
            loadLocalConfiguration();
            if (localConfiguration == null) {
                throw new RuntimeException("Unable to load remote or local configuration, this is an unexpected error");
            }
            return localConfiguration;
        }
    }

    protected void loadLocalConfiguration() {
        try {
            if (localConfiguration == null) {
                synchronized (lock) {
                    if (localConfiguration == null) {
                        try (final InputStream is = HiveMQRemoteConfigurationServiceImpl.class.getResourceAsStream(
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
