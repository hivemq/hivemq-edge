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
package com.hivemq.edge.adapters.opcua.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import java.time.Duration;
import java.util.List;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

/**
 * EDG-835: a recovery whose connection has been closed stops instead of finishing against a dead client.
 * <p>
 * A reconnect does not coordinate with a recovery already in flight — {@code OpcUaClientConnection.stop()}
 * closes the old client and the adapter builds a new connection that subscribes every tag from scratch.
 * Whatever the old recovery was still doing is then work against a disconnected client on a subscription
 * nobody holds, and every failure is reported to the operator as a tag that cannot be subscribed — for tags
 * that are perfectly fine.
 * <p>
 * Usually those calls fail fast, so the cost is spurious events rather than delay. Against a server that is
 * reachable but simply not answering — the state that produces a transfer failure in the first place — each
 * one waits its full ten-second timeout, so twenty condition tags is up to four hundred seconds. The check
 * bounds that to a single wait.
 */
public class OpcUaSubscriptionAbandonIT {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @NotNull OpcUaClient client;

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.disconnect();
        }
    }

    @Test
    @Timeout(120)
    void whenAbandonedFirst_thenSubscribingStopsInsteadOfVerifyingEveryTag() throws Exception {
        // Condition tags, because they are the ones that cost round trips: each needs its declared type
        // verified against the device and its notifier resolved, both blocking with a ten-second ceiling.
        final List<OpcuaTag> tags = List.of(
                conditionTag("alarm-1", "ns=2;s=Alarm1"),
                conditionTag("alarm-2", "ns=2;s=Alarm2"),
                conditionTag("alarm-3", "ns=2;s=Alarm3"));

        final OpcUaSubscriptionLifecycleHandler handler = handlerFor(tags);

        // Stands for the reconnect having taken over: the connection this handler belongs to is gone.
        handler.abandon();

        final long startNanos = System.nanoTime();
        final var subscription = handler.subscribe(client);
        final long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;

        assertThat(subscription)
                .as("an abandoned handler must not report a usable subscription")
                .isEmpty();
        // Not a timing assertion on the server — nothing was asked of it. Generous enough to be about the
        // absence of blocking rather than about how fast the machine is.
        assertThat(elapsedMillis)
                .as("no tag may be verified once the connection is gone")
                .isLessThan(5_000L);
    }

    @Test
    @Timeout(120)
    void whenNotAbandoned_thenTagsAreStillVerifiedNormally() throws Exception {
        // The other half of the contract: the flag must not be a blanket refusal to subscribe. A real
        // condition on the server, so verification has something to succeed against.
        final String conditionNodeId =
                opcUaServerExtension.getTestNamespace().addAcknowledgeableConditionNode("AbandonAlarm", 91_001);

        final OpcUaSubscriptionLifecycleHandler handler =
                handlerFor(List.of(conditionTag("live-alarm", conditionNodeId)));

        assertThat(handler.subscribe(client))
                .as("a handler that was never abandoned subscribes as usual")
                .isPresent();
    }

    /**
     * EDG-878: {@code onTransferFailed} must post its rebuild, not perform it.
     * <p>
     * Milo calls this on the subscription's delivery queue, which runs one task at a time — so anything done
     * inline stops every notification for that subscription, including the keep-alives Edge uses to decide
     * the connection is alive. The rebuild is slow enough (a blocking {@code create()}, then two ten-second
     * round trips per condition tag) that the health check would see stale keep-alives and fire a reconnect
     * against the recovery still running.
     * <p>
     * <b>What this does not cover:</b> that the rebuild subsequently succeeds. Provoking a real transfer
     * failure needs a server that accepts a session and then refuses {@code TransferSubscriptions}, which
     * the embedded test server cannot do — see EDG-878. This pins one property, that the call returns
     * without doing the work, and nothing more.
     */
    @Test
    @Timeout(120)
    void onTransferFailedRebuildsOnItsOwnThread() throws Exception {
        final OpcUaSubscriptionLifecycleHandler handler =
                handlerFor(List.of(conditionTag("absent-1", "ns=2;s=NoSuchAlarm1")));

        // Which thread, not how long. A timing assertion would not bite: against this test server the
        // inline rebuild also finishes in milliseconds, and the ten-second ceilings that make the inline
        // version harmful only fire against a server that is reachable and not answering -- exactly what
        // cannot be arranged here. The thread name is the property itself rather than a proxy for it.
        final String callerThread = Thread.currentThread().getName();
        handler.onTransferFailed(mock(OpcUaSubscription.class), StatusCode.GOOD);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(recoveryThreadNames())
                .as("the rebuild must run on the recovery thread, never on the caller's")
                .isNotEmpty()
                .doesNotContain(callerThread));

        handler.abandon();
    }

    /** Live thread names belonging to a subscription-recovery executor. */
    private static @NotNull List<String> recoveryThreadNames() {
        return Thread.getAllStackTraces().keySet().stream()
                .map(Thread::getName)
                .filter(name -> name.startsWith("opcua-subscription-recovery-"))
                .toList();
    }

    private static @NotNull OpcuaTag conditionTag(final @NotNull String name, final @NotNull String node) {
        return new OpcuaTag(name, "", new OpcuaTagDefinition(node, OpcuaTagKind.CONDITION));
    }

    private @NotNull OpcUaSubscriptionLifecycleHandler handlerFor(final @NotNull List<OpcuaTag> tags) throws Exception {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);

        client = OpcUaClient.create(opcUaServerExtension.getServerUri());
        client.connect();

        return new OpcUaSubscriptionLifecycleHandler(
                mock(ProtocolAdapterMetricsService.class),
                mock(ProtocolAdapterTagStreamingService.class),
                new FakeEventService(),
                "test-adapter",
                tags,
                client,
                config);
    }
}
