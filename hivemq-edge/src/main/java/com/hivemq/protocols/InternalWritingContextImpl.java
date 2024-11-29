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

import com.hivemq.persistence.mappings.SoutboundMapping;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import org.jetbrains.annotations.NotNull;

public class InternalWritingContextImpl implements InternalWritingContext {

    private final @NotNull SoutboundMapping soutboundMapping;


    public InternalWritingContextImpl(@NotNull final SoutboundMapping soutboundMapping) {
        this.soutboundMapping = soutboundMapping;
    }

    @Override
    public FieldMapping getFieldMapping() {
        return soutboundMapping.getFieldMapping();
    }

    @Override
    public @NotNull String getTagName() {
        return soutboundMapping.getTagName();
    }

    @Override
    public @NotNull String getTopicFilter() {
        return soutboundMapping.getTopicFilter();
    }

    @Override
    public int getMaxQoS() {
        return soutboundMapping.getMaxQoS();
    }
}
