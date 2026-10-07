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

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// A configuration written before method calls existed must still parse.
///
/// The four method-call flags are optional elements with a default of false. That is not a style choice: a
/// `required` element would make JaxB reject every file written until now, so an adapter that worked before an
/// upgrade would refuse to start after it. These tests read XML that omits the elements entirely -- which is what
/// every existing config on disk looks like -- and assert that it parses and that the flags come out off.
class MappingEntityBackwardCompatibilityTest {

    private static <T> @NotNull T unmarshal(final @NotNull Class<T> type, final @NotNull String xml) throws Exception {
        final JAXBContext context = JAXBContext.newInstance(type);
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
        return unmarshaller.unmarshal(reader, type).getValue();
    }

    /// Marshals an entity that carries no `@XmlRootElement`.
    ///
    /// These entities are only ever written as part of a whole configuration, so none of them declares a root
    /// element. Wrapping in a JAXBElement supplies the name the marshaller would otherwise demand, without
    /// annotating production classes to suit a test.
    private static <T> @NotNull String marshal(final @NotNull Class<T> type, final @NotNull T entity) throws Exception {
        final JAXBContext context = JAXBContext.newInstance(type);
        final java.io.StringWriter writer = new java.io.StringWriter();
        context.createMarshaller()
                .marshal(
                        new jakarta.xml.bind.JAXBElement<>(new javax.xml.namespace.QName("mapping"), type, entity),
                        writer);
        return writer.toString();
    }

    @Test
    void southboundMappingWithoutTheFlags_parsesWithThemOff() throws Exception {
        // Exactly what an existing config contains: the four elements that have always been there, and nothing
        // else.
        final String oldConfig = """
                <southboundMapping>
                  <topicFilter>plant/boiler1/call</topicFilter>
                  <tagName>boiler1</tagName>
                  <fieldMapping/>
                  <fromNorthSchema>{}</fromNorthSchema>
                </southboundMapping>
                """;

        final SouthboundMappingEntity entity = unmarshal(SouthboundMappingEntity.class, oldConfig);

        assertThat(entity.getTagName()).isEqualTo("boiler1");
        assertThat(entity.getTopicFilter()).isEqualTo("plant/boiler1/call");
        assertThat(entity.isMethodCalls()).isFalse();
        assertThat(entity.isCorrelationData()).isFalse();
        assertThat(entity.isResponseTopic()).isFalse();
    }

    @Test
    void southboundMappingWithTheFlags_parsesThemThrough() throws Exception {
        final String newConfig = """
                <southboundMapping>
                  <topicFilter>plant/boiler1/call</topicFilter>
                  <tagName>boiler1</tagName>
                  <fieldMapping/>
                  <fromNorthSchema>{}</fromNorthSchema>
                  <methodCalls>true</methodCalls>
                  <correlationData>true</correlationData>
                  <responseTopic>true</responseTopic>
                </southboundMapping>
                """;

        final SouthboundMappingEntity entity = unmarshal(SouthboundMappingEntity.class, newConfig);

        assertThat(entity.isMethodCalls()).isTrue();
        assertThat(entity.isCorrelationData()).isTrue();
        assertThat(entity.isResponseTopic()).isTrue();
    }

    @Test
    void southboundMappingWithSomeFlags_leavesTheRestOff() throws Exception {
        // A fire-and-forget method tag: it carries method calls and neither of the optional two. The flags are
        // independent, so naming one must not imply another.
        final String partial = """
                <southboundMapping>
                  <topicFilter>plant/boiler1/call</topicFilter>
                  <tagName>boiler1</tagName>
                  <fieldMapping/>
                  <fromNorthSchema>{}</fromNorthSchema>
                  <methodCalls>true</methodCalls>
                </southboundMapping>
                """;

        final SouthboundMappingEntity entity = unmarshal(SouthboundMappingEntity.class, partial);

        assertThat(entity.isMethodCalls()).isTrue();
        assertThat(entity.isCorrelationData()).isFalse();
        assertThat(entity.isResponseTopic()).isFalse();
    }

    @Test
    void northboundMappingWithoutTheFlags_parsesWithThemOff() throws Exception {
        final String oldConfig = """
                <northboundMapping>
                  <topic>plant/boiler1/result</topic>
                  <tagName>boiler1</tagName>
                  <maxQoS>1</maxQoS>
                  <includeTagNames>false</includeTagNames>
                  <includeTimestamp>true</includeTimestamp>
                  <includeMetadata>false</includeMetadata>
                  <mqttUserProperties/>
                </northboundMapping>
                """;

        final NorthboundMappingEntity entity = unmarshal(NorthboundMappingEntity.class, oldConfig);

        assertThat(entity.getTagName()).isEqualTo("boiler1");
        assertThat(entity.isWriteCorrelationData()).isFalse();
    }

