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
package com.hivemq.protocols.northbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterPublishBuilder;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishServiceImpl;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// What a datapoint's `context` child becomes on the wire, and which topic it is published to.
///
/// An adapter uses `context` for fields that belong to the message but are not the value -- a method call's
/// `methodName`, the caller's `correlationData`, an `errorCode`. These pin that those reach the published message
/// beside `value` rather than nested, and that the one entry naming a destination is consumed rather than
/// published.
class NorthboundTagConsumerContextTest {

    private static final @NotNull Tag TAG = new Tag() {
        @Override
        public @NotNull TagDefinition getDefinition() {
            return new TagDefinition() {};
        }

        @Override
        public @NotNull String getName() {
            return "boiler1";
        }

        @Override
        public @NotNull String getDescription() {
            return "";
        }
    };

    private record Published(@NotNull String topic, @NotNull JsonNode payload) {}

    /// Runs one datapoint through a consumer and reports what was published.
    private static @NotNull Published publish(
            final @NotNull NorthboundMapping mapping, final @NotNull DataPoint dataPoint) throws Exception {

        final AtomicReference<String> topic = new AtomicReference<>();
        final AtomicReference<byte[]> payload = new AtomicReference<>();

        final var builder = mock(ProtocolAdapterPublishBuilder.class);
        when(builder.withTopic(any())).thenAnswer(invocation -> {
            topic.set(invocation.getArgument(0));
            return builder;
        });
        when(builder.withQoS(org.mockito.ArgumentMatchers.anyInt())).thenReturn(builder);
        when(builder.withPayload(any())).thenAnswer(invocation -> {
            payload.set(invocation.getArgument(0));
            return builder;
        });
        when(builder.withAdapter(any())).thenReturn(builder);
        when(builder.send()).thenReturn(CompletableFuture.completedFuture(null));

        final var publishService = mock(ProtocolAdapterPublishServiceImpl.class);
        when(publishService.createPublish()).thenReturn(builder);

        final var wrapper = mock(ProtocolAdapterWrapper.class);
        when(wrapper.getId()).thenReturn("adapter-1");
        final var info = mock(ProtocolAdapterInformation.class);
        when(info.getProtocolId()).thenReturn("opcua");
        when(wrapper.getAdapterInformation()).thenReturn(info);

        final var consumer = new NorthboundTagConsumer(
                mapping,
                wrapper,
                new ObjectMapper(),
                publishService,
                mock(ProtocolAdapterMetricsService.class),
                mock(EventService.class));

        consumer.accept(dataPoint);

        return new Published(topic.get(), new ObjectMapper().readTree(payload.get()));
    }

    /// A mapping that echoes a caller's correlation token, which is what every test here but
    /// {@link #correlationData_isWithheldWhenTheMappingSaysNotToWriteIt()} is about.
    private static @NotNull NorthboundMapping mapping() {
        return mapping(true);
    }

    private static @NotNull NorthboundMapping mapping(final boolean writeCorrelationData) {
        return new NorthboundMapping(
                "boiler1",
                "plant/boiler1/result",
                1,
                false,
                false,
                false,
                List.of(),
                Long.MAX_VALUE,
                writeCorrelationData);
    }

    /// Builds a datapoint carrying a value and the given context entries.
    private static @NotNull DataPoint dataPoint(final @NotNull String... contextPairs) {
        final var builder =
                (DataPointWithMetadata.DataPointBuilderImpl<Void>) DataPointWithMetadata.<Void>builder(TAG, b -> null);
        builder.value(42);
        if (contextPairs.length > 0) {
            final var context = builder.startObjectContext();
            for (int i = 0; i < contextPairs.length; i += 2) {
                context.put(contextPairs[i], contextPairs[i + 1]);
            }
            context.endObject();
        }
        builder.endDataPoint();
        return builder.build("adapter-1");
    }

    @Test
    void contextEntries_arePublishedBesideValue_notNested() throws Exception {
        final Published published =
                publish(mapping(), dataPoint("methodName", "Start", "correlationData", "mes-east-00417"));

        assertThat(published.payload().get("methodName").textValue()).isEqualTo("Start");
        assertThat(published.payload().get("correlationData").textValue()).isEqualTo("mes-east-00417");
        // The child is a carrier inside Edge, not a level a consumer should descend.
        assertThat(published.payload().has("context")).isFalse();
        assertThat(published.payload().get("value").intValue()).isEqualTo(42);
    }

