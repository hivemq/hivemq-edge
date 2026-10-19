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
package com.hivemq.configuration.entity.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.EntityValidatable;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import com.hivemq.edge.api.model.FieldMapping;
import com.hivemq.edge.api.model.Instruction;
import com.hivemq.persistence.mappings.SouthboundMapping;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class SouthboundMappingEntity implements EntityValidatable {

    @XmlElement(name = "topicFilter", required = true)
    private final @NotNull String topicFilter;

    @XmlElement(name = "tagName", required = true)
    private final @NotNull String tagName;

    @XmlElement(name = "fieldMapping", required = true)
    private final @NotNull FieldMappingEntity fieldMapping;

    @XmlElement(name = "fromNorthSchema", required = true)
    private final @NotNull String fromNorthSchema;

    // The three method-call flags are deliberately NOT `required`. Every config written before method calls
    // existed omits them, and a required element would make JaxB reject those files outright -- an adapter that
    // worked yesterday failing to start after an upgrade.
    //
    // Boxed and left null when unset, rather than a primitive boolean. JaxB marshals a primitive
    // unconditionally, so emitting these would have rewritten every existing mapping with three new lines on
    // the first config write after an upgrade -- a diff in a file customers may keep in version control, for
    // settings they never asked about. Null means absent on the way out and false on the way in.
    @XmlElement(name = "methodCalls")
    private final @Nullable Boolean methodCalls;

    @XmlElement(name = "correlationData")
    private final @Nullable Boolean correlationData;

    @XmlElement(name = "responseTopic")
    private final @Nullable Boolean responseTopic;

    // no-arg constructor for JaxB
    @SuppressWarnings("NullAway.Init")
    public SouthboundMappingEntity() {
        topicFilter = "";
        tagName = "";
        fieldMapping = new FieldMappingEntity();
        fromNorthSchema = "";
        // Null, not false: that is what keeps the element out of the file again when it is rewritten. JaxB
        // overwrites only the fields its XML names, so these three stay null whenever the file omits them.
        methodCalls = null;
        correlationData = null;
        responseTopic = null;
    }

    public SouthboundMappingEntity(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final @NotNull FieldMappingEntity fieldMapping,
            final @NotNull String fromNorthSchema) {
        this(tagName, topicFilter, fieldMapping, fromNorthSchema, false, false, false);
    }

    public SouthboundMappingEntity(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final @NotNull FieldMappingEntity fieldMapping,
            final @NotNull String fromNorthSchema,
            final boolean methodCalls,
            final boolean correlationData,
            final boolean responseTopic) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.fieldMapping = fieldMapping;
        this.fromNorthSchema = fromNorthSchema;
        // False is stored as null so it marshals to nothing. Without this a config written from a mapping that
        // simply has no method calls -- which is every mapping that exists today -- would gain three elements
        // saying so, and the round trip would no longer be a no-op.
        this.methodCalls = methodCalls ? Boolean.TRUE : null;
        this.correlationData = correlationData ? Boolean.TRUE : null;
        this.responseTopic = responseTopic ? Boolean.TRUE : null;
    }

    public boolean isMethodCalls() {
        return Boolean.TRUE.equals(methodCalls);
    }

    public boolean isCorrelationData() {
        return Boolean.TRUE.equals(correlationData);
    }

    public boolean isResponseTopic() {
        return Boolean.TRUE.equals(responseTopic);
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public @Nullable FieldMappingEntity getFieldMapping() {
        return fieldMapping;
    }

    @Override
    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        EntityValidatable.notEmpty(validationEvents, topicFilter, "topicFilter");
        EntityValidatable.notEmpty(validationEvents, tagName, "tagName");
        EntityValidatable.notEmpty(validationEvents, fromNorthSchema, "fromNorthSchema");
        fieldMapping.validate(validationEvents);
    }

    /// A mapping with no instructions gets the default one, which is what the REST and UI path has always
    /// done -- see `SouthboundMapping.fromModel`. Without this, the two ways of configuring Edge disagreed:
    /// a `<southboundMapping>` written in config.xml with no `<fieldMapping>` produced a mapping whose
    /// generated transform rebuilt the payload from nothing, so the value never reached the device and every
    /// request was quarantined for a missing `value`. Silently, and identically for every adapter and every
    /// tag kind.
    ///
    /// An empty `<instructions/>` is therefore no longer distinguishable from an absent `<fieldMapping>`.
    /// Nothing is lost by that: no configuration wants a mapping that discards every message, and the file
    /// could not record such an intent in any case -- an instruction carries only `source`, `destination`
    /// and `origin`, and a surrounding XML comment does not survive the next save, because the file is
    /// regenerated from the object tree rather than edited.
    public @NotNull SouthboundMapping toPersistence(final @NotNull ObjectMapper mapper) {
        return new SouthboundMapping(
                this.getTagName(),
                this.getTopicFilter(),
                this.fieldMapping.getInstructions().isEmpty()
                        ? com.hivemq.persistence.mappings.fieldmapping.FieldMapping.DEFAULT_FIELD_MAPPING
                        : this.fieldMapping.to(mapper),
                this.fromNorthSchema,
                isMethodCalls(),
                isCorrelationData(),
                isResponseTopic());
    }

    public @NotNull com.hivemq.edge.api.model.SouthboundMapping toAPi() {
        return com.hivemq.edge.api.model.SouthboundMapping.builder()
                .tagName(this.getTagName())
                .topicFilter(this.getTopicFilter())
                .fieldMapping(FieldMapping.builder()
                        .instructions(this.fieldMapping.getInstructions().stream()
                                .map(instruction -> (Instruction) Instruction.builder()
                                        .destination(instruction.getDestinationFieldName())
                                        .source(instruction.getSourceFieldName())
                                        .build())
                                .toList())
                        .build())
                .methodCalls(isMethodCalls())
                .correlationData(isCorrelationData())
                .responseTopic(isResponseTopic())
                .build();
    }

    public static @NotNull SouthboundMappingEntity fromPersistence(final @NotNull SouthboundMapping southboundMapping) {
        return new SouthboundMappingEntity(
                southboundMapping.getTagName(),
                southboundMapping.getTopicFilter(),
                Objects.requireNonNull(FieldMappingEntity.from(southboundMapping.getFieldMapping())),
                southboundMapping.getSchema(),
                southboundMapping.isMethodCalls(),
                southboundMapping.isCorrelationData(),
                southboundMapping.isResponseTopic());
    }

    public static @NotNull SouthboundMappingEntity fromApi(
            final @NotNull com.hivemq.edge.api.model.SouthboundMapping southboundMapping,
            final @NotNull String schema) {
        return new SouthboundMappingEntity(
                southboundMapping.getTagName(),
                southboundMapping.getTopicFilter(),
                Objects.requireNonNull(FieldMappingEntity.from(southboundMapping.getFieldMapping())),
                schema,
                Boolean.TRUE.equals(southboundMapping.getMethodCalls()),
                Boolean.TRUE.equals(southboundMapping.getCorrelationData()),
                Boolean.TRUE.equals(southboundMapping.getResponseTopic()));
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof final SouthboundMappingEntity that)) return false;
        return Objects.equals(getTopicFilter(), that.getTopicFilter())
                && Objects.equals(getTagName(), that.getTagName())
                && Objects.equals(fieldMapping, that.fieldMapping)
                && Objects.equals(fromNorthSchema, that.fromNorthSchema)
                // Compared through the accessors, not the fields: null and false are the same state, so an
                // entity read from a file that omits the element must equal one built with the flag off.
                // Included at all because a flag change IS a mapping change -- the config layer compares
                // entities to decide whether an adapter has to be reapplied, and leaving these out would accept
                // a flag edit and never act on it.
                && isMethodCalls() == that.isMethodCalls()
                && isCorrelationData() == that.isCorrelationData()
                && isResponseTopic() == that.isResponseTopic();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getTopicFilter(),
                getTagName(),
                fieldMapping,
                fromNorthSchema,
                isMethodCalls(),
                isCorrelationData(),
                isResponseTopic());
    }
}
