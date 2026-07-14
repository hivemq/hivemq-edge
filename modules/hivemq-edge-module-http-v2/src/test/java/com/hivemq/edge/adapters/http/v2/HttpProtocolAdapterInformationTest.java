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
package com.hivemq.edge.adapters.http.v2;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import org.junit.jupiter.api.Test;

class HttpProtocolAdapterInformationTest {

    private final ProtocolAdapterInformation information = HttpProtocolAdapterInformation.INSTANCE;

    @Test
    void protocolIdIsHttpV2() {
        assertThat(information.protocolId()).isEqualTo("http-v2");
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
    void nodeClassIsHttpNode() {
        assertThat(information.nodeClass()).isEqualTo(HttpNode.class);
    }

    @Test
    void categoryIsConnectivity() {
        assertThat(information.category()).isEqualTo(ProtocolAdapterCategory.CONNECTIVITY);
    }

    @Test
    void tagsAreInternetTcpAndWeb() {
        assertThat(information.tags())
                .containsExactly(ProtocolAdapterTag.INTERNET, ProtocolAdapterTag.TCP, ProtocolAdapterTag.WEB);
    }

    @Test
    void logoUrlPointsAtTheHttpIcon() {
        assertThat(information.logoUrl()).isEqualTo("/images/http-icon.png");
    }
}
