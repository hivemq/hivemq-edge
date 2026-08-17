/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.config.opcua2mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * What a configuration written before a field existed reads back as.
 * <p>
 * Review-02 finding 6. Jackson builds this record through its canonical constructor, so a property the
 * document does not carry arrives as whatever the parameter type has for "nothing" — and for a primitive
 * {@code int} that is zero. {@code @ModuleConfigField(defaultValue = "64")} is schema and UI metadata that
 * Jackson never reads, so an {@code opcuaToMqtt} element written before {@code eventQueueSize} existed
 * deserialised as a request for a queue of <b>zero</b>: below the {@code numberMin = 1} the same annotation
 * declares, and with none of the burst protection the field was added for.
 * <p>
 * That is the state of every configuration in the field, which is what makes it a migration defect rather
 * than a validation one. The existing suite missed it because its "defaults" fixture omits the
 * {@code opcuaToMqtt} element <em>entirely</em>, and an absent element takes a different path — the outer
 * {@code requireNonNullElse} that substitutes a whole default object, which has always produced 64.
 * <p>
 * Edge is configured through two surfaces — JSON from the REST API and the UI, XML from a hand-authored
 * {@code config.xml} — and this file drives the JSON one directly. The XML side is covered where it belongs,
 * by {@code OpcUaProtocolAdapterConfigTest} against {@code opcua-adapter-full-config.xml}, whose
 * {@code opcuaToMqtt} element carries the two older fields and not this one: the same migration shape, through
 * the real configuration machinery rather than a mapper assembled here.
 */
class OpcUaToMqttConfigDefaultsTest {

    private final @NotNull ObjectMapper json = new ObjectMapper();

    @Test
    void aConfigWrittenBeforeEventsExistedGetsTheDocumentedEventQueue() throws Exception {
        // The finding, in the exact shape a deployed config has: the two fields that predate condition tags
        // are present and the one added with them is not.
        final OpcUaToMqttConfig parsed =
                json.readValue("{\"serverQueueSize\":1,\"publishingInterval\":1000}", OpcUaToMqttConfig.class);

        assertThat(parsed.eventQueueSize())
                .as("64, not 0 -- zero is below the declared minimum and asks the server for its own depth")
                .isEqualTo(OpcUaToMqttConfig.DEFAULT_EVENT_QUEUE_SIZE);
    }

    @Test
    void anEmptyObjectGetsEveryDefault() throws Exception {
        // The two older fields have the same shape and the same latent defect; it has simply never fired
        // because generated configurations always write them. Fixing only the new field would leave the trap
        // set for whichever field is added next.
        final OpcUaToMqttConfig parsed = json.readValue("{}", OpcUaToMqttConfig.class);

        assertThat(parsed.serverQueueSize()).isEqualTo(OpcUaToMqttConfig.DEFAULT_SERVER_QUEUE_SIZE);
        assertThat(parsed.publishingInterval()).isEqualTo(OpcUaToMqttConfig.DEFAULT_PUBLISHING_INTERVAL);
        assertThat(parsed.eventQueueSize()).isEqualTo(OpcUaToMqttConfig.DEFAULT_EVENT_QUEUE_SIZE);
    }

    @Test
    void anExplicitEventQueueSizeIsKept() throws Exception {
        // The control. A default that overrode what someone wrote would be a worse defect than the one being
        // fixed, and both ends of the useful range are pinned.
        assertThat(json.readValue("{\"eventQueueSize\":1}", OpcUaToMqttConfig.class)
                        .eventQueueSize())
                .isEqualTo(1);
        assertThat(json.readValue("{\"eventQueueSize\":128}", OpcUaToMqttConfig.class)
                        .eventQueueSize())
                .isEqualTo(128);
    }

    @Test
    void anExplicitOutOfRangeValueIsLeftAsWritten() throws Exception {
        // A deliberate choice rather than an oversight. numberMin is the declared validation surface and is
        // enforced where configuration is authored; turning a value that has always loaded into a startup
        // failure would be a wider change than this defect calls for. A wrong-but-explicit number is also a
        // different problem from a value nobody wrote -- which is the whole of this finding.
        assertThat(json.readValue("{\"eventQueueSize\":0}", OpcUaToMqttConfig.class)
                        .eventQueueSize())
                .isZero();
    }

    @Test
    void aFullyWrittenConfigSurvivesARoundTrip() throws Exception {
        // Boxing the components changes how this serialises as well as how it parses, and a config that
        // cannot be written back is a defect of its own -- the API reads, edits and re-posts these objects.
        final OpcUaToMqttConfig original = new OpcUaToMqttConfig(13, 12, 96);

        final OpcUaToMqttConfig roundTripped =
                json.readValue(json.writeValueAsString(original), OpcUaToMqttConfig.class);

        assertThat(roundTripped).isEqualTo(original);
    }
}
