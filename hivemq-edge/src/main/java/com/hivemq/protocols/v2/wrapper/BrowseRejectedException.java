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
package com.hivemq.protocols.v2.wrapper;

import org.jetbrains.annotations.NotNull;

/**
 * The way the browse bridge fails a browse {@code CompletableFuture}. The manager and the wrapper
 * complete the future exceptionally with one of these when a browse cannot run; the REST resource maps the
 * {@link Reason} onto the HTTP status code (the only place the actor model touches HTTP). It lives in the wrapper
 * package — the lowest layer both the manager (which depends on the wrapper) and the resource share — so neither
 * the wrapper depends on the manager nor the manager on the resource.
 */
public final class BrowseRejectedException extends RuntimeException {

    /**
     * Why a browse was rejected, and the HTTP status the resource maps it to.
     */
    public enum Reason {
        /** The adapter is not {@code CONNECTED}; no browse can run (HTTP 409). */
        NOT_CONNECTED,
        /** A browse is already in flight on the adapter; only one at a time, no queue (HTTP 409). */
        ALREADY_IN_FLIGHT,
        /** The adapter type does not declare the {@code BROWSE} capability (HTTP 400). */
        UNSUPPORTED,
        /** The adapter did not return a browse result before the deadline (HTTP 504). */
        TIMED_OUT,
        /** A browse DISCOVER page or RESOLVE batch failed at the device (HTTP 500). */
        FAILED
    }

    private final @NotNull Reason reason;

    /**
     * @param reason  why the browse was rejected.
     * @param message a human-readable description.
     */
    public BrowseRejectedException(final @NotNull Reason reason, final @NotNull String message) {
        super(message);
        this.reason = reason;
    }

    /**
     * @return why the browse was rejected.
     */
    public @NotNull Reason reason() {
        return reason;
    }
}
