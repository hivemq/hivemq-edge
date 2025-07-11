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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;

public record Truststore(@JsonProperty(value = "path") @ModuleConfigField(title = "Truststore path",
                                                                          description = "Path on the local file system to the truststore.") @NotNull String path,
                         @JsonProperty(value = "password") @ModuleConfigField(title = "Truststore password",
                                                                              description = "Password to open the truststore.") @NotNull String password) {

    @JsonCreator
    public Truststore {
    }

    @Override
    public @NotNull String path() {
        return path;
    }

    @Override
    public @NotNull String password() {
        return password;
    }
}
