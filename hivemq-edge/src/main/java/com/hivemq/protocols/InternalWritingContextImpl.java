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
import com.hivemq.protocols.tag.TagSchemaCreationOutputImpl;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class InternalWritingContextImpl implements InternalWritingContext {

    private final @NotNull SouthboundMapping southboundMapping;

    /**
     * Every southbound mapping of the same adapter, this one included.
     * <p>
     * Kept so {@link #tagSchemaOutput()} can resolve a tag's request fields across all the mappings that name it:
     * a tag has one schema, and which fields it declares cannot be read off a single mapping.
     */
    private final @NotNull List<SouthboundMapping> siblings;

    public InternalWritingContextImpl(final @NotNull SouthboundMapping southboundMapping) {
        this(southboundMapping, List.of(southboundMapping));
    }

    public InternalWritingContextImpl(
            final @NotNull SouthboundMapping southboundMapping, final @NotNull List<SouthboundMapping> siblings) {
        this.southboundMapping = southboundMapping;
        this.siblings = siblings;
    }

    /**
     * A schema builder for this mapping's tag, with the request fields already resolved.
     * <p>
     * Here rather than at the caller because the resolution needs every mapping that names the tag, and this is
     * the object that has them. A caller asks for a tag's schema and supplies nothing else.
     */
    @Override
    public @NotNull TagSchemaCreationOutputImpl tagSchemaOutput() {
        return TagSchemaCreationOutputImpl.forTag(siblings, southboundMapping.getTagName());
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
        return MethodRequestInstructions.augment(
                configured,
                southboundMapping.isMethodCalls(),
                southboundMapping.isCorrelationData(),
                southboundMapping.isResponseTopic());
    }

    @Override
    public @NotNull String getSchema() {
        return southboundMapping.getSchema();
    }

    @Override
    public boolean isMethodCalls() {
        return southboundMapping.isMethodCalls();
    }

    @Override
    public boolean isCorrelationData() {
        return southboundMapping.isCorrelationData();
    }

    @Override
    public boolean isResponseTopic() {
        return southboundMapping.isResponseTopic();
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
