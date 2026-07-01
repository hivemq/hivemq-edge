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
package com.hivemq.edge.adapters.etherip_cip_odva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonToCipValueTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static CipTag tag(final CipDataType dataType, final int numberOfElements) {
        return new CipTag("t", "t", new CipTagDefinition("@1/2/3", numberOfElements, dataType, 0d, null, 0, null));
    }

    private static com.fasterxml.jackson.databind.JsonNode json(final String raw) throws Exception {
        return MAPPER.readTree(raw);
    }

    @Test
    void scalar_bool() throws Exception {
        assertThat(EthernetIPCipOdvaPollingProtocolAdapter.jsonToCipValue(tag(CipDataType.BOOL, 1), json("true")))
                .isEqualTo(true);
    }

    @Test
    void scalar_int_isLong() throws Exception {
        assertThat(EthernetIPCipOdvaPollingProtocolAdapter.jsonToCipValue(tag(CipDataType.INT, 1), json("519")))
                .isEqualTo(519L);
    }

    @Test
    void scalar_real_isDouble() throws Exception {
        assertThat(EthernetIPCipOdvaPollingProtocolAdapter.jsonToCipValue(tag(CipDataType.REAL, 1), json("1.5")))
                .isEqualTo(1.5d);
    }

    @Test
    void scalar_string_isText() throws Exception {
        assertThat(EthernetIPCipOdvaPollingProtocolAdapter.jsonToCipValue(
                        tag(CipDataType.STRING, 1), json("\"hello\"")))
                .isEqualTo("hello");
    }

    @Test
    void array_int_isListOfLong() throws Exception {
        assertThat(EthernetIPCipOdvaPollingProtocolAdapter.jsonToCipValue(tag(CipDataType.DINT, 3), json("[1,2,3]")))
                .isEqualTo(List.of(1L, 2L, 3L));
    }

    @Test
    void array_butScalarPayload_fails() {
        assertThatThrownBy(() -> EthernetIPCipOdvaPollingProtocolAdapter.jsonToCipValue(
                        tag(CipDataType.DINT, 3), MAPPER.getNodeFactory().numberNode(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void composite_scalar_fails() throws Exception {
        assertThatThrownBy(() -> EthernetIPCipOdvaPollingProtocolAdapter.jsonToCipValue(
                        tag(CipDataType.COMPOSITE, 1), json("1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
