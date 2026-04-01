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
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/** Parses YAML files into source model objects. */
public class YamlFileParser {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public @NotNull SourceFile parseSourceFile(final @NotNull Path path) throws IOException {
        final SourceFile file = YAML_MAPPER.readValue(path.toFile(), SourceFile.class);
        file.path = path;
        return file;
    }

    public @NotNull EdgeProjectDescriptor parseProjectDescriptor(final @NotNull Path path) throws IOException {
        return YAML_MAPPER.readValue(path.toFile(), EdgeProjectDescriptor.class);
    }
}
