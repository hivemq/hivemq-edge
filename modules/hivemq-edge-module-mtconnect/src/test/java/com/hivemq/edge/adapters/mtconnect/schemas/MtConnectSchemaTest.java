/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.mtconnect.schemas;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MtConnectSchemaTest {
    @Test
    public void whenInputXmlIsDevices_1_1_thenXmlValidationShouldPass() throws Exception {
        final Unmarshaller unmarshaller = MtConnectSchema.Devices_1_1.getUnmarshaller();
        try (final StringReader stringReader = new StringReader(IOUtils.resourceToString("/devices/devices-1-1.xml",
                StandardCharsets.UTF_8))) {
            final JAXBElement<?> element =
                    (JAXBElement<?>) Objects.requireNonNull(unmarshaller).unmarshal(stringReader);
            assertThat(element.getValue()).isNotNull();
            final com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_1.MTConnectDevicesType
                    mtConnectDevicesType =
                    (com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_1.MTConnectDevicesType) element.getValue();
            assertThat(mtConnectDevicesType).isNotNull();
            assertThat(mtConnectDevicesType.getDevices().getDevice()).hasSize(3);
        }
    }

    @Test
    public void whenInputXmlIsIncorrectDevices_1_3_smstestbed_thenXmlValidationShouldFailed() throws Exception {
        final Unmarshaller unmarshaller = MtConnectSchema.Devices_1_3.getUnmarshaller();
        String xmlString = IOUtils.resourceToString("/devices/devices-1-3-smstestbed.xml", StandardCharsets.UTF_8);
        xmlString = xmlString.replace("<Header ", "<Test ");
        try (final StringReader stringReader = new StringReader(xmlString)) {
            assertThatThrownBy(() -> Objects.requireNonNull(unmarshaller).unmarshal(stringReader)).isInstanceOf(
                    JAXBException.class).hasMessageStartingWith("unexpected element");
        }
    }

    @Test
    public void whenInputXmlIsDevices_1_3_smstestbed_thenXmlValidationShouldPass() throws Exception {
        final Unmarshaller unmarshaller = MtConnectSchema.Devices_1_3.getUnmarshaller();
        try (final StringReader stringReader = new StringReader(IOUtils.resourceToString(
                "/devices/devices-1-3-smstestbed.xml",
                StandardCharsets.UTF_8))) {
            final JAXBElement<?> element =
                    (JAXBElement<?>) Objects.requireNonNull(unmarshaller).unmarshal(stringReader);
            assertThat(element.getValue()).isNotNull();
            final com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_3.MTConnectDevicesType
                    mtConnectDevicesType =
                    (com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_3.MTConnectDevicesType) element.getValue();
            assertThat(mtConnectDevicesType).isNotNull();
            assertThat(mtConnectDevicesType.getDevices().getDevice()).hasSize(8);
        }
    }

    @Test
    public void whenInputXmlIsDevices_1_4_thenSchemaLocationShouldBeDetected() throws Exception {
        final String schemaLocation = MtConnectSchema.extractSchemaLocation(IOUtils.resourceToString(
                "/devices/devices-1-4.xml",
                StandardCharsets.UTF_8));
        assertThat(schemaLocation).isNotNull();
        final MtConnectSchema mtConnectSchema = MtConnectSchema.of(schemaLocation);
        assertThat(mtConnectSchema).isNotNull().isEqualTo(MtConnectSchema.Devices_1_4);
    }

    @Test
    public void whenInputXmlIsDevices_1_4_thenXmlValidationShouldPass() throws Exception {
        final Unmarshaller unmarshaller = MtConnectSchema.Devices_1_4.getUnmarshaller();
        try (final StringReader stringReader = new StringReader(IOUtils.resourceToString("/devices/devices-1-4.xml",
                StandardCharsets.UTF_8))) {
            final JAXBElement<?> element =
                    (JAXBElement<?>) Objects.requireNonNull(unmarshaller).unmarshal(stringReader);
            assertThat(element.getValue()).isNotNull();
            final com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_4.MTConnectDevicesType
                    mtConnectDevicesType =
                    (com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_4.MTConnectDevicesType) element.getValue();
            assertThat(mtConnectDevicesType).isNotNull();
            assertThat(mtConnectDevicesType.getDevices().getDevice()).hasSize(1);
        }
    }

    @Test
    public void whenInputXmlIsStreams_1_3_thenXmlValidationShouldPass() throws Exception {
        final Unmarshaller unmarshaller = MtConnectSchema.Streams_1_3.getUnmarshaller();
        try (final StringReader stringReader = new StringReader(IOUtils.resourceToString("/streams/streams-1-3.xml",
                StandardCharsets.UTF_8))) {
            final JAXBElement<?> element = (JAXBElement<?>) unmarshaller.unmarshal(stringReader);
            assertThat(element.getValue()).isNotNull();
            final com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_3.MTConnectStreamsType
                    mtConnectStreamsType =
                    (com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_3.MTConnectStreamsType) element.getValue();
            assertThat(mtConnectStreamsType).isNotNull();
            assertThat(mtConnectStreamsType.getStreams().getDeviceStream()).hasSize(1);
            assertThat(mtConnectStreamsType.getStreams().getDeviceStream().get(0).getComponentStream()).hasSize(17);
        }
    }

    @Test
    public void whenInputXmlIsStreams_1_4_thenSchemaLocationShouldBeDetected() throws Exception {
        final String schemaLocation = MtConnectSchema.extractSchemaLocation(IOUtils.resourceToString(
                "/streams/streams-1-4.xml",
                StandardCharsets.UTF_8));
        assertThat(schemaLocation).isNotNull();
        final MtConnectSchema mtConnectSchema = MtConnectSchema.of(schemaLocation);
        assertThat(mtConnectSchema).isNotNull().isEqualTo(MtConnectSchema.Streams_1_4);
    }

    @Test
    public void whenInputXmlIsStreams_1_4_thenXmlValidationShouldPass() throws Exception {
        final Unmarshaller unmarshaller = MtConnectSchema.Streams_1_4.getUnmarshaller();
        try (final StringReader stringReader = new StringReader(IOUtils.resourceToString("/streams/streams-1-4.xml",
                StandardCharsets.UTF_8))) {
            final JAXBElement<?> element = (JAXBElement<?>) unmarshaller.unmarshal(stringReader);
            assertThat(element.getValue()).isNotNull();
            final com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_4.MTConnectStreamsType
                    mtConnectStreamsType =
                    (com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_4.MTConnectStreamsType) element.getValue();
            assertThat(mtConnectStreamsType).isNotNull();
            assertThat(mtConnectStreamsType.getStreams().getDeviceStream()).hasSize(1);
            assertThat(mtConnectStreamsType.getStreams().getDeviceStream().get(0).getComponentStream()).hasSize(3);
        }
    }

    @Test
    public void whenInputXmlIsStreams_1_5_thenXmlValidationShouldPass() throws Exception {
        final Unmarshaller unmarshaller = MtConnectSchema.Streams_1_5.getUnmarshaller();
        try (final StringReader stringReader = new StringReader(IOUtils.resourceToString("/streams/streams-1-5.xml",
                StandardCharsets.UTF_8))) {
            final JAXBElement<?> element = (JAXBElement<?>) unmarshaller.unmarshal(stringReader);
            assertThat(element.getValue()).isNotNull();
            final com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_5.MTConnectStreamsType
                    mtConnectStreamsType =
                    (com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_1_5.MTConnectStreamsType) element.getValue();
            assertThat(mtConnectStreamsType).isNotNull();
            assertThat(mtConnectStreamsType.getStreams().getDeviceStream()).hasSize(1);
            assertThat(mtConnectStreamsType.getStreams().getDeviceStream().get(0).getComponentStream()).hasSize(4);
        }
    }

    @Test
    public void whenInputXmlIsStreams_2_0_thenXmlValidationShouldPass() throws Exception {
        final Unmarshaller unmarshaller = MtConnectSchema.Streams_2_0.getUnmarshaller();
        try (final StringReader stringReader = new StringReader(IOUtils.resourceToString("/streams/streams-2-0.xml",
                StandardCharsets.UTF_8))) {
            final JAXBElement<?> element = (JAXBElement<?>) unmarshaller.unmarshal(stringReader);
            assertThat(element.getValue()).isNotNull();
            final com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_0.MTConnectStreamsType
                    mtConnectStreamsType =
                    (com.hivemq.edge.adapters.mtconnect.schemas.streams.streams_2_0.MTConnectStreamsType) element.getValue();
            assertThat(mtConnectStreamsType).isNotNull();
            assertThat(mtConnectStreamsType.getStreams().getDeviceStream()).hasSize(2);
            assertThat(mtConnectStreamsType.getStreams().getDeviceStream().get(0).getComponentStream()).hasSize(2);
        }
    }
}
