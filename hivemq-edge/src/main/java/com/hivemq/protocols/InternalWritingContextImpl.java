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

import com.hivemq.persistence.mappings.SouthboundMapping;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import org.jetbrains.annotations.NotNull;

public class InternalWritingContextImpl implements InternalWritingContext {

    private final @NotNull SouthboundMapping southboundMapping;


    public InternalWritingContextImpl(final @NotNull SouthboundMapping southboundMapping) {
        this.southboundMapping = southboundMapping;
    }

    @Override
    public FieldMapping getFieldMapping() {
        return southboundMapping.getFieldMapping();
    }

    @Override
    public @NotNull String getTagName() {
        return southboundMapping.getTagName();
    }

    @Override
    public @NotNull String getTopicFilter() {
        return southboundMapping.getTopicFilter();
    }

    @Override
    public int getMaxQoS() {
        return southboundMapping.getMaxQoS();
    }


    @Override
    public @NotNull String toString() {
        return "InternalWritingContextImpl{" + "southboundMapping=" + southboundMapping + '}';
    }
}
