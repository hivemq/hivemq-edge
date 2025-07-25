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
package com.hivemq.edge.adapters.databases;


import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.databases.config.DatabasesAdapterConfig;
import com.hivemq.edge.adapters.databases.config.DatabasesAdapterTag;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;

public class DatabasesProtocolAdapterInformation implements ProtocolAdapterInformation {

    public static final @NotNull ProtocolAdapterInformation INSTANCE = new DatabasesProtocolAdapterInformation();
    private static final @NotNull Logger LOG = LoggerFactory.getLogger(DatabasesProtocolAdapterInformation.class);

    protected DatabasesProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        // the returned string will be used for logging information on the protocol adapter
        return "Databases";
    }

    @Override
    public @NotNull String getProtocolId() {
        // this id is very important as this is how the adapters configurations in the config.xml are linked to the adapter implementations.
        // any change here means you will need to edit the config.xml
        return "databases";
    }

    @Override
    public @NotNull String getDisplayName() {
        // the name for this protocol adapter type that will be displayed within edge's ui
        return "Databases Protocol Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        // the description that will be shown for this protocol adapter within edge's ui
        return "This protocol adapter allow you to execute database query on a database (PostgreSQL, MySQL, MSSQL), retrieve the result and send it via MQTT.";
    }

    @Override
    public @NotNull String getUrl() {
        // this url will be displayed in the ui as a link to further documentation on this protocol adapter.
        // e.g. this could be a link to the source code and a readme
        return "https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#databases-protocol-adapter";
    }

    @Override
    public @NotNull String getVersion() {
        // the version of this protocol adapter, the usage of semantic versioning is advised.
        return "${edge-version}-ALPHA";
    }

    @Override
    public @NotNull EnumSet<ProtocolAdapterCapability> getCapabilities() {
        // this indicates what capabilities this protocol adapter has. E.g. READ/WRITE. See the ProtocolAdapterCapability enum for more information.
        return EnumSet.of(ProtocolAdapterCapability.READ);
    }

    @Override
    public @NotNull String getLogoUrl() {
        // this is a default image that is always available.
        return "/images/database.png";
    }

    @Override
    public @NotNull String getAuthor() {
        // your name/nick
        return "HiveMQ";
    }

    @Override
    public @Nullable ProtocolAdapterCategory getCategory() {
        // this indicates for which use cases this protocol adapter is intended. See the ProtocolAdapterConstants.CATEGORY enum for more information.
        return ProtocolAdapterCategory.CONNECTIVITY;
    }

    @Override
    public List<ProtocolAdapterTag> getTags() {
        // here you can set which Tags should be applied to this protocol adapter
        return List.of(ProtocolAdapterTag.INTERNET, ProtocolAdapterTag.TCP, ProtocolAdapterTag.AUTOMATION);
    }


    @Override
    public @Nullable String getUiSchema() {
        try (final InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream("databases-adapter-ui-schema.json")) {
            if (is == null) {
                LOG.warn("The UISchema for the Databases Adapter could not be loaded from resources: Not found.");
                return null;
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            LOG.warn("The UISchema for the Databases Adapter could not be loaded from resources:", e);
            return null;
        }
    }

    @Override
    public int getCurrentConfigVersion() {
        return 1;
    }

    @Override
    public @NotNull Class<? extends Tag> tagConfigurationClass() {
        return DatabasesAdapterTag.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthbound() {
        return DatabasesAdapterConfig.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthAndSouthbound() {
        return DatabasesAdapterConfig.class;
    }


}
