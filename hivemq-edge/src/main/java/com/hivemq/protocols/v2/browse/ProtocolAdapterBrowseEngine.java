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

import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseNode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The single browse traversal engine — the policy the framework's wrapper drives in production <b>and</b> the
 * reference engine the SDK-v2 conformance suites validate against a real device. Written purely against the
 * SDK v2 browse contract ({@code browse} / {@code browseNext} / {@code readNodeAttributes} and their page /
 * attribute / error answers), it has no dependency on the runtime, so the same code runs both places.
 * <p>
 * It advances <b>one protocol adapter command at a time</b>, awaiting each answer, in two phases:
 * <ul>
 *   <li><b>DISCOVER</b> — a breadth-first frontier walk below the filter node. Each node's continuation pages
 *       are drained ({@code browseNext}) <i>before</i> any sibling is browsed; a visited-set dedups shared
 *       nodes and breaks reference cycles. Each discovered node's path is assembled from its ancestors'
 *       {@link BrowseNode#browseName() browse names}.</li>
 *   <li><b>RESOLVE</b> — the discovered variables, sorted by path, are attribute-read in batches of
 *       {@link #RESOLVE_BATCH} ({@code readNodeAttributes}); each is paired with its path and a deduplicated
 *       default tag name.</li>
 * </ul>
 *
 * <h2>Request correlation (the in-flight contract)</h2>
 * The engine owns the {@code requestId}: it allocates a fresh one per {@link #start} and stamps every command
 * with it. <b>Exactly one browse is in flight per engine</b> — {@link #start} on an {@link #isActive() active}
 * engine is a programming error. Any page, attribute result, or error carrying a different id (from a
 * superseded or already-finished browse) is silently ignored, so a late answer can never corrupt a later
 * browse. The driving thread is single-threaded (the wrapper's dispatch thread in production, the test thread
 * under a synchronous dispatcher); the engine holds no locks.
 *
 * <h2>Termination</h2>
 * The engine reports its terminal outcome to the {@link BrowseSink} exactly once: {@link BrowseSink#complete}
 * on success, {@link BrowseSink#fail} on a device {@code browseError}. An <b>external</b> interruption — a
 * caller deadline or a lost connection — is signalled with {@link #abort()}, which issues
 * {@link ProtocolAdapter#browseCancel(int)} so the device can release any open continuation point and then
 * resets the engine; it does <b>not</b> call the sink, because only the caller knows why it aborted and how to
 * surface it.
 */
public final class ProtocolAdapterBrowseEngine {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdapterBrowseEngine.class);

    /** Discovered variables are attribute-read in batches of this size. */
    public static final int RESOLVE_BATCH = 100;

    private enum Phase {
        IDLE,
        DISCOVERING,
        RESOLVING
    }

    // counters, observable by tests (pagination / batch assertions)
    private int browseCommands;
    private int browseNextCommands;
    private int resolveBatches;

    private @Nullable ProtocolAdapter adapter;
    private @Nullable BrowseSink sink;
    private int requestId;
    private int maxReferences;
    private @NotNull Phase phase = Phase.IDLE;

    // DISCOVER
    private final @NotNull Deque<FrontierEntry> frontier = new ArrayDeque<>();
    private final @NotNull Set<String> visited = new HashSet<>();
    private final @NotNull List<DiscoveredVariable> discovered = new ArrayList<>();
    private final @NotNull Map<String, BrowseNode> entryByNode = new HashMap<>();
    private @NotNull String activePath = "";
    private int activeDepth;
    private @Nullable BrowseContinuation activeContinuation;

    // RESOLVE
    private @NotNull List<DiscoveredVariable> sorted = List.of();
    private final @NotNull Map<String, String> pathByNode = new HashMap<>();
    private final @NotNull Map<String, String> tagNameByNode = new HashMap<>();
    private final @NotNull Map<String, ResolvedAttributes> resolvedByNode = new HashMap<>();
    private final @NotNull List<String> pendingResolveBatch = new ArrayList<>();
    private int resolveOffset;

    /**
     * @return whether a browse is currently in flight.
     */
    public boolean isActive() {
        return phase != Phase.IDLE;
    }

    /**
     * @return the number of {@code browse(...)} commands issued by the current/last walk — one per frontier node.
     */
    public int browseCommands() {
        return browseCommands;
    }

    /**
     * @return the number of {@code browseNext(...)} commands issued — one per drained continuation page.
     */
    public int browseNextCommands() {
        return browseNextCommands;
    }

    /**
     * @return the number of {@code readNodeAttributes(...)} batches issued.
     */
    public int resolveBatches() {
        return resolveBatches;
    }

    /**
     * Start a two-phase browse below {@code root}, reporting the outcome to {@code sink}.
     *
     * @param adapter       the pure-mechanism adapter the engine issues commands to.
     * @param root          the filter node to browse below.
     * @param maxReferences max entries per page ({@code 0} lets the device decide, {@code >0} forces pagination).
     * @param maxDepth      levels below the root to descend ({@code 0} = unlimited).
     * @param sink          where the terminal outcome is reported.
     * @throws IllegalStateException if a browse is already in flight.
     */
    public void start(
            final @NotNull ProtocolAdapter adapter,
            final @NotNull Node root,
            final int maxReferences,
            final int maxDepth,
            final @NotNull BrowseSink sink) {
        if (isActive()) {
            throw new IllegalStateException("a browse is already in flight on this engine");
        }
        this.adapter = adapter;
        this.sink = sink;
        this.requestId++;
        this.maxReferences = maxReferences;
        browseCommands = 0;
        browseNextCommands = 0;
        resolveBatches = 0;
        frontier.clear();
        visited.clear();
        discovered.clear();
        entryByNode.clear();
        pathByNode.clear();
        tagNameByNode.clear();
        resolvedByNode.clear();
        pendingResolveBatch.clear();
        resolveOffset = 0;
        activePath = "";
        activeContinuation = null;
        final int depth = maxDepth > 0 ? maxDepth : Integer.MAX_VALUE;
        frontier.addLast(new FrontierEntry(root, "", depth)); // the root contributes no path segment
        visited.add(root.nodeId());
        phase = Phase.DISCOVERING;
        issueNext();
    }

    /** Take the single correct step: drain the open continuation, else browse the next frontier node, else resolve. */
    private void issueNext() {
        if (activeContinuation != null) { // drain THIS node's pages before any sibling
            browseNextCommands++;
            requireAdapter().browseNext(requestId, activeContinuation);
        } else if (!frontier.isEmpty()) {
            final FrontierEntry entry = frontier.removeFirst();
            activePath = entry.path();
            activeDepth = entry.remainingDepth();
            browseCommands++;
            requireAdapter().browse(requestId, new BrowseFilter(entry.node()), maxReferences);
        } else {
            enterResolve();
        }
    }

    // ── DISCOVER ──────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Consume one DISCOVER page. Ignored when no browse is in flight, the engine is past DISCOVER, or the page
     * carries a superseded {@code requestId}.
     *
     * @param requestId    the browse this page belongs to.
     * @param entries      the discovered nodes in this page.
     * @param continuation the next-page token, or {@code null} if this is the last page.
     */
    public void onBrowsePage(
            final int requestId,
            final @NotNull List<BrowseNode> entries,
            final @Nullable BrowseContinuation continuation) {
        if (phase != Phase.DISCOVERING || requestId != this.requestId) {
            return;
        }
        for (final BrowseNode entry : entries) {
            final String nodeId = entry.node().nodeId();
            final String childPath = activePath + "/" + pathSegment(entry.browseName());
            if (entry.selectable()) {
                if (visited.add(nodeId)) {
                    discovered.add(new DiscoveredVariable(entry.node(), childPath));
                    entryByNode.put(nodeId, entry);
                }
            } else if (activeDepth > 1 && visited.add(nodeId)) {
                frontier.addLast(new FrontierEntry(entry.node(), childPath, activeDepth - 1));
            }
        }
        activeContinuation = continuation; // null ⇒ this node is fully drained
        issueNext();
    }

    // ── RESOLVE ───────────────────────────────────────────────────────────────────────────────────────────

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
        pendingResolveBatch.clear();
        for (final DiscoveredVariable variable : sorted.subList(resolveOffset, end)) {
            batch.add(variable.node());
            pendingResolveBatch.add(variable.node().nodeId());
        }
        resolveOffset = end;
        resolveBatches++;
        requireAdapter().readNodeAttributes(requestId, batch);
    }

    /**
     * Consume one RESOLVE batch result. Ignored when not resolving or the result carries a superseded
     * {@code requestId}.
     * <p>
     * The batch must resolve <b>exactly and completely</b>: one attribute per requested node, with no missing,
     * duplicate, or unrequested node. A short, padded, or foreign result would otherwise silently drop a discovered
     * <i>selectable</i> node from an apparently-successful browse (the operator would see a {@code 200 OK} with fewer
     * tags than exist on the device). Any such mismatch fails the whole browse through the sink instead of dropping.
     *
     * @param requestId  the browse this batch belongs to.
     * @param attributes the resolved attributes, one per requested node.
     */
    public void onAttributesResolved(final int requestId, final @NotNull List<ResolvedAttributes> attributes) {
        if (phase != Phase.RESOLVING || requestId != this.requestId) {
            return;
        }
        final Set<String> requested = new HashSet<>(pendingResolveBatch);
        final Set<String> outstanding = new HashSet<>(requested);
        for (final ResolvedAttributes attribute : attributes) {
            final String nodeId = attribute.node().nodeId();
            if (!requested.contains(nodeId)) {
                failBrowse("attribute for unrequested node '" + nodeId + "'");
                return;
            }
            if (!outstanding.remove(nodeId)) {
                failBrowse("duplicate attribute for node '" + nodeId + "'");
                return;
            }
            resolvedByNode.put(nodeId, attribute);
        }
        if (!outstanding.isEmpty()) {
            failBrowse("missing attributes for requested node(s) " + missingInRequestOrder(outstanding));
            return;
        }
        readNextBatch();
    }

    /** The still-unresolved node ids of the active batch, in the order they were requested (a stable error message). */
    private @NotNull List<String> missingInRequestOrder(final @NotNull Set<String> outstanding) {
        final List<String> missing = new ArrayList<>(outstanding.size());
        for (final String nodeId : pendingResolveBatch) {
            if (outstanding.contains(nodeId)) {
                missing.add(nodeId);
            }
        }
        return missing;
    }

    // ── termination ───────────────────────────────────────────────────────────────────────────────────────

    /**
     * A device-reported failure of the in-flight browse step. Ignored when no browse is in flight or the error
     * carries a superseded {@code requestId}.
     *
     * @param requestId the browse/resolve that failed.
     * @param reason    a human-readable description.
     */
    public void onBrowseError(final int requestId, final @NotNull String reason) {
        if (!isActive() || requestId != this.requestId) {
            return;
        }
        failBrowse(reason);
    }

    /** Terminate the in-flight browse as failed — tag the reason with the phase it failed in, then go idle. */
    private void failBrowse(final @NotNull String reason) {
        final Phase failedIn = phase;
        phase = Phase.IDLE;
        requireSink().fail(failedIn + ": " + reason);
    }

    /**
     * Abort an in-flight browse on an external interruption (a caller deadline or a lost connection): issue
     * {@link ProtocolAdapter#browseCancel(int)} so the device releases any open continuation point, then reset.
     * Does not call the sink — the caller surfaces the interruption. A no-op when no browse is in flight.
     * <p>
     * {@code browseCancel} is a best-effort courtesy to the device: a raw adapter that does real synchronous work
     * there may throw, but the framework must not trust an adapter callback. The throw is caught and logged, and the
     * engine is reset in a {@code finally} so the in-flight slot is always released — otherwise a throwing cancel
     * would strand the engine {@link #isActive() active} forever and every later browse on that adapter would be
     * rejected as already in flight.
     */
    public void abort() {
        if (!isActive()) {
            return;
        }
        try {
            requireAdapter().browseCancel(requestId);
        } catch (final Exception cancelFailure) {
            log.warn(
                    "browseCancel while aborting a browse threw; releasing the in-flight slot regardless",
                    cancelFailure);
        } finally {
            reset();
        }
    }

    private void finish() {
        final List<BrowsedNode> result = new ArrayList<>(resolvedByNode.size());
        for (final DiscoveredVariable variable : sorted) {
            final String nodeId = variable.node().nodeId();
            final ResolvedAttributes attributes = resolvedByNode.get(nodeId);
            final BrowseNode entry = entryByNode.get(nodeId);
            final String path = pathByNode.get(nodeId);
            final String tagName = tagNameByNode.get(nodeId);
            if (attributes != null && entry != null && path != null && tagName != null) {
                result.add(new BrowsedNode(entry, path, tagName, attributes));
            }
        }
        final BrowseSink target = requireSink();
        reset();
        target.complete(result);
    }

    private void reset() {
        phase = Phase.IDLE;
        adapter = null;
        sink = null;
    }

    private @NotNull ProtocolAdapter requireAdapter() {
        if (adapter == null) {
            throw new IllegalStateException("engine not started");
        }
        return adapter;
    }

    private @NotNull BrowseSink requireSink() {
        if (sink == null) {
            throw new IllegalStateException("engine not started");
        }
        return sink;
    }

    // ── tag-name policy (pure helpers) ──────────────────────────────────────────────────────────────────────

    /** Lower-case, non-alphanumeric → {@code '-'}, collapse and trim dashes. */
    public static @NotNull String sanitize(final @NotNull String text) {
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
    public static @NotNull String tagNameDefault(final @NotNull String path) {
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
     * Append a numeric suffix until each name is unique: name, name-2, name-3, … A generated suffix is itself
     * re-checked, so a generated {@code name-2} cannot collide with an organically present {@code name-2} (it
     * advances to {@code name-2-2}).
     *
     * @param defaults the default tag names, in order.
     * @return the deduplicated names, in the same order.
     */
    public static @NotNull List<String> dedupDefaults(final @NotNull List<String> defaults) {
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
     * path levels. Collapse any embedded separator so each browse name stays a single segment.
     *
     * @param browseName the node's browse name.
     * @return the browse name as a single safe path segment.
     */
    public static @NotNull String pathSegment(final @NotNull String browseName) {
        return browseName.indexOf('/') < 0 ? browseName : browseName.replace('/', '_');
    }

    private record FrontierEntry(
            @NotNull Node node, @NotNull String path, int remainingDepth) {}

    private record DiscoveredVariable(
            @NotNull Node node, @NotNull String path) {}
}
