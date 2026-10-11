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
package com.hivemq.edge.adapters.opcua;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.hivemq.edge.adapters.opcua.southbound.OpcUaPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// Where a method call's answer is addressed.
///
/// A request may name a `responseTopic`, asking for the answer somewhere other than the northbound mapping's own
/// topic. The adapter does not publish to a topic at all -- it emits a datapoint, and `NorthboundTagConsumer`
/// chooses the destination, using the mapping's configured topic when the datapoint names none. So **returning
/// null here is the instruction to use the default**, and that is what these tests pin.
///
/// The rule: an address that cannot be published to is dropped rather than carried, so a request naming one and
/// a request naming none land in the same place. Without this an unpublishable address was passed along, the
/// publish failed inside the consumer with only a logged warning, and the caller waited forever for an answer
/// nobody could deliver.
///
/// **A guard rather than the validation.** Such a request is rejected in Edge now, where the request's `context`
/// is assembled before the data policy runs, and is quarantined without the device being touched -- so these
/// inputs cannot arrive through the normal path at all. They are pinned anyway, because the adapter must not
/// depend on another component for its own routing to be correct.
class ReplyTopicRoutingTest {

    private static @NotNull OpcUaPayload requestAsking(final @Nullable String replyTopic) {
        return new OpcUaPayload(
                JsonNodeFactory.instance.objectNode(), new OpcUaPayload.RequestContext("Start", null, replyTopic));
    }

    @Test
    void aPublishableAddressIsHonoured() {
        assertThat(OpcUaProtocolAdapter.publishableReplyTopic(requestAsking("plant/boiler1/result/mes-east")))
                .isEqualTo("plant/boiler1/result/mes-east");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a/b/#", "#", "a/+/b", "+", "plant/boiler1/#"})
    void anAddressCarryingAWildcardIsDropped(final @NotNull String wildcardTopic) {
        // A wildcard belongs to a subscription, not to a publish. MQTT has no way to publish to one, so an
        // answer addressed here could never be delivered.
        assertThat(OpcUaProtocolAdapter.publishableReplyTopic(requestAsking(wildcardTopic)))
                .isNull();
    }

    @Test
    void aMissingAddressIsDropped() {
        assertThat(OpcUaProtocolAdapter.publishableReplyTopic(requestAsking(null)))
                .isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void aBlankAddressIsDropped(final @NotNull String blank) {
        // A caller sending an empty string has named no topic. Treated as absent rather than as an error, so
        // the answer goes to the mapping's own topic instead of failing far from the cause.
        assertThat(OpcUaProtocolAdapter.publishableReplyTopic(requestAsking(blank)))
                .isNull();
    }

    @Test
    void aMissingAndAnUnpublishableAddressAgree() {
        // The property that matters, stated directly: both reach the mapping's own topic, because both answer
        // null and null is what the consumer falls back on. A caller that mistypes its address is not left
        // waiting -- the answer is delivered, just not where it asked.
        assertThat(OpcUaProtocolAdapter.publishableReplyTopic(requestAsking("a/b/#")))
                .isEqualTo(OpcUaProtocolAdapter.publishableReplyTopic(requestAsking(null)));
    }

    @Test
    void anAddressCarryingANullCharacterIsDropped() {
        // Not a wildcard, and still not publishable: a topic may not contain U+0000. Worth pinning because the
        // check delegates to the runtime's own predicate rather than testing for '#' and '+' itself, and this
        // is what that delegation buys.
        assertThat(OpcUaProtocolAdapter.publishableReplyTopic(requestAsking("plant/boiler\u0000/result")))
                .isNull();
    }

    @Test
    void anAddressThatMerelyLooksOddIsStillHonoured() {
        // The boundary. Only publishability is judged here -- not whether the address is sensible, reserved, or
        // one an operator would have chosen. Whether a caller may name `$SYS/...` at all is a separate
        // governance question, and this must not quietly answer it.
        assertThat(OpcUaProtocolAdapter.publishableReplyTopic(requestAsking("$SYS/methods/result")))
                .isEqualTo("$SYS/methods/result");
        assertThat(OpcUaProtocolAdapter.publishableReplyTopic(requestAsking("/")))
                .isEqualTo("/");
    }
}
