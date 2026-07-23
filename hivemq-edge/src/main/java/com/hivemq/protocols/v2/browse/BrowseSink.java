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

/**
 * Where a {@link ProtocolAdapterBrowseEngine} delivers a browse's terminal outcome — exactly once, on the
 * engine's driving thread. The engine calls {@link #complete(List)} when DISCOVER → RESOLVE finishes and
 * {@link #fail(String)} on a device-reported {@code browseError}. It does <b>not</b> call back on
 * {@link ProtocolAdapterBrowseEngine#abort()} — an externally-driven interruption (a deadline or a lost
 * connection) is the caller's to surface, since only the caller knows why it aborted.
 * <p>
 * The framework's wrapper supplies a sink that completes the REST request's future and maps the reason to an HTTP
 * status; a test supplies {@link BrowseOutcome} and polls it.
 */
public interface BrowseSink {

    /**
     * The two-phase walk finished. The nodes are the resolved, selectable variables in path order.
     *
     * @param nodes the assembled results (possibly empty).
     */
    void complete(@NotNull List<BrowsedNode> nodes);

    /**
     * A browse step failed at the device (a {@code browseError}). The reason is prefixed with the phase the
     * engine was in.
     *
     * @param reason a human-readable description of the failure.
     */
    void fail(@NotNull String reason);
}
