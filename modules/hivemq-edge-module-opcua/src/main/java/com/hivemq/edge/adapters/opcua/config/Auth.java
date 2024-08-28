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
import org.jetbrains.annotations.Nullable;

public class Auth {

    @JsonProperty("basic")
    @ModuleConfigField(title = "Basic Authentication", description = "Username / password based authentication")
    private final @Nullable BasicAuth basicAuth;

    @JsonProperty("x509")
    @ModuleConfigField(title = "X509 Authentication", description = "Authentication based on certificate / private key")
    private final @Nullable X509Auth x509Auth;

    @JsonCreator
    public Auth(
            @JsonProperty("basic") final @Nullable BasicAuth basicAuth,
            @JsonProperty("x509") final @Nullable X509Auth x509Auth) {
        this.basicAuth = basicAuth;
        this.x509Auth = x509Auth;
    }

    public @Nullable BasicAuth getBasicAuth() {
        return basicAuth;
    }

    public @Nullable X509Auth getX509Auth() {
        return x509Auth;
    }
}
