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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.edge.compiler.lib.model.CompiledAdapterConfig;
import com.hivemq.edge.compiler.lib.model.CompiledConfig;
import com.hivemq.edge.compiler.lib.model.CompiledTag;
import com.hivemq.edge.compiler.lib.serialization.CompiledConfigSerializer;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class CompileHappyPathTest {

    @Test
    void compilesKnappogueExampleWithoutErrors() throws Exception {
        final Path fixtureDir = fixtureDir("fixtures/knappogue-example");

        final EdgeCompiler compiler = new EdgeCompiler();
        final EdgeCompiler.Result result = compiler.compile(fixtureDir);

        assertThat(result.diagnostics().errors()).as("compilation errors").isEmpty();
        assertThat(result.compiledConfig()).isNotNull();
    }

    @Test
    void compiledOutputHasCorrectGuardFields() throws Exception {
        final CompiledConfig config = compile("fixtures/knappogue-example");

        assertThat(config.notice()).isEqualTo(CompiledConfig.NOTICE);
        assertThat(config.signature()).isEqualTo(CompiledConfig.SIGNATURE_UNSIGNED);
        assertThat(config.formatVersion()).isEqualTo(CompiledConfig.FORMAT_VERSION);
        assertThat(config.edgeVersion()).isEqualTo("2.5");
    }

    @Test
    void compiledOutputHasTwoAdapters() throws Exception {
        final CompiledConfig config = compile("fixtures/knappogue-example");

        assertThat(config.protocolAdapters()).hasSize(2);
    }

    @Test
    void opcUaAdapterHasCorrectStructure() throws Exception {
        final CompiledConfig config = compile("fixtures/knappogue-example");

        final CompiledAdapterConfig extruder = adapterById(config, "extruder-01");
        assertThat(extruder.protocolId()).isEqualTo("opcua");
        assertThat(extruder.config()).containsKey("host");
        assertThat(extruder.config().get("host")).isEqualTo("192.168.1.10");

        // NozzlePressure + BarrelTemp — standalone device tags (discovered.yaml) produce no compiled tags
        assertThat(extruder.tags()).hasSize(2);
        final CompiledTag nozzle = tagByName(extruder, "NozzlePressure");
        assertThat(nozzle.definition()).containsKey("node");
        assertThat(nozzle.definition().get("node")).isEqualTo("ns=2;i=1003");

        assertThat(extruder.northboundMappings()).hasSize(1);
        assertThat(extruder.northboundMappings().get(0).tagName()).isEqualTo("NozzlePressure");
        assertThat(extruder.northboundMappings().get(0).topic())
                .isEqualTo("factory/berlin/extruder-01/nozzle-pressure");
    }

    @Test
    void bacNetAdapterHasCorrectStructure() throws Exception {
        final CompiledConfig config = compile("fixtures/knappogue-example");

        final CompiledAdapterConfig hvac = adapterById(config, "hvac-01");
        assertThat(hvac.protocolId()).isEqualTo("bacnetip");

        assertThat(hvac.tags()).hasSize(1);
        final CompiledTag zoneTemp = tagByName(hvac, "ZoneTemp");
        assertThat(zoneTemp.definition()).containsKey("address");
        assertThat(zoneTemp.definition().get("address")).isEqualTo("1::0/analog-input/present-value");
    }

    @Test
    void compiledOutputHasOneDataCombiner() throws Exception {
        final CompiledConfig config = compile("fixtures/knappogue-example");

        assertThat(config.dataCombiners()).hasSize(1);
        assertThat(config.dataCombiners().get(0).name()).isEqualTo("ExtruderStatus");
        assertThat(config.dataCombiners().get(0).mappings()).hasSize(1);
        assertThat(config.dataCombiners().get(0).mappings().get(0).instructions())
                .hasSize(3);
    }

    @Test
    void compiledConfigIsSerializableAndDeserializable() throws Exception {
        final CompiledConfig config = compile("fixtures/knappogue-example");

        final CompiledConfigSerializer serializer = new CompiledConfigSerializer();
        final String json = serializer.toJson(config);

        assertThat(json).contains("\"_notice\"");
        assertThat(json).contains("COMPILED CONFIGURATION");

        final CompiledConfig roundTripped = serializer.fromJson(json);
        assertThat(roundTripped.protocolAdapters())
                .hasSize(config.protocolAdapters().size());
        assertThat(roundTripped.dataCombiners()).hasSize(config.dataCombiners().size());
    }

    @Test
    void northboundMappingDefaultsAreApplied() throws Exception {
        final CompiledConfig config = compile("fixtures/knappogue-example");
        final CompiledAdapterConfig extruder = adapterById(config, "extruder-01");

        final var mapping = extruder.northboundMappings().get(0);
        assertThat(mapping.includeTagNames()).isFalse();
        assertThat(mapping.includeTimestamp()).isTrue();
        assertThat(mapping.includeMetadata()).isFalse();
        assertThat(mapping.messageExpiryInterval()).isEqualTo(Long.MAX_VALUE);
    }

    // ── Per-instance compilation ──────────────────────────────────────────────

    @Test
    void compileWithExplicitInstanceIdProducesSameOutputAsFlatCompile() throws Exception {
        final Path fixtureDir = fixtureDir("fixtures/knappogue-example");
        final EdgeCompiler compiler = new EdgeCompiler();

        final EdgeCompiler.Result flatResult = compiler.compile(fixtureDir);
        final EdgeCompiler.Result instanceResult = compiler.compile(fixtureDir, "factory-berlin");

        assertThat(instanceResult.diagnostics().errors())
                .as("per-instance compile errors")
                .isEmpty();
        assertThat(instanceResult.compiledConfig()).isNotNull();
        assertThat(instanceResult.compiledConfig().protocolAdapters())
                .hasSize(flatResult.compiledConfig().protocolAdapters().size());
        assertThat(instanceResult.compiledConfig().dataCombiners())
                .hasSize(flatResult.compiledConfig().dataCombiners().size());
    }

    @Test
    void compileWithNonExistentInstanceThrowsIoException() throws Exception {
        final Path fixtureDir = fixtureDir("fixtures/knappogue-example");
        final EdgeCompiler compiler = new EdgeCompiler();

        assertThatThrownBy(() -> compiler.compile(fixtureDir, "no-such-instance"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("no-such-instance");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CompiledConfig compile(final String fixturePath) throws Exception {
        final EdgeCompiler compiler = new EdgeCompiler();
        final EdgeCompiler.Result result = compiler.compile(fixtureDir(fixturePath));
        assertThat(result.diagnostics().errors())
                .as("unexpected compilation errors")
                .isEmpty();
        assertThat(result.compiledConfig()).isNotNull();
        return result.compiledConfig();
    }

    private Path fixtureDir(final String relativePath) throws Exception {
        final URL url = getClass().getClassLoader().getResource(relativePath);
        assertThat(url).as("fixture not found: " + relativePath).isNotNull();
        return Paths.get(url.toURI());
    }

    private CompiledAdapterConfig adapterById(final CompiledConfig config, final String adapterId) {
        return config.protocolAdapters().stream()
                .filter(a -> a.adapterId().equals(adapterId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("adapter not found: " + adapterId));
    }

    private CompiledTag tagByName(final CompiledAdapterConfig adapter, final String tagName) {
        return adapter.tags().stream()
                .filter(t -> t.name().equals(tagName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("tag not found: " + tagName));
    }
}
