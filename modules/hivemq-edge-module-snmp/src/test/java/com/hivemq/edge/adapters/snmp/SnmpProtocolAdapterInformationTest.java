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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hivemq.edge.adapters.snmp.config.SnmpSpecificAdapterConfig;
import com.hivemq.edge.adapters.snmp.config.tag.SnmpTag;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SnmpProtocolAdapterInformationTest {

    private final SnmpProtocolAdapterInformation information = new SnmpProtocolAdapterInformation();

    @Test
    void getProtocolId_mustNotContainWhitespace() {
        assertFalse(information.getProtocolId().contains(" "));
    }

    @Test
    void getProtocolId_mustBeAlphanumericOrUnderscore() {
        final Pattern alphaNumPattern = Pattern.compile("[A-Za-z0-9_]*");
        assertTrue(alphaNumPattern.matcher(information.getProtocolId()).matches());
    }

    @Test
    void getDisplayName_mustNotBeBlank() {
        assertFalse(information.getDisplayName().isBlank());
    }

    @Test
    void getDescription_mustNotBeBlank() {
        assertFalse(information.getDescription().isBlank());
    }

    @Test
    void tagConfigurationClass_mustBeSnmpTag() {
        assertTrue(SnmpTag.class.isAssignableFrom(information.tagConfigurationClass()));
    }

    @Test
    void configurationClass_mustBeSnmpSpecificAdapterConfig() {
        assertTrue(SnmpSpecificAdapterConfig.class.isAssignableFrom(information.configurationClassNorthbound()));
    }

    @Test
    void getUiSchema_mustLoadFromResources() {
        // UI schema JSON must be present on the classpath and non-empty
        final String uiSchema = information.getUiSchema();
        assertNotNull(uiSchema, "UI schema resource must be present on the classpath");
        assertFalse(uiSchema.isBlank());
    }
}
