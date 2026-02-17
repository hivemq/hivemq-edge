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
package com.hivemq.api.resources.impl;

import static com.hivemq.edge.HiveMQEdgeConstants.CONFIGURATION_EXPORT_ENABLED;
import static com.hivemq.edge.HiveMQEdgeConstants.MUTABLE_CONFIGURAION_ENABLED;
import static com.hivemq.edge.HiveMQEdgeConstants.VERSION_PROPERTY;

import com.google.common.collect.ImmutableList;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.model.components.EnvironmentProperties;
import com.hivemq.api.model.components.ExtensionList;
import com.hivemq.api.model.components.GatewayConfiguration;
import com.hivemq.api.model.components.Link;
import com.hivemq.api.model.components.LinkList;
import com.hivemq.api.model.components.ModuleList;
import com.hivemq.api.model.components.Notification;
import com.hivemq.api.model.components.NotificationList;
import com.hivemq.api.model.components.PreLoginNotice;
import com.hivemq.api.model.firstuse.FirstUseInformation;
import com.hivemq.api.utils.ApiUtils;
import com.hivemq.api.utils.LoremIpsum;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.ModulesAndExtensionsService;
import com.hivemq.edge.api.FrontendApi;
import com.hivemq.edge.api.model.Capability;
import com.hivemq.edge.api.model.CapabilityList;
import com.hivemq.http.core.UsernamePasswordRoles;
import com.hivemq.protocols.ProtocolAdapterManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class FrontendResourceImpl extends AbstractApi implements FrontendApi {

    private final @NotNull ConfigurationService configurationService;
    private final @NotNull ProtocolAdapterManager protocolAdapterManager;
    private final @NotNull ModulesAndExtensionsService modulesAndExtensionsService;
    private final @NotNull HiveMQEdgeRemoteService hiveMQEdgeRemoteConfigurationService;
    private final @NotNull HiveMQCapabilityService capabilityService;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull HivemqId hivemqId;

    @Inject
    public FrontendResourceImpl(
            final @NotNull ConfigurationService configurationService,
            final @NotNull ProtocolAdapterManager protocolAdapterManager,
            final @NotNull ModulesAndExtensionsService modulesAndExtensionsService,
            final @NotNull HiveMQEdgeRemoteService hiveMQEdgeRemoteConfigurationService,
            final @NotNull HiveMQCapabilityService capabilityService,
            final @NotNull SystemInformation systemInformation,
            final @NotNull HivemqId hivemqId) {
        this.configurationService = configurationService;
        this.protocolAdapterManager = protocolAdapterManager;
        this.modulesAndExtensionsService = modulesAndExtensionsService;
        this.hiveMQEdgeRemoteConfigurationService = hiveMQEdgeRemoteConfigurationService;
        this.capabilityService = capabilityService;
        this.systemInformation = systemInformation;
        this.hivemqId = hivemqId;
    }

    private static @NotNull Capability fromModel(final @NotNull com.hivemq.api.model.capabilities.Capability cap) {
        return Capability.builder()
                .id(Capability.IdEnum.fromString(cap.getId()))
                .description(cap.getDescription())
                .displayName(cap.getDisplayName())
                .build();
    }

    @Override
    public @NotNull Response getConfiguration() {
        return Response.ok(new GatewayConfiguration(
                        getEnvironmentProperties(),
                        getCloudLink(),
                        getGitHubLink(),
                        getDocumentationLink(),
                        getFirstUse(),
                        getDashboardCTAs(),
                        getResources(),
                        getModules(),
                        getExtensions(),
                        hivemqId.get(),
                        configurationService.usageTrackingConfiguration().isUsageTrackingEnabled(),
                        configurationService.apiConfiguration().getPreLoginNotice()))
                .build();
    }

    @Override
    public @NotNull Response getNotifications() {
        final ImmutableList.Builder<@NotNull Notification> notifs = new ImmutableList.Builder<>();
        final Optional<Long> lastUpdate = configurationService.getLastUpdateTime();
        if (!configurationService.gatewayConfiguration().isMutableConfigurationEnabled()
                && configurationService.gatewayConfiguration().isConfigurationExportEnabled()
                && lastUpdate.isPresent()
                && lastUpdate.get() > System.currentTimeMillis() - (60000 * 5)) {
            notifs.add(new Notification(
                    Notification.LEVEL.NOTICE,
                    "Configuration Has Changed",
                    "The gateway configuration has recently been modify. In order to persist these changes across runtimes, please export your configuration for use in your containers.",
                    new Link(
                            "Download XML Configuration", "/configuration-download", null, null, null, Boolean.FALSE)));
        }
        if (ApiUtils.hasDefaultUser(configurationService.apiConfiguration().getUserList())) {
            notifs.add(new Notification(
                    Notification.LEVEL.WARNING,
                    "Default Credentials Need Changing!",
                    "Your gateway access is configured to use the default username/password combination. This is a security risk. Please ensure you modify your access credentials in your config.xml file.",
                    null));
        }
        return Response.ok(new NotificationList(notifs.build())).build();
    }

    @Override
    public @NotNull Response getCapabilities() {
        return Response.ok(new CapabilityList(capabilityService.getList().getItems().stream()
                        .map(FrontendResourceImpl::fromModel)
                        .toList()))
                .build();
    }

    private @NotNull LinkList getDashboardCTAs() {
        return new LinkList(List.of(
                new Link(
                        "Connect My First Device",
                        "./protocol-adapters?from=dashboard-cta",
                        LoremIpsum.generate(40),
                        null,
                        null,
                        Boolean.FALSE),
                new Link(
                        "Connect To My MQTT Broker",
                        "./bridges?from=dashboard-cta",
                        LoremIpsum.generate(40),
                        null,
                        null,
                        Boolean.FALSE),
                new Link(
                        "Learn More",
                        "resources?from=dashboard-cta",
                        LoremIpsum.generate(40),
                        null,
                        null,
                        Boolean.FALSE)));
    }

    private @NotNull PreLoginNotice getPreLoginNotice() {
        return configurationService.apiConfiguration().getPreLoginNotice();
    }

    private @NotNull Link getCloudLink() {
        return hiveMQEdgeRemoteConfigurationService.getConfiguration().getCloudLink();
    }

    private @NotNull Link getGitHubLink() {
        return hiveMQEdgeRemoteConfigurationService.getConfiguration().getGitHubLink();
    }

    private @NotNull Link getDocumentationLink() {
        return hiveMQEdgeRemoteConfigurationService.getConfiguration().getDocumentationLink();
    }

    private @NotNull LinkList getResources() {
        return new LinkList(
                hiveMQEdgeRemoteConfigurationService.getConfiguration().getResources());
    }

    private @NotNull ExtensionList getExtensions() {
        return new ExtensionList(modulesAndExtensionsService.getExtensions());
    }

    private @NotNull ModuleList getModules() {
        return new ModuleList(modulesAndExtensionsService.getModules());
    }

    private @NotNull FirstUseInformation getFirstUse() {
        // -- First use is determined by zero configuration
        final boolean firstUse =
                configurationService.bridgeExtractor().getBridges().isEmpty()
                        && protocolAdapterManager.getProtocolAdapters().isEmpty();
        // -- Populate login prefill
        String prefillUsername = null;
        String prefillPassword = null;
        String firstUseTitle = null;
        String firstUseDescription = null;
        if (ApiUtils.hasDefaultUser(configurationService.apiConfiguration().getUserList())) {
            prefillUsername = UsernamePasswordRoles.DEFAULT_USERNAME;
            prefillPassword = UsernamePasswordRoles.DEFAULT_PASSWORD;
            firstUseTitle = "Welcome To HiveMQ Edge";
            firstUseDescription =
                    "We have determined this is a new installation and have therefore pre-populated the admin credentials with the system defaults. IMPORTANT: Please update the default credentials in your config.xml document.";
        }
        return new FirstUseInformation(firstUse, prefillUsername, prefillPassword, firstUseTitle, firstUseDescription);
    }

    private @NotNull EnvironmentProperties getEnvironmentProperties() {
        return new EnvironmentProperties(Map.of(
                VERSION_PROPERTY,
                systemInformation.getHiveMQVersion(),
                MUTABLE_CONFIGURAION_ENABLED,
                String.valueOf(configurationService.gatewayConfiguration().isMutableConfigurationEnabled()),
                CONFIGURATION_EXPORT_ENABLED,
                String.valueOf(configurationService.gatewayConfiguration().isConfigurationExportEnabled())));
    }
}
