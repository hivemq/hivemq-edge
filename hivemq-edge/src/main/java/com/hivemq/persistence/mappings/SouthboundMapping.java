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

import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import com.hivemq.protocols.InternalWritingContext;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class SouthboundMapping implements InternalWritingContext {

    private final @NotNull String topicFilter;
    private final @NotNull String tagName;
    private final @NotNull FieldMapping fieldMapping;
    private final @NotNull String schema;
    private final boolean methodCalls;
    private final boolean correlationData;
    private final boolean responseTopic;

    public SouthboundMapping(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final @NotNull FieldMapping fieldMapping,
            final @NotNull String schema) {
        this(tagName, topicFilter, fieldMapping, schema, false, false, false);
    }

    public SouthboundMapping(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final @NotNull FieldMapping fieldMapping,
            final @NotNull String schema,
            final boolean methodCalls,
            final boolean correlationData,
            final boolean responseTopic) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.fieldMapping = fieldMapping;
        this.schema = schema;
        this.methodCalls = methodCalls;
        this.correlationData = correlationData;
        this.responseTopic = responseTopic;
    }

    /**
     * Whether messages on this mapping are method calls, naming the method in {@code methodName}.
     * <p>
     * This is what marks a mapping as carrying method requests at all, and the other two refine it. It cannot be
     * inferred from them: a fire-and-forget call sets neither, and inferring would then strip the method name from
     * the simplest request there is.
     */
    @Override
    public boolean isMethodCalls() {
        return methodCalls;
    }

    /** Whether requests carry a correlation token to be read and echoed on the response. */
    @Override
    public boolean isCorrelationData() {
        return correlationData;
    }

    /** Whether requests may name their own response topic. */
    @Override
    public boolean isResponseTopic() {
        return responseTopic;
    }

    @Override
    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    @SuppressWarnings("NullAway") // SouthboundMapping may have null fieldMapping; callers handle it
    public @NotNull FieldMapping getFieldMapping() {
        return fieldMapping;
    }

    public static @NotNull SouthboundMapping fromModel(
            final @NotNull com.hivemq.edge.api.model.SouthboundMapping southboundMapping,
            final @NotNull String schema) {
        return new SouthboundMapping(
                southboundMapping.getTagName(),
                southboundMapping.getTopicFilter(),
                southboundMapping.getFieldMapping() != null
                        ? FieldMapping.fromModel(southboundMapping.getFieldMapping())
                        : FieldMapping.DEFAULT_FIELD_MAPPING,
                schema,
                Boolean.TRUE.equals(southboundMapping.getMethodCalls()),
                Boolean.TRUE.equals(southboundMapping.getCorrelationData()),
                Boolean.TRUE.equals(southboundMapping.getResponseTopic()));
    }

    public @NotNull com.hivemq.edge.api.model.SouthboundMapping toModel() {
        return new com.hivemq.edge.api.model.SouthboundMapping()
                .topicFilter(this.getTopicFilter())
                .tagName(this.getTagName())
                .fieldMapping(this.fieldMapping.toModel())
                .methodCalls(this.methodCalls)
                .correlationData(this.correlationData)
                .responseTopic(this.responseTopic);
    }

    @Override
    public @NotNull String getSchema() {
        return schema;
    }

    @Override
    public @NotNull String toString() {
        return "SouthboundMapping{" + "fieldMapping="
                + fieldMapping
                + ", topicFilter='"
                + topicFilter
                + '\''
                + ", tagName='"
                + tagName
                + '\''
                + '}';
    }

    /**
     * Compares every field, the three method-call flags included.
     * <p>
     * <b>The flags were missing, and leaving them out became dangerous the moment {@code FieldMapping} gained
     * value equality.</b> Until then this comparison could never return true -- the field mapping fell back to
     * identity and a configuration is parsed afresh on every change -- so an adapter was always treated as
     * changed and always restarted. That masked the gap. With the field mapping comparing by value, a mapping
     * whose *only* difference is one of these flags would have compared equal, and
     * {@code ProtocolAdapterManager.refreshUpdatedAdapters} would have skipped the restart: the flag would be
     * stored and never take effect. The persisted entity ({@code SouthboundMappingEntity}) does compare them,
     * so the configuration on disk and the running adapter would have disagreed with nothing to say so.
     * <p>
     * Which is why this lands in the same change as that one, and not after it (Miguel's M2 on PR 1765).
     */
    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof SouthboundMapping that)) return false;
        return Objects.equals(getTopicFilter(), that.getTopicFilter())
                && Objects.equals(getTagName(), that.getTagName())
                && Objects.equals(getFieldMapping(), that.getFieldMapping())
                && Objects.equals(getSchema(), that.getSchema())
                && methodCalls == that.methodCalls
                && correlationData == that.correlationData
                && responseTopic == that.responseTopic;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getTopicFilter(),
                getTagName(),
                getFieldMapping(),
                getSchema(),
                methodCalls,
                correlationData,
                responseTopic);
    }
}
