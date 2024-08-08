package com.hivemq.edge.adapters.etherip;

import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.edge.adapters.etherip.model.EtherIpAdapterConfig;
import com.hivemq.edge.adapters.etherip.model.EtherIpDataTypes;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.stream.Stream;

import static com.hivemq.edge.adapters.etherip.Constants.TAG_REQUIRES_VPN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withPrecision;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag(TAG_REQUIRES_VPN)
public class EtherIpPollingProtocolAdapterIT {

    private static String TAG_INT = "at_int_tag";
    private static String TAG_BOOL = "at_bool_tag";
    private static String TAG_PROGRAM_BOOL_TRUE = "program:MainProgram.dev_bool_tag_t";
    private static String TAG_PROGRAM_BOOL_FALSE = "program:MainProgram.dev_bool_tag_f";
    private static String TAG_REAL = "at_real_tag";
    private static String TAG_STRING = "at_string_tag";

    private static final String HOST = "172.16.10.60";

    public static Stream<Arguments> tagsToExpectedValues() {
        return Stream.of(
                Arguments.of(TAG_INT, EtherIpDataTypes.DATA_TYPE.INT, TAG_INT + ":INT", 3),
                Arguments.of(TAG_BOOL, EtherIpDataTypes.DATA_TYPE.BOOL, TAG_BOOL + ":BOOL", true),
                Arguments.of(TAG_PROGRAM_BOOL_TRUE, EtherIpDataTypes.DATA_TYPE.BOOL, TAG_PROGRAM_BOOL_TRUE + ":BOOL", true),
                Arguments.of(TAG_PROGRAM_BOOL_FALSE, EtherIpDataTypes.DATA_TYPE.BOOL, TAG_PROGRAM_BOOL_FALSE + ":BOOL", false),
                Arguments.of(TAG_STRING, EtherIpDataTypes.DATA_TYPE.STRING, TAG_STRING + ":STRING", "test"),
                Arguments.of(TAG_REAL, EtherIpDataTypes.DATA_TYPE.REAL, TAG_REAL + ":REAL", 5.59)
        );
    }

    @ParameterizedTest
    @MethodSource("tagsToExpectedValues")
    public void test_parameterized(String tagAddress, EtherIpDataTypes.DATA_TYPE tagType, String expectedName, Object expectedValue) {
        EtherIpAdapterConfig config = mock(EtherIpAdapterConfig.class);
        when(config.getHost()).thenReturn(HOST);
        when(config.getSlot()).thenReturn(0);

        ProtocolAdapterInput<EtherIpAdapterConfig> inputMock = mock(ProtocolAdapterInput.class);
        when(inputMock.getConfig()).thenReturn(config);

        EtherIpAdapterConfig.EIPPollingContextImpl ctx = mock(EtherIpAdapterConfig.EIPPollingContextImpl.class);
        when(ctx.getTagAddress()).thenReturn(tagAddress);
        when(ctx.getDataType()).thenReturn(tagType);

        PollingInput<EtherIpAdapterConfig.PollingContextImpl> input = mock(PollingInput.class);
        when(input.getPollingContext()).thenReturn(ctx);

        PollingOutput output = mock(PollingOutput.class);

        EtherIpPollingProtocolAdapter adapter = new EtherIpPollingProtocolAdapter(
                new EtherIpProtocolAdapterInformation(),
                inputMock);

        adapter.start(null, mock(ProtocolAdapterStartOutput.class));

        ArgumentCaptor<String> captorName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> captorValue = ArgumentCaptor.forClass(Object.class);

        adapter.poll(input, output);
        verify(output).addDataPoint(captorName.capture(), captorValue.capture());

        assertThat(captorName.getAllValues()).first().isEqualTo(expectedName);
        if (expectedValue instanceof Double) {
            assertThat(captorValue.getAllValues())
                    .first()
                    .isInstanceOf(Double.class)
                    .asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                    .isEqualTo((Double) expectedValue, withPrecision(2d));
        } else {
            assertThat(captorValue.getAllValues())
                    .first()
                    .isEqualTo(expectedValue);
        }
    }
}
