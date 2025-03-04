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

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.edge.adapters.etherip.config.EipDataType;
import com.hivemq.edge.adapters.etherip.config.EipSpecificAdapterConfig;
import com.hivemq.edge.adapters.etherip.config.EipToMqttConfig;
import com.hivemq.edge.adapters.etherip.config.EipToMqttMapping;
import com.hivemq.edge.adapters.etherip.config.tag.EipTag;
import com.hivemq.edge.adapters.etherip.config.tag.EipTagDefinition;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
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
        return Stream.of(Arguments.of(TAG_INT, EipDataType.INT, TAG_INT, 3),
                Arguments.of(TAG_BOOL, EipDataType.BOOL, TAG_BOOL, false),
                Arguments.of(TAG_PROGRAM_BOOL_TRUE, EipDataType.BOOL, TAG_PROGRAM_BOOL_TRUE, true),
                Arguments.of(TAG_PROGRAM_BOOL_FALSE, EipDataType.BOOL, TAG_PROGRAM_BOOL_FALSE, false),
                Arguments.of(TAG_STRING, EipDataType.STRING, TAG_STRING, "test"),
                Arguments.of(TAG_REAL, EipDataType.REAL, TAG_REAL, 5.59));
    }

    @ParameterizedTest
    @MethodSource("tagsToExpectedValues")
    public void test_parameterized(
            final @NotNull String tagAddress,
            final @NotNull EipDataType tagType,
            final @NotNull String expectedName,
            final @NotNull Object expectedValue) {


        final EipToMqttMapping eipToMqttMapping = new EipToMqttMapping("topic",
                1,
                MessageHandlingOptions.MQTTMessagePerTag,
                true,
                true,
                tagAddress,
                List.of());

        final EipSpecificAdapterConfig config =
                new EipSpecificAdapterConfig(44818, HOST, 1, 0, new EipToMqttConfig(1000, 10, true));




        final ProtocolAdapterInput<EipSpecificAdapterConfig> inputMock = mock(ProtocolAdapterInput.class);
        when(inputMock.getConfig()).thenReturn(config);
        when(inputMock.getTags()).thenReturn(List.of(new EipTag(tagAddress,
                tagAddress,
                new EipTagDefinition(tagAddress, tagType))));

        final BatchPollingInput input = mock(BatchPollingInput.class);
        final List eipToMqttMapping1 = List.of(eipToMqttMapping);

        final BatchPollingOutput output = mock();

        final EipPollingProtocolAdapter adapter =
                new EipPollingProtocolAdapter(EipProtocolAdapterInformation.INSTANCE, inputMock);

        adapter.start(null, mock(ProtocolAdapterStartOutput.class));

        final ArgumentCaptor<String> captorName = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Object> captorValue = ArgumentCaptor.forClass(Object.class);

        adapter.poll(input, output);
        verify(output).addDataPoint(captorName.capture(), captorValue.capture());

        assertThat(captorName.getAllValues()).first().isEqualTo(expectedName);
        if (expectedValue instanceof Double) {
            assertThat(captorValue.getValue()).isInstanceOf(Double.class)
                    .asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                    .isEqualTo((Double) expectedValue, withPrecision(2d));
        } else {
            assertThat(captorValue.getValue()).isEqualTo(expectedValue);
        }
    }

    @Test
    public void test_PublishChangedDataOnly_False() {
        final EipToMqttMapping eipToMqttMapping = new EipToMqttMapping("topic",
                1,
                MessageHandlingOptions.MQTTMessagePerTag,
                true,
                true,
                TAG_INT,
                List.of());

        final EipSpecificAdapterConfig config =
                new EipSpecificAdapterConfig(44818, HOST, 1, 0, new EipToMqttConfig(1000, 10, false));




        final ProtocolAdapterInput<EipSpecificAdapterConfig> inputMock = mock(ProtocolAdapterInput.class);
        when(inputMock.getConfig()).thenReturn(config);
        when(inputMock.getTags()).thenReturn(List.of(new EipTag(TAG_INT,
                TAG_INT,
                new EipTagDefinition(TAG_INT, EipDataType.INT))));

        final BatchPollingInput input = mock();

        final BatchPollingOutput output = mock();
        final ArgumentCaptor<String> captorName = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Object> captorValue = ArgumentCaptor.forClass(Object.class);

        final EipPollingProtocolAdapter adapter =
                new EipPollingProtocolAdapter(EipProtocolAdapterInformation.INSTANCE, inputMock);

        adapter.start(null, mock(ProtocolAdapterStartOutput.class));

        adapter.poll(input, output);
        adapter.poll(input, output);

        verify(output, times(2)).addDataPoint(captorName.capture(), captorValue.capture());

        assertThat(captorName.getAllValues()).allMatch(n -> n.equals(TAG_INT));
        assertThat(captorValue.getAllValues()).allMatch(v -> v.equals(3));
    }
}
