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
package com.hivemq.edge.adapters.snmp;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.adapters.snmp.config.SnmpSpecificAdapterConfig;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;

/**
 * Table-driven unit tests for SnmpClient.convertVariable(), covering every supported SNMP4J type.
 */
class SnmpClientConvertVariableTest {

    static Stream<Arguments> variableConversions() {
        return Stream.of(
                // Integer32 → int
                Arguments.of(new Integer32(42), 42),
                Arguments.of(new Integer32(-1), -1),
                // Counter32 → long
                Arguments.of(new Counter32(0), 0L),
                Arguments.of(new Counter32(4294967295L), 4294967295L),
                // Counter64 → long
                Arguments.of(new Counter64(Long.MAX_VALUE), Long.MAX_VALUE),
                // Gauge32 → long
                Arguments.of(new Gauge32(100), 100L),
                // TimeTicks → double (hundredths-of-second → seconds)
                Arguments.of(new TimeTicks(0), 0.0),
                Arguments.of(new TimeTicks(100), 1.0),
                Arguments.of(new TimeTicks(123456), 1234.56),
                // IpAddress → dotted-decimal String
                Arguments.of(new IpAddress("192.168.1.1"), "192.168.1.1"),
                // OID → dotted-decimal String
                Arguments.of(new OID("1.3.6.1.2.1.1.5.0"), "1.3.6.1.2.1.1.5.0"),
                // OctetString printable → String
                Arguments.of(new OctetString("HiveMQ"), "HiveMQ"),
                // OctetString non-printable → hex
                Arguments.of(new OctetString(new byte[] {0x00, 0x01, (byte) 0xFF}), "00:01:ff"));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("variableConversions")
    void convertVariable_mapsToExpectedJavaType(final Variable input, final Object expected) throws IOException {
        final SnmpSpecificAdapterConfig config = SnmpSpecificAdapterConfig.forV2c("127.0.0.1", 161, "public", 1000, 0);
        final SnmpClient client = new SnmpClient(config);
        // convertVariable is package-private — accessible from the same package
        assertThat(client.convertVariable(input)).isEqualTo(expected);
    }
}
