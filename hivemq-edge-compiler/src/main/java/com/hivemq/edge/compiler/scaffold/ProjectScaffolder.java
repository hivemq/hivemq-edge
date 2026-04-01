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
package com.hivemq.edge.compiler.scaffold;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Creates a new Edge config project scaffold.
 *
 * <p>Canonical implementation — the Authoring Frontend wizard and IDE plugins replicate or delegate to this logic.
 */
public class ProjectScaffolder {

    public void scaffold(
            final @NotNull Path parentDir, final @NotNull String projectName, final @NotNull String adapterType)
            throws IOException {
        final Path projectDir = parentDir.resolve(projectName);
        if (Files.exists(projectDir)) {
            throw new IOException("Directory already exists: " + projectDir);
        }

        final String instanceName = "my-instance";
        final String adapterName = "my-adapter";

        Files.createDirectories(projectDir);

        // edge-project.yaml
        write(projectDir.resolve("edge-project.yaml"), edgeProjectYaml());

        // .gitignore
        write(projectDir.resolve(".gitignore"), "build/\n");

        // build.gradle.kts
        write(projectDir.resolve("build.gradle.kts"), buildGradleKts(projectName));

        // adapter directory
        final Path adapterDir = projectDir
                .resolve("instances")
                .resolve(instanceName)
                .resolve("adapters")
                .resolve(adapterName);
        Files.createDirectories(adapterDir);

        // adapter.yaml stub
        write(adapterDir.resolve("adapter.yaml"), adapterYaml(adapterType, adapterName));

        // example tag stub
        write(adapterDir.resolve("example-tag.yaml"), exampleTagYaml(adapterType));
    }

    private @NotNull String edgeProjectYaml() {
        return "# HiveMQ Edge configuration project\n"
                + "edgeVersion: \"2.5\"\n"
                + "sources:\n"
                + "  - instances/\n"
                + "output: build/\n"
                + "preprocessing: []\n";
    }

    private @NotNull String buildGradleKts(final @NotNull String projectName) {
        return "// HiveMQ Edge config project: " + projectName + "\n"
                + "// Run './gradlew compile' to validate and compile the configuration.\n"
                + "tasks.register<Exec>(\"compile\") {\n"
                + "    commandLine(\"edge-compiler\", \"compile\", \"--project\", projectDir.absolutePath)\n"
                + "}\n";
    }

    private @NotNull String adapterYaml(final @NotNull String adapterType, final @NotNull String adapterName) {
        return switch (adapterType.toLowerCase()) {
            case "opcua" ->
                "type: opcua\n"
                        + "id: " + adapterName + "\n"
                        + "name: My OPC-UA Adapter\n"
                        + "connection:\n"
                        + "  host: 192.168.1.10   # TODO: set the OPC-UA server host\n"
                        + "  port: 4840\n"
                        + "  securityPolicy: None\n";
            case "bacnetip" ->
                "type: bacnetip\n"
                        + "id: " + adapterName + "\n"
                        + "name: My BACnet/IP Adapter\n"
                        + "connection:\n"
                        + "  host: 192.168.1.20   # TODO: set the BACnet/IP controller host\n"
                        + "  port: 47808\n";
            default ->
                "type: " + adapterType + "\n" + "id: " + adapterName + "\n" + "name: My Adapter\n" + "connection: {}\n";
        };
    }

    private @NotNull String exampleTagYaml(final @NotNull String adapterType) {
        return switch (adapterType.toLowerCase()) {
            case "opcua" ->
                "tags:\n"
                        + "  - name: MyTag\n"
                        + "    deviceTag:\n"
                        + "      id: \"ns=2;i=1\"   # TODO: set the OPC-UA node id\n"
                        + "      dataType: Float\n"
                        + "      description: My first tag\n"
                        + "\n"
                        + "northbound:\n"
                        + "  - tagName: MyTag\n"
                        + "    topic: my-instance/my-adapter/my-tag\n"
                        + "    qos: 1\n";
            case "bacnetip" ->
                "tags:\n"
                        + "  - name: MyTag\n"
                        + "    deviceTag:\n"
                        + "      id: \"1::0/analog-input/present-value\"   # TODO: set BACnet address\n"
                        + "      dataType: Float\n"
                        + "      description: My first tag\n"
                        + "\n"
                        + "northbound:\n"
                        + "  - tagName: MyTag\n"
                        + "    topic: my-instance/my-adapter/my-tag\n"
                        + "    qos: 1\n";
            default ->
                "tags:\n"
                        + "  - name: MyTag\n"
                        + "    deviceTag:\n"
                        + "      id: \"TODO\"   # TODO: set the device tag id\n"
                        + "\n"
                        + "northbound:\n"
                        + "  - tagName: MyTag\n"
                        + "    topic: my-instance/my-adapter/my-tag\n"
                        + "    qos: 1\n";
        };
    }

    private void write(final @NotNull Path path, final @NotNull String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