    @Test
    void correlationData_isWithheldWhenTheMappingSaysNotToWriteIt() throws Exception {
        // The northbound flag has to actually govern the published message. It was offered in the mapping form,
        // persisted, and carried into the runtime mapping while nothing read it -- so a deployment that turned it
        // off still published the token. A visible setting that does nothing is worse than no setting.
        final Published published =
                publish(mapping(false), dataPoint("methodName", "Start", "correlationData", "mes-east-00417"));

        assertThat(published.payload().has("correlationData")).isFalse();
        // Only that one entry is withheld: the rest of the response is unaffected.
        assertThat(published.payload().get("methodName").textValue()).isEqualTo("Start");
        assertThat(published.payload().get("value").intValue()).isEqualTo(42);
    }

    @Test
    void correlationData_isPublishedWhenTheMappingSaysToWriteIt() throws Exception {
        final Published published =
                publish(mapping(true), dataPoint("methodName", "Start", "correlationData", "mes-east-00417"));

        assertThat(published.payload().get("correlationData").textValue()).isEqualTo("mes-east-00417");
    }

    @Test
    void responseTopic_isConsumedNotPublished() throws Exception {
        // The one entry that says where the answer goes rather than anything about it. Publishing it as well would
        // put a routing instruction into the routed message.
        final Published published =
                publish(mapping(), dataPoint("methodName", "Start", "responseTopic", "plant/mes-east"));

        assertThat(published.topic()).isEqualTo("plant/mes-east");
        assertThat(published.payload().has("responseTopic")).isFalse();
        assertThat(published.payload().get("methodName").textValue()).isEqualTo("Start");
    }

    @Test
    void aResponseTopicIsAlwaysHonoured_withNoNorthboundSettingGatingIt() throws Exception {
        // The decisive test, and it pins a deliberate reversal. An earlier design had a northbound flag here, so a
        // response address was honoured only when the mapping had been told to -- the reasoning being that a caller
        // must not redirect an adapter's output by sending a field.
        //
        // That protection lives southbound instead, and only there. The field mapping is a whitelist: a southbound
        // mapping not configured to read a response topic drops the field, so the adapter never receives it and a
        // datapoint never carries it. By the time a datapoint reaches here, the address being present already means
        // the configuration asked for it. A second gate added no safety and only let the pair be half-configured.
        final Published published = publish(mapping(), dataPoint("responseTopic", "plant/mes-east"));

        assertThat(published.topic()).isEqualTo("plant/mes-east");
    }

    @Test
    void aBlankResponseTopic_fallsBackToTheConfiguredTopic() throws Exception {
        // A caller sending an empty string has named no topic; publishing to "" would fail far from the cause.
        final Published published = publish(mapping(), dataPoint("responseTopic", "   "));

        assertThat(published.topic()).isEqualTo("plant/boiler1/result");
    }

    @Test
    void noResponseTopic_withTheFlagOn_fallsBackToTheConfiguredTopic() throws Exception {
        // A fire-and-forget call on a mapping that would honour an address: there is none to honour.
        final Published published = publish(mapping(), dataPoint("methodName", "Stop"));

        assertThat(published.topic()).isEqualTo("plant/boiler1/result");
    }

    @Test
    void aDatapointWithNoContext_isUnaffected() throws Exception {
        // Every value tag that exists today. The flattening must be invisible to them.
        final Published published = publish(mapping(), dataPoint());

        assertThat(published.topic()).isEqualTo("plant/boiler1/result");
        assertThat(published.payload().get("value").intValue()).isEqualTo(42);
        assertThat(published.payload().has("context")).isFalse();
    }

    @Test
    void aContextEntryNeverOverwritesAnAspect() throws Exception {
        // Nothing in Edge fills context with an aspect name today, but a copy that silently replaced `value` would
        // be far harder to diagnose than one that declines to.
        final Published published = publish(mapping(), dataPoint("value", "hijacked"));

        assertThat(published.payload().get("value").intValue()).isEqualTo(42);
    }
}
