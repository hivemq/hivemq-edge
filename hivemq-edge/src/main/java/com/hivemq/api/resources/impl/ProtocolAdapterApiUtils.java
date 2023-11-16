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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.api.model.adapters.ProtocolAdapter;
import com.hivemq.api.model.adapters.ProtocolAdapterCategory;
import com.hivemq.api.model.components.Module;
import com.hivemq.api.utils.ApiUtils;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.modules.adapters.ProtocolAdapterConstants;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterCapability;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.HttpConstants;
import com.hivemq.protocols.ProtocolAdapterManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilities that handle the display, sort and filter logic relating to
 * protocol adapters.
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterApiUtils {

    /**
     * Convert between the internal system representation of a ProtocolAdapter type and its API based sibling.
     * This decoupling allows for flexibility when internal model changes would otherwise impact API support
     * @return The instance to be sent across the API
     */
    public static ProtocolAdapter convertInstalledAdapterType(final @NotNull ObjectMapper objectMapper,
                                                              final @NotNull ProtocolAdapterManager adapterManager,
                                                              final @NotNull ProtocolAdapterInformation info,
                                                              final @NotNull ConfigurationService configurationService){

        Preconditions.checkNotNull(adapterManager);
        Preconditions.checkNotNull(info);
        Preconditions.checkNotNull(configurationService);
        String logoUrl = info.getLogoUrl();
        if(logoUrl != null){
            logoUrl = logoUrl.startsWith("/") ? "/module" + logoUrl : "/module/" + logoUrl;
            logoUrl = applyAbsoluteServerAddressInDeveloperMode(logoUrl, configurationService);
        }
        return new ProtocolAdapter(info.getProtocolId(),
                info.getProtocolName(),
                info.getDisplayName(),
                info.getDescription(),
                info.getUrl(),
                info.getVersion(),
                logoUrl,
                null,
                info.getAuthor(),
                true,
                getCapabilities(info),
                info == null ? null : convertApiCategory(info.getCategory()),
                info.getTags() == null ? null : info.getTags().stream().
                        map(Enum::toString).collect(Collectors.toList()),
                adapterManager.getProtocolAdapterFactory(info.getProtocolId()).
                        getConfigSchemaManager(objectMapper).generateSchemaNode());
    }

    /**
     * Convert between the internal system representation of a Module type and its API protocol adapter type.
     * @return The instance to be sent across the API
     */
    public static ProtocolAdapter convertModuleAdapterType(final @NotNull Module module, final @NotNull ConfigurationService configurationService){

        Preconditions.checkNotNull(module);
        Preconditions.checkNotNull(configurationService);
        String logoUrl = module.getLogoUrl() == null ? null : module.getLogoUrl().getUrl();
        String documentationUrl = module.getDocumentationLink() == null ? null : module.getDocumentationLink().getUrl();
        String provisioningUrl = module.getProvisioningLink() == null ? null : module.getProvisioningLink().getUrl();
        if(logoUrl != null){
            logoUrl = logoUrl.startsWith("/") ? "/module" + logoUrl : "/module/" + logoUrl;
            logoUrl = applyAbsoluteServerAddressInDeveloperMode(logoUrl, configurationService);
        }
        return new ProtocolAdapter(
                module.getId(),
                module.getId(),
                module.getName(),
                module.getDescription(),
                documentationUrl,
                module.getVersion(),
                logoUrl,
                provisioningUrl,
                module.getAuthor(),
                false,
                List.of(),
                null,
                null,
                null);
    }

    public static String applyAbsoluteServerAddressInDeveloperMode(@NotNull String logoUrl, final @NotNull ConfigurationService configurationService){
        Preconditions.checkNotNull(logoUrl);
        Preconditions.checkNotNull(configurationService);
        if(logoUrl != null && Boolean.getBoolean(HiveMQEdgeConstants.DEVELOPMENT_MODE)){
            //-- when we're in developer mode, ensure we make the logo urls fully qualified
            //-- as the FE maybe being run from a different development server.
            if(!logoUrl.startsWith(HttpConstants.HTTP)){
                logoUrl = ApiUtils.getWebContextRoot(configurationService.apiConfiguration(),
                        !logoUrl.startsWith(HttpConstants.SLASH)) + logoUrl;
            }
        }
        return logoUrl;
    }

    /**
     * Obtain a list of supported capabilities of this adapter.
     * @param info
     * @return
     */
    public static List<ProtocolAdapter.Capability> getCapabilities(final @NotNull ProtocolAdapterInformation info){
        Preconditions.checkNotNull(info);
        ImmutableList.Builder builder = ImmutableList.<ProtocolAdapter.Capability>builder();
        if(ProtocolAdapterCapability.supportsCapability(info, ProtocolAdapterCapability.READ)){
            builder.add(ProtocolAdapter.Capability.READ);
        }
        if(ProtocolAdapterCapability.supportsCapability(info, ProtocolAdapterCapability.WRITE)){
            builder.add(ProtocolAdapter.Capability.WRITE);
        }
        if(ProtocolAdapterCapability.supportsCapability(info, ProtocolAdapterCapability.DISCOVER)){
            builder.add(ProtocolAdapter.Capability.DISCOVER);
        }
        return builder.build();
    }

    /**
     * Convert category from internal enum to external API transport model.
     * @param category the category enum to convert
     */
    public static ProtocolAdapterCategory convertApiCategory(ProtocolAdapterConstants.CATEGORY category){
        return new ProtocolAdapterCategory(category.name(),
                category.getDisplayName(),
                category.getDescription(),
                category.getImage());
    }
}
