package com.hivemq.api.resources.impl;

import com.google.common.base.Preconditions;
import com.hivemq.api.model.adapters.ProtocolAdapter;
import com.hivemq.api.model.adapters.ProtocolAdapterCategory;
import com.hivemq.api.model.components.Module;
import com.hivemq.api.utils.ApiUtils;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.modules.adapters.ProtocolAdapterConstants;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.HttpConstants;
import com.hivemq.protocols.ProtocolAdapterManager;

import java.util.stream.Collectors;

/**
 * Utilities that handle the display, sort and filter logic relating to
 * protocol adapters.
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterApiUtils {

    public static ProtocolAdapter convertInstalledAdapterType(final @NotNull ProtocolAdapterManager adapterManager,
                                                              final @NotNull ProtocolAdapterInformation info,
                                                              final @NotNull ConfigurationService configurationService){
        String logoUrl = info.getLogoUrl();
        if(logoUrl != null){
            logoUrl = applyAbsoluteServerAddressInDeveloperMode(logoUrl, configurationService);
        }

        return new ProtocolAdapter(info.getProtocolId(),
                info.getProtocolName(),
                info.getName(),
                info.getDescription(),
                info.getUrl(),
                info.getVersion(),
                logoUrl,
                null,
                info.getAuthor(),
                true,
                info == null ? null : convertApiCategory(info.getCategory()),
                info.getTags() == null ? null : info.getTags().stream().
                        map(Enum::toString).collect(Collectors.toList()),
                adapterManager.getSchemaManager(info).generateSchemaNode());
    }

    public static ProtocolAdapter convertModuleAdapterType(final @NotNull Module module, final @NotNull ConfigurationService configurationService){

        String logoUrl = module.getLogoUrl() == null ? null : module.getLogoUrl().getUrl();
        String documentationUrl = module.getDocumentationLink() == null ? null : module.getDocumentationLink().getUrl();
        String provisioningUrl = module.getProvisioningLink() == null ? null : module.getProvisioningLink().getUrl();
        if(logoUrl != null){
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
                logoUrl = ApiUtils.getWebContextRoot(configurationService.apiConfiguration(), false) + logoUrl;
            }
        }
        return logoUrl;
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
