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

    /**
     * The field mapping the southbound transform is generated from, with a method request's own fields added.
     * <p>
     * Added here rather than when the mapping is stored, because this is the last point before the script
     * generator and the first at which nothing else reads the result: the synthetic instructions therefore exist
     * only in the generated transform, never in the persisted configuration, the REST model or the mapping editor.
     * <p>
     * A mapping that carries no method calls gets its own instructions back unchanged, which is every southbound
     * mapping that exists today.
     */
    @Override
    @SuppressWarnings(
            "NullAway") // southboundMapping.getFieldMapping() may return null but the interface declares @NotNull
    public FieldMapping getFieldMapping() {
        final FieldMapping configured = southboundMapping.getFieldMapping();
        if (configured == null) {
            return configured;
        }
        return MethodRequestInstructions.augment(configured, southboundMapping.isMethodCalls());
    }

    @Override
    public @NotNull String getSchema() {
        return southboundMapping.getSchema();
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
    public @NotNull String toString() {
        return "InternalWritingContextImpl{" + "southboundMapping=" + southboundMapping + '}';
    }
}
