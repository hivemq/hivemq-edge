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
package com.hivemq.protocols.fsm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProtocolAdapterDefaultMethodsTest {

    @Test
    void start_directionOverload_delegatesToTwoArgDefaultPath() {
        final ProtocolAdapterInformation info = mockInfo(Set.of(ProtocolAdapterCapability.READ));
        final RecordingTwoArgAdapter adapter = new RecordingTwoArgAdapter("adapter-1", info);
        final ProtocolAdapterStartOutput output = mock(ProtocolAdapterStartOutput.class);

        adapter.start(ProtocolAdapterConnectionDirection.Southbound, mock(ProtocolAdapterStartInput.class), output);

        assertThat(adapter.start2ArgCalls.get()).isEqualTo(1);
        verify(output).startedSuccessfully();
    }

    @Test
    void stop_directionOverload_delegatesToTwoArgDefaultPath() {
        final ProtocolAdapterInformation info = mockInfo(Set.of(ProtocolAdapterCapability.READ));
        final RecordingTwoArgAdapter adapter = new RecordingTwoArgAdapter("adapter-1", info);
        final ProtocolAdapterStopOutput output = mock(ProtocolAdapterStopOutput.class);

        adapter.stop(ProtocolAdapterConnectionDirection.Southbound, mock(ProtocolAdapterStopInput.class), output);

        assertThat(adapter.stop2ArgCalls.get()).isEqualTo(1);
        verify(output).stoppedSuccessfully();
    }

    @Test
    void precheck_defaultIsNoOp() {
        final ProtocolAdapter adapter =
                new MinimalAdapter("adapter-1", mockInfo(Set.of(ProtocolAdapterCapability.READ)));

        assertThatCode(adapter::precheck).doesNotThrowAnyException();
    }

    @Test
    void baseDefaults_signalSuccessWhenNoLifecycleOverridesExist() {
        final ProtocolAdapter adapter =
                new MinimalAdapter("adapter-1", mockInfo(Set.of(ProtocolAdapterCapability.READ)));
        final ProtocolAdapterStartOutput startOutput = mock(ProtocolAdapterStartOutput.class);
        final ProtocolAdapterStopOutput stopOutput = mock(ProtocolAdapterStopOutput.class);

        adapter.start(
                ProtocolAdapterConnectionDirection.Northbound, mock(ProtocolAdapterStartInput.class), startOutput);
        adapter.stop(ProtocolAdapterConnectionDirection.Northbound, mock(ProtocolAdapterStopInput.class), stopOutput);

        verify(startOutput).startedSuccessfully();
        verify(stopOutput).stoppedSuccessfully();
    }

    private static @NotNull ProtocolAdapterInformation mockInfo(final @NotNull Set<ProtocolAdapterCapability> caps) {
        final ProtocolAdapterInformation info = mock(ProtocolAdapterInformation.class);
        when(info.getCapabilities()).thenReturn(EnumSet.copyOf(caps));
        return info;
    }

    private static class MinimalAdapter implements ProtocolAdapter {
        private final @NotNull String id;
        private final @NotNull ProtocolAdapterInformation info;

        private MinimalAdapter(final @NotNull String id, final @NotNull ProtocolAdapterInformation info) {
            this.id = id;
            this.info = info;
        }

        @Override
        public @NotNull String getId() {
            return id;
        }

        @Override
        public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
            return info;
        }
    }

    private static final class RecordingTwoArgAdapter extends MinimalAdapter {
        private final @NotNull AtomicInteger start2ArgCalls = new AtomicInteger();
        private final @NotNull AtomicInteger stop2ArgCalls = new AtomicInteger();

        private RecordingTwoArgAdapter(final @NotNull String id, final @NotNull ProtocolAdapterInformation info) {
            super(id, info);
        }

        @Override
        public void start(
                final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
            start2ArgCalls.incrementAndGet();
            output.startedSuccessfully();
        }

        @Override
        public void stop(
                final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
            stop2ArgCalls.incrementAndGet();
            output.stoppedSuccessfully();
        }
    }
}
