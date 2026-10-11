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
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishBuilderImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishServiceImpl;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    /// What one datapoint produced: the topic, the payload, and the MQTT 5 Correlation Data property if set.
    private record Published(
            @NotNull String topic,
            @NotNull JsonNode payload,
            byte @org.jetbrains.annotations.Nullable [] correlationData) {}

    /// Runs one datapoint through a consumer and reports what was published.
    private static @NotNull Published publish(
            final @NotNull NorthboundMapping mapping, final @NotNull DataPoint dataPoint) throws Exception {

        final AtomicReference<String> topic = new AtomicReference<>();
        final AtomicReference<byte[]> payload = new AtomicReference<>();
        final AtomicReference<byte[]> correlationData = new AtomicReference<>();

        // The implementation, not the SDK interface. The consumer holds that type so it can set the MQTT 5
        // Correlation Data property, which is deliberately not on the interface -- see
        // ProtocolAdapterPublishBuilderImpl.withCorrelationData.
        final var builder = mock(ProtocolAdapterPublishBuilderImpl.class);
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
        when(builder.withCorrelationData(any())).thenAnswer(invocation -> {
            correlationData.set(invocation.getArgument(0));
            return builder;
        });
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

        return new Published(topic.get(), new ObjectMapper().readTree(payload.get()), correlationData.get());
    }

    /// A plain northbound mapping, which is the only kind there is.
    ///
    /// It takes no parameter because a northbound mapping has no method-call configuration: what a response
    /// carries is decided by the southbound mapping that sent the request and travels with the request in its
    /// context, so every test here varies the datapoint's context rather than the mapping.
    private static @NotNull NorthboundMapping mapping() {
        return new NorthboundMapping(
                "boiler1", "plant/boiler1/result", 1, false, false, false, List.of(), Long.MAX_VALUE);
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
    void aContextEntryTheConsumerDoesNotName_isNotPublished() throws Exception {
        // The consumer publishes four named fields, not whatever `context` holds. `context` is the protocol's
        // namespace -- Edge does not own it and must not republish it wholesale.
        //
        // An earlier version copied every entry, which published `{"key": "value"}` -- a placeholder the polling
        // datapoint builder has written into `context` since 2026-03, shipped unseen in nine releases because
        // nothing published `context` -- on every message of all eight polling adapters. SimpleHttpToMqttIT
        // caught it on a strict comparison; this catches it here instead.
        final Published published = publish(mapping(), dataPoint("methodName", "Start", "key", "value"));

        assertThat(published.payload().has("key"))
                .withFailMessage("an unnamed context entry reached the message: %s", published.payload())
                .isFalse();
        assertThat(published.payload().get("methodName").textValue()).isEqualTo("Start");
        assertThat(published.payload().get("value").intValue()).isEqualTo(42);
    }

    @Test
    void statusInfo_isPublishedWhenTheResponseCarriesIt() throws Exception {
        // The fourth named field, and the only one this class did not already cover.
        final Published published = publish(mapping(), dataPoint("methodName", "Start", "statusInfo", "detail"));

        assertThat(published.payload().get("statusInfo").textValue()).isEqualTo("detail");
    }

    @Test
    void correlationData_isPublishedWheneverTheContextCarriesIt() throws Exception {
        // The presence of the token in the context is the whole decision. There is deliberately no setting that
        // withholds it: the request either carried a token or it did not, and whether a given subscriber sees it
        // is settled further downstream, where a version-5 client gets the MQTT property and a version-3 client
        // cannot. A setting here could only disagree with that.
        final Published published =
                publish(mapping(), dataPoint("methodName", "Start", "correlationData", "mes-east-00417"));

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

    @Test
    void aRawPrefixedToken_reachesTheMqtt5PropertyAsItsOwnText() throws Exception {
        // A token a caller put in the payload. It is text already, so the property carries its UTF-8 bytes and
        // the payload carries it with the prefix it arrived with.
        final Published published =
                publish(mapping(), dataPoint("methodName", "Start", "correlationData", "raw:mes-east-00417"));

        assertThat(published.correlationData()).isEqualTo("mes-east-00417".getBytes(StandardCharsets.UTF_8));
        assertThat(published.payload().get("correlationData").textValue()).isEqualTo("raw:mes-east-00417");
    }

    @Test
    void aBase64PrefixedToken_isDecodedIntoTheMqtt5Property() throws Exception {
        // The decisive case for the round trip. A caller setting the MQTT 5 property sent bytes; the context
        // carried them base64 because a user property value is text; the property must get those exact bytes
        // back. Without the prefix this is indistinguishable from a caller who typed base64 into the payload.
        final byte[] token = {0x00, 0x01, (byte) 0xFF, 0x7F};
        final Published published = publish(
                mapping(),
                dataPoint(
                        "methodName",
                        "Start",
                        "correlationData",
                        "base64:" + Base64.getEncoder().encodeToString(token)));

        assertThat(published.correlationData()).isEqualTo(token);
    }

    @Test
    void anUnprefixedToken_isTreatedAsRaw() throws Exception {
        // Not a shape Edge produces -- it would mean a hand-edited datapoint or a future producer. UTF-8 bytes
        // are the least surprising reading of plain text, and dropping the correlation would be worse.
        final Published published = publish(mapping(), dataPoint("methodName", "Start", "correlationData", "plain"));

        assertThat(published.correlationData()).isEqualTo("plain".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void noToken_setsNoMqtt5Property() throws Exception {
        final Published published = publish(mapping(), dataPoint("methodName", "Start"));

        assertThat(published.correlationData()).isNull();
    }

    @Test
    void aToken_reachesBothThePayloadAndTheMqtt5Property() throws Exception {
        // Both slots, always, from the one token in the context. These are different slots reaching different
        // callers -- a version-3 client can read only the payload copy, a version-5 client can use the property
        // -- and the published message is one object serialised per subscriber, so writing both is what lets the
        // encoder give each client what it can actually use.
        final Published published = publish(mapping(), dataPoint("methodName", "Start", "correlationData", "raw:abc"));

        assertThat(published.payload().get("correlationData").textValue()).isEqualTo("raw:abc");
        assertThat(published.correlationData()).isEqualTo("abc".getBytes(StandardCharsets.UTF_8));
    }
}
