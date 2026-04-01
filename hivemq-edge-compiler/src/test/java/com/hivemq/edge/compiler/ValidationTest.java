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
package com.hivemq.edge.compiler;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.compiler.source.model.SourceDeviceTag;
import com.hivemq.edge.compiler.source.model.SourceFile;
import com.hivemq.edge.compiler.source.model.SourceNorthboundMapping;
import com.hivemq.edge.compiler.source.model.SourceTag;
import com.hivemq.edge.compiler.source.resolution.AdapterScopeResolver;
import com.hivemq.edge.compiler.source.resolution.GlobalResolver;
import com.hivemq.edge.compiler.source.translation.OpcUaTypeDescriptor;
import com.hivemq.edge.compiler.source.validation.DiagnosticCollector;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationTest {

    @Test
    void danglingTagReferenceInNorthboundMappingIsAnError() {
        final SourceFile manifest = adapterManifest("extruder-01", "opcua");
        final SourceNorthboundMapping mapping = new SourceNorthboundMapping();
        mapping.tagName = "NonExistentTag";
        mapping.topic = "some/topic";
        mapping.qos = 1;
        final SourceFile tagFile = new SourceFile();
        tagFile.northbound = List.of(mapping);

        final DiagnosticCollector errors = new DiagnosticCollector();
        new AdapterScopeResolver().resolve(manifest, List.of(manifest, tagFile), errors);

        assertThat(errors.errors()).as("should report UNRESOLVED_TAG_REFERENCE").anyMatch(d -> d.code()
                .equals("UNRESOLVED_TAG_REFERENCE"));
    }

    @Test
    void danglingDeviceTagReferenceIsAnError() {
        final SourceFile manifest = adapterManifest("extruder-01", "opcua");
        final SourceTag tag = new SourceTag();
        tag.name = "MyTag";
        tag.deviceTagId = "ns=2;i=9999"; // not declared anywhere
        final SourceFile tagFile = new SourceFile();
        tagFile.tags = List.of(tag);

        final DiagnosticCollector errors = new DiagnosticCollector();
        new AdapterScopeResolver().resolve(manifest, List.of(manifest, tagFile), errors);

        assertThat(errors.errors())
                .as("should report UNRESOLVED_DEVICE_TAG_REFERENCE")
                .anyMatch(d -> d.code().equals("UNRESOLVED_DEVICE_TAG_REFERENCE"));
    }

    @Test
    void duplicateTagNameIsAnError() {
        final SourceFile manifest = adapterManifest("extruder-01", "opcua");

        final SourceTag tag1 = tagWithInlineDeviceTag("Pressure", "ns=2;i=1");
        final SourceTag tag2 = tagWithInlineDeviceTag("Pressure", "ns=2;i=2"); // same name!

        final SourceFile tagFile = new SourceFile();
        tagFile.tags = List.of(tag1, tag2);

        final DiagnosticCollector errors = new DiagnosticCollector();
        new AdapterScopeResolver().resolve(manifest, List.of(manifest, tagFile), errors);

        assertThat(errors.errors()).as("should report DUPLICATE_TAG_NAME").anyMatch(d -> d.code()
                .equals("DUPLICATE_TAG_NAME"));
    }

    @Test
    void multipleAdapterConfigsInOneDirIsAnError() {
        final SourceFile manifest1 = adapterManifest("adapter-a", "opcua");
        manifest1.path = Path.of("adapters/my-dir/adapter-a.yaml");
        final SourceFile manifest2 = adapterManifest("adapter-b", "opcua");
        manifest2.path = Path.of("adapters/my-dir/adapter-b.yaml");

        final DiagnosticCollector errors = new DiagnosticCollector();
        new GlobalResolver().resolve(List.of(manifest1, manifest2), errors);

        assertThat(errors.errors()).as("should report MULTIPLE_ADAPTER_CONFIGS").anyMatch(d -> d.code()
                .equals("MULTIPLE_ADAPTER_CONFIGS"));
    }

    @Test
    void adapterIdMismatchIsAnError() {
        final SourceFile manifest = adapterManifest("wrong-id", "opcua");
        manifest.path = Path.of("adapters/correct-dir-name/adapter.yaml");

        final DiagnosticCollector errors = new DiagnosticCollector();
        new GlobalResolver().resolve(List.of(manifest), errors);

        assertThat(errors.errors()).as("should report ADAPTER_ID_MISMATCH").anyMatch(d -> d.code()
                .equals("ADAPTER_ID_MISMATCH"));
    }

    @Test
    void crossAdapterReferenceToUnknownAdapterIsAnError() throws Exception {
        // A combiner references adapterId "no-such-adapter"
        final java.net.URL url = getClass().getClassLoader().getResource("fixtures/knappogue-example");
        assertThat(url).isNotNull();
        final java.nio.file.Path fixtureDir = java.nio.file.Paths.get(url.toURI());

        // Replace extruder-status.yaml with an invalid cross-adapter ref by injecting via resolver
        // Instead — build from scratch with a bad reference
        final SourceFile combinerFile = new SourceFile();
        final var combiner = new com.hivemq.edge.compiler.source.model.SourceDataCombiner();
        combiner.name = "BadCombiner";
        final var mapping = new com.hivemq.edge.compiler.source.model.SourceCombinerMapping();
        final var trigger = new com.hivemq.edge.compiler.source.model.SourceTrigger();
        trigger.tag = "no-such-adapter::SomeTag";
        mapping.trigger = trigger;
        mapping.output = new com.hivemq.edge.compiler.source.model.SourceCombinerOutput();
        mapping.output.topic = "some/topic";
        combiner.mappings = List.of(mapping);
        combinerFile.dataCombiners = List.of(combiner);

        combinerFile.path = Path.of("data-combiners/bad-combiner.yaml");

        final DiagnosticCollector errors = new DiagnosticCollector();
        new GlobalResolver().resolve(List.of(combinerFile), errors);

        assertThat(errors.errors())
                .as("should report UNKNOWN_ADAPTER_REFERENCE")
                .anyMatch(d -> d.code().equals("UNKNOWN_ADAPTER_REFERENCE"));
    }

    @Test
    void adapterTagBothInlineAndByIdIsAnError() {
        final SourceFile manifest = adapterManifest("extruder-01", "opcua");
        final SourceTag tag = new SourceTag();
        tag.name = "MyTag";
        tag.deviceTagId = "ns=2;i=1";
        tag.deviceTag = new SourceDeviceTag();
        tag.deviceTag.id = "ns=2;i=2";
        final SourceFile tagFile = new SourceFile();
        tagFile.tags = List.of(tag);

        final DiagnosticCollector errors = new DiagnosticCollector();
        new AdapterScopeResolver().resolve(manifest, List.of(manifest, tagFile), errors);

        assertThat(errors.errors()).as("should report TAG_AMBIGUOUS_DEVICE_TAG").anyMatch(d -> d.code()
                .equals("TAG_AMBIGUOUS_DEVICE_TAG"));
    }

    @Test
    void opcUaConnectionWithUriIsAnError() {
        final SourceFile manifest = new SourceFile();
        manifest.id = "extruder-01";
        manifest.path = Path.of("adapters/extruder-01/adapter.yaml");

        final DiagnosticCollector errors = new DiagnosticCollector();
        new OpcUaTypeDescriptor()
                .validateConnectionConfig(
                        java.util.Map.of("host", "localhost", "port", 4840, "uri", "opc.tcp://localhost:4840"),
                        manifest,
                        errors);

        assertThat(errors.errors())
                .as("should report ADAPTER_INVALID_CONNECTION_FIELD for uri")
                .anyMatch(d -> d.code().equals("ADAPTER_INVALID_CONNECTION_FIELD"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SourceFile adapterManifest(final String id, final String type) {
        final SourceFile manifest = new SourceFile();
        manifest.id = id;
        manifest.type = type;
        manifest.connection = java.util.Map.of("host", "localhost", "port", 4840);
        manifest.path = Path.of("adapters/" + id + "/adapter.yaml");
        return manifest;
    }

    private SourceTag tagWithInlineDeviceTag(final String tagName, final String nodeId) {
        final SourceTag tag = new SourceTag();
        tag.name = tagName;
        tag.deviceTag = new SourceDeviceTag();
        tag.deviceTag.id = nodeId;
        return tag;
    }
}
