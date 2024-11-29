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

public class SouthboundMapping implements WritingContext {

    private final @NotNull String topicFilter;
    private final @NotNull String tagName;
    private final int maxQoS;
    private final @Nullable FieldMapping fieldMapping;

    public SouthboundMapping(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final int maxQoS,
            @Nullable FieldMapping fieldMapping) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.maxQoS = maxQoS;
        this.fieldMapping = fieldMapping;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public int getMaxQoS() {
        return maxQoS;
    }

    public @Nullable FieldMapping getFieldMapping() {
        return fieldMapping;
    }

    public static @NotNull SouthboundMapping from(
            final @NotNull InternalWritingContext writingContext) {
        return new SouthboundMapping(
                writingContext.getTagName(),
                writingContext.getTopicFilter(),
                writingContext.getMaxQoS(),
                writingContext.getFieldMapping());
    }
}
