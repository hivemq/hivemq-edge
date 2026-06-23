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
package com.hivemq.protocols.v2.view;

import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.CONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.ERROR;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.STOPPED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTION_RETRY;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_DISCONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_STARTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_STOPPED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_VERIFICATION;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import org.junit.jupiter.api.Test;

/**
 * The machine-state → color fold (design §11.2): every state maps, and each maps to the documented color.
 */
class AdapterStatusColorTest {

    @Test
    void everyMachineStateMapsToAColor() {
        for (final ProtocolAdapterWrapperState state : ProtocolAdapterWrapperState.values()) {
            assertThat(AdapterStatusColor.of(state)).as("color for %s", state).isNotNull();
        }
    }

    @Test
    void mapsEachStateToTheDocumentedColor() {
        assertThat(AdapterStatusColor.of(STOPPED)).isEqualTo(AdapterStatusColor.GREY_STOPPED);
        assertThat(AdapterStatusColor.of(WAITING_FOR_STARTED)).isEqualTo(AdapterStatusColor.YELLOW_CONNECTING);
        assertThat(AdapterStatusColor.of(WAITING_FOR_CONNECTED)).isEqualTo(AdapterStatusColor.YELLOW_CONNECTING);
        assertThat(AdapterStatusColor.of(WAITING_FOR_VERIFICATION)).isEqualTo(AdapterStatusColor.YELLOW_CONNECTING);
        assertThat(AdapterStatusColor.of(CONNECTED)).isEqualTo(AdapterStatusColor.GREEN_CONNECTED);
        assertThat(AdapterStatusColor.of(WAITING_FOR_CONNECTION_RETRY)).isEqualTo(AdapterStatusColor.AMBER_RETRYING);
        assertThat(AdapterStatusColor.of(WAITING_FOR_DISCONNECTED)).isEqualTo(AdapterStatusColor.YELLOW_STOPPING);
        assertThat(AdapterStatusColor.of(WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT))
                .isEqualTo(AdapterStatusColor.YELLOW_STOPPING);
        assertThat(AdapterStatusColor.of(WAITING_FOR_STOPPED)).isEqualTo(AdapterStatusColor.YELLOW_STOPPING);
        assertThat(AdapterStatusColor.of(ERROR)).isEqualTo(AdapterStatusColor.RED_ERROR);
    }
}
