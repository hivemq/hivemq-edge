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
package com.hivemq.protocols.v2.runtime;

import org.jetbrains.annotations.NotNull;

/**
 * Helpers for the adapter fault boundary (EDG-824 #4/#7/#12). Supervisory paths that scope a misbehaving adapter's
 * failure to that adapter catch {@link Throwable} — a mispackaged adapter jar throws {@link LinkageError}, an
 * {@code Error}, and containing it must not abort a sibling or leave the adapter without an ERROR handle. That wide
 * catch must still let a <b>fatal JVM condition</b> propagate: swallowing an {@link OutOfMemoryError} or
 * {@link StackOverflowError} and pretending the pass continued would hide a process-level failure the JVM cannot
 * recover from at the adapter granularity.
 * <p>
 * {@link #rethrowIfFatal} is therefore the <b>first statement of every {@code catch (Throwable)} on an
 * adapter-facing path</b> — including secondary/error-path catches, the snapshot fallback, and the dispatcher's loop
 * backstop. A boundary that skips it silently overrides this policy for everything downstream of it.
 * <p>
 * <b>{@code ThreadDeath} policy (decided, not overlooked):</b> it is contained like any other throwable, because on
 * the Java 21 baseline nothing can raise it — {@code Thread.stop()} has thrown
 * {@link UnsupportedOperationException} unconditionally since JDK 20 and {@code ThreadDeath} is deprecated for
 * removal. A branch for it would encode a failure mode the platform no longer has.
 */
public final class AdapterFaults {

    private AdapterFaults() {}

    /**
     * Rethrow {@code throwable} when it is a fatal JVM condition that must never be scoped to a single adapter —
     * every {@link VirtualMachineError} (out-of-memory, stack overflow, internal/unknown VM errors). All other
     * throwables, including {@link LinkageError} from a mispackaged adapter jar, return normally so the caller can
     * log them and surface the failing adapter as ERROR.
     *
     * @param throwable the throwable a {@code catch (Throwable)} at the adapter boundary observed.
     */
    public static void rethrowIfFatal(final @NotNull Throwable throwable) {
        if (throwable instanceof final VirtualMachineError fatal) {
            throw fatal;
        }
    }
}
