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
package com.hivemq.protocols;

import com.hivemq.persistence.fieldmapping.FieldMappings;
import org.jetbrains.annotations.NotNull;

public class WritingContextImpl implements InternalWritingContext {

    private final @NotNull String tagName;
    private final @NotNull String topicFilter;
    private final int mqttMaxQoS;
    private final @com.hivemq.extension.sdk.api.annotations.NotNull FieldMappings fieldMappings;

    public WritingContextImpl(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final int mqttMaxQoS,
            final @NotNull FieldMappings fieldMappings) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.mqttMaxQoS = mqttMaxQoS;
        this.fieldMappings = fieldMappings;
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    public @NotNull String getMqttTopicFilter() {
        return topicFilter;
    }

    @Override
    public int getMqttMaxQos() {
        return mqttMaxQoS;
    }

    @Override
    public @NotNull FieldMappings getFieldMappings() {
        return fieldMappings;
    }
}
