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
package com.hivemq.edge.adapters.modbus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapter2;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
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
class ModbusProtocolAdapter2Test {

    @Mock
    private @NotNull ProtocolAdapter delegate;

    @Mock
    private @NotNull ModuleServices moduleServices;

    @Mock
    private @NotNull ProtocolAdapterInformation adapterInfo;

    @Mock
    private @NotNull ProtocolAdapterFactoryInput factoryInput;

    @BeforeEach
    void setUp() {
        when(delegate.getId()).thenReturn("test-modbus");
        when(delegate.getProtocolAdapterInformation()).thenReturn(adapterInfo);
    }

    @Test
    void supportsSouthbound_returnsFalse() {
        final ModbusProtocolAdapter2 adapter = new ModbusProtocolAdapter2(delegate, moduleServices);
        assertThat(adapter.supportsSouthbound()).isFalse();
    }

    @Test
    void exposesLegacyAdapterAndModuleServices() {
        final ModbusProtocolAdapter2 adapter = new ModbusProtocolAdapter2(delegate, moduleServices);
        assertThat(adapter.getLegacyAdapter()).isSameAs(delegate);
        assertThat(adapter.getModuleServices()).isSameAs(moduleServices);
    }

    @Test
    void delegatesIdToWrappedAdapter() {
        final ModbusProtocolAdapter2 adapter = new ModbusProtocolAdapter2(delegate, moduleServices);
        assertThat(adapter.getId()).isEqualTo("test-modbus");
    }

    @Test
    void factoryCreatesCorrectType() {
        final ModbusProtocolAdapterFactory factory = new ModbusProtocolAdapterFactory(factoryInput);
        final ProtocolAdapter2 result = factory.createProtocolAdapter2(delegate, moduleServices);
        assertThat(result).isInstanceOf(ModbusProtocolAdapter2.class);
        assertThat(result.supportsSouthbound()).isFalse();
    }
}
