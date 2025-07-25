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

import com.hivemq.bootstrap.HiveMQEdgeNettyBootstrap;
import com.hivemq.bootstrap.ListenerStartupInformation;
import com.hivemq.bootstrap.StartupListenerVerifier;
import com.hivemq.combining.runtime.DataCombinerManager;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.exceptions.HiveMQEdgeStartupException;
import com.hivemq.extension.sdk.api.services.admin.AdminService;
import com.hivemq.extensions.ExtensionBootstrap;
import com.hivemq.extensions.services.admin.AdminServiceImpl;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.util.Checkpoints;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.inject.Inject;
import java.util.List;

public class HiveMQEdgeGateway {

    private final @NotNull HiveMQEdgeNettyBootstrap nettyBootstrap;
    private final @NotNull ExtensionBootstrap extensionBootstrap;
    private final @NotNull AdminService adminService;
    private final @NotNull ProtocolAdapterManager protocolAdapterManager;
    private final @NotNull DataCombinerManager dataCombinerManager;

    @Inject
    public HiveMQEdgeGateway(
            final @NotNull HiveMQEdgeNettyBootstrap nettyBootstrap,
            final @NotNull ExtensionBootstrap extensionBootstrap,
            final @NotNull AdminService adminService,
            final @NotNull ProtocolAdapterManager protocolAdapterManager,
            final @NotNull DataCombinerManager dataCombinerManager) {
        this.nettyBootstrap = nettyBootstrap;
        this.extensionBootstrap = extensionBootstrap;
        this.adminService = adminService;
        this.protocolAdapterManager = protocolAdapterManager;
        this.dataCombinerManager = dataCombinerManager;
    }

    public void start(final @Nullable EmbeddedExtension embeddedExtension) throws HiveMQEdgeStartupException {
        try {
            extensionBootstrap.startExtensionSystem(embeddedExtension).get();
            protocolAdapterManager.start();
            dataCombinerManager.start();

            final List<ListenerStartupInformation> startupInformation = nettyBootstrap.bootstrapServer().get();
            Checkpoints.checkpoint("listener-started");
            new StartupListenerVerifier(startupInformation).verifyAndPrint();



            ((AdminServiceImpl) adminService).hivemqStarted();
        } catch (Exception e) {
            throw new HiveMQEdgeStartupException(e);
        }

    }
}
