/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.pulse.converters;

import com.hivemq.configuration.entity.EntityConverter;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class PulseAgentAssetSchemaConverter implements EntityConverter<String, String> {
    public static final PulseAgentAssetSchemaConverter INSTANCE = new PulseAgentAssetSchemaConverter();
    public static final String DATA_APPLICATION_SCHEMA_JSON_BASE64 = "data:application/schema+json;base64,";

    private PulseAgentAssetSchemaConverter() {
    }

    @Override
    public @NotNull String toInternalEntity(final @NotNull String str) {
        if (!Objects.requireNonNull(str).startsWith(DATA_APPLICATION_SCHEMA_JSON_BASE64)) {
            return str;
        }
        final String base64String = str.substring(DATA_APPLICATION_SCHEMA_JSON_BASE64.length());
        return new String(Base64.getDecoder().decode(base64String), StandardCharsets.UTF_8);
    }

    @Override
    public @NotNull String toRestEntity(final @NotNull String str) {
        final String base64String =
                Base64.getEncoder().encodeToString(Objects.requireNonNull(str).getBytes(StandardCharsets.UTF_8));
        return DATA_APPLICATION_SCHEMA_JSON_BASE64 + base64String;
    }
}
