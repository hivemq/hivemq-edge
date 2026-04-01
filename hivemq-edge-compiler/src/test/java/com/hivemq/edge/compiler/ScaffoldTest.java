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

import com.hivemq.edge.compiler.scaffold.ProjectScaffolder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScaffoldTest {

    @Test
    void createsExpectedFilesForOpcUa(@TempDir final Path tempDir) throws Exception {
        new ProjectScaffolder().scaffold(tempDir, "my-project", "opcua");

        final Path project = tempDir.resolve("my-project");
        assertThat(project.resolve("edge-project.yaml")).exists();
        assertThat(project.resolve(".gitignore")).exists();
        assertThat(project.resolve("build.gradle.kts")).exists();

        final Path adapterDir = project.resolve("instances/my-instance/adapters/my-adapter");
        assertThat(adapterDir.resolve("adapter.yaml")).exists();
        assertThat(adapterDir.resolve("example-tag.yaml")).exists();
    }

    @Test
    void adapterYamlContainsOpcUaType(@TempDir final Path tempDir) throws Exception {
        new ProjectScaffolder().scaffold(tempDir, "p", "opcua");

        final String content =
                Files.readString(tempDir.resolve("p/instances/my-instance/adapters/my-adapter/adapter.yaml"));
        assertThat(content).contains("type: opcua");
    }

    @Test
    void adapterYamlContainsBacNetType(@TempDir final Path tempDir) throws Exception {
        new ProjectScaffolder().scaffold(tempDir, "p", "bacnetip");

        final String content =
                Files.readString(tempDir.resolve("p/instances/my-instance/adapters/my-adapter/adapter.yaml"));
        assertThat(content).contains("type: bacnetip");
    }

    @Test
    void gitignoreExcludesBuildDir(@TempDir final Path tempDir) throws Exception {
        new ProjectScaffolder().scaffold(tempDir, "p", "opcua");

        final String gitignore = Files.readString(tempDir.resolve("p/.gitignore"));
        assertThat(gitignore).contains("build/");
    }

    @Test
    void scaffoldFailsIfDirectoryAlreadyExists(@TempDir final Path tempDir) throws Exception {
        Files.createDirectories(tempDir.resolve("existing-project"));

        assertThatThrownBy(() -> new ProjectScaffolder().scaffold(tempDir, "existing-project", "opcua"))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void scaffoldedProjectCompilesSuccessfully(@TempDir final Path tempDir) throws Exception {
        new ProjectScaffolder().scaffold(tempDir, "test-project", "opcua");

        final EdgeCompiler compiler = new EdgeCompiler();
        final EdgeCompiler.Result result = compiler.compile(tempDir.resolve("test-project"));

        // The stub adapter.yaml and example-tag.yaml should compile without errors
        assertThat(result.diagnostics().errors())
                .as("scaffold should produce a compilable project")
                .isEmpty();
    }
}
