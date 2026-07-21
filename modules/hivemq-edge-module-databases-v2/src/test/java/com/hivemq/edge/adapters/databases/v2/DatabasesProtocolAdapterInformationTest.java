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
package com.hivemq.edge.adapters.databases.v2;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import org.junit.jupiter.api.Test;

class DatabasesProtocolAdapterInformationTest {

    private final ProtocolAdapterInformation information = DatabasesProtocolAdapterInformation.INSTANCE;

    @Test
    void protocolIdIsDatabasesV2() {
        assertThat(information.protocolId()).isEqualTo("databases-v2");
    }

    @Test
    void protocolIdContainsNoWhitespaceAndOnlyLowercaseAlphanumericsAndDashes() {
        assertThat(information.protocolId()).matches("^[a-z0-9-]+$");
    }

    @Test
    void versionIsALiteral() {
        assertThat(information.version()).isEqualTo("1.0.0").doesNotContain("${edge-version}");
    }

    @Test
    void capabilitiesAreEmpty() {
        assertThat(information.capabilities()).isEmpty();
    }

    @Test
    void currentConfigVersionIsTwo() {
        assertThat(information.currentConfigVersion()).isEqualTo(2);
    }

    @Test
    void nodeClassIsDatabaseNode() {
        assertThat(information.nodeClass()).isEqualTo(DatabaseNode.class);
    }

    @Test
    void categoryIsConnectivity() {
        assertThat(information.category()).isEqualTo(ProtocolAdapterCategory.CONNECTIVITY);
    }

    @Test
    void tagsAreCarriedOverFromTheV1Adapter() {
        assertThat(information.tags())
                .containsExactly(ProtocolAdapterTag.INTERNET, ProtocolAdapterTag.TCP, ProtocolAdapterTag.AUTOMATION);
    }

    @Test
    void logoUrlPointsAtTheDatabaseImage() {
        assertThat(information.logoUrl()).isEqualTo("/images/database.png");
    }

    @Test
    void authorIsHiveMq() {
        assertThat(information.author()).isEqualTo("HiveMQ");
    }
}
