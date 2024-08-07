package com.hivemq.edge.adapters.etherip;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.edge.adapters.etherip.model.EtherIpAdapterConfig;
import com.hivemq.edge.adapters.etherip.model.EtherIpDataTypes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.hivemq.edge.adapters.etherip.Constants.TAG_REQUIRES_VPN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag(TAG_REQUIRES_VPN)
public class EtherIpPollingProtocolAdapterIT {

    private static String programTag = "program:MainProgram.test_tag";
    private static String controllerTag = "dev_int_tag";

    @Test
    public void test() {
        EtherIpAdapterConfig config = mock(EtherIpAdapterConfig.class);
        when(config.getHost()).thenReturn("172.16.10.60");
        when(config.getSlot()).thenReturn(0);

        ProtocolAdapterInput<EtherIpAdapterConfig> inputMock = mock(ProtocolAdapterInput.class);
        when(inputMock.getConfig()).thenReturn(config);

        EtherIpAdapterConfig.EIPPollingContextImpl ctx = mock(EtherIpAdapterConfig.EIPPollingContextImpl.class);
        when(ctx.getTagAddress()).thenReturn(controllerTag);
        when(ctx.getDataType()).thenReturn(EtherIpDataTypes.DATA_TYPE.INT);

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

        assertThat(captorName.getAllValues()).first().isEqualTo("dev_int_tag:INT");
        assertThat(captorValue.getAllValues()).first().isEqualTo((short)3);
    }
}
