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
package com.hivemq.edge.modules.api.adapters;

import com.hivemq.edge.modules.adapters.ProtocolAdapterConstants;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;

/**
 * A metadata object that describes the Adapter type into the platform. Will also give an indication
 * to the features and allows it to be categorized in the UI / API.
 */
public interface ProtocolAdapterInformation {

    /**
     * The technically correct protocol name as defined by the standard, for example "Http" or "Mqtt".
     */
    @NotNull String getProtocolName();

    /**
     * Protocol ID that will be used by the platform to group types, search and categories.
     * NOTE: The format of this ID is important, it must be alpha-numeric without spaces and unique
     * within the system.
     */
    @NotNull String getProtocolId();

    /**
     * The visual name to display in the protocol adapter catalog for example "HTTP(s) to MQTT Protocol Adapter"
     */
    @NotNull String getDisplayName();

    @NotNull String getDescription();

    @NotNull String getUrl();

    @NotNull String getVersion();

    @NotNull String getLogoUrl();

    /**
     * The entity (person or company) who is responsible for producing the adapter
     * @return the name of the authoring entity
     */
    @NotNull String getAuthor();

    /**
     * An adapter can be in a single category. This helps discovery purposes
     * @return the category in which the adapter resides
     */
    @Nullable ProtocolAdapterConstants.CATEGORY getCategory();

    /**
     * Tag represents the keywords that can be associated with this type of adapter
     * @return a list of associated tags that can be used for search purposes
     */
    @Nullable List<ProtocolAdapterConstants.TAG> getTags();

    /**
     * Get the capabilities associated with the adapter. For more information on capabilities, please
     * refer to the {@link ProtocolAdapterCapability} descriptions.
     * @return
     */
    default byte getCapabilities(){
        return ProtocolAdapterCapability.READ | ProtocolAdapterCapability.DISCOVER;
    }
}
