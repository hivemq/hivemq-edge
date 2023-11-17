package com.hivemq.edge.adapters.http;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.hivemq.edge.adapters.http.model.HttpData;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishBuilderImpl;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.hivemq.edge.adapters.http.HttpProtocolAdapter.RESPONSE_DATA;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpProtocolAdapterTest {

    private final @NotNull MetricRegistry metricRegistry = new MetricRegistry();
    private final @NotNull HttpAdapterConfig httpAdapterConfig = new HttpAdapterConfig("adapterId");
    private final @NotNull HttpProtocolAdapter httpProtocolAdapter =
            new HttpProtocolAdapter(HttpProtocolAdapterInformation.INSTANCE, httpAdapterConfig, metricRegistry);
    private final @NotNull ProtocolAdapterPublishService publishService = mock(ProtocolAdapterPublishService.class);
    private final @NotNull ModuleServices moduleServices = mock(ModuleServices.class);
    private final @NotNull ProtocolAdapterPublishBuilderImpl.SendCallback sendCallback =
            mock(ProtocolAdapterPublishBuilderImpl.SendCallback.class);
    private final @NotNull ArgumentCaptor<PUBLISH> publishArgumentCaptor = ArgumentCaptor.forClass(PUBLISH.class);

    @BeforeEach
    void setUp() {
        when(moduleServices.adapterPublishService()).thenReturn(publishService);
        when(moduleServices.eventService()).thenReturn(mock(EventService.class));
        httpProtocolAdapter.bindServices(moduleServices);
        //noinspection unchecked
        when(sendCallback.onPublishSend(publishArgumentCaptor.capture(), any(), any(ImmutableMap.class))).thenReturn(
                CompletableFuture.completedFuture(PublishReturnCode.DELIVERED));
        final ProtocolAdapterPublishBuilderImpl protocolAdapterPublishBuilder =
                new ProtocolAdapterPublishBuilderImpl("hivemq", sendCallback);
        protocolAdapterPublishBuilder.withAdapter(httpProtocolAdapter);
        when(publishService.publish()).thenReturn(protocolAdapterPublishBuilder);
    }

    @Test
    void test_captureDataSample_expectedPayloadPresent()
            throws ExecutionException, InterruptedException, JsonProcessingException {
        final AbstractProtocolAdapterConfig.Subscription subscription =
                new AbstractProtocolAdapterConfig.Subscription("topic", 2);
        final HttpData httpData = new HttpData(subscription, "http://localhost:8080", 200, "text/plain");
        httpData.addDataPoint(RESPONSE_DATA, "hello world");

        httpProtocolAdapter.captureDataSample(httpData).get();

        final String payloadAsString = new String(publishArgumentCaptor.getValue().getPayload());
        assertThatJson(payloadAsString).node("timestamp").isIntegralNumber();
        assertThatJson(payloadAsString).node("value").isString().isEqualTo("hello world");
    }
}
