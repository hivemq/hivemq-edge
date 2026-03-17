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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import java.util.EnumSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProtocolAdapter2BridgeTest {

    @Mock
    private @NotNull ProtocolAdapter delegate;

    @Mock
    private @NotNull ModuleServices moduleServices;

    @Mock
    private @NotNull ProtocolAdapterInformation adapterInfo;

    private @NotNull ProtocolAdapter2Bridge bridge;

    @BeforeEach
    void setUp() {
        when(delegate.getId()).thenReturn("test-adapter");
        when(delegate.getProtocolAdapterInformation()).thenReturn(adapterInfo);
        when(adapterInfo.getCapabilities()).thenReturn(EnumSet.of(ProtocolAdapterCapability.READ));
        bridge = new ProtocolAdapter2Bridge(delegate, moduleServices);
    }

    @Test
    void getId_delegatesToOldAdapter() {
        assertThat(bridge.getId()).isEqualTo("test-adapter");
    }

    @Test
    void getProtocolAdapterInformation_delegatesToOldAdapter() {
        assertThat(bridge.getProtocolAdapterInformation()).isSameAs(adapterInfo);
    }

    @Test
    void supportsSouthbound_falseWhenNoWriteCapability() {
        when(adapterInfo.getCapabilities()).thenReturn(EnumSet.of(ProtocolAdapterCapability.READ));
        assertThat(bridge.supportsSouthbound()).isFalse();
    }

    @Test
    void supportsSouthbound_trueWhenWriteCapability() {
        when(adapterInfo.getCapabilities())
                .thenReturn(EnumSet.of(ProtocolAdapterCapability.READ, ProtocolAdapterCapability.WRITE));
        assertThat(bridge.supportsSouthbound()).isTrue();
    }

    @Test
    void precheck_isNoOp() throws ProtocolAdapterException {
        bridge.precheck();
        // No exception, no interaction with delegate
    }

    @Test
    void connectNorthbound_callsDelegateStartAndBlocksUntilSuccess() throws ProtocolAdapterException {
        doAnswer(invocation -> {
                    final ProtocolAdapterStartOutput output = invocation.getArgument(1);
                    output.startedSuccessfully();
                    return null;
                })
                .when(delegate)
                .start(any(ProtocolAdapterStartInput.class), any(ProtocolAdapterStartOutput.class));

        bridge.connect(ConnectionContext.of(ConnectionContext.Direction.NORTHBOUND));

        verify(delegate).start(any(ProtocolAdapterStartInput.class), any(ProtocolAdapterStartOutput.class));
    }

    @Test
    void connectNorthbound_throwsWhenDelegateStartFails() {
        doAnswer(invocation -> {
                    final ProtocolAdapterStartOutput output = invocation.getArgument(1);
                    output.failStart(new RuntimeException("connection refused"), "Connection refused");
                    return null;
                })
                .when(delegate)
                .start(any(ProtocolAdapterStartInput.class), any(ProtocolAdapterStartOutput.class));

        assertThatThrownBy(() -> bridge.connect(ConnectionContext.of(ConnectionContext.Direction.NORTHBOUND)))
                .isInstanceOf(ProtocolAdapterException.class)
                .hasMessageContaining("test-adapter");
    }

    @Test
    void connectSouthbound_doesNotCallDelegateStart() throws ProtocolAdapterException {
        bridge.connect(ConnectionContext.of(ConnectionContext.Direction.SOUTHBOUND));

        verify(delegate, never()).start(any(), any());
    }

    @Test
    void disconnectNorthbound_callsDelegateStopAndBlocksUntilSuccess() {
        doAnswer(invocation -> {
                    final ProtocolAdapterStopOutput output = invocation.getArgument(1);
                    output.stoppedSuccessfully();
                    return null;
                })
                .when(delegate)
                .stop(any(ProtocolAdapterStopInput.class), any(ProtocolAdapterStopOutput.class));

        bridge.disconnect(ConnectionContext.of(ConnectionContext.Direction.NORTHBOUND));

        verify(delegate).stop(any(ProtocolAdapterStopInput.class), any(ProtocolAdapterStopOutput.class));
    }

    @Test
    void disconnectNorthbound_logsWarningWhenDelegateStopFails() {
        doAnswer(invocation -> {
                    final ProtocolAdapterStopOutput output = invocation.getArgument(1);
                    output.failStop(new RuntimeException("stop error"), "Stop error");
                    return null;
                })
                .when(delegate)
                .stop(any(ProtocolAdapterStopInput.class), any(ProtocolAdapterStopOutput.class));

        // Should not throw - disconnect logs errors instead
        bridge.disconnect(ConnectionContext.of(ConnectionContext.Direction.NORTHBOUND));

        verify(delegate).stop(any(ProtocolAdapterStopInput.class), any(ProtocolAdapterStopOutput.class));
    }

    @Test
    void disconnectSouthbound_doesNotCallDelegateStop() {
        bridge.disconnect(ConnectionContext.of(ConnectionContext.Direction.SOUTHBOUND));

        verify(delegate, never()).stop(any(), any());
    }

    @Test
    void destroy_delegatesToOldAdapter() {
        bridge.destroy();

        verify(delegate).destroy();
    }

    @Test
    void connectNorthbound_passesModuleServicesThroughInput() throws ProtocolAdapterException {
        doAnswer(invocation -> {
                    final ProtocolAdapterStartInput input = invocation.getArgument(0);
                    assertThat(input.moduleServices()).isSameAs(moduleServices);
                    final ProtocolAdapterStartOutput output = invocation.getArgument(1);
                    output.startedSuccessfully();
                    return null;
                })
                .when(delegate)
                .start(any(ProtocolAdapterStartInput.class), any(ProtocolAdapterStartOutput.class));

        bridge.connect(ConnectionContext.of(ConnectionContext.Direction.NORTHBOUND));
    }
}
