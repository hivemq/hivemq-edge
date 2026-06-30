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

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.protocols.v2.browse.BrowseOutcome;
import com.hivemq.protocols.v2.browse.ProtocolAdapterBrowseEngine;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * EDG-737 — pure unit checks for the shared {@link ProtocolAdapterBrowseEngine}'s tag-name policy (the
 * path→default-tag-name derivation that the {@code browseName} carried on each browse entry now makes possible)
 * and its superseded-request guard. No server needed.
 */
class ProtocolAdapterBrowseEngineTest {

    @Test
    void tagNameDefault_sanitizesEachPathSegment() {
        assertThat(ProtocolAdapterBrowseEngine.tagNameDefault("/Plant/Line 1/Temperature"))
                .isEqualTo("plant-line-1-temperature");
        assertThat(ProtocolAdapterBrowseEngine.tagNameDefault("/A/B/C")).isEqualTo("a-b-c");
        assertThat(ProtocolAdapterBrowseEngine.tagNameDefault("/shared")).isEqualTo("shared");
        assertThat(ProtocolAdapterBrowseEngine.tagNameDefault("")).isEmpty();
    }

    @Test
    void sanitize_collapsesAndTrimsNonAlphanumerics() {
        assertThat(ProtocolAdapterBrowseEngine.sanitize("Foo Bar Baz")).isEqualTo("foo-bar-baz");
        assertThat(ProtocolAdapterBrowseEngine.sanitize("--A__B--")).isEqualTo("a-b");
    }

    @Test
    void dedupDefaults_suffixesCollisions() {
        assertThat(ProtocolAdapterBrowseEngine.dedupDefaults(List.of("a", "a", "b", "a")))
                .containsExactly("a", "a-2", "b", "a-3");
    }

    @Test
    void dedupDefaults_doesNotRecollideAGeneratedSuffixWithAnOrganicOne() {
        // "a","a" -> "a","a-2"; the organic "a-2" must then advance to "a-2-2", never a duplicate "a-2"
        assertThat(ProtocolAdapterBrowseEngine.dedupDefaults(List.of("a", "a", "a-2")))
                .containsExactly("a", "a-2", "a-2-2")
                .doesNotHaveDuplicates();
    }

    @Test
    void pathSegment_collapsesSeparatorsSoABrowseNameCannotForgePathLevels() {
        assertThat(ProtocolAdapterBrowseEngine.pathSegment("Temperature")).isEqualTo("Temperature");
        assertThat(ProtocolAdapterBrowseEngine.pathSegment("Flow/Rate")).isEqualTo("Flow_Rate");
    }

    @Test
    void events_fromASupersededRequestId_areIgnored() {
        final RecordingAdapter adapter = new RecordingAdapter();
        final ProtocolAdapterBrowseEngine engine = new ProtocolAdapterBrowseEngine();
        engine.start(adapter, new ConformanceNode("ns=0;i=85"), 0, 0, new BrowseOutcome());

        // start() seeds the root and issues exactly one browse for it, then awaits that page
        assertThat(adapter.browseCalls).as("root browse issued").isEqualTo(1);
        assertThat(engine.isActive()).isTrue();

        // a page tagged with a superseded requestId must be dropped: nothing discovered, no follow-up command.
        // (Were it processed, the engine would advance to RESOLVE and issue readNodeAttributes for the ghost.)
        engine.onBrowsePage(
                99,
                List.of(new BrowseResultEntry(new ConformanceNode("ns=1;i=1"), NodeType.VALUE, true, "ghost")),
                null);
        assertThat(adapter.browseCalls)
                .as("stale page issues no further browse")
                .isEqualTo(1);
        assertThat(adapter.readAttrsCalls)
                .as("stale page advances nothing into RESOLVE")
                .isZero();
        assertThat(engine.isActive()).as("engine still awaiting the real page").isTrue();

        // a resolve result from a superseded request is likewise ignored
        engine.onAttributesResolved(99, List.of());
        assertThat(adapter.readAttrsCalls).isZero();
        assertThat(engine.isActive()).isTrue();
    }

    /** Counts the commands the engine issues; all callbacks are no-ops (the test drives events directly). */
    private static final class RecordingAdapter implements ProtocolAdapter {
        private int browseCalls;
        private int readAttrsCalls;

        @Override
        public @NotNull String adapterId() {
            return "browse-engine-test";
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        public void connect() {}

        @Override
        public void disconnect() {}

        @Override
        public void verifyBatch(final @NotNull List<Node> nodes) {}

        @Override
        public void pollBatch(final @NotNull List<Node> nodes) {}

        @Override
        public void addSubscriptionBatch(final @NotNull List<Node> nodes) {}

        @Override
        public void removeSubscriptionBatch(final @NotNull List<Node> nodes) {}

        @Override
        public void writeBatch(final @NotNull List<WriteEntry> entries) {}

        @Override
        public void browse(final int requestId, final @NotNull BrowseFilter filter, final int maxReferences) {
            browseCalls++;
        }

        @Override
        public void browseNext(final int requestId, final @NotNull BrowseContinuation continuation) {}

        @Override
        public void readNodeAttributes(final int requestId, final @NotNull List<Node> nodes) {
            readAttrsCalls++;
        }
    }
}
