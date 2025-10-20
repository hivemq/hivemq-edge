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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.api.model.adapters.ProtocolAdapter;
import com.hivemq.api.model.adapters.ProtocolAdapterCategory;
import com.hivemq.api.model.components.Module;
import com.hivemq.api.utils.ApiUtils;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.VersionProvider;
import com.hivemq.http.HttpConstants;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.protocols.ProtocolAdapterSchemaManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

/**
 * Utilities that handle the display, sort and filter logic relating to protocol adapters.
 */
public class ProtocolAdapterApiUtils {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdapterApiUtils.class);
    private static final @NotNull String DEFAULT_SCHEMA = """
            {
              "ui:tabs": [],
              "id": {
                "ui:disabled": true
              },
              "ui:order": ["id",  "*"]
            }""";

    /**
     * Convert between the internal system representation of a ProtocolAdapter type and its API based sibling.
     * This decoupling allows for flexibility when internal model changes would otherwise impact API support
     *
     * @return The instance to be sent across the API
     */
    public static @Nullable ProtocolAdapter convertInstalledAdapterType(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterManager adapterManager,
            final @NotNull ProtocolAdapterInformation info,
            final @NotNull ConfigurationService configurationService,
            final @NotNull VersionProvider versionProvider,
            final @Nullable String xOriginalURI) {

        Preconditions.checkNotNull(adapterManager);
        Preconditions.checkNotNull(info);
        Preconditions.checkNotNull(configurationService);

        if (!adapterManager.protocolAdapterFactoryExists(info.getProtocolId())) {
            // this can only happen if the adapter somehow got removed from
            // the manager concurrently, which is not possible right now
            log.error("Factory for adapter '{}' was not found while conversion of adapter to information for REST API.",
                    info.getDisplayName());
            return null;
        }

        return new ProtocolAdapter(info.getProtocolId(),
                info.getProtocolName(),
                info.getDisplayName(),
                info.getDescription(),
                info.getUrl(),
                info.getVersion().replace("${edge-version}", versionProvider.getVersion()),
                getLogoUrl(info, configurationService, xOriginalURI),
                null,
                info.getAuthor(),
                true,
                info.getCapabilities()
                        .stream()
                        .filter(cap -> cap != ProtocolAdapterCapability.WRITE || adapterManager.writingEnabled())
                        .map(ProtocolAdapter.Capability::from)
                        .collect(Collectors.toSet()),
                convertApiCategory(info.getCategory()),
                info.getTags() != null ? info.getTags().stream().map(Enum::toString).toList() : null,
                new ProtocolAdapterSchemaManager(objectMapper,
                        adapterManager.writingEnabled() ?
                                info.configurationClassNorthAndSouthbound() :
                                info.configurationClassNorthbound()).generateSchemaNode(),
                getUiSchemaForAdapter(objectMapper, info));
    }


    @VisibleForTesting
    protected static @NotNull JsonNode getUiSchemaForAdapter(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterInformation info) {
        final String uiSchemaAsString = info.getUiSchema();
        if (uiSchemaAsString != null) {
            try {
                return objectMapper.reader().withFeatures(JsonParser.Feature.ALLOW_COMMENTS).readTree(uiSchemaAsString);
            } catch (final JsonProcessingException e) {
                log.warn("Ui schema for adapter '{}' is not parsable, the default schema will be applied. ",
                        info.getDisplayName(),
                        e);
                // fall through to parsing the DEFAULT SCHEMA
            }
        }

        try {
            return objectMapper.readTree(DEFAULT_SCHEMA);
        } catch (final JsonProcessingException e) {
            log.error("Exception during parsing of default schema: ", e);
            // this should never happen as we control the input (default schema)
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert between the internal system representation of a Module type and its API protocol adapter type.
     *
     * @return The instance to be sent across the API
     */
    public static @NotNull ProtocolAdapter convertModuleAdapterType(
            final @NotNull Module module,
            final @NotNull ConfigurationService configurationService) {
        Preconditions.checkNotNull(module);
        Preconditions.checkNotNull(configurationService);
        return new ProtocolAdapter(module.getId(),
                module.getId(),
                module.getName(),
                requireNonNullElse(module.getDescription(), ""),
                module.getDocumentationLink() != null ? module.getDocumentationLink().getUrl() : null,
                module.getVersion(),
                getLogoUrl(module, configurationService),
                module.getProvisioningLink() != null ? module.getProvisioningLink().getUrl() : null,
                module.getAuthor(),
                false,
                Set.of(),
                null,
                null,
                null,
                null);
    }

    private static @Nullable String getLogoUrl(
            final @NotNull Module module,
            final @NotNull ConfigurationService configurationService) {
        String logoUrl = null;
        if (module.getLogoUrl() != null) {
            logoUrl = module.getLogoUrl().getUrl();
            if (logoUrl != null) {
                logoUrl = logoUrl.startsWith("/") ? "/module" + logoUrl : logoUrl;
                logoUrl = applyAbsoluteServerAddressInDeveloperMode(logoUrl, configurationService);
            }
        }
        return logoUrl;
    }

    private static @NotNull String getLogoUrl(
            final @NotNull ProtocolAdapterInformation info,
            final @NotNull ConfigurationService configurationService,
            final @Nullable String xOriginalURI) {
        String logoUrl = info.getLogoUrl();
        if (StringUtils.isNotBlank(logoUrl)) {
            while (logoUrl.startsWith(HttpConstants.SLASH)) {
                logoUrl = logoUrl.substring(HttpConstants.SLASH.length());
            }
            if (StringUtils.isEmpty(xOriginalURI)) {
                // We are not behind a reverse proxy, so we can use the absolute path.
                logoUrl = "/module/" + logoUrl;
            } else {
                // We are behind a reverse proxy, so we need to use the relative path.
                final int index = xOriginalURI.lastIndexOf("/api/v1/");
                if (index == -1) {
                    logoUrl = "../../module/" + logoUrl;
                } else {
                    logoUrl = xOriginalURI.substring(0, index) + "/module/" + logoUrl;
                }
            }
            logoUrl = applyAbsoluteServerAddressInDeveloperMode(logoUrl, configurationService);
        } else {
            // although it is marked as not null it is input from outside (possible customer adapter),
            // so we should trust but validate and at least log.
            log.warn("Logo url for adapter '{}' was null. ", info.getDisplayName());
        }
        return logoUrl;
    }

    @VisibleForTesting
    public static @NotNull String applyAbsoluteServerAddressInDeveloperMode(
            final @NotNull String logoUrl,
            final @NotNull ConfigurationService configurationService) {
        Preconditions.checkNotNull(logoUrl);
        Preconditions.checkNotNull(configurationService);
        if (Boolean.getBoolean(HiveMQEdgeConstants.DEVELOPMENT_MODE)) {
            //-- when we're in developer mode, ensure we make the logo urls fully qualified
            //-- as the FE maybe being run from a different development server.
            if (!logoUrl.startsWith(HttpConstants.HTTP)) {
                return ApiUtils.getWebContextRoot(configurationService.apiConfiguration(),
                        !logoUrl.startsWith(HttpConstants.SLASH)) + logoUrl;
            }
        }
        return logoUrl;
    }

    /**
     * Convert category from internal enum to external API transport model.
     *
     * @param category the category enum to convert
     */
    @VisibleForTesting
    public static @Nullable ProtocolAdapterCategory convertApiCategory(final @Nullable com.hivemq.adapter.sdk.api.ProtocolAdapterCategory category) {
        return category != null ?
                new ProtocolAdapterCategory(category.name(),
                        category.getDisplayName(),
                        category.getDescription(),
                        category.getImage()) :
                null;
    }
}
