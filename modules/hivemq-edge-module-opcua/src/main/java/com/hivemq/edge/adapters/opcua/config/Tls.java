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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public record Tls (@JsonProperty("enabled")
                   @ModuleConfigField(title = "Enable TLS",
                                      description = "Enables TLS encrypted connection",
                                      defaultValue = "false")
                   boolean enabled,

                   @JsonProperty("noChecks")
                   @ModuleConfigField(title = "Disable certificate validation",
                                      description = "Allows to disable the validation of a certificate",
                                      defaultValue = "false")
                   boolean noChecks,

                   @JsonProperty("keystore")
                   @JsonInclude(NON_NULL)
                   @ModuleConfigField(title = "Keystore",
                                      description = "Keystore that contains the client certificate including the chain. Required for X509 authentication.")
                   @Nullable Keystore keystore,

                   @JsonProperty("truststore")
                   @JsonInclude(NON_NULL)
                   @ModuleConfigField(title = "Truststore",
                                      description = "Truststore which contains the trusted server certificates or trusted intermediates.")
                   @Nullable Truststore truststore
                   ) {
    @JsonCreator
    public Tls{
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final Tls tls = (Tls) o;
        return enabled() == tls.enabled() &&
                noChecks() == tls.noChecks() &&
                Objects.equals(keystore(), tls.keystore()) &&
                Objects.equals(truststore(), tls.truststore());
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled(), noChecks(), keystore(), truststore());
    }
}
