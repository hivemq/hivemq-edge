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
package com.hivemq.edge.adapters.plc4x.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.edge.adapters.plc4x.types.ads.ADSProtocolAdapterInformation;
import com.hivemq.edge.adapters.plc4x.types.siemens.S7ProtocolAdapterInformation;
import com.hivemq.edge.modules.adapters.impl.polling.PollingOutputImpl;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/// Edge takes a cancellation from a PLC4X adapter for a poll not carried out only by the adapter's protocol id:
/// these tests hold Edge's list of those ids to the ids the S7 and ADS adapters report, so a renamed id cannot turn
/// their skipped polls into polling errors unnoticed.
class Plc4xPollNotCarriedOutTest {

    static @NotNull Stream<ProtocolAdapterInformation> plc4xAdapters() {
        return Stream.of(S7ProtocolAdapterInformation.INSTANCE, ADSProtocolAdapterInformation.INSTANCE);
    }

    /// A poll the adapter ended with its own cancellation, as the sampler's future passes it on, is not carried out.
    @ParameterizedTest
    @MethodSource("plc4xAdapters")
    void aCancelledPollOfAPlc4xAdapter_isNotCarriedOut(final @NotNull ProtocolAdapterInformation information) {
        assertThat(PollingOutputImpl.isNotCarriedOut(
                        information.getProtocolId(),
                        new CompletionException(new CancellationException("Too little of the poll's time was left"))))
                .isTrue();
    }

    /// A read the driver cancelled reaches Edge wrapped by the adapter and is a polling error.
    @ParameterizedTest
    @MethodSource("plc4xAdapters")
    void aReadCancelledByTheDriver_isAPollingError(final @NotNull ProtocolAdapterInformation information) {
        assertThat(PollingOutputImpl.isNotCarriedOut(
                        information.getProtocolId(),
                        new IllegalStateException("The read was cancelled", new CancellationException())))
                .isFalse();
    }
}
