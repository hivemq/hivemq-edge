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
package com.hivemq.protocols.v2.browse;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A pollable {@link BrowseSink} that records a browse's terminal outcome — the convenient sink for a
 * synchronously-driven engine (the conformance tests drive one {@code drainAll()} then read the result).
 */
public final class BrowseOutcome implements BrowseSink {

    private boolean done;
    private boolean ok;
    private @Nullable String failure;
    private @NotNull List<BrowsedNode> result = List.of();

    @Override
    public void complete(final @NotNull List<BrowsedNode> nodes) {
        this.result = List.copyOf(nodes);
        this.ok = true;
        this.done = true;
    }

    @Override
    public void fail(final @NotNull String reason) {
        this.failure = reason;
        this.done = true;
    }

    /**
     * @return whether the browse has terminated (completed or failed).
     */
    public boolean isDone() {
        return done;
    }

    /**
     * @return whether the browse completed successfully.
     */
    public boolean isOk() {
        return ok;
    }

    /**
     * @return the failure reason, or {@code null} if the browse succeeded or has not terminated.
     */
    public @Nullable String failure() {
        return failure;
    }

    /**
     * @return the assembled results on success, or an empty list otherwise.
     */
    public @NotNull List<BrowsedNode> result() {
        return result;
    }
}
