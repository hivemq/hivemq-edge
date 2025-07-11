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
package com.hivemq.bootstrap.ioc;

import com.hivemq.bridge.BridgeService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.edge.ModulesAndExtensionsService;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extensions.core.HandlerService;
import com.hivemq.persistence.SingleWriterService;

import jakarta.inject.Inject;

public class Services {

    private final @NotNull HiveMQCapabilityService capabilityService;
    private final @NotNull BridgeService bridgeService;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull HandlerService handlerService;
    private final @NotNull ModulesAndExtensionsService modulesAndExtensionsService;


    @Inject
    public Services(
            final @NotNull HiveMQCapabilityService capabilityService,
            final @NotNull BridgeService bridgeService,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull HandlerService handlerService,
            final @NotNull ModulesAndExtensionsService modulesAndExtensionsService) {
        this.capabilityService = capabilityService;
        this.bridgeService = bridgeService;
        this.singleWriterService = singleWriterService;
        this.handlerService = handlerService;
        this.modulesAndExtensionsService = modulesAndExtensionsService;
    }

    public @NotNull HiveMQCapabilityService capabilityService() {
        return capabilityService;
    }

    public @NotNull BridgeService bridgeService() {
        return bridgeService;
    }

    public @NotNull SingleWriterService singleWriterService() {
        return singleWriterService;
    }

    public @NotNull HandlerService handlerService() {
        return handlerService;
    }

    public @NotNull ModulesAndExtensionsService modulesAndExtensionsService() {
        return modulesAndExtensionsService;
    }
}
