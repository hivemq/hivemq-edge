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
package com.hivemq.edge.adapters.opcua.conformance;

import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.model.ResolvedAttributes;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * EDG-737 — the <b>reference</b> browse engine: the traversal policy the framework's PAW will own, written
 * against the SDK v2 contract and exercised end-to-end against a real (embedded) OPC-UA server. Test scope;
 * never shipped — it exists to prove the {@code browse}/{@code browseNext}/{@code readNodeAttributes} contract
 * is sufficient to drive a complete, correct, two-phase browse, and to be the model the production engine is
 * built from.
 * <p>
 * It is a goal-state sub-FSM that advances <b>one PA command at a time</b>, waiting for each event before
 * issuing the next, in two phases:
 * <ul>
 *   <li><b>DISCOVER</b> — a breadth-first frontier walk below the root. Each node's continuation pages are
 *       drained ({@code browseNext}) <i>before</i> any sibling is browsed; a visited-set dedups shared nodes
 *       and breaks reference cycles. Each discovered node's <b>path</b> is assembled from its ancestors'
 *       {@link BrowseResultEntry#browseName() browse names}.</li>
 *   <li><b>RESOLVE</b> — the discovered variables, sorted by path, are attribute-read in batches of
 *       {@link #RESOLVE_BATCH} ({@code readNodeAttributes}); the result pairs each {@link ResolvedAttributes}
 *       with its path and a deduplicated default tag name.</li>
 * </ul>
 * <p>
 * <b>Deliberately not modelled</b> (production-PAW concerns the synchronous conformance harness does not need):
 * the per-step watchdog timer and mid-browse cancel on leaving {@code CONNECTED}. What it <i>does</i> model —
 * frontier, visited-set, drain-before-advance, depth bound, path/tag-name assembly, and attribute batching — is
 * the load-bearing traversal policy.
 */
final class ReferenceBrowseEngine {

    /** Discovered variables are attribute-read in batches of this size, as the production engine will. */
    static final int RESOLVE_BATCH = 100;

    private enum Phase {
        IDLE,
        DISCOVERING,
        RESOLVING
    }

    // ---- counters, observable by the test (pagination / batch assertions) ---------------------------- //
    int browseCommands; // browse(...) issued — one per frontier node
    int browseNextCommands; // browseNext(...) issued — one per drained continuation page
    int resolveBatches; // readNodeAttributes(...) issued — one per attribute batch

    private @Nullable ProtocolAdapter adapter;
    private @Nullable BrowseOutcome outcome;
    private int requestId;
    private int maxReferences;
    private @NotNull Phase phase = Phase.IDLE;

    // DISCOVER
    private final @NotNull Deque<FrontierEntry> frontier = new ArrayDeque<>();
    private final @NotNull Set<String> visited = new HashSet<>();
    private final @NotNull List<DiscoveredVariable> discovered = new ArrayList<>();
    private @NotNull String activePath = "";
    private int activeDepth;
    private @Nullable BrowseContinuation activeContinuation;

    // RESOLVE
    private @NotNull List<DiscoveredVariable> sorted = List.of();
    private final @NotNull Map<String, String> pathByNode = new HashMap<>();
    private final @NotNull Map<String, String> tagNameByNode = new HashMap<>();
    private int resolveOffset;
    private final @NotNull List<BrowsedTag> results = new ArrayList<>();

    // ---- tag-name policy (pure helpers, as the production engine will need) --------------------------- //

    /** Lower-case, non-alphanumeric → '-', collapse and trim dashes. */
    static @NotNull String sanitize(final @NotNull String text) {
        final StringBuilder sb = new StringBuilder();
        for (final char c : text.toLowerCase().toCharArray()) {
            sb.append(Character.isLetterOrDigit(c) ? c : '-');
        }
        String out = sb.toString();
        while (out.contains("--")) {
            out = out.replace("--", "-");
        }
        int start = 0;
        int end = out.length();
        while (start < end && out.charAt(start) == '-') {
            start++;
        }
        while (end > start && out.charAt(end - 1) == '-') {
            end--;
        }
        return out.substring(start, end);
    }

    /** Sanitised path → default tag name, e.g. {@code "/A/B/C" -> "a-b-c"}. */
    static @NotNull String tagNameDefault(final @NotNull String path) {
        final String stripped = path.startsWith("/") ? path.substring(1) : path;
        if (stripped.isEmpty()) {
            return "";
        }
        final List<String> parts = new ArrayList<>();
        for (final String segment : stripped.split("/", -1)) {
            parts.add(sanitize(segment));
        }
        return String.join("-", parts);
    }

    /**
     * Append a numeric suffix until the name is unique: name, name-2, name-3, … A generated suffix is itself
     * re-checked, so a generated {@code name-2} cannot collide with an <i>organically</i> present {@code name-2}
     * (it advances to {@code name-2-2}). Real address spaces produce exactly that shape.
     */
    static @NotNull List<String> dedupDefaults(final @NotNull List<String> defaults) {
        final Set<String> used = new HashSet<>();
        final List<String> result = new ArrayList<>(defaults.size());
        for (final String base : defaults) {
            String candidate = base;
            int suffix = 1;
            while (!used.add(candidate)) {
                suffix++;
                candidate = base + "-" + suffix;
            }
            result.add(candidate);
        }
        return result;
    }

    /**
     * A browse name becomes one path segment, and {@link #tagNameDefault(String)} later splits the path on
     * {@code '/'} — so a browse name that itself contains {@code '/'} (OPC-UA permits it) would forge spurious
     * path levels. Collapse any embedded separator to {@code '_'} so each browse name stays a single segment.
     */
    static @NotNull String pathSegment(final @NotNull String browseName) {
        return browseName.indexOf('/') < 0 ? browseName : browseName.replace('/', '_');
    }

    boolean isActive() {
        return phase != Phase.IDLE;
    }

    /**
     * Start a browse below {@code root}. Returns the outcome the caller drains the dispatcher against — with the
     * synchronous {@code DrainOnCallDispatcher}, one {@code drainAll()} runs the whole walk to completion.
     *
     * @param maxReferences max entries per page ({@code 0} = server decides, {@code >0} forces pagination).
     * @param maxDepth      levels below the root to descend ({@code 0} = unlimited).
     */
    @NotNull
    BrowseOutcome start(
            final @NotNull ProtocolAdapter adapter,
            final @NotNull Node root,
            final int maxReferences,
            final int maxDepth) {
        this.adapter = adapter;
        this.requestId = 1;
        this.maxReferences = maxReferences;
        this.outcome = new BrowseOutcome();
        frontier.clear();
        visited.clear();
        discovered.clear();
        results.clear();
        pathByNode.clear();
        tagNameByNode.clear();
        resolveOffset = 0;
        activeContinuation = null;
        final int depth = maxDepth > 0 ? maxDepth : Integer.MAX_VALUE;
        frontier.addLast(new FrontierEntry(root, "", depth)); // root contributes no path segment
        visited.add(root.nodeId());
        phase = Phase.DISCOVERING;
        issueNext();
        return outcome;
    }

    /** Take the single correct step: drain the open continuation, else browse the next frontier node, else resolve. */
    private void issueNext() {
        if (activeContinuation != null) { // drain THIS node's pages before any sibling
            browseNextCommands++;
            requireAdapter().browseNext(requestId, activeContinuation);
        } else if (!frontier.isEmpty()) { // browse the next frontier node
            final FrontierEntry entry = frontier.removeFirst();
            activePath = entry.path();
            activeDepth = entry.remainingDepth();
            browseCommands++;
            requireAdapter().browse(requestId, new BrowseFilter(entry.node()), maxReferences);
        } else { // DISCOVER complete
            enterResolve();
        }
    }

    // ---- DISCOVER ------------------------------------------------------------------------------------- //

    void onBrowsePage(
            final int requestId,
            final @NotNull List<BrowseResultEntry> entries,
            final @Nullable BrowseContinuation continuation) {
        if (phase != Phase.DISCOVERING || requestId != this.requestId) {
            return; // stale / superseded
        }
        for (final BrowseResultEntry entry : entries) {
            final String nodeId = entry.node().nodeId();
            final String childPath = activePath + "/" + pathSegment(entry.browseName());
            if (entry.selectable()) { // a variable
                if (visited.add(nodeId)) {
                    discovered.add(new DiscoveredVariable(entry.node(), childPath));
                }
            } else if (activeDepth > 1 && visited.add(nodeId)) { // a folder/object — recurse if depth remains
                frontier.addLast(new FrontierEntry(entry.node(), childPath, activeDepth - 1));
            }
        }
        activeContinuation = continuation; // null ⇒ this node is fully drained
        issueNext();
    }

    // ---- RESOLVE -------------------------------------------------------------------------------------- //

    private void enterResolve() {
        if (discovered.isEmpty()) {
            finish();
            return;
        }
        sorted = new ArrayList<>(discovered);
        sorted.sort(Comparator.comparing(DiscoveredVariable::path));
        final List<String> defaults = new ArrayList<>(sorted.size());
        for (final DiscoveredVariable variable : sorted) {
            defaults.add(tagNameDefault(variable.path()));
        }
        final List<String> deduped = dedupDefaults(defaults);
        for (int i = 0; i < sorted.size(); i++) {
            final String nodeId = sorted.get(i).node().nodeId();
            pathByNode.put(nodeId, sorted.get(i).path());
            tagNameByNode.put(nodeId, deduped.get(i));
        }
        resolveOffset = 0;
        phase = Phase.RESOLVING;
        readNextBatch();
    }

    private void readNextBatch() {
        if (resolveOffset >= sorted.size()) {
            finish();
            return;
        }
        final int end = Math.min(resolveOffset + RESOLVE_BATCH, sorted.size());
        final List<Node> batch = new ArrayList<>(end - resolveOffset);
        for (final DiscoveredVariable variable : sorted.subList(resolveOffset, end)) {
            batch.add(variable.node());
        }
        resolveOffset = end;
        resolveBatches++;
        requireAdapter().readNodeAttributes(requestId, batch);
    }

    void onReadAttributesResult(final int requestId, final @NotNull List<ResolvedAttributes> attributes) {
        if (phase != Phase.RESOLVING || requestId != this.requestId) {
            return;
        }
        for (final ResolvedAttributes attribute : attributes) {
            final String nodeId = attribute.node().nodeId();
            results.add(new BrowsedTag(attribute, pathByNode.get(nodeId), tagNameByNode.get(nodeId)));
        }
        readNextBatch();
    }

    // ---- termination ---------------------------------------------------------------------------------- //

    void onBrowseError(final int requestId, final @NotNull String reason) {
        if (!isActive() || requestId != this.requestId) {
            return;
        }
        phase = Phase.IDLE;
        requireOutcome().fail(reason);
    }

    private void finish() {
        phase = Phase.IDLE;
        requireOutcome().complete(List.copyOf(results));
    }

    private @NotNull ProtocolAdapter requireAdapter() {
        if (adapter == null) {
            throw new IllegalStateException("engine not started");
        }
        return adapter;
    }

    private @NotNull BrowseOutcome requireOutcome() {
        if (outcome == null) {
            throw new IllegalStateException("engine not started");
        }
        return outcome;
    }

    private record FrontierEntry(
            @NotNull Node node, @NotNull String path, int remainingDepth) {}

    private record DiscoveredVariable(
            @NotNull Node node, @NotNull String path) {}

    /** One assembled result: the device-resolved attributes, the node's path, and its default tag name. */
    record BrowsedTag(
            @NotNull ResolvedAttributes attributes,
            @NotNull String path,
            @NotNull String tagName) {}

    /** The completion handle: after a {@code drainAll()} the walk has finished and this is done. */
    static final class BrowseOutcome {
        private boolean done;
        private boolean ok;
        private @Nullable String failure;
        private @NotNull List<BrowsedTag> result = List.of();

        private void complete(final @NotNull List<BrowsedTag> result) {
            this.result = result;
            this.ok = true;
            this.done = true;
        }

        private void fail(final @NotNull String reason) {
            this.failure = reason;
            this.done = true;
        }

        boolean isDone() {
            return done;
        }

        boolean isOk() {
            return ok;
        }

        @Nullable
        String failure() {
            return failure;
        }

        @NotNull
        List<BrowsedTag> result() {
            return result;
        }
    }
}
