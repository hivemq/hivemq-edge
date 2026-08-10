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

public record Truststore(
        @JsonProperty(value = "path")
        @ModuleConfigField(title = "Truststore path", description = "Path on the local file system to the truststore.")
        @NotNull
        String path,

        @JsonProperty(value = "password")
        @ModuleConfigField(title = "Truststore password", description = "Password to open the truststore.")
        @NotNull
        String password) {

    @JsonCreator
    public Truststore {}

    /**
     * Accepts text where an object was expected — the same collapse {@link Tls#fromText} documents,
     * one element further down: Edge's XML-to-map conversion replaces a nested element with its text
     * content whenever the element's first child element is empty, so
     * {@code <truststore><path></path><password>pw</password></truststore>} arrives here as
     * {@code "pw"} and {@code <truststore/>} arrives as {@code ""}.
     *
     * <ul>
     *   <li><b>Empty</b> means no truststore is configured, which is the JVM {@code cacerts} bundle —
     *       exactly what leaving the element out does, and what the "system truststore" example in the
     *       documentation shows. Without this creator the empty-string coercion produced the same
     *       {@code null}; the creator takes over that job because Jackson prefers it.
     *   <li><b>Anything else</b> is a truststore that can no longer be read, and it <b>throws</b>. The
     *       remaining text cannot be attributed back to the element it came from, and guessing would
     *       mean choosing trust anchors the operator did not write. Before this creator existed the
     *       same input failed with a raw {@code Cannot construct instance of Truststore} coercion
     *       error, which names neither the element nor the fix.
     * </ul>
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static @Nullable Truststore fromText(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        throw new IllegalArgumentException(("The 'truststore' configuration could not be read: it arrived as the "
                        + "text '%s' rather than as a set of elements, which happens when its first child element "
                        + "is left empty (for example <path></path>). Which element each value belonged to cannot "
                        + "be recovered, so the adapter configuration has been rejected. Give every element a value "
                        + "or remove it entirely. An empty <truststore/> is valid and means the JVM cacerts are "
                        + "trusted.")
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
}
