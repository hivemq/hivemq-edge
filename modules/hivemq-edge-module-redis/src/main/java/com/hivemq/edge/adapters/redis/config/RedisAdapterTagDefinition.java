/*
 * Copyright 2024-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.redis.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

public class RedisAdapterTagDefinition implements TagDefinition {
    @JsonProperty(value = "key", required = true)
    @ModuleConfigField(title = "Key",
            description = "Key to get from Redis",
            required = true,
            format = ModuleConfigField.FieldType.UNSPECIFIED)
    protected @Nullable String key;

    @JsonProperty(value = "field")
    @ModuleConfigField(title = "Field",
            description = "Optional field to get from Redis")
    protected @Nullable String field;

    @JsonProperty(value = "type", required = true)
    @ModuleConfigField(title = "Type",
            description = "Type of the Key to get from Redis",
            required = true,
            defaultValue = "STRING",
            format = ModuleConfigField.FieldType.UNSPECIFIED)
    protected @Nullable RedisDataType type;

    @JsonProperty(value = "getall", required = true)
    @ModuleConfigField(title = "Get all values",
            description = "Get all value from the hash", defaultValue = "false",
            format = ModuleConfigField.FieldType.BOOLEAN)
    protected boolean getAll;

    @JsonCreator
    public RedisAdapterTagDefinition(
            @JsonProperty("key") @Nullable final String key,
            @JsonProperty("field") @Nullable final String field,
            @JsonProperty("getall") final boolean getAll,
            @JsonProperty("type") @Nullable final RedisDataType type) {
        this.key = key;
        this.field = field;
        this.type = type;
        this.getAll = getAll;

    }

    public @Nullable String getKey(){return key;}
    public @Nullable String getField(){return field;}
    public @Nullable RedisDataType getType(){return type;}
    public Boolean getAll(){return getAll;}
}
