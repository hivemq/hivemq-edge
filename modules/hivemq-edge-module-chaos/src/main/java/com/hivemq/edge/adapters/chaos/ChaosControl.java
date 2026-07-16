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
package com.hivemq.edge.adapters.chaos;

import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;

/**
 * The process-wide control seam between a test that boots a real Edge runtime and the {@link ChaosProtocolAdapter}
 * instances that runtime creates from configuration. A wired adapter is created by the framework — the test never
 * holds its constructor — so the script that adapter must follow cannot be handed in directly; instead the test
 * <b>registers</b> a script for an {@code adapter-id} here before the runtime starts, and the no-argument
 * {@link ChaosProtocolAdapterFactory} (the form the module loader instantiates) resolves each instance's script from
 * this registry by its {@code adapter-id}.
 * <p>
 * The same seam captures, per created instance, the exact {@link Node} references the framework wrapper correlates
 * per-node events against (the wrapper keys its per-node state on reference identity, and a {@link ChaosNode} has no
 * value identity), so a test can address a southbound write or a spontaneous node error to the right node through
 * {@link #nodeFor(String, String)}.
 * <p>
 * <b>Lifecycle.</b> This holds static state, so each test must {@link #reset()} it (and register its scripts) before
 * the runtime starts. The chaos classes resolve through the application classloader (the module classloader delegates
 * to its parent first), so the registry the test writes and the registry the wrapper-created factory reads are the
 * same one.
 */
public final class ChaosControl {

    private static final @NotNull Map<String, ChaosScript> SCRIPTS = new ConcurrentHashMap<>();
    private static final @NotNull Map<String, List<NodeTagPair>> NODES = new ConcurrentHashMap<>();
    private static final @NotNull Map<String, List<WriteEntry>> WRITES = new ConcurrentHashMap<>();
    private static final @NotNull ChaosScript EMPTY = ChaosScript.builder().build();

    private ChaosControl() {}

    /**
     * Register the script a configured chaos adapter must follow. Call before the runtime starts.
     *
     * @param adapterId the adapter instance id the script applies to.
     * @param script    the script that instance follows.
     */
    public static void register(final @NotNull String adapterId, final @NotNull ChaosScript script) {
        SCRIPTS.put(adapterId, script);
    }

    /**
     * @param adapterId the adapter instance id to resolve.
     * @return the registered script for the id, or an empty script (every command takes its safe default) when none
     *         was registered.
     */
    public static @NotNull ChaosScript scriptFor(final @NotNull String adapterId) {
        return SCRIPTS.getOrDefault(adapterId, EMPTY);
    }

    /**
     * @param adapterId the adapter that owns the tag.
     * @param tagName   the tag whose protocol node to resolve.
     * @return the exact node reference the framework wrapper holds for the tag, for node-keyed event injection.
     * @throws IllegalArgumentException if no node has been captured for that adapter and tag yet.
     */
    public static @NotNull Node nodeFor(final @NotNull String adapterId, final @NotNull String tagName) {
        final List<NodeTagPair> nodes = NODES.get(adapterId);
        if (nodes != null) {
            for (final NodeTagPair pair : nodes) {
                if (pair.tag().name().equals(tagName)) {
                    return pair.node();
                }
            }
        }
        throw new IllegalArgumentException("no node captured for adapter [" + adapterId + "] tag [" + tagName + "]");
    }

    /**
     * @param adapterId the adapter whose device-side executions to observe.
     * @return every write the instance's {@code writeBatch} received, in arrival order — the device's own record of
     *         what was actually executed, for exactly-once and at-least-once assertions.
     */
    public static @NotNull List<WriteEntry> writesFor(final @NotNull String adapterId) {
        return List.copyOf(WRITES.getOrDefault(adapterId, List.of()));
    }

    /**
     * Record one write reaching the device. Called by the adapter's {@code writeBatch}.
     *
     * @param adapterId the receiving instance's id.
     * @param entry     the write entry as delivered.
     */
    static void recordWrite(final @NotNull String adapterId, final @NotNull WriteEntry entry) {
        WRITES.computeIfAbsent(adapterId, ignored -> new CopyOnWriteArrayList<>())
                .add(entry);
    }

    /**
     * Forget every registered script, captured node, and recorded write. Call before each test so one test's
     * adapters never leak into the next.
     */
    public static void reset() {
        SCRIPTS.clear();
        NODES.clear();
        WRITES.clear();
    }

    /**
     * Capture the node references of a freshly-created instance. Called by the no-argument factory as the framework
     * builds each chaos adapter.
     *
     * @param input the instance input the framework constructs the adapter from.
     */
    static void captureNodes(final @NotNull ProtocolAdapterInput input) {
        NODES.put(input.adapterId(), List.copyOf(input.nodes()));
    }
}
