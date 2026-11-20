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
package com.hivemq.edge.adapters.etherip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.etherip.config.EipDataType;
import com.hivemq.edge.adapters.etherip.config.EipSpecificAdapterConfig;
import com.hivemq.edge.adapters.etherip.config.EipToMqttConfig;
import com.hivemq.edge.adapters.etherip.config.tag.EipTag;
import com.hivemq.edge.adapters.etherip.config.tag.EipTagDefinition;
import java.time.Duration;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// Reproduces EDG-1095: the adapter must reconnect after the device was unreachable, both at start-up and while
/// polling. The adapter talks through the real `etherip` library and a real TCP socket to
/// [FakeEtherNetIpDevice], which simulates the outage by closing every connection and refusing new ones.
class EipPollingProtocolAdapterReconnectTest {

    private static final @NotNull String TAG_ADDRESS = "at_int_tag";
    private static final short TAG_VALUE = 42;

    /// Polls that may fail after the device came back before the test gives up. The fixed adapter needs one.
    private static final int POLLS_TO_RECOVER = 3;

    private @NotNull FakeEtherNetIpDevice device;
    private @NotNull EipTag tag;
    private @NotNull EipPollingProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        device = FakeEtherNetIpDevice.createOrSkip(TAG_VALUE);

        final ProtocolAdapterInput<EipSpecificAdapterConfig> input = mock();
        when(input.getAdapterId()).thenReturn("eip-reconnect-test");
        when(input.getConfig())
                .thenReturn(new EipSpecificAdapterConfig(
                        FakeEtherNetIpDevice.PORT,
                        FakeEtherNetIpDevice.HOST,
                        1,
                        0,
                        new EipToMqttConfig(1000, -1, false)));
        tag = new EipTag(TAG_ADDRESS, TAG_ADDRESS, new EipTagDefinition(TAG_ADDRESS, EipDataType.INT));
        when(input.getTags()).thenReturn(List.of(tag));
        when(input.getProtocolAdapterState()).thenReturn(mock(ProtocolAdapterState.class));
        adapter = new EipPollingProtocolAdapter(EipProtocolAdapterInformation.INSTANCE, input);
    }

    @AfterEach
    void tearDown() {
        adapter.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));
        device.close();
    }

    @Test
    void outageWhilePolling_reconnectsWhenDeviceIsBack() {
        device.goOnline();
        startAdapter();
        assertThat(poll()).as("poll before the outage").isEqualTo(PollResult.PUBLISHED);

        device.goOffline();
        assertThat(poll()).as("poll during the outage").isEqualTo(PollResult.FAILED);
        assertThat(poll()).as("second poll during the outage").isEqualTo(PollResult.FAILED);

        device.goOnline();
        assertThat(pollUntilPublished())
                .as("adapter publishes again after the outage")
                .isTrue();
        assertThat(device.sessionCount()).as("sessions the device registered").isEqualTo(2);
    }

    @Test
    void deviceDownAtStart_adapterStartsAndConnectsWhenDeviceIsUp() {
        final ProtocolAdapterStartOutput startOutput = startAdapter();
        verify(startOutput).startedSuccessfully();
        verify(startOutput, never()).failStart(any(), nullable(String.class));
        assertThat(poll()).as("poll while the device is down").isEqualTo(PollResult.FAILED);

        device.goOnline();
        assertThat(pollUntilPublished())
                .as("adapter publishes once the device is up")
                .isTrue();
        assertThat(device.sessionCount()).as("sessions the device registered").isEqualTo(1);
    }

    @Test
    void cipErrorFromDevice_keepsSession() {
        device.goOnline();
        startAdapter();
        device.answerWithCipError(true);
        assertThat(poll()).as("poll answered with a CIP error").isEqualTo(PollResult.FAILED);
        device.answerWithCipError(false);
        assertThat(poll()).as("poll after the CIP error").isEqualTo(PollResult.PUBLISHED);
        assertThat(device.sessionCount()).as("sessions the device registered").isEqualTo(1);
    }

    @Test
    void pollAfterStop_doesNotConnect() throws InterruptedException {
        device.goOnline();
        startAdapter();
        adapter.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));

        assertThat(poll()).as("poll after stop").isEqualTo(PollResult.FAILED);
        assertThat(device.sessionCount()).as("sessions the device registered").isEqualTo(1);
        assertThat(device.awaitNoOpenConnections(Duration.ofSeconds(5)))
                .as("adapter closed every connection")
                .isTrue();
    }

    private @NotNull ProtocolAdapterStartOutput startAdapter() {
        final ProtocolAdapterStartOutput startOutput = mock();
        adapter.start(mock(ProtocolAdapterStartInput.class), startOutput);
        return startOutput;
    }

    private boolean pollUntilPublished() {
        for (int i = 0; i < POLLS_TO_RECOVER; i++) {
            if (poll() == PollResult.PUBLISHED) {
                return true;
            }
        }
        return false;
    }

    /// Polls once, checks that the poll reported exactly one outcome, and returns it.
    private @NotNull PollResult poll() {
        final BatchPollingOutput output = mock(RETURNS_DEEP_STUBS);
        adapter.poll(mock(BatchPollingInput.class), output);
        final DataPointListBuilder publisher = output.dataPointListPublisher();
        final long failCalls = calls(output, "fail");
        final long publishCalls = calls(publisher, "publish");
        assertThat(failCalls + publishCalls).as("outcomes the poll reported").isEqualTo(1);
        if (publishCalls == 0) {
            return PollResult.FAILED;
        }
        verify(publisher.addDataPoint(tag)).value((int) TAG_VALUE);
        return PollResult.PUBLISHED;
    }

    private static long calls(final @NotNull Object mock, final @NotNull String methodName) {
        return mockingDetails(mock).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals(methodName))
                .count();
    }

    private enum PollResult {
        PUBLISHED,
        FAILED
    }
}
