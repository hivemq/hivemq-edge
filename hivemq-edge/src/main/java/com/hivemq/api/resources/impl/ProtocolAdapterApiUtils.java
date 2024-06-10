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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.api.model.adapters.ProtocolAdapter;
import com.hivemq.api.model.adapters.ProtocolAdapterCategory;
import com.hivemq.api.model.components.Module;
import com.hivemq.api.utils.ApiUtils;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.VersionProvider;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.http.HttpConstants;
import com.hivemq.protocols.ProtocolAdapterManager;
import com.hivemq.protocols.ProtocolAdapterSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilities that handle the display, sort and filter logic relating to
 * protocol adapters.
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterApiUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolAdapterApiUtils.class);
    private static final String DEFAULT_SCHEMA = "{\n" +
            "  \"ui:tabs\": [\n" +
            "    {\n" +
            "      \"id\": \"coreFields\",\n" +
            "      \"title\": \"Core Fields\",\n" +
            "      \"properties\": [\"id\", \"port\", \"host\", \"uri\", \"url\", \"timeout\"]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"subFields\",\n" +
            "      \"title\": \"Subscription\",\n" +
            "      \"properties\": [\"subscriptions\"]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"security\",\n" +
            "      \"title\": \"protocolAdapter.uiSchema.groups.security\",\n" +
            "      \"properties\": [\"security\", \"tls\"]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"publishing\",\n" +
            "      \"title\": \"protocolAdapter.uiSchema.groups.publishing\",\n" +
            "      \"properties\": [\n" +
            "        \"maxPollingErrorsBeforeRemoval\",\n" +
            "        \"publishChangedDataOnly\",\n" +
            "        \"publishingInterval\",\n" +
            "        \"pollingIntervalMillis\",\n" +
            "        \"destination\",\n" +
            "        \"qos\",\n" +
            "        \"minValue\",\n" +
            "        \"maxValue\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"authentication\",\n" +
            "      \"title\": \"protocolAdapter.uiSchema.groups.authentication\",\n" +
            "      \"properties\": [\"auth\"]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"http\",\n" +
            "      \"title\": \"protocolAdapter.uiSchema.groups.http\",\n" +
            "      \"properties\": [\n" +
            "        \"httpRequestMethod\",\n" +
            "        \"httpRequestBodyContentType\",\n" +
            "        \"httpRequestBody\",\n" +
            "        \"httpHeaders\",\n" +
            "        \"httpConnectTimeout\",\n" +
            "        \"httpRequestBodyContentType\",\n" +
            "        \"assertResponseIsJson\",\n" +
            "        \"httpPublishSuccessStatusCodeOnly\",\n" +
            "        \"allowUntrustedCertificates\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"ads\",\n" +
            "      \"title\": \"protocolAdapter.uiSchema.groups.ads\",\n" +
            "      \"properties\": [\"sourceAmsPort\", \"targetAmsPort\", \"sourceAmsNetId\", \"targetAmsNetId\"]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"eip\",\n" +
            "      \"title\": \"protocolAdapter.uiSchema.groups.eip\",\n" +
            "      \"properties\": [\"slot\", \"backplane\"]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"s7advanced\",\n" +
            "      \"title\": \"protocolAdapter.uiSchema.groups.s7advanced\",\n" +
            "      \"properties\": [\n" +
            "        \"controllerType\",\n" +
            "        \"remoteRack\",\n" +
            "        \"remoteSlot\",\n" +
            "        \"ping\",\n" +
            "        \"pingTime\",\n" +
            "        \"maxAmqCaller\",\n" +
            "        \"maxAmqCallee\",\n" +
            "        \"remoteTsap\",\n" +
            "        \"remoteRack2\",\n" +
            "        \"remoteSlot2\",\n" +
            "        \"pduSize\",\n" +
            "        \"retryTime\",\n" +
            "        \"retryTimeout\",\n" +
            "        \"readTimeout\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"ui:submitButtonOptions\": {\n" +
            "    \"norender\": true\n" +
            "  },\n" +
            "  \"id\": {\n" +
            "    \"ui:disabled\": true\n" +
            "  },\n" +
            "  \"port\": {\n" +
            "    \"ui:widget\": \"updown\"\n" +
            "  },\n" +
            "  \"httpRequestBody\": {\n" +
            "    \"ui:widget\": \"textarea\"\n" +
            "  },\n" +
            "  \"subscriptions\": {\n" +
            "    \"ui:batchMode\": true,\n" +
            "    \"items\": {\n" +
            "      \"ui:order\": [\"node\", \"holding-registers\", \"mqtt-topic\", \"destination\", \"qos\", \"*\"],\n" +
            "      \"ui:collapsable\": {\n" +
            "        \"titleKey\": \"destination\"\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"auth\": {\n" +
            "    \"basic\": {\n" +
            "      \"ui:order\": [\"username\", \"password\", \"*\"]\n" +
            "    }\n" +
            "  }\n" +
            "}";

    /**
     * Convert between the internal system representation of a ProtocolAdapter type and its API based sibling.
     * This decoupling allows for flexibility when internal model changes would otherwise impact API support
     *
     * @return The instance to be sent across the API
     */
    public static ProtocolAdapter convertInstalledAdapterType(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterManager adapterManager,
            final @NotNull ProtocolAdapterInformation info,
            final @NotNull ConfigurationService configurationService,
            final @NotNull VersionProvider versionProvider) {

        Preconditions.checkNotNull(adapterManager);
        Preconditions.checkNotNull(info);
        Preconditions.checkNotNull(configurationService);
        String logoUrl = info.getLogoUrl();
        //noinspection ConstantValue
        if (logoUrl != null) {
            logoUrl = logoUrl.startsWith("/") ? "/module" + logoUrl : "/module/" + logoUrl;
            logoUrl = applyAbsoluteServerAddressInDeveloperMode(logoUrl, configurationService);
        } else {
            // although it is marked as not null it is input from outside (possible customer adapter),
            // so we should trust but validate and at least log.
            LOG.warn("Logo url for adapter '{}' was null. ", info.getDisplayName());
        }
        final ProtocolAdapterFactory<?> protocolAdapterFactory =
                adapterManager.getProtocolAdapterFactory(info.getProtocolId());
        if (protocolAdapterFactory == null) {
            // this can only happen if the adapter somehow got removed from the manager concurrently, which is not possible right now
            LOG.warn("Factory for adapter '{}' was not found while conversion of adapter to information for REST API.",
                    info.getDisplayName());
            return null;
        }

        final ProtocolAdapterSchemaManager protocolAdapterSchemaManager =
                new ProtocolAdapterSchemaManager(objectMapper, protocolAdapterFactory.getConfigClass());


        final String rawVersion = info.getVersion();
        final String version = rawVersion.replace("${edge-version}", versionProvider.getVersion());
        final JsonNode uiSchema = getUiSchemaForAdapter(objectMapper, info);
        return new ProtocolAdapter(info.getProtocolId(),
                info.getProtocolName(),
                info.getDisplayName(),
                info.getDescription(),
                info.getUrl(),
                version,
                logoUrl,
                null,
                info.getAuthor(),
                true,
                getCapabilities(info),
                convertApiCategory(info.getCategory()),
                info.getTags() == null ?
                        null :
                        info.getTags().stream().map(Enum::toString).collect(Collectors.toList()),
                protocolAdapterSchemaManager.generateSchemaNode(),
                uiSchema);
    }

    @VisibleForTesting
    protected static @NotNull JsonNode getUiSchemaForAdapter(
            @NotNull ObjectMapper objectMapper, @NotNull ProtocolAdapterInformation info) {
        final String uiSchemaAsString = info.getUiSchema();
        if (uiSchemaAsString != null) {
            try {
                return objectMapper.readTree(uiSchemaAsString);
            } catch (JsonProcessingException e) {
                LOG.warn("Ui schema for adapter '{}' is not parsable, the default zu schema will be applied. ",
                        info.getDisplayName(),
                        e);
                // fall through to parsing the DEFAULT SCHEMA
            }
        }

        try {
            return objectMapper.readTree(Objects.requireNonNullElse(uiSchemaAsString, DEFAULT_SCHEMA));
        } catch (JsonProcessingException e) {
            LOG.error("Exception during parsing of default zu schema: ", e);
            // this should never happen as we control the input (default schema)
            throw new RuntimeException(e);
        }
    }

    private static @NotNull Set<ProtocolAdapter.Capability> getCapabilities(final @NotNull ProtocolAdapterInformation info) {
        Set<ProtocolAdapter.Capability> capabilities = new HashSet<>();
        for (final ProtocolAdapterCapability capability : info.getCapabilities()) {
            capabilities.add(ProtocolAdapter.Capability.from(capability));
        }
        return capabilities;
    }

    /**
     * Convert between the internal system representation of a Module type and its API protocol adapter type.
     *
     * @return The instance to be sent across the API
     */
    public static ProtocolAdapter convertModuleAdapterType(
            final @NotNull Module module, final @NotNull ConfigurationService configurationService) {

        Preconditions.checkNotNull(module);
        Preconditions.checkNotNull(configurationService);
        String logoUrl = module.getLogoUrl() == null ? null : module.getLogoUrl().getUrl();
        String documentationUrl = module.getDocumentationLink() == null ? null : module.getDocumentationLink().getUrl();
        String provisioningUrl = module.getProvisioningLink() == null ? null : module.getProvisioningLink().getUrl();
        if (logoUrl != null) {
            logoUrl = logoUrl.startsWith("/") ? "/module" + logoUrl : "/module/" + logoUrl;
            logoUrl = applyAbsoluteServerAddressInDeveloperMode(logoUrl, configurationService);
        }
        return new ProtocolAdapter(module.getId(),
                module.getId(),
                module.getName(),
                module.getDescription(),
                documentationUrl,
                module.getVersion(),
                logoUrl,
                provisioningUrl,
                module.getAuthor(),
                false,
                Set.of(),
                null,
                null,
                null,
                null);
    }

    public static @NotNull String applyAbsoluteServerAddressInDeveloperMode(
            final @NotNull String logoUrl, final @NotNull ConfigurationService configurationService) {
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
    public static @Nullable ProtocolAdapterCategory convertApiCategory(final @Nullable com.hivemq.adapter.sdk.api.ProtocolAdapterCategory category) {
        if (category == null) {
            return null;
        }
        return new ProtocolAdapterCategory(category.name(),
                category.getDisplayName(),
                category.getDescription(),
                category.getImage());
    }
}
