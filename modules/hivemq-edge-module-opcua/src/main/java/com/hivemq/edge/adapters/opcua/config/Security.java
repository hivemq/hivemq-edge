/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.edge.adapters.opcua.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Security {

    @JsonProperty("policy")
    @ModuleConfigField(title = "OPC UA security policy",
                       description = "Security policy to use for communication with the server.",
                       defaultValue = "NONE")
    private final @NotNull SecPolicy policy;

    public Security(@JsonProperty("policy") final @Nullable SecPolicy policy) {
        this.policy = Objects.requireNonNullElse(policy, Constants.DEFAULT_SECURITY_POLICY);
    }

    public @NotNull SecPolicy getPolicy() {
        return policy;
    }
}
