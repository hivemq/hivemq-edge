package com.hivemq.edge.adapters.http;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.hivemq.edge.adapters.http.model.HttpData;
import com.hivemq.edge.modules.adapters.data.DataPoint;
import com.hivemq.edge.modules.adapters.factories.DataPointFactory;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishBuilderImpl;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterInput;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishBuilder;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.edge.modules.config.AdapterSubscription;
import com.hivemq.edge.modules.config.impl.AdapterSubscriptionImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.PublishReturnCode;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
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
    private final @NotNull ProtocolAdapterPublishService publishService = mock(ProtocolAdapterPublishService.class);
    private final @NotNull ModuleServices moduleServices = mock(ModuleServices.class);
    private final @NotNull ProtocolAdapterPublishBuilderImpl.SendCallback sendCallback =
            mock(ProtocolAdapterPublishBuilderImpl.SendCallback.class);
    private final @NotNull ArgumentCaptor<PUBLISH> publishArgumentCaptor = ArgumentCaptor.forClass(PUBLISH.class);

    private @NotNull HttpProtocolAdapter httpProtocolAdapter;

    private final @NotNull ProtocolAdapterInput protocolAdapterInput = mock();


    @BeforeEach
    void setUp() {
        when(protocolAdapterInput.moduleServices()).thenReturn(moduleServices);

        when(moduleServices.adapterPublishService()).thenReturn(publishService);
        when(moduleServices.eventService()).thenReturn(mock(EventService.class));
        //noinspection unchecked
        when(sendCallback.onPublishSend(publishArgumentCaptor.capture(), any(), any(ImmutableMap.class))).thenReturn(
                CompletableFuture.completedFuture(PublishReturnCode.DELIVERED));
        final ProtocolAdapterPublishBuilder protocolAdapterPublishBuilder =
                new ProtocolAdapterPublishBuilderImpl("hivemq", sendCallback);
        protocolAdapterPublishBuilder.withAdapter(httpProtocolAdapter);
        when(publishService.publish()).thenReturn(protocolAdapterPublishBuilder);

        httpProtocolAdapter = new HttpProtocolAdapter(HttpProtocolAdapterInformation.INSTANCE,
                httpAdapterConfig,
                metricRegistry,
                "someVersion",
                protocolAdapterInput);
    }

    @Test
    void test_captureDataSample_expectedPayloadPresent() throws ExecutionException, InterruptedException {
        final AdapterSubscription subscription = new AdapterSubscriptionImpl("topic", 2, null);
        final HttpData httpData = new HttpData(new AdapterSubscriptionImpl(), "http://localhost:8080", 200, "text/plain", new TestDataPointFactory());
        httpData.addDataPoint(RESPONSE_DATA, "hello world");

        httpProtocolAdapter.poll().get();

        final String payloadAsString = new String(publishArgumentCaptor.getValue().getPayload());
        assertThatJson(payloadAsString).node("timestamp").isIntegralNumber();
        assertThatJson(payloadAsString).node("value").isString().isEqualTo("hello world");
    }

    @Test
    void test_captureDataSample_errorPayloadFormat() throws ExecutionException, InterruptedException {
        final AdapterSubscription subscription = new AdapterSubscriptionImpl("topic", 2, null);
        final HttpData httpData = new HttpData(new AdapterSubscriptionImpl(),"http://localhost:8080", 200, "text/plain", new TestDataPointFactory());
        final HttpData payload = new HttpData(new AdapterSubscriptionImpl(),"http://localhost:8080", 404, "text/plain", new TestDataPointFactory());
        httpData.addDataPoint(RESPONSE_DATA, payload);

        httpProtocolAdapter.poll().get();

        final String payloadAsString = new String(publishArgumentCaptor.getValue().getPayload());
        assertThatJson(payloadAsString).node("timestamp").isIntegralNumber();
        assertThatJson(payloadAsString).node("value")
                .isObject()
                .node("httpStatusCode")
                .isNumber()
                .isEqualTo(BigDecimal.valueOf(404));
    }

    private static class TestDataPointFactory implements DataPointFactory {
        @Override
        public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new DataPoint() {
                @Override
                public Object getTagValue() {
                    return tagValue;
                }
                @Override
                public String getTagName() {
                    return tagName;
                }
            };
        }
    }
}
