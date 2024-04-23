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
package com.hivemq.edge.modules.config.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.config.ProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import static com.hivemq.edge.HiveMQEdgeConstants.ID_REGEX;

/**
 * @author Simon L Johnson
 */
public class AbstractProtocolAdapterConfig implements ProtocolAdapterConfig {

    @JsonProperty(value = "id", required = true)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    protected @NotNull String id;

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public void setId(final @NotNull String id) {
        this.id = id;
    }

}
