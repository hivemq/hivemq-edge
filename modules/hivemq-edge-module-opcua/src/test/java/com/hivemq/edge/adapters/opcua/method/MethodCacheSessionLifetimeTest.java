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
package com.hivemq.edge.adapters.opcua.method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaSessionActivityListener;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// That what the adapter remembers about an object's methods lasts exactly one session.
///
/// A remembered entry holds each method's NodeId, and OPC 10000-3 §5.2.2 says which part of that may move: a
/// server **shall** persist the identifier and the namespace URI, but **may** change the `namespaceIndex` "with
/// future Sessions and therefore a Client shall not assume the namespaceIndex will not change". So an entry that
/// outlives its session can name a different node than the one it was read for, and a call then reaches the
/// wrong operation.
///
/// Two seams start a session, and only one of them used to invalidate:
///
///   - the adapter claiming a connection -- a first connect, and any reconnect the adapter drives by building a
///     replacement connection. This one always did.
///   - the session-active hook -- a reconnect Milo performs **inside** the existing client. Milo owns the
///     session lifecycle, so it activates a fresh session and no connection is swapped; nothing the adapter does
///     to connections is involved. These entries therefore survived precisely the event that may renumber them.
///
/// These pin the second seam, by composing the reconnect runnable the way `OpcUaClientConnection` composes it.
class MethodCacheSessionLifetimeTest {

    private static final @NotNull UaSession SESSION = mock(UaSession.class);

    private @NotNull MethodCache cache;
    private @NotNull AtomicInteger browses;
    private @NotNull AtomicInteger refreshes;
    private @NotNull OpcUaSessionActivityListener listener;

    @BeforeEach
    void setUp() {
        cache = new MethodCache();
        browses = new AtomicInteger();
        refreshes = new AtomicInteger();

        final ProtocolAdapterState state = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        listener =
                new OpcUaSessionActivityListener(mock(), new FakeEventService(), "test-adapter-id", state, () -> true);
        // Exactly what OpcUaClientConnection installs: discard the per-session state, then ask the server to
        // re-report its conditions. Invalidation first, so a request admitted during the refresh round trip
        // cannot be served from what the previous session reported.
        listener.setOnReconnect(() -> {
            cache.invalidate();
            refreshes.incrementAndGet();
        });
    }

    /// Reads the methods of a tag, counting how often the device is actually asked.
    private @NotNull MethodVerifier.Result read(final @NotNull String tagName) {
        return cache.get(tagName, () -> {
                    browses.incrementAndGet();
                    return CompletableFuture.completedFuture(new MethodVerifier.Result.Verified(List.of()));
                })
                .join();
    }

    /// Milo calls this on first activation and again on every reactivation.
    private void sessionActive() {
        listener.onSessionActive(SESSION);
    }

    @Test
    void withinOneSession_theDeviceIsAskedOnce() {
        sessionActive();

        read("boiler1");
        read("boiler1");

        assertThat(browses).hasValue(1);
    }

    @Test
    void aSessionMiloReplacesInsideTheClient_discardsWhatWasRemembered() {
        // The decisive test. Before this, invalidation hung only off the adapter replacing a connection object,
        // and this path replaces no connection -- so the NodeIds read in the previous session were still used to
        // call methods in the new one.
        sessionActive();
        read("boiler1");
        assertThat(browses).hasValue(1);

        sessionActive();

        read("boiler1");
        assertThat(browses).hasValue(2);
    }

    @Test
    void everyTagIsDiscarded_notOnlyTheOneBeingCalled() {
        // The index that moved belongs to a namespace, not to a tag, so every entry read in the old session is
        // suspect -- including tags no request has touched since.
        sessionActive();
        read("boiler1");
        read("boiler2");
        assertThat(browses).hasValue(2);

        sessionActive();

        read("boiler1");
        read("boiler2");
        assertThat(browses).hasValue(4);
    }

    @Test
    void theFirstActivationDiscardsNothing() {
        // The initial connect is not a reconnect: Milo reports the first activation through the same hook, and
        // treating it as one would throw away a cache populated between connecting and the hook firing, and ask
        // the server for a condition refresh the subscription setup already performs.
        sessionActive();

        assertThat(refreshes).hasValue(0);
    }

    @Test
    void theRefreshStillHappensOnAReconnect() {
        // Invalidation is composed with the condition refresh rather than replacing it. Losing the refresh would
        // leave a transferred subscription never re-reporting its retained alarms.
        sessionActive();
        sessionActive();

        assertThat(refreshes).hasValue(1);
    }

    @Test
    void repeatedReconnects_eachDiscardAgain() {
        sessionActive();
        read("boiler1");

        sessionActive();
        read("boiler1");

        sessionActive();
        read("boiler1");

        assertThat(browses).hasValue(3);
        assertThat(refreshes).hasValue(2);
    }

    @Test
    void theGenerationChangesOnEveryInvalidation_soALongRunCanTellItsSessionEnded() {
        // What the connect-time pre-warm reads before it starts and compares before each tag. It browses one
        // tag at a time and may run for minutes on a device that serves metadata slowly, so it can still be
        // going when its session ends -- and an entry stored then would belong to the dead session while the
        // cache had already been cleared for the live one.
        //
        // Object identity of the connection cannot detect this: Milo activates a fresh session *inside* the
        // existing client on a reconnect, so the connection object is unchanged across exactly the event that
        // may renumber the node ids being stored. That is the seam exercised here.
        sessionActive();
        final long first = cache.generation();

        read("boiler1");
        assertThat(cache.generation()).isEqualTo(first);

        sessionActive();

        assertThat(cache.generation()).isNotEqualTo(first);
    }

    @Test
    void aFailedBrowseLeavesNoEntry_whichIsWhatMakesPrewarmingSafe() {
        // The reason the pre-warm may fail without escalating, and the reason the old "a cache filled at
        // connect would be silently empty and report every method as absent" objection does not apply: there is
        // no state in which this cache answers from a browse that did not happen.
        sessionActive();

        assertThatThrownBy(() -> cache.get(
                                "boiler1",
                                () -> CompletableFuture.failedFuture(
                                        new IllegalStateException("device did not answer")))
                        .join())
                .hasRootCauseMessage("device did not answer");

        // The next read browses again rather than inheriting the failure.
        read("boiler1");

        assertThat(browses).hasValue(1);
    }

    @Test
    void anUnverifiableOutcomeLeavesNoEntry() {
        // The other evicted case: the device could not be asked, so nothing was learned. A pre-warm that gets
        // this answer must not turn it into a fact for the session.
        sessionActive();

        final MethodVerifier.Result first = cache.get(
                        "boiler1",
                        () -> CompletableFuture.completedFuture(new MethodVerifier.Result.Unverifiable("no answer")))
                .join();
        assertThat(first).isInstanceOf(MethodVerifier.Result.Unverifiable.class);

        final MethodVerifier.Result second = read("boiler1");

        assertThat(second).isInstanceOf(MethodVerifier.Result.Verified.class);
        assertThat(browses).hasValue(1);
    }
}
