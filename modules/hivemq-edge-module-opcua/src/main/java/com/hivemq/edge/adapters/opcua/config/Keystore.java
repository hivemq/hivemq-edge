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

public class Keystore {

    @JsonProperty(value = "path", required = true)
    @ModuleConfigField(title = "Keystore path",
                       description = "Path on the local file system to the keystore.",
                       required = true)
    private final @NotNull String path;

    @JsonProperty(value = "password", required = true)
    @ModuleConfigField(title = "Keystore password", description = "Password to open the keystore.", required = true)
    private final @NotNull String password;

    @JsonProperty(value = "privateKeyPassword", required = true)
    @ModuleConfigField(title = "Private key password",
                       description = "Password to access the private key.",
                       required = true)
    private final @NotNull String privateKeyPassword;

    @JsonCreator
    public Keystore(
            @JsonProperty(value = "path", required = true) final @NotNull String path,
            @JsonProperty(value = "password", required = true) final @NotNull String password,
            @JsonProperty(value = "privateKeyPassword", required = true) final @NotNull String privateKeyPassword) {
        this.path = path;
        this.password = password;
        this.privateKeyPassword = privateKeyPassword;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getPassword() {
        return password;
    }

    public @NotNull String getPrivateKeyPassword() {
        return privateKeyPassword;
    }
}
