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
import org.jetbrains.annotations.Nullable;

public record Keystore(
        @JsonProperty(value = "path")
        @ModuleConfigField(title = "Keystore path", description = "Path on the local file system to the keystore.")
        @NotNull
        String path,

        @JsonProperty(value = "password")
        @ModuleConfigField(title = "Keystore password", description = "Password to open the keystore.")
        @NotNull
        String password,

        @JsonProperty(value = "privateKeyPassword")
        @ModuleConfigField(title = "Private key password", description = "Password to access the private key.")
        @NotNull
        String privateKeyPassword) {

    @JsonCreator
    public Keystore {}

    /**
     * Accepts text where an object was expected; see {@link Truststore#fromText} for the collapse
     * this handles and why guessing at the remaining text is not an option.
     *
     * <p>Empty means no keystore, which is no client certificate — the same as leaving the element
     * out. Anything else is rejected naming the element, in place of the raw
     * {@code Cannot construct instance of Keystore} coercion error the same input used to produce.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static @Nullable Keystore fromText(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        throw new IllegalArgumentException(("The 'keystore' configuration could not be read: it arrived as the text "
                        + "'%s' rather than as a set of elements, which happens when its first child element is "
                        + "left empty (for example <path></path>). Which element each value belonged to cannot be "
                        + "recovered, so the adapter configuration has been rejected. Give every element a value or "
                        + "remove it entirely. An empty <keystore/> is valid and means no client certificate is "
                        + "configured.")
                .formatted(value.trim()));
    }

    @Override
    public @NotNull String path() {
        return path;
    }

    @Override
    public @NotNull String password() {
        return password;
    }

    @Override
    public @NotNull String privateKeyPassword() {
        return privateKeyPassword;
    }
}
