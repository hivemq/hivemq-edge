package com.hivemq.edge.adapters.http;

import com.google.common.collect.ImmutableMap;
import com.hivemq.adapter.sdk.api.ProtocolAdapterPublishBuilder;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.api.mqtt.PublishReturnCode;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishBuilderImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpProtocolAdapterTest {

    private final @NotNull HttpAdapterConfig httpAdapterConfig = new HttpAdapterConfig("adapterId");
    private final @NotNull ProtocolAdapterPublishService publishService = mock(ProtocolAdapterPublishService.class);
    private final @NotNull ModuleServices moduleServices = mock(ModuleServices.class);
    private final @NotNull ProtocolAdapterPublishBuilderImpl.SendCallback sendCallback =
            mock(ProtocolAdapterPublishBuilderImpl.SendCallback.class);
    private final @NotNull ArgumentCaptor<PUBLISH> publishArgumentCaptor = ArgumentCaptor.forClass(PUBLISH.class);

    private @NotNull HttpProtocolAdapter httpProtocolAdapter;

    @SuppressWarnings("unchecked")
    private final @NotNull ProtocolAdapterInput<HttpAdapterConfig> protocolAdapterInput = mock();


    @BeforeEach
    void setUp() {
        when(protocolAdapterInput.moduleServices()).thenReturn(moduleServices);
        when(protocolAdapterInput.getConfig()).thenReturn(httpAdapterConfig);
        when(protocolAdapterInput.getVersion()).thenReturn("someVersion");
        when(moduleServices.adapterPublishService()).thenReturn(publishService);
        when(moduleServices.eventService()).thenReturn(mock(EventService.class));
        //noinspection unchecked
        when(sendCallback.onPublishSend(publishArgumentCaptor.capture(), any(), any(ImmutableMap.class))).thenReturn(
                CompletableFuture.completedFuture(PublishReturnCode.DELIVERED));
        final ProtocolAdapterPublishBuilder protocolAdapterPublishBuilder =
                new ProtocolAdapterPublishBuilderImpl("hivemq", sendCallback);
        protocolAdapterPublishBuilder.withAdapter(httpProtocolAdapter);
        when(publishService.publish()).thenReturn(protocolAdapterPublishBuilder);

        httpProtocolAdapter = new HttpProtocolAdapter(HttpProtocolAdapterInformation.INSTANCE, protocolAdapterInput);
    }


    /*
    @Test
    void test_captureDataSample_expectedPayloadPresent() throws ExecutionException, InterruptedException {
        final AdapterSubscription subscription = new AdapterSubscriptionImpl("topic", 2, null);
        final HttpData httpData = new HttpData(subscription,
                "http://localhost:8080",
                200,
                "text/plain",
                new TestDataPointFactory());
        httpData.addDataPoint(RESPONSE_DATA, "hello world");

        httpProtocolAdapter.poll().get();

        final String payloadAsString = new String(publishArgumentCaptor.getValue().getPayload());
        assertThatJson(payloadAsString).node("timestamp").isIntegralNumber();
        assertThatJson(payloadAsString).node("value").isString().isEqualTo("hello world");
    }

    @Test
    void test_captureDataSample_errorPayloadFormat() throws ExecutionException, InterruptedException {
        final AdapterSubscription subscription = new AdapterSubscriptionImpl("topic", 2, null);
        final HttpData httpData = new HttpData(subscription,
                "http://localhost:8080",
                200,
                "text/plain",
                new TestDataPointFactory());
        final HttpData payload = new HttpData(subscription,
                "http://localhost:8080",
                404,
                "text/plain",
                new TestDataPointFactory());
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
     */

    private static class TestDataPointFactory implements DataPointFactory {
        @Override
        public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new DataPoint() {
                @Override
                public @NotNull Object getTagValue() {
                    return tagValue;
                }

                @Override
                public @NotNull String getTagName() {
                    return tagName;
                }
            };
        }
    }
}
