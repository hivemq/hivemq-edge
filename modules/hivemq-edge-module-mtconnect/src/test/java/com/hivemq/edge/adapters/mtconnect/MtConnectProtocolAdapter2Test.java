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
package com.hivemq.edge.adapters.mtconnect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapter2;
import com.hivemq.adapter.sdk.api.ProtocolAdapter2Bridge;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
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
class MtConnectProtocolAdapter2Test {

    @Mock
    private @NotNull ProtocolAdapter delegate;

    @Mock
    private @NotNull ModuleServices moduleServices;

    @Mock
    private @NotNull ProtocolAdapterInformation adapterInfo;

    @BeforeEach
    void setUp() {
        when(delegate.getId()).thenReturn("test-mtconnect");
        when(delegate.getProtocolAdapterInformation()).thenReturn(adapterInfo);
    }

    @Test
    void supportsSouthbound_returnsFalse() {
        final MtConnectProtocolAdapter2 adapter = new MtConnectProtocolAdapter2(delegate, moduleServices);
        assertThat(adapter.supportsSouthbound()).isFalse();
    }

    @Test
    void extendsProtocolAdapter2Bridge() {
        final MtConnectProtocolAdapter2 adapter = new MtConnectProtocolAdapter2(delegate, moduleServices);
        assertThat(adapter).isInstanceOf(ProtocolAdapter2Bridge.class);
    }

    @Test
    void delegatesIdToWrappedAdapter() {
        final MtConnectProtocolAdapter2 adapter = new MtConnectProtocolAdapter2(delegate, moduleServices);
        assertThat(adapter.getId()).isEqualTo("test-mtconnect");
    }

    @Test
    void factoryCreatesCorrectType() {
        final MtConnectProtocolAdapterFactory factory = new MtConnectProtocolAdapterFactory();
        final ProtocolAdapter2 result = factory.createProtocolAdapter2(delegate, moduleServices);
        assertThat(result).isInstanceOf(MtConnectProtocolAdapter2.class);
        assertThat(result.supportsSouthbound()).isFalse();
    }
}