    @Test
    void northboundMappingWithTheFlag_parsesItThrough() throws Exception {
        final String newConfig = """
                <northboundMapping>
                  <topic>plant/boiler1/result</topic>
                  <tagName>boiler1</tagName>
                  <maxQoS>1</maxQoS>
                  <includeTagNames>false</includeTagNames>
                  <includeTimestamp>true</includeTimestamp>
                  <includeMetadata>false</includeMetadata>
                  <mqttUserProperties/>
                  <writeCorrelationData>true</writeCorrelationData>
                </northboundMapping>
                """;

        final NorthboundMappingEntity entity = unmarshal(NorthboundMappingEntity.class, newConfig);

        assertThat(entity.isWriteCorrelationData()).isTrue();
    }

    @Test
    void southboundFlagsSurviveARoundTripThroughPersistence() throws Exception {
        // The reason the whole entity layer exists: a flag set through the API has to come back after a restart.
        // Entity -> runtime mapping -> entity is the path a restart takes.
        final SouthboundMappingEntity original = new SouthboundMappingEntity(
                "boiler1", "plant/boiler1/call", new FieldMappingEntity(), "{}", true, true, false);

        final var roundTripped = SouthboundMappingEntity.fromPersistence(
                original.toPersistence(new com.fasterxml.jackson.databind.ObjectMapper()));

        assertThat(roundTripped.isMethodCalls()).isTrue();
        assertThat(roundTripped.isCorrelationData()).isTrue();
        assertThat(roundTripped.isResponseTopic()).isFalse();
    }

    @Test
    void theNorthboundFlagSurvivesARoundTripThroughPersistence() throws Exception {
        final NorthboundMappingEntity original = new NorthboundMappingEntity(
                "boiler1",
                "plant/boiler1/result",
                1,
                null,
                false,
                true,
                false,
                java.util.List.of(),
                Long.MAX_VALUE,
                true);

        final var roundTripped = NorthboundMappingEntity.fromPersistence(original.toPersistence());

        assertThat(roundTripped.isWriteCorrelationData()).isTrue();
    }

    @Test
    void anUnsetFlagMarshalsToNothing() throws Exception {
        // The property that keeps an upgrade from editing config files. Edge rewrites its own configuration, so
        // a flag that marshalled unconditionally would add elements to every existing mapping on the first write
        // after an upgrade -- a diff in a file customers may keep in version control, for settings they never
        // asked about.
        final SouthboundMappingEntity noFlags = new SouthboundMappingEntity(
                "boiler1", "plant/boiler1/call", new FieldMappingEntity(), "{}", false, false, false);

        final String xml = marshal(SouthboundMappingEntity.class, noFlags);

        assertThat(xml).doesNotContain("methodCalls");
        assertThat(xml).doesNotContain("correlationData");
        assertThat(xml).doesNotContain("responseTopic");
    }

    @Test
    void aSetFlagMarshalsAndTheOthersStayAbsent() throws Exception {
        final SouthboundMappingEntity oneFlag = new SouthboundMappingEntity(
                "boiler1", "plant/boiler1/call", new FieldMappingEntity(), "{}", true, false, false);

        final String xml = marshal(SouthboundMappingEntity.class, oneFlag);

        assertThat(xml).contains("<methodCalls>true</methodCalls>");
        assertThat(xml).doesNotContain("correlationData");
        assertThat(xml).doesNotContain("responseTopic");
    }

    @Test
    void anUnsetNorthboundFlagMarshalsToNothing() throws Exception {
        final NorthboundMappingEntity noFlags = new NorthboundMappingEntity(
                "boiler1", "plant/boiler1/result", 1, null, false, true, false, java.util.List.of(), Long.MAX_VALUE);

        final String xml = marshal(NorthboundMappingEntity.class, noFlags);

        assertThat(xml).doesNotContain("writeCorrelationData");
    }

    @Test
    void anEntityWithoutTheElements_equalsOneBuiltWithTheFlagsOff() throws Exception {
        // Null and false are the same state, so reading a file that omits the elements must produce an entity
        // equal to one built explicitly with the flags off. Config-level equality decides whether an adapter is
        // reapplied, and a spurious inequality there would restart adapters on every config read.
        final SouthboundMappingEntity fromOldFile = unmarshal(SouthboundMappingEntity.class, """
                <southboundMapping>
                  <topicFilter>plant/boiler1/call</topicFilter>
                  <tagName>boiler1</tagName>
                  <fieldMapping/>
                  <fromNorthSchema>{}</fromNorthSchema>
                </southboundMapping>
                """);
        final SouthboundMappingEntity built = new SouthboundMappingEntity(
                "boiler1", "plant/boiler1/call", new FieldMappingEntity(), "{}", false, false, false);

        assertThat(fromOldFile).isEqualTo(built);
        assertThat(fromOldFile.hashCode()).isEqualTo(built.hashCode());
    }

    @Test
    void aFlagChangeMakesEntitiesUnequal() {
        // Adapter-level equality compares mapping lists to decide whether anything must be reapplied, so an
        // entity that ignored a flag would accept an edit and never act on it.
        final var off = new SouthboundMappingEntity(
                "boiler1", "plant/boiler1/call", new FieldMappingEntity(), "{}", false, false, false);
        final var on = new SouthboundMappingEntity(
                "boiler1", "plant/boiler1/call", new FieldMappingEntity(), "{}", true, false, false);

        assertThat(off).isNotEqualTo(on);
        assertThat(off.hashCode()).isNotEqualTo(on.hashCode());
    }
}
