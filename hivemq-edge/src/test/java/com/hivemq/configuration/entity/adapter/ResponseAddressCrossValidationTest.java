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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import jakarta.xml.bind.ValidationEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/// A caller-chosen response address needs both mappings of a tag configured for it.
///
/// The southbound side reads the address a request names; the northbound side publishes there. Either alone does
/// nothing at all -- a caller sets its response topic, the answer arrives somewhere else, and without this
/// warning nothing anywhere says why. These pin that the warning fires for each one-sided case and stays quiet
/// for the two coherent ones.
class ResponseAddressCrossValidationTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(ProtocolAdapterEntity.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    private @NotNull List<String> warnings() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private static @NotNull SouthboundMappingEntity southbound(
            final @NotNull String tagName, final boolean readsResponseTopic) {
        return new SouthboundMappingEntity(
                tagName, "plant/" + tagName + "/call", new FieldMappingEntity(), "{}", true, false, readsResponseTopic);
    }

    private static @NotNull NorthboundMappingEntity northbound(
            final @NotNull String tagName, final boolean honoursResponseTopic) {
        return new NorthboundMappingEntity(
                tagName,
                "plant/" + tagName + "/result",
                1,
                null,
                false,
                true,
                false,
                List.of(),
                Long.MAX_VALUE,
                false,
                honoursResponseTopic);
    }

    private static void validate(
            final @NotNull List<NorthboundMappingEntity> northbound,
            final @NotNull List<SouthboundMappingEntity> southbound) {

        final List<String> tagNames = new ArrayList<>();
        northbound.forEach(mapping -> tagNames.add(mapping.getTagName()));
        southbound.forEach(mapping -> tagNames.add(mapping.getTagName()));

        final var entity = new ProtocolAdapterEntity(
                "adapter-1",
                "opcua",
                1,
                Map.of(),
                northbound,
                southbound,
                tagNames.stream()
                        .distinct()
                        .map(name -> new TagEntity(name, "", Map.of()))
                        .toList());

        entity.validate(new ArrayList<ValidationEvent>());
    }

    @Test
    void southboundReadsAnAddressNothingHonours_warns() {
        validate(List.of(northbound("boiler1", false)), List.of(southbound("boiler1", true)));

        assertThat(warnings()).hasSize(1);
        assertThat(warnings().get(0))
                .contains("boiler1")
                .contains("reads a response topic")
                .contains("will be ignored");
    }

    @Test
    void northboundHonoursAnAddressNothingReads_warns() {
        validate(List.of(northbound("boiler1", true)), List.of(southbound("boiler1", false)));

        assertThat(warnings()).hasSize(1);
        assertThat(warnings().get(0)).contains("boiler1").contains("no southbound mapping for it reads one");
    }

    @Test
    void bothSidesConfigured_staysQuiet() {
        validate(List.of(northbound("boiler1", true)), List.of(southbound("boiler1", true)));

        assertThat(warnings()).isEmpty();
    }

    @Test
    void neitherSideConfigured_staysQuiet() {
        // The ordinary case for every mapping that exists today. A warning here would fire for every adapter in
        // every configuration, which is how a warning stops being read.
        validate(List.of(northbound("boiler1", false)), List.of(southbound("boiler1", false)));

        assertThat(warnings()).isEmpty();
    }

    @Test
    void theWarningIsPerTag_notPerAdapter() {
        // One tag correctly configured must not silence a second that is not.
        validate(
                List.of(northbound("boiler1", true), northbound("boiler2", false)),
                List.of(southbound("boiler1", true), southbound("boiler2", true)));

        assertThat(warnings()).hasSize(1);
        assertThat(warnings().get(0)).contains("boiler2");
    }

    @Test
    void aTagWithNoNorthboundMappingAtAll_warns() {
        // Reading an address when nothing publishes the response at all is the same mistake in a starker form.
        validate(List.of(), List.of(southbound("boiler1", true)));

        assertThat(warnings()).hasSize(1);
        assertThat(warnings().get(0)).contains("boiler1");
    }
}
