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
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterDiscoveryInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterDiscoveryOutput;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;

public interface ProtocolAdapterInformation {

    @NotNull String getProtocolName();

    @NotNull String getProtocolId();

    @NotNull String getName();

    @NotNull String getDescription();

    @NotNull String getUrl();

    @NotNull String getVersion();

    @NotNull String getLogoUrl();

    @NotNull String getAuthor();

    @Nullable ProtocolAdapterConstants.CATEGORY getCategory();

    @Nullable List<ProtocolAdapterConstants.TAG> getTags();

    /**
     * When enabled, API & UI will allow a call to the @{link} {@link ProtocolAdapter#discoverValues(ProtocolAdapterDiscoveryInput, ProtocolAdapterDiscoveryOutput)}
     * method. It is then up to the adapter to support discovery.
     */
    default boolean supportsDiscovery(){
        return true;
    }

    @NotNull Class<? extends CustomConfig> getConfigClass();

    default @Nullable String getConfigJsonSchema() {
        //null means the schema will be auto-generated
        return null;
    }
}
