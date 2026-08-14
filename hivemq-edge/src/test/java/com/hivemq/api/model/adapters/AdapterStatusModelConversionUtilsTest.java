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
package com.hivemq.api.model.adapters;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.api.model.Status;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Review-09 finding 3: what the API says an adapter's connection is doing.
 * <p>
 * The conversion used to enumerate four members and send everything else to {@code UNKNOWN}. That was
 * harmless for as long as nothing published the fifth: {@code CONNECTING} existed in the SDK enum and no
 * adapter ever set it. When the OPC UA adapter began publishing it truthfully at the start of every attempt,
 * the fall-through turned a normal connect window into {@code UNKNOWN} — which is not a vaguer version of
 * the truth but a different claim, one consumers reasonably read as a fault and the workspace painted red.
 * <p>
 * These tests are about the shape of the mapping rather than any one member, because the defect was never
 * really about {@code CONNECTING}: it was about a {@code default} arm quietly absorbing whatever the SDK
 * added next.
 */
class AdapterStatusModelConversionUtilsTest {

    @ParameterizedTest
    @EnumSource(ProtocolAdapterState.ConnectionStatus.class)
    void everySdkConnectionStatusHasAnApiMemberOfTheSameName(
            final ProtocolAdapterState.ConnectionStatus connectionStatus) {

        // Name equality rather than a hand-written table, so that adding a member to either side without the
        // other fails here. A table would have to be edited by the same person who forgot the switch arm.
        assertThat(AdapterStatusModelConversionUtils.convertConnectionStatus(connectionStatus)
                        .name())
                .isEqualTo(connectionStatus.name());
    }

    @Test
    void connectingIsPublishedAsConnectingRatherThanUnknown() {
        // The regression itself, named. Worth its own test beyond the sweep above because this is the one
        // that shipped, and a reader of the finding should be able to find it by name.
        assertThat(AdapterStatusModelConversionUtils.convertConnectionStatus(
                        ProtocolAdapterState.ConnectionStatus.CONNECTING))
                .isEqualTo(Status.ConnectionEnum.CONNECTING);
    }

    @Test
    void onlyTheGenuinelyUnknownStatusConvertsToUnknown() {
        // The property that made the old default arm a lie: UNKNOWN has to mean the server cannot say. If any
        // other status reaches it, consumers cannot tell "we do not know" from "we did not bother to map it".
        assertThat(Arrays.stream(ProtocolAdapterState.ConnectionStatus.values())
                        .filter(status -> AdapterStatusModelConversionUtils.convertConnectionStatus(status)
                                == Status.ConnectionEnum.UNKNOWN)
                        .toList())
                .containsExactly(ProtocolAdapterState.ConnectionStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(ProtocolAdapterState.RuntimeStatus.class)
    void everyRuntimeStatusConverts(final ProtocolAdapterState.RuntimeStatus runtimeStatus) {
        assertThat(AdapterStatusModelConversionUtils.convertRuntimeStatus(runtimeStatus)
                        .name())
                .isEqualTo(runtimeStatus.name());
    }
}
