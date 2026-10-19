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
package com.hivemq.edge.adapters.opcua.northbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.Test;

/// The encoding context is the server's type descriptions, read over the network. Only a structure that
/// arrives still encoded needs them; everything else is published from what is already in hand.
///
/// These tests pin that, by handing the converter a supplier that **throws** and asserting a value of a
/// builtin type is published anyway. Before this, the context was fetched for every value before anything
/// looked at its type, so a failed read turned a perfectly decodable number into `null` on the method path
/// and dropped a whole notification batch on the value and event paths.
///
/// A test using a structure would pass before and after -- a structure genuinely still fails when the
/// descriptions cannot be read -- so every case here uses a builtin value deliberately.
class EncodingContextIsAskedForOnlyWhenNeededTest {

    /// A context that cannot be obtained, as when a metadata read fails under load.
    private static final @org.jetbrains.annotations.NotNull Supplier<EncodingContext> UNAVAILABLE = () -> {
        throw new IllegalStateException("the DataType hierarchy could not be read");
    };

    private static DataPointBuilder<Void> builder() {
        return DataPointWithMetadata.builder(
                new OpcuaTag("test-tag", "", new OpcuaTagDefinition("ns=2;i=1001")), b -> null);
    }

    private static DataPointWithMetadata.DataPointBuilderImpl<Void> metadataBuilder() {
        return (DataPointWithMetadata.DataPointBuilderImpl<Void>) DataPointWithMetadata.<Void>builder(
                new OpcuaTag("test-tag", "", new OpcuaTagDefinition("ns=2;i=1001")), b -> null);
    }

    @Test
    void anIntegerValue_isPublishedWithoutTheEncodingContext() {
        final var builder = metadataBuilder();

        OpcUaToJsonConverter.convertPayload(UNAVAILABLE, new DataValue(new Variant(9), StatusCode.GOOD, null), builder);

        assertThat(builder.build("test-adapter").getTagValue().asInt()).isEqualTo(9);
    }

    @Test
    void everyOtherBuiltinValue_isPublishedWithoutTheEncodingContext() {
        // One case per builtin that has its own branch and might plausibly have been thought to need the
        // descriptions: the two that render as objects, the one that renders as base64, and a timestamp.
        assertThat(published(new Variant(NodeId.parse("ns=3;s=Motor")))).isNotNull();
        assertThat(published(new Variant(new QualifiedName(2, "Motor")))).isNotNull();
        assertThat(published(new Variant(LocalizedText.english("Running")))).isNotNull();
        assertThat(published(new Variant(ByteString.of(new byte[] {1, 2, 3})))).isNotNull();
        assertThat(published(new Variant(DateTime.now()))).isNotNull();
        assertThat(published(new Variant(new StatusCode(0)))).isNotNull();
    }

    @Test
    void anArrayOfABuiltin_isPublishedWithoutTheEncodingContext() {
        // The array dispatch is a separate method with its own pass-through of the context, so it is pinned
        // separately: a fix applied to the scalar dispatch alone would leave this one fetching.
        final var builder = metadataBuilder();

        OpcUaToJsonConverter.convertPayload(
                UNAVAILABLE, new DataValue(new Variant(new Integer[] {1, 2, 3}), StatusCode.GOOD, null), builder);

        assertThat(builder.build("test-adapter").getTagValue()).hasSize(3);
    }

    @Test
    void aStructureValue_stillAsksForTheEncodingContext() {
        // The other half of the rule, and the reason the supplier exists at all rather than being removed:
        // a structure that arrives still encoded cannot be rendered without the descriptions, so it must
        // still ask -- and still fail when they are unavailable.
        final ExtensionObject stillEncoded =
                ExtensionObject.of(ByteString.of(new byte[] {1, 2, 3}), NodeId.parse("ns=2;i=5001"));

        assertThatThrownBy(() -> OpcUaToJsonConverter.convertPayload(
                        UNAVAILABLE, new DataValue(new Variant(stillEncoded), StatusCode.GOOD, null), builder()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be read");
    }

    @Test
    void theEncodingContextIsFetchedAtMostOnce_andOnlyForAStructure() {
        // Deferring the fetch must not turn one fetch into one per value. Milo caches it, so the cost of a
        // repeat is small, but counting it here is what would catch a later edit that moved the `get()` into
        // the recursion.
        final AtomicInteger fetches = new AtomicInteger();
        final Supplier<EncodingContext> counting = () -> {
            fetches.incrementAndGet();
            return DefaultEncodingContext.INSTANCE;
        };

        OpcUaToJsonConverter.convertPayload(
                counting,
                new DataValue(new Variant(new Integer[] {1, 2, 3}), StatusCode.GOOD, null),
                metadataBuilder());

        assertThat(fetches).hasValue(0);
    }

    private static JsonNode published(final Variant value) {
        final var builder = metadataBuilder();
        OpcUaToJsonConverter.convertPayload(UNAVAILABLE, new DataValue(value, StatusCode.GOOD, null), builder);
        return builder.build("test-adapter").getTagValue();
    }
}
