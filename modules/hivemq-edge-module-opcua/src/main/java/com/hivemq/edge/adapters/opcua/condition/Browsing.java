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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /**
     * How many pages to read before giving up.
     * <p>
     * A bound rather than a limit anyone should reach: a condition with thousands of references is already
     * beyond anything this adapter reasons about, and an unbounded loop against a server that keeps issuing
     * continuation points would hang adapter start.
     */
    private static final int MAX_PAGES = 50;

    /** How many async wrappers to look through for the real cause. Nothing here nests more than two deep. */
    private static final int MAX_UNWRAP_DEPTH = 8;

    private Browsing() {}

    /**
     * A browse that did not produce a complete answer — a transport failure, a bad service status, or a
     * continuation the server would not follow.
     * <p>
     * A distinct type because the distinction it carries is the whole point: an <em>empty</em> reference list
     * is a fact about the address space, while a failure is a fact about the connection, and the callers here
     * act on the first. Treating the second as the first is how a transient network error became "this
     * condition has no Acknowledge method" and then a fallback to a different call.
     */
    static final class BrowseFailedException extends RuntimeException {

        @java.io.Serial
        private static final long serialVersionUID = 1L;

        /**
         * The operation status the server answered the browse with, when it answered one at all.
         * <p>
         * Null for a transport failure, a timeout, or an incomplete continuation — cases where no server
         * verdict about the node exists to record. Carried rather than only rendered into the message
         * because one of these statuses is load-bearing: see {@link #nodeNotInAddressSpace}.
         */
        private final transient @Nullable StatusCode status;

        BrowseFailedException(final @NotNull String message) {
            this(message, (StatusCode) null);
        }

        BrowseFailedException(final @NotNull String message, final @Nullable StatusCode status) {
            super(message);
            this.status = status;
        }

        BrowseFailedException(final @NotNull String message, final @NotNull Throwable cause) {
            super(message, cause);
            this.status = null;
        }

        @Nullable
        StatusCode status() {
            return status;
        }
    }

    /**
     * Whether a browse failure is the server stating that the node is not in its AddressSpace at all.
     * <p>
     * This is the one distinction that must survive the trip out of {@link #browseAll}, because OPC 10000-9
     * builds a whole server model on it: §5.7.3, §5.5.6 and §5.8.17.2 each note that "some Servers do not
     * expose Condition instances in the AddressSpace" and require every server to accept the ConditionId as
     * the ObjectId regardless. On such a server a browse of the condition is <em>supposed</em> to come back
     * {@code Bad_NodeIdUnknown} — that answer is the model working, not the connection failing, and a caller
     * that treats it like a transport error refuses a command the specification says must work.
     * <p>
     * <b>{@code Bad_NodeIdUnknown} alone.</b> {@code Bad_NodeIdInvalid} is deliberately excluded even though
     * the two sit together in {@code NotifierResolver}'s equivalent set: it means the id is not well formed,
     * which is a fact about the tag's configuration rather than about the server's exposure model, and
     * reading it as "hidden instance" would take a typo as licence to call a method with it. Everything else
     * — authorization, transport, timeouts, a truncated continuation — keeps propagating, since none of them
     * is evidence about whether the node exists.
     */
    static boolean nodeNotInAddressSpace(final @Nullable Throwable throwable) {
        return throwable != null
                && rootOf(throwable) instanceof final BrowseFailedException failure
                && failure.status() != null
                && failure.status().value() == StatusCodes.Bad_NodeIdUnknown;
    }

    /**
     * The throwable underneath the async plumbing's wrappers.
     * <p>
     * Bounded rather than a {@code while (true)}: a self-referential cause would otherwise hang the caller,
     * and nothing here nests more than a couple of stages deep.
     */
    private static @NotNull Throwable rootOf(final @NotNull Throwable throwable) {
        Throwable current = throwable;
        for (int depth = 0; depth < MAX_UNWRAP_DEPTH; depth++) {
            if (!(current instanceof CompletionException || current instanceof ExecutionException)) {
                return current;
            }
            final Throwable cause = current.getCause();
            if (cause == null || cause == current) {
                return current;
            }
            current = cause;
        }
        return current;
    }

    /**
     * Every reference the browse yields, following continuation points to the end.
     * <p>
     * <b>Completes exceptionally when the answer is incomplete</b>, with a {@link BrowseFailedException}.
     * That is a deliberate reversal: this used to swallow every failure into an empty list, so a transport
     * exception, an authorization refusal, {@code Bad_NodeIdUnknown}, a timeout and a genuinely empty node
     * all produced the same answer. Every caller asks a browse "is X present?", so erasing the cause turned
     * a temporary failure into a permanent-sounding fact about the device — a tag reported as having no
     * notifier, a type reported as unreadable rather than unread, and worst of all a method reported absent,
     * which sends {@code ConditionUpdateWriter} down a fallback path against a server that never answered.
     * <p>
     * Per-tag isolation is preserved where it belongs, at the caller: {@code ConditionTypeVerifier} and
     * {@code NotifierResolver} both convert a failure into a rejection of <em>their</em> tag. Isolation is a
     * property of the boundary, not something a shared primitive should buy by discarding information.
     *
     * @param client the connected client.
     * @param browse what to browse; the caller owns the direction, reference type and node class mask.
     * @return the references, in server order. Empty when the node genuinely has none.
     */
    static @NotNull CompletableFuture<List<ReferenceDescription>> browseAll(
            final @NotNull OpcUaClient client, final @NotNull BrowseDescription browse) {

        return client.browseAsync(browse)
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        throw new BrowseFailedException(
                                "browsing " + browse.getNodeId() + " failed: " + describeException(throwable),
                                throwable);
                    }
                    return result;
                })
                .thenCompose(result -> {
                    // The service call succeeded; the operation inside it may still not have. A BrowseResult
                    // carries its own status, and reading getReferences() without checking it takes
                    // Bad_NodeIdUnknown -- or a permissions refusal -- for a node with no references.
                    final StatusCode status = result.getStatusCode();
                    if (status != null && status.isBad()) {
                        // The status is carried, not just printed. This is the one place a server answers
                        // "that node is not here", and callers that browse a Condition need to tell it apart
                        // from every other bad outcome -- see nodeNotInAddressSpace. Only this status is
                        // carried: a BrowseNext refusal below is about a continuation point rather than
                        // about the node, so attaching it would let a paging failure read as a hidden node.
                        return CompletableFuture.failedFuture(new BrowseFailedException(
                                "browsing " + browse.getNodeId() + " was refused by the server: " + status, status));
                    }
                    return collect(client, result, new ArrayList<>(), 1);
                });
    }

    /**
     * Describes a throwable for a browse-failure message, unwrapping the {@link CompletionException} that
     * the async plumbing wraps everything in. Falls back to the class name where there is no message, since
     * several of the exceptions reachable here carry none and "failed: null" describes nothing.
     */
    static @NotNull String describeException(final @NotNull Throwable throwable) {
        final Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        final String message = cause.getMessage();
        return message != null && !message.isBlank()
                ? message
                : cause.getClass().getSimpleName();
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
            //
            // A failure rather than a truncated success, for the same reason a transport error is: the list
            // is incomplete, and a caller asking "is X present?" cannot tell an incomplete list from a
            // complete one. Fifty pages of references on one node is already past anything this adapter
            // reasons about, so this reports a server doing something unexpected rather than a limit anyone
            // should meet.
            return release(client, continuationPoint)
                    .thenCompose(released -> CompletableFuture.failedFuture(new BrowseFailedException("browsing a "
                            + "node returned more than " + MAX_PAGES + " pages of references; the reference "
                            + "list is incomplete, so a node that exists could be reported as absent")));
        }
        return client.browseNextAsync(false, List.of(continuationPoint))
                // Hand the token back before giving up. The class comment cites the requirement and the
                // max-page branch above honours it, but this path did not: a BrowseNext that fails at the
                // transport leaves the server holding the continuation point, and against a flaky server that
                // repeats until the session is torn down. Best effort, and the original failure is what
                // propagates either way -- a release that also fails must not replace the reason.
                //
                // Attached to this call alone, not to the recursion below it. A continuation point is
                // consumed the moment the server answers -- what comes back is a *new* token, or none -- so
                // once this call succeeds there is nothing here left to release. Wrapping the recursion too
                // released a spent token once per frame as a deeper failure unwound: fifty release calls for
                // one failure, forty-nine of them naming tokens the server had already reclaimed.
                .exceptionallyCompose(throwable -> release(client, continuationPoint)
                        .thenCompose(released -> CompletableFuture.failedFuture(asBrowseFailure(throwable, page))))
                .thenCompose(response -> {
                    final BrowseResult[] results = response.getResults();
                    if (results == null || results.length == 0) {
                        return CompletableFuture.<List<ReferenceDescription>>failedFuture(
                                new BrowseFailedException("BrowseNext returned no result for a continuation "
                                        + "point after " + page + " page(s); the reference list is incomplete"));
                    }
                    final StatusCode status = results[0].getStatusCode();
                    if (status != null && status.isBad()) {
                        // No release: a BrowseNext that answers with a bad status has already released the
                        // continuation point server-side (OPC 10000-4 §5.9.3), so handing it back again would
                        // be a second call about a token that no longer exists.
                        return CompletableFuture.<List<ReferenceDescription>>failedFuture(
                                new BrowseFailedException("BrowseNext was refused after " + page + " page(s): " + status
                                        + "; the reference list is incomplete"));
                    }
                    return collect(client, results[0], collected, page + 1);
                });
    }

    /** The failure to propagate from a continuation attempt, already described if it is not one of ours. */
    private static @NotNull Throwable asBrowseFailure(final @NotNull Throwable throwable, final int page) {
        final Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        if (cause instanceof BrowseFailedException) {
            return cause;
        }
        return new BrowseFailedException(
                "BrowseNext failed after " + page + " page(s): " + describeException(cause)
                        + "; the reference list is incomplete",
                cause);
    }

    /**
     * Whether a browse name is the standard one of that name — same string <em>and</em> namespace 0.
     * <p>
     * A {@code QualifiedName} is a namespace and a string, and the namespace is what makes it unique: OPC
     * 10000-3 §5.2.4 says as much, noting that "different organizations may use the same string having a
     * slightly different meaning". Every name this module looks for — {@code Acknowledge},
     * {@code ShelvingState}, {@code Suppress2} — is defined by the specification, and specification names
     * live in namespace 0 (OPC 10000-5 §5.4.2: "Index 0 is reserved for the OPC UA namespace").
     * <p>
     * Matching on the string alone would take a vendor's own {@code Suppress}, defined in its own namespace
     * as a component of the same condition, whenever the server happened to return it first — calling the
     * wrong method, or failing on an argument mismatch, with nothing to indicate a collision occurred.
     * <p>
     * Namespace 0 is the one index safe to hardcode. Part 5 warns that a server may renumber its namespace
     * table between sessions, but 0 is reserved and fixed.
     */
    static boolean isStandardName(final @Nullable QualifiedName browseName, final @NotNull String expected) {
        return browseName != null
                && browseName.getNamespaceIndex() != null
                && browseName.getNamespaceIndex().intValue() == 0
                && expected.equals(browseName.getName());
    }

    /** Hands a continuation point back to the server, ignoring the outcome. */
    private static @NotNull CompletableFuture<Void> release(
            final @NotNull OpcUaClient client, final @NotNull ByteString continuationPoint) {
        return client.browseNextAsync(true, List.of(continuationPoint)).handle((response, throwable) -> null);
    }
}
