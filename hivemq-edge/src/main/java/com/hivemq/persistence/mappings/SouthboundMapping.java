/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.persistence.mappings;

import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import com.hivemq.protocols.InternalWritingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SouthboundMapping implements InternalWritingContext {

    private final @NotNull String topicFilter;
    private final @NotNull String tagName;
    private final @NotNull FieldMapping fieldMapping;
    private final @NotNull String schema;

    public SouthboundMapping(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final @NotNull FieldMapping fieldMapping,
            final @NotNull String schema) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.fieldMapping = fieldMapping;
        this.schema = schema;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull FieldMapping getFieldMapping() {
        return fieldMapping;
    }

    @Override
    public @NotNull String getSchema() {
        return schema;
    }

    @Override
    public @NotNull String toString() {
        return "SouthboundMapping{" +
                "fieldMapping=" +
                fieldMapping +
                ", topicFilter='" +
                topicFilter +
                '\'' +
                ", tagName='" +
                tagName +
                '\'' +
                '}';
    }
}
