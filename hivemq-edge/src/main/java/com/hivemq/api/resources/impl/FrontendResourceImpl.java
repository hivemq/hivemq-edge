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

import com.google.common.collect.ImmutableList;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.model.components.EnvironmentProperties;
import com.hivemq.api.model.components.ExtensionList;
import com.hivemq.api.model.components.GatewayConfiguration;
import com.hivemq.api.model.components.Link;
import com.hivemq.api.model.components.LinkList;
import com.hivemq.api.model.components.Listener;
import com.hivemq.api.model.components.ListenerList;
import com.hivemq.api.model.components.ModuleList;
import com.hivemq.api.model.components.Notification;
import com.hivemq.api.model.components.NotificationList;
import com.hivemq.api.model.firstuse.FirstUseInformation;
import com.hivemq.api.resources.FrontendApi;
import com.hivemq.api.utils.ApiUtils;
import com.hivemq.api.utils.LoremIpsum;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeRemoteConfigurationService;
import com.hivemq.edge.ModulesAndExtensionsService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.UsernamePasswordRoles;
import com.hivemq.protocols.ProtocolAdapterManager;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public class FrontendResourceImpl extends AbstractApi implements FrontendApi {

    private final @NotNull ConfigurationService configurationService;
    private final @NotNull ProtocolAdapterManager protocolAdapterManager;
    private final @NotNull ModulesAndExtensionsService modulesAndExtensionsService;
    private final @NotNull HiveMQEdgeRemoteConfigurationService hiveMQEdgeRemoteConfigurationService;

    @Inject
    public FrontendResourceImpl(
            final @NotNull ConfigurationService configurationService,
            final @NotNull ProtocolAdapterManager protocolAdapterManager,
            final @NotNull ModulesAndExtensionsService modulesAndExtensionsService,
            final @NotNull HiveMQEdgeRemoteConfigurationService hiveMQEdgeRemoteConfigurationService) {
        this.configurationService = configurationService;
        this.protocolAdapterManager = protocolAdapterManager;
        this.modulesAndExtensionsService = modulesAndExtensionsService;
        this.hiveMQEdgeRemoteConfigurationService = hiveMQEdgeRemoteConfigurationService;
    }

    @Override
    public @NotNull Response getConfiguration() {

        GatewayConfiguration configuration = new GatewayConfiguration(getEnvironmentProperties(),
                getCloudLink(),
                getGitHubLink(),
                getDocumentationLink(),
                getFirstUse(),
                getDashboardCTAs(),
                getResources(),
                getModules(),
                getExtensions());
        return Response.ok(configuration).build();
    }


    public @NotNull LinkList getDashboardCTAs() {
        ImmutableList.Builder<Link> links = new ImmutableList.Builder().add(new Link("Connect My First Device",
                        "./protocol-adapters?from=dashboard-cta",
                        LoremIpsum.generate(40),
                        null,
                        null,
                        Boolean.FALSE),
                new Link("Connect To My MQTT Broker",
                        "./bridges?from=dashboard-cta",
                        LoremIpsum.generate(40),
                        null,
                        null,
                        Boolean.FALSE),
                new Link("Learn More",
                        "resources?from=dashboard-cta",
                        LoremIpsum.generate(40),
                        null,
                        null,
                        Boolean.FALSE));
        return new LinkList(links.build());
    }

    protected @NotNull Link getCloudLink() {
        return new Link("HiveMQ Cloud", "https://hivemq.com/cloud", LoremIpsum.generate(40), null, null, Boolean.TRUE);
    }

    protected @NotNull Link getGitHubLink() {
        return new Link("GitHub",
                "https://github.com/hivemq/hivemq-edge",
                LoremIpsum.generate(40),
                null,
                null,
                Boolean.TRUE);
    }

    protected @NotNull Link getDocumentationLink() {
        return new Link("Documentation",
                "https://github.com/hivemq/hivemq-edge/wiki",
                LoremIpsum.generate(40),
                null,
                null,
                Boolean.TRUE);
    }

    protected @NotNull LinkList getResources() {
        return new LinkList(hiveMQEdgeRemoteConfigurationService.getConfiguration().getResources());
    }

    protected @NotNull ExtensionList getExtensions() {
        return new ExtensionList(modulesAndExtensionsService.getExtensions());
    }

    protected @NotNull ModuleList getModules() {
        return new ModuleList(modulesAndExtensionsService.getModules());
    }

    protected @NotNull FirstUseInformation getFirstUse() {
        //-- First use is determined by zero configuration
        boolean firstUse = configurationService.bridgeConfiguration().getBridges().isEmpty() &&
                protocolAdapterManager.getProtocolAdapters().isEmpty();
        //-- Populate login prefill
        String prefillUsername = null;
        String prefillPassword = null;
        String firstUseTitle = null;
        String firstUseDescription = null;
        if (ApiUtils.hasDefaultUser(configurationService.apiConfiguration().getUserList())) {
            prefillUsername = UsernamePasswordRoles.DEFAULT_USERNAME;
            prefillPassword = UsernamePasswordRoles.DEFAULT_PASSWORD;

            firstUseTitle = "Welcome To HiveMQ Edge";
            firstUseDescription = "We have determined this is a new installation and have therefore pre-populated the admin credentials with the system defaults. IMPORTANT: Please update the default credentials in your config.xml document.";
        }



        return new FirstUseInformation(firstUse, prefillUsername, prefillPassword, firstUseTitle, firstUseDescription);
    }

    protected @NotNull  EnvironmentProperties getEnvironmentProperties() {
        Map<String, String> env = new HashMap<>();
        env.put("environment-type", "TEST");
        return new EnvironmentProperties(env);
    }

    @Override
    public @NotNull  Response getNotifications() {

        ImmutableList.Builder<Notification> notifs = new ImmutableList.Builder();
        Optional<Long> lastUpdate = configurationService.getLastUpdateTime();
        if (!configurationService.gatewayConfiguration().isMutableConfigurationEnabled() &&
                configurationService.gatewayConfiguration().isConfigurationExportEnabled() &&
                lastUpdate.isPresent() &&
                lastUpdate.get() > System.currentTimeMillis() - (60000 * 5)) {
            Link xmlDownload =
                    new Link("Download XML Configuration", "/configuration-download", null, null, null, Boolean.FALSE);
            notifs.add(new Notification(Notification.LEVEL.NOTICE,
                    "Configuration Has Changed",
                    "The gateway configuration has recently been modify. In order to persist these changes across runtimes, please export your configuration for use in your containers.",
                    xmlDownload));
        }
        if (ApiUtils.hasDefaultUser(configurationService.apiConfiguration().getUserList())) {
            notifs.add(new Notification(Notification.LEVEL.WARNING,
                    "Default Credentials Need Changing!",
                    "Your gateway access is configured to use the default username/password combination. This is a security risk. Please ensure you modify your access credentials in your configuration.xml file.",
                    null));
        }
        return Response.ok(new NotificationList(notifs.build())).build();
    }

    @Override
    public @NotNull Response getXmlConfiguration() {
        return Response.ok((StreamingOutput) output -> configurationService.writeConfiguration(new OutputStreamWriter(
                output))).build();
    }

    @Override
    public @NotNull Response getListeners() {

        //-- Netty configured objects
        ImmutableList.Builder<Listener> builder = new ImmutableList.Builder<>();
        configurationService.listenerConfiguration()
                .getListeners()
                .stream()
                .map(this::convertListener)
                .forEachOrdered(builder::add);

        return Response.ok(new ListenerList(builder.build())).build();
    }

    private Listener convertListener(com.hivemq.configuration.service.entity.Listener listener) {
        return new Listener(listener.getName(),
                listener.getBindAddress(),
                listener.getPort(),
                listener.getReadableName(),
                listener.getExternalHostname());
    }
}
