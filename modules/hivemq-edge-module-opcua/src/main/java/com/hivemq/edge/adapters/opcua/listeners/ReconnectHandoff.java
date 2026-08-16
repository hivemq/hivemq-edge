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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Who is going to run the refresh a reconnect owes — the handoff between the session thread and the
 * connection thread, on its own.
 * <p>
 * The two facts involved are not independent: "a reconnect happened" and "there is somewhere to send it" are
 * two halves of one question, and answering it means reading one and writing the other. Doing that in two
 * steps loses reconnects. With a volatile callback and a separate {@code AtomicBoolean} this interleaving
 * leaves nobody responsible:
 * <ol>
 *   <li>the session thread reads the callback and finds it null;</li>
 *   <li>the connection thread stores the callback;</li>
 *   <li>the connection thread tests the owed flag, still false, and returns;</li>
 *   <li>the session thread sets the owed flag.</li>
 * </ol>
 * The flag then says a refresh is owed and the callback that would honour it is already installed. Nothing
 * runs it, and nothing notices until a later reconnect happens to consume the stale flag — so the retained
 * alarm picture stays as it was before the disconnect. {@code volatile} makes each field's value visible; it
 * does not make check-then-act atomic, which is what this needs.
 * <p>
 * <b>Both methods answer with what to run rather than running it.</b> The refresh is a server round trip, and
 * holding this lock across it would block the session thread's next activation for no benefit — so the rule
 * is "decide inside, run outside". Expressing it in the return type is what makes it a rule rather than a
 * convention two callers have to remember separately.
 * <p>
 * Extracted from {@link OpcUaSessionActivityListener} for review-03 finding 10. The property that matters —
 * a reconnect produces exactly one refresh whichever thread arrives first — used to be checked by racing two
 * threads a quarter of a million times, which is a probabilistic oracle: on the measured hit rate of the old
 * defect, a run had roughly a 3% chance of seeing nothing and passing. Here the decision is a pure function
 * of state, so both orderings are enumerable, and the mutual exclusion that rules out any third ordering is
 * provable directly through {@link #insideCriticalSection()}.
 */
class ReconnectHandoff {

    private final @NotNull Object lock = new Object();

    /** Set when a reconnect arrived before there was a callback to give it to. */
    private boolean owed;

    private @Nullable Runnable callback;

    /**
     * Installs the callback, claiming any reconnect that arrived before it existed.
     *
     * @return what the caller must run, or null if nothing is owed.
     */
    @Nullable
    Runnable install(final @NotNull Runnable onReconnect) {
        synchronized (lock) {
            insideCriticalSection();
            this.callback = onReconnect;
            if (!owed) {
                return null;
            }
            owed = false;
            return onReconnect;
        }
    }

    /**
     * Records that a reconnect happened.
     *
     * @return the callback to run, or null when there is none yet — in which case the reconnect is
     *         remembered and the next {@link #install} will run it.
     */
    @Nullable
    Runnable reconnected() {
        synchronized (lock) {
            insideCriticalSection();
            if (callback == null) {
                owed = true;
                return null;
            }
            return callback;
        }
    }

    /**
     * A seam inside the critical section, so a test can hold one half open and prove the other cannot enter.
     * <p>
     * Empty here, and overridden only by the test that pins mutual exclusion. That property is the whole
     * reason this class has a lock rather than two atomics, and it is the one thing about the handoff that
     * sequential calls cannot demonstrate: every ordering of two <em>atomic</em> steps is fine, so a test
     * that cannot pause one of them mid-step is testing the outcome and assuming the atomicity.
     * <p>
     * A method rather than an injectable field: nothing is mutable, the production path is an empty method
     * the JIT removes, and a subclass in the test is the only thing that can observe it.
     */
    void insideCriticalSection() {
        // Nothing. See the Javadoc.
    }
}
