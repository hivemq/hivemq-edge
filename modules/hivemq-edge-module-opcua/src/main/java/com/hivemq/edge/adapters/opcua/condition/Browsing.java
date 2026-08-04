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
package com.hivemq.edge.adapters.opcua.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Browsing that reads every page rather than only the first.
 * <p>
 * A browse response is a <em>page</em>, not the whole answer. When a server decides a node's references do
 * not fit in one reply it returns a first batch plus a {@code continuationPoint} — a token meaning "call
 * BrowseNext with this for the rest". A client that ignores it sees a truncated list and cannot tell that
 * from a complete one.
 * <p>
 * That distinction matters here because every caller uses a browse to answer "is X present?". A truncated
 * list turns a present method into an absent one, and the resulting error blames the device for something
 * the client did. It is also threshold-dependent — it fires on an alarm with many children and not on a lean
 * one — so it reproduces at one customer and not another.
 * <p>
 * Only the paging is shared. What to browse and what to do with the answer differ at every call site: one
 * matches a name, one tries each candidate asynchronously, one recurses upward, one compares a type. Those
 * have almost nothing in common, and a helper trying to cover them all would need a direction, a reference
 * type, a subtype flag, a class mask and a predicate before it did anything. So this returns the complete
 * reference list and leaves the searching to the caller.
 */
final class Browsing {

    private static final @NotNull Logger log = LoggerFactory.getLogger(Browsing.class);

    /**
     * How many pages to read before giving up.
     * <p>
     * A bound rather than a limit anyone should reach: a condition with thousands of references is already
     * beyond anything this adapter reasons about, and an unbounded loop against a server that keeps issuing
     * continuation points would hang adapter start.
     */
    private static final int MAX_PAGES = 50;

    private Browsing() {}

    /**
     * Every reference the browse yields, following continuation points to the end.
     * <p>
     * Never completes exceptionally: a failed BrowseNext yields the references gathered so far, because
     * every caller already treats "not found" as an answer and a partial list is closer to the truth than a
     * thrown exception during adapter start. It is logged, though — "absent because the server stopped
     * answering" and "absent because it is not there" are different problems that would otherwise look
     * identical.
     *
     * @param client the connected client.
     * @param browse what to browse; the caller owns the direction, reference type and node class mask.
     * @return the references, in server order. Empty when the node has none.
     */
    static @NotNull CompletableFuture<List<ReferenceDescription>> browseAll(
            final @NotNull OpcUaClient client, final @NotNull BrowseDescription browse) {

        return client.browseAsync(browse)
                .thenCompose(result -> collect(client, result, new ArrayList<>(), 1))
                .exceptionally(throwable -> {
                    log.debug("Browse of {} failed; treating it as no references", browse.getNodeId(), throwable);
                    return List.of();
                });
    }

    /** Accumulates one page and follows its continuation point, if it has one. */
    private static @NotNull CompletableFuture<List<ReferenceDescription>> collect(
            final @NotNull OpcUaClient client,
            final @NotNull BrowseResult result,
            final @NotNull List<ReferenceDescription> collected,
            final int page) {

        final ReferenceDescription[] references = result.getReferences();
        if (references != null) {
            collected.addAll(Arrays.asList(references));
        }

        final ByteString continuationPoint = result.getContinuationPoint();
        if (continuationPoint == null || continuationPoint.isNullOrEmpty()) {
            return CompletableFuture.completedFuture(collected);
        }
        if (page >= MAX_PAGES) {
            // Stop, but hand the token back: OPC 10000-4 §5.9.3 is explicit that a client shall always use
            // the continuation point to free the server's resources, including when it wants no more data.
            log.warn(
                    "Browse of a node returned more than {} pages of references; stopping and releasing the "
                            + "continuation point. The reference list is incomplete.",
                    MAX_PAGES);
            return release(client, continuationPoint).thenApply(released -> collected);
        }
        return client.browseNextAsync(false, List.of(continuationPoint))
                .thenCompose(response -> {
                    final BrowseResult[] results = response.getResults();
                    if (results == null || results.length == 0) {
                        return CompletableFuture.completedFuture(collected);
                    }
                    return collect(client, results[0], collected, page + 1);
                })
                .exceptionally(throwable -> {
                    log.warn(
                            "BrowseNext failed after {} page(s); the reference list is incomplete, so a node "
                                    + "that exists may be reported as absent.",
                            page,
                            throwable);
                    return collected;
                });
    }

    /** Hands a continuation point back to the server, ignoring the outcome. */
    private static @NotNull CompletableFuture<Void> release(
            final @NotNull OpcUaClient client, final @NotNull ByteString continuationPoint) {
        return client.browseNextAsync(true, List.of(continuationPoint)).handle((response, throwable) -> null);
    }
}
