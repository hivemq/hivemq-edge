/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.protocols.fsm;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProtocolAdapterWrapperTest {
    @Mock
    private @NotNull ProtocolAdapter protocolAdapter;

    @BeforeEach
    public void setUp() {
        when(protocolAdapter.getId()).thenReturn("test");
    }

    @Test
    public void whenAdapterIsValid_thenStartAndStopWork() {
        final ProtocolAdapterWrapper2 wrapper = new ProtocolAdapterWrapper2(protocolAdapter);
        assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Stopped);
        assertThat(wrapper.start()).isTrue();
        assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Started);
        assertThat(wrapper.stop(true)).isTrue();
        assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Stopped);
    }
}
