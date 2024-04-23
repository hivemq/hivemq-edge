package com.hivemq.edge.adapters.modbus;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.hivemq.edge.adapters.modbus.model.ModBusData;
import com.hivemq.edge.adapters.modbus.util.AdapterDataUtils;
import com.hivemq.edge.modules.adapters.data.DataPoint;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishBuilderImpl;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.edge.modules.config.AdapterSubscription;
import com.hivemq.edge.modules.config.impl.AdapterSubscriptionImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModbusProtocolAdapterTest {

    private final @NotNull MetricRegistry metricRegistry = new MetricRegistry();
    private final @NotNull ModbusAdapterConfig adapterConfig = new ModbusAdapterConfig("adapterId");
    private final @NotNull ModbusProtocolAdapter adapter =
            new ModbusProtocolAdapter(ModbusProtocolAdapterInformation.INSTANCE, adapterConfig, metricRegistry);
    private final @NotNull ProtocolAdapterPublishService publishService = mock(ProtocolAdapterPublishService.class);
    private final @NotNull ModuleServices moduleServices = mock(ModuleServices.class);
    private final @NotNull ProtocolAdapterPublishBuilderImpl.SendCallback sendCallback =
            mock(ProtocolAdapterPublishBuilderImpl.SendCallback.class);
    private final @NotNull ArgumentCaptor<PUBLISH> publishArgumentCaptor = ArgumentCaptor.forClass(PUBLISH.class);

    @BeforeEach
    void setUp() {
        when(moduleServices.adapterPublishService()).thenReturn(publishService);
        when(moduleServices.eventService()).thenReturn(mock(EventService.class));
        adapter.bindServices(moduleServices);
        //noinspection unchecked
        when(sendCallback.onPublishSend(publishArgumentCaptor.capture(), any(), any(ImmutableMap.class))).thenReturn(
                CompletableFuture.completedFuture(PublishReturnCode.DELIVERED));
        final ProtocolAdapterPublishBuilderImpl protocolAdapterPublishBuilder =
                new ProtocolAdapterPublishBuilderImpl("hivemq", sendCallback);
        protocolAdapterPublishBuilder.withAdapter(adapter);
        when(publishService.publish()).thenReturn(protocolAdapterPublishBuilder);
    }

    @Test
    void test_captureDataSample_expectedPayloadPresent() throws ExecutionException, InterruptedException {
        final ModbusAdapterConfig.AdapterSubscription subscription =
                new ModbusAdapterConfig.AdapterSubscription("topic", 2, null, null);
        final ModBusData data = new ModBusData(subscription, ModBusData.TYPE.INPUT_REGISTERS);
        data.addDataPoint("register", "hello world");

        adapter.captureDataSample(data).get();

        final String payloadAsString = new String(publishArgumentCaptor.getValue().getPayload());
        assertThatJson(payloadAsString).node("timestamp").isIntegralNumber();
        assertThatJson(payloadAsString).node("value").isString().isEqualTo("hello world");
    }

    @Test
    void test_deltaSamples() {

        final ModBusData data1 = createSampleData(10);
        final ModBusData data2 = createSampleData(10);

        Assertions.assertEquals(0,
                AdapterDataUtils.mergeChangedSamples(data1.getDataPoints(), data2.getDataPoints()).size(),
                "There should be no deltas");
        data2.getDataPoints().set(5, new DataPointImpl("register-5", 777));

        Assertions.assertEquals(1,
                AdapterDataUtils.mergeChangedSamples(data1.getDataPoints(), data2.getDataPoints()).size(),
                "There should be 1 delta");
    }

    @Test
    void test_mergedSamples() {

        final ModBusData data1 = createSampleData(10);
        final ModBusData data2 = createSampleData(10);
        data2.getDataPoints().set(5, new DataPointImpl("register-5", 777));

        AdapterDataUtils.mergeChangedSamples(data1.getDataPoints(), data2.getDataPoints());

        Assertions.assertEquals(777, ((DataPoint)data1.getDataPoints().get(5)).getTagValue(), "Merged data should contain new value");
    }

    protected static ModBusData createSampleData(final int registerCount){
        final AdapterSubscription adapterSubscription =
                new AdapterSubscriptionImpl("topic", 2, List.of());
        final ModBusData data = new ModBusData(adapterSubscription, ModBusData.TYPE.INPUT_REGISTERS);
        for (int i = 0; i < registerCount; i++){
            data.addDataPoint("register-" + i, i);
        }
        return data;
    }
}
