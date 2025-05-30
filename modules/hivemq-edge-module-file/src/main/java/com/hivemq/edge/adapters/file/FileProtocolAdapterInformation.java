/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.file;


import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.file.config.FileSpecificAdapterConfig;
import com.hivemq.edge.adapters.file.tag.FileTag;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;

public class FileProtocolAdapterInformation implements ProtocolAdapterInformation {

    public static final @NotNull ProtocolAdapterInformation INSTANCE = new FileProtocolAdapterInformation();
    public static final String PROTOCOL_ID = "file";

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(FileProtocolAdapterInformation.class);
    private static final int CURRENT_CONFIG_VERSION = 1;


    protected FileProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "File Protocol";
    }

    @Override
    public @NotNull List<String> getLegacyProtocolIds() {
        return List.of("file_input", "file_polling_protocol");
    }

    @Override
    public @NotNull String getProtocolId() {
        return PROTOCOL_ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "File Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "This adapter polls and publishes the content of files on regular basis.";
    }

    @Override
    public @NotNull String getUrl() {
        // FIXME: we dont have documentation for the adapter yet. so there is no good link available yet.
        return "https://docs.hivemq.com/hivemq-edge/protocol-adapters.html";
    }

    @Override
    public @NotNull String getVersion() {
        return "${edge-version}";
    }

    @Override
    public @NotNull EnumSet<ProtocolAdapterCapability> getCapabilities() {
        return EnumSet.of(ProtocolAdapterCapability.READ);
    }

    @Override
    public @NotNull String getLogoUrl() {
        return "/images/file.png";
    }

    @Override
    public @NotNull String getAuthor() {
        return "HiveMQ";
    }

    @Override
    public @Nullable ProtocolAdapterCategory getCategory() {
        return ProtocolAdapterCategory.CONNECTIVITY;
    }

    @Override
    public List<ProtocolAdapterTag> getTags() {
        return List.of(ProtocolAdapterTag.IOT, ProtocolAdapterTag.IIOT);
    }

    @Override
    public @Nullable String getUiSchema() {
        try (final InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream("file-adapter-ui-schema.json")) {
            if (is == null) {
                LOG.warn("The UISchema for the File Adapter could not be loaded from resources: Not found.");
                return null;
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            LOG.warn("The UISchema for the File Adapter could not be loaded from resources:", e);
            return null;
        }
    }

    @Override
    public @NotNull Class<? extends Tag> tagConfigurationClass() {
        return FileTag.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthbound() {
        return FileSpecificAdapterConfig.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthAndSouthbound() {
        return FileSpecificAdapterConfig.class;
    }

    @Override
    public int getCurrentConfigVersion() {
        return CURRENT_CONFIG_VERSION;
    }
}
