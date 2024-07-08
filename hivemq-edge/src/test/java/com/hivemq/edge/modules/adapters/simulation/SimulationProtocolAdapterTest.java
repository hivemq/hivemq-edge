package com.hivemq.edge.modules.adapters.simulation;

import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSampleImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.PollingInputImpl;
import com.hivemq.protocols.PollingOutputImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
class SimulationProtocolAdapterTest {

    private final @NotNull ProtocolAdapterInput input = mock();
    private final @NotNull SimulationAdapterConfig protocolAdapterConfig = mock();
    private @NotNull SimulationProtocolAdapter simulationProtocolAdapter;
    private final @NotNull SimulationPollingContext simulationPollingContext =
            new SimulationPollingContext("test", 1, null);
    private final @NotNull PollingInputImpl pollingInput = new PollingInputImpl(simulationPollingContext);
    private final @NotNull PollingOutputImpl pollingOutput =
            new PollingOutputImpl(new ProtocolAdapterDataSampleImpl(simulationPollingContext));
    private final @NotNull TimeWaiter timeWaiter = mock();


    @BeforeEach
    void setUp() {
        when(input.getProtocolAdapterState()).thenReturn(new ProtocolAdapterStateImpl(mock(),
                "simulation",
                "test-simulator"));
        when(input.getConfig()).thenReturn(protocolAdapterConfig);
        simulationProtocolAdapter =
                new SimulationProtocolAdapter(SimulationProtocolAdapterInformation.INSTANCE, input, timeWaiter);
    }

    @Test
    void test_poll_whenMinDelayIsBiggerThanMax_thenExecutionException() throws InterruptedException {
        when(protocolAdapterConfig.getMinDelay()).thenReturn(2);
        when(protocolAdapterConfig.getMaxDelay()).thenReturn(1);

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);
        assertThrows(ExecutionException.class, () -> pollingOutput.getOutputFuture().get());
        verify(timeWaiter, never()).sleep(anyInt());
    }

    @Test
    @Timeout(2)
    void test_poll_whenMinAndMaxIsTheSame_thenThreadWaitsExactlyTimeAmount() throws InterruptedException {
        when(protocolAdapterConfig.getMinDelay()).thenReturn(1);
        when(protocolAdapterConfig.getMaxDelay()).thenReturn(1);

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);

        verify(timeWaiter, times(1)).sleep(eq(1));
    }

    @Test
    @Timeout(2)
    void test_poll_whenMaxBiggerMin_thenThreadWaitsBetweem() throws InterruptedException {
        when(protocolAdapterConfig.getMinDelay()).thenReturn(1);
        when(protocolAdapterConfig.getMaxDelay()).thenReturn(3);

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);

        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(timeWaiter, times(1)).sleep(argumentCaptor.capture());
        final Integer sleepTimeMillis = argumentCaptor.getValue();
        assertTrue(sleepTimeMillis >= 1);
        assertTrue(sleepTimeMillis <= 3);
    }

    @Test
    @Timeout(2)
    void test_poll_whenMaxAndMinAreZero_thenDoNotSleep() throws InterruptedException {
        when(protocolAdapterConfig.getMinDelay()).thenReturn(0);
        when(protocolAdapterConfig.getMaxDelay()).thenReturn(0);

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);

        verify(timeWaiter, never()).sleep(anyInt());
    }
}
