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

import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.edge.adapters.etherip.config.EipAdapterConfig;
import com.hivemq.edge.adapters.etherip.config.EipDataType;
import com.hivemq.edge.adapters.etherip.config.EipToMqttMapping;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.stream.Stream;

import static com.hivemq.edge.adapters.etherip.Constants.TAG_REQUIRES_VPN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withPrecision;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag(TAG_REQUIRES_VPN)
public class EipPollingProtocolAdapterIT {

    private static final @NotNull String TAG_INT = "at_int_tag";
    private static final @NotNull String TAG_BOOL = "at_bool_tag";
    private static final @NotNull String TAG_PROGRAM_BOOL_TRUE = "program:MainProgram.dev_bool_tag_t";
    private static final @NotNull String TAG_PROGRAM_BOOL_FALSE = "program:MainProgram.dev_bool_tag_f";
    private static final @NotNull String TAG_REAL = "at_real_tag";
    private static final @NotNull String TAG_STRING = "at_string_tag";

    private static final @NotNull String HOST = "172.16.10.60";

    public static Stream<Arguments> tagsToExpectedValues() {
        return Stream.of(
                Arguments.of(TAG_INT, EipDataType.INT, TAG_INT + ":INT", 3),
                Arguments.of(TAG_BOOL, EipDataType.BOOL, TAG_BOOL + ":BOOL", false),
                Arguments.of(TAG_PROGRAM_BOOL_TRUE, EipDataType.BOOL, TAG_PROGRAM_BOOL_TRUE + ":BOOL", true),
                Arguments.of(TAG_PROGRAM_BOOL_FALSE, EipDataType.BOOL, TAG_PROGRAM_BOOL_FALSE + ":BOOL", false),
                Arguments.of(TAG_STRING, EipDataType.STRING, TAG_STRING + ":STRING", "test"),
                Arguments.of(TAG_REAL, EipDataType.REAL, TAG_REAL + ":REAL", 5.59)
        );
    }

    @ParameterizedTest
    @MethodSource("tagsToExpectedValues")
    public void test_parameterized(@NotNull String tagAddress, @NotNull EipDataType tagType, @NotNull String expectedName, @NotNull Object expectedValue) {
        EipAdapterConfig config = mock(EipAdapterConfig.class);
        when(config.getHost()).thenReturn(HOST);
        when(config.getSlot()).thenReturn(0);

        ProtocolAdapterInput<EipAdapterConfig> inputMock = mock(ProtocolAdapterInput.class);
        when(inputMock.getConfig()).thenReturn(config);

        EipToMqttMapping ctx = mock(EipToMqttMapping.class);
        when(ctx.getDataType()).thenReturn(tagType);

        PollingInput<EipToMqttMapping> input = mock(PollingInput.class);
        when(input.getPollingContext()).thenReturn(ctx);

        PollingOutput output = mock(PollingOutput.class);

        EipPollingProtocolAdapter adapter = new EipPollingProtocolAdapter(
                EipProtocolAdapterInformation.INSTANCE,
                inputMock);

        adapter.start(null, mock(ProtocolAdapterStartOutput.class));

        ArgumentCaptor<String> captorName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> captorValue = ArgumentCaptor.forClass(Object.class);

        adapter.poll(input, output);
        verify(output).addDataPoint(captorName.capture(), captorValue.capture());

        assertThat(captorName.getAllValues()).first().isEqualTo(expectedName);
        if (expectedValue instanceof Double) {
            assertThat(captorValue.getValue())
                    .isInstanceOf(Double.class)
                    .asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                    .isEqualTo((Double) expectedValue, withPrecision(2d));
        } else {
            assertThat(captorValue.getValue())
                    .isEqualTo(expectedValue);
        }
    }

    @Test
    public void test_PublishChangedDataOnly_False() {
        EipAdapterConfig config = mock(EipAdapterConfig.class);
        when(config.getHost()).thenReturn(HOST);
        when(config.getSlot()).thenReturn(0);

        ProtocolAdapterInput<EipAdapterConfig> inputMock = mock(ProtocolAdapterInput.class);
        when(inputMock.getConfig()).thenReturn(config);

        EipToMqttMapping ctx = mock(EipToMqttMapping.class);
        when(ctx.getDataType()).thenReturn(EipDataType.INT);

        PollingInput<EipToMqttMapping> input = mock(PollingInput.class);
        when(input.getPollingContext()).thenReturn(ctx);

        PollingOutput output = mock(PollingOutput.class);

        EipPollingProtocolAdapter adapter = new EipPollingProtocolAdapter(
                EipProtocolAdapterInformation.INSTANCE,
                inputMock);

        adapter.start(null, mock(ProtocolAdapterStartOutput.class));

        ArgumentCaptor<String> captorName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> captorValue = ArgumentCaptor.forClass(Object.class);

        adapter.poll(input, output);
        adapter.poll(input, output);
        verify(output, times(2)).addDataPoint(captorName.capture(), captorValue.capture());

        assertThat(captorName.getAllValues()).allMatch(n -> n.equals(TAG_INT + ":INT"));
        assertThat(captorValue.getAllValues()).allMatch(v -> v.equals(3));
    }

    @Test
    public void test_PublishChangedDataOnly_True() {
        EipAdapterConfig config = mock(EipAdapterConfig.class);
        when(config.getHost()).thenReturn(HOST);
        when(config.getSlot()).thenReturn(0);
        when(config.getEipToMqttConfig().getPublishChangedDataOnly()).thenReturn(true);

        ProtocolAdapterInput<EipAdapterConfig> inputMock = mock(ProtocolAdapterInput.class);
        when(inputMock.getConfig()).thenReturn(config);

        EipToMqttMapping ctx = mock(EipToMqttMapping.class);
        when(ctx.getDataType()).thenReturn(EipDataType.INT);

        PollingInput<EipToMqttMapping> input = mock(PollingInput.class);
        when(input.getPollingContext()).thenReturn(ctx);

        PollingOutput output = mock(PollingOutput.class);

        EipPollingProtocolAdapter adapter = new EipPollingProtocolAdapter(
                EipProtocolAdapterInformation.INSTANCE,
                inputMock);

        adapter.start(mock(), mock(ProtocolAdapterStartOutput.class));

        ArgumentCaptor<String> captorName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> captorValue = ArgumentCaptor.forClass(Object.class);

        adapter.poll(input, output);
        adapter.poll(input, output);
        verify(output, times(1)).addDataPoint(captorName.capture(), captorValue.capture());

        assertThat(captorName.getAllValues()).hasSize(1);
        assertThat(captorName.getValue()).isEqualTo(TAG_INT + ":INT");
        assertThat(captorValue.getAllValues()).hasSize(1);
        assertThat(captorValue.getValue()).isEqualTo(3);
    }
}
