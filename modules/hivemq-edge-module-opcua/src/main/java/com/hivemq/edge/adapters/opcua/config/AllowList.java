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

/**
 * Location of the server-certificate allow-list used by {@link TrustMode#ALLOW_LIST}.
 *
 * <p>The file is authored offline and read only — the adapter never writes to it. That is what makes
 * it deployable as a read-only secret and keeps the root of trust out of reach of anything running at
 * runtime.
 */
public record AllowList(
        @JsonProperty(value = "path")
        @ModuleConfigField(
                title = "Allow-list path",
                description = "Path on the local file system to the server-certificate allow-list: one SHA-256 "
                        + "certificate fingerprint per line, hexadecimal, optional ':' separators, '#' for "
                        + "comments. Required when the effective trust mode is ALLOW_LIST.",
                required = true)
        @Nullable
        String path) {

    @JsonCreator
    public AllowList {
        // The path is nullable because an operator can write `<allowList/>` with nothing inside it, and
        // Jackson binds that to a null component. Neither rejecting nor defaulting it here would be
        // right: throwing aborts the conversion of every other adapter in the same refresh, and
        // substituting a value would rewrite what the operator wrote. A missing path is instead caught
        // by TlsChecksProjection, which reports it as the same actionable start-up error as an absent
        // allowList element.
    }
}
