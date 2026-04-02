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
package com.hivemq.edge.compiler.source.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hivemq.edge.compiler.source.model.EdgeProjectDescriptor;
import com.hivemq.edge.compiler.source.model.SourceFile;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/** Parses YAML files into source model objects. */
public class YamlFileParser {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public @NotNull SourceFile parseSourceFile(final @NotNull Path path) throws IOException {
        final SourceFile file = YAML_MAPPER.readValue(path.toFile(), SourceFile.class);
        file.path = path;
        annotatePositions(file, path);
        return file;
    }

    public @NotNull EdgeProjectDescriptor parseProjectDescriptor(final @NotNull Path path) throws IOException {
        return YAML_MAPPER.readValue(path.toFile(), EdgeProjectDescriptor.class);
    }

    /**
     * Second pass using SnakeYAML to annotate source model objects with their line/character positions. This is
     * best-effort — any exception is silently swallowed so position annotation never blocks compilation.
     *
     * <p>Lines and characters are 0-based (LSP convention). SnakeYAML's {@code Mark.getLine()} and
     * {@code Mark.getColumn()} are both 0-based, so no adjustment is needed.
     */
    private void annotatePositions(final @NotNull SourceFile file, final @NotNull Path path) {
        try (final Reader reader = Files.newBufferedReader(path)) {
            final Node root = new Yaml().compose(reader);
            if (!(root instanceof MappingNode rootMap)) {
                return;
            }
            for (final NodeTuple tuple : rootMap.getValue()) {
                if (!(tuple.getKeyNode() instanceof ScalarNode keyNode)) {
                    continue;
                }
                final Node valueNode = tuple.getValueNode();
                switch (keyNode.getValue()) {
                    case "tags" -> annotateSimpleSeq(file.tags, valueNode);
                    case "deviceTags" -> annotateSimpleSeq(file.deviceTags, valueNode);
                    case "northbound" -> annotateSimpleSeq(file.northbound, valueNode);
                    case "dataCombiners" -> annotateCombiners(file, valueNode);
                    default -> {
                        /* not a list we annotate */
                    }
                }
            }
        } catch (final Exception ignored) {
            // Position annotation is best-effort — never fail compilation because of this.
        }
    }

    /**
     * Annotates each element of a flat sequence (tags, deviceTags, northbound) with the start position of its YAML
     * node.
     */
    private void annotateSimpleSeq(final @NotNull List<?> items, final @NotNull Node seqNode) {
        if (!(seqNode instanceof SequenceNode seq)) {
            return;
        }
        final List<Node> nodes = seq.getValue();
        for (int i = 0; i < Math.min(items.size(), nodes.size()); i++) {
            setPosition(items.get(i), nodes.get(i));
        }
    }

    /** Annotates combiner objects and their nested mappings with start positions. */
    private void annotateCombiners(final @NotNull SourceFile file, final @NotNull Node seqNode) {
        if (!(seqNode instanceof SequenceNode seq)) {
            return;
        }
        final List<Node> combinerNodes = seq.getValue();
        for (int i = 0; i < Math.min(file.dataCombiners.size(), combinerNodes.size()); i++) {
            final Node combinerNode = combinerNodes.get(i);
            setPosition(file.dataCombiners.get(i), combinerNode);

            if (!(combinerNode instanceof MappingNode combinerMap)) {
                continue;
            }
            for (final NodeTuple tuple : combinerMap.getValue()) {
                if (!(tuple.getKeyNode() instanceof ScalarNode k) || !"mappings".equals(k.getValue())) {
                    continue;
                }
                if (!(tuple.getValueNode() instanceof SequenceNode mappingSeq)) {
                    continue;
                }
                final List<Node> mappingNodes = mappingSeq.getValue();
                final var mappings = file.dataCombiners.get(i).mappings;
                for (int j = 0; j < Math.min(mappings.size(), mappingNodes.size()); j++) {
                    setPosition(mappings.get(j), mappingNodes.get(j));
                }
            }
        }
    }

    /**
     * Reflectively sets {@code line} and {@code character} on any source model object that carries those fields.
     * Using reflection here avoids a common interface dependency between the parser and source model, while keeping
     * the source model classes clean.
     */
    private void setPosition(final @NotNull Object item, final @NotNull Node node) {
        try {
            final var mark = node.getStartMark();
            item.getClass().getField("line").setInt(item, mark.getLine());
            item.getClass().getField("character").setInt(item, mark.getColumn());
        } catch (final Exception ignored) {
            // Field doesn't exist or isn't accessible — skip silently.
        }
    }
}
