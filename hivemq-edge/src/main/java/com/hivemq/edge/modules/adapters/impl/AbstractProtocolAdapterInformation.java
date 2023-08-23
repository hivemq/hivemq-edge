package com.hivemq.edge.modules.adapters.impl;

import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.modules.adapters.ProtocolAdapterConstants;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

import static com.hivemq.configuration.info.SystemInformationImpl.DEVELOPMENT_VERSION;

/**
 * @author Simon L Johnson
 */
public abstract class AbstractProtocolAdapterInformation implements ProtocolAdapterInformation {

    protected final @NotNull String hivemqVersion;

    protected AbstractProtocolAdapterInformation() {
//        final String versionFromManifest =
//                ManifestUtils.getValueFromManifest(getClass(), "HiveMQ-Edge-Version");
        if (Boolean.getBoolean(HiveMQEdgeConstants.DEVELOPMENT_MODE)) {
            hivemqVersion = DEVELOPMENT_VERSION;
        } else {
            //-- The version we compiled against (for now)
            hivemqVersion = HiveMQEdgeConstants.VERSION;
        }
    }

    public @NotNull String getUrl() {
        return "https://github.com/hivemq/hivemq-edge/wiki/Protocol-adapters#" + getProtocolId();
    }

    @Override
    public @NotNull String getVersion() {
        return hivemqVersion;
    }

    @Override
    public @NotNull String getLogoUrl() {
        return String.format("/images/%s-icon.png", getProtocolId());
    }

    @Override
    public @NotNull String getAuthor() {
        return "HiveMQ";
    }

    @Override
    public ProtocolAdapterConstants.CATEGORY getCategory() {
        return ProtocolAdapterConstants.CATEGORY.INDUSTRIAL;
    }

    @Override
    public List<ProtocolAdapterConstants.TAG> getTags() {
        return List.of();
    }
}
