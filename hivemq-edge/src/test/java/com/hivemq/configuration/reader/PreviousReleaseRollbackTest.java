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
package com.hivemq.configuration.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.SAXException;

/**
 * A rollback must keep working: after this release loads a configuration and writes it back, the previous release
 * must still accept the file. The previous release validates {@code config.xml} against its own schema at start and
 * refuses to start on any element it does not know, so the guard validates the written file against that schema,
 * kept in {@code previous-release/config.xsd}. A new configuration field written by default -- the defect class of
 * EDG-1133 and EDG-1146 -- fails here, whatever its name.
 *
 * <p>The inputs are configurations the previous release accepts: an empty one, the shipped stock files, and the
 * previous release's own {@code test-config.xml} ({@code previous-release/config.xml}), which exercises most sections.
 *
 * <p><b>On every release</b>, replace both files under {@code previous-release/} with the ones of the release just
 * shipped: {@code git show <tag>:hivemq-edge/src/main/resources/config.xsd} and
 * {@code git show <tag>:hivemq-edge/src/test/resources/test-config.xml}. They are 2026.14's now.
 */
public class PreviousReleaseRollbackTest extends AbstractConfigurationTest {

    private static Schema previousReleaseSchema;

    @BeforeAll
    static void loadPreviousReleaseSchema() throws SAXException {
        previousReleaseSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(PreviousReleaseRollbackTest.class.getResource("/previous-release/config.xsd"));
    }

    static @NotNull Stream<Arguments> configurationsThePreviousReleaseAccepts() throws IOException {
        return Stream.of(
                Arguments.of("empty", "<hivemq/>"),
                Arguments.of("stock config.xml", Files.readString(Path.of("src/main/resources/config.xml"))),
                Arguments.of("Docker config.xml", Files.readString(Path.of("../docker/config.xml"))),
                Arguments.of(
                        "previous release test-config.xml",
                        Files.readString(Path.of("src/test/resources/previous-release/config.xml"))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("configurationsThePreviousReleaseAccepts")
    void whenAConfigurationIsWrittenBack_thenThePreviousReleaseStillAcceptsIt(
            final @NotNull String name, final @NotNull String configXml) throws IOException {
        // the input itself must be one the previous release accepts, or the check below proves nothing
        assertThatCode(() -> validateAgainstThePreviousRelease(configXml))
                .as("input '%s' is not a configuration the previous release accepts", name)
                .doesNotThrowAnyException();
        Files.writeString(xmlFile.toPath(), configXml, UTF_8);
        reader.applyConfig();

        reader.writeConfigWithSync();

        final String written = Files.readString(xmlFile.toPath(), UTF_8);
        assertThatCode(() -> validateAgainstThePreviousRelease(written))
                .as("the previous release refuses '%s' after this release wrote it back:%n%s", name, written)
                .doesNotThrowAnyException();
    }

    private static void validateAgainstThePreviousRelease(final @NotNull String xml) throws IOException, SAXException {
        previousReleaseSchema.newValidator().validate(new StreamSource(new StringReader(xml)));
    }
}
