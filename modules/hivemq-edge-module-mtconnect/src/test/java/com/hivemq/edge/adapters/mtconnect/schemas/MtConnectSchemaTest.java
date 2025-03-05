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

import com.hivemq.edge.adapters.mtconnect.schemas.devices.devices_1_3.MTConnectDevicesType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class MtConnectSchemaTest {
    @Test
    public void whenGetUnmarshallerIsCallForDevices_1_3_thenXmlValidationShouldPass() throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(MTConnectDevicesType.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        try (StringReader stringReader = new StringReader(IOUtils.resourceToString(
                "/smstestbed/volatile-data-stream-schema.xml",
                StandardCharsets.UTF_8))) {
            JAXBElement<?> element = (JAXBElement<?>) unmarshaller.unmarshal(stringReader);
            assertThat(element.getValue()).isNotNull();
            MTConnectDevicesType mtConnectDevicesType = (MTConnectDevicesType) element.getValue();
            assertThat(mtConnectDevicesType).isNotNull();
            assertThat(mtConnectDevicesType.getDevices().getDevice()).hasSize(8);
        }
    }

}
