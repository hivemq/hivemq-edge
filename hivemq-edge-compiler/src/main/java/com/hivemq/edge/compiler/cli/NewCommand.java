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
package com.hivemq.edge.compiler.cli;

import com.hivemq.edge.compiler.scaffold.ProjectScaffolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "new", description = "Scaffold a new Edge config project.", mixinStandardHelpOptions = true)
public class NewCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Name of the new project (becomes the directory name).")
    private @Nullable String projectName;

    @Option(
            names = {"--type", "-t"},
            description = "Adapter type to scaffold (opcua | bacnetip). Default: opcua.",
            defaultValue = "opcua")
    private @Nullable String adapterType;

    @Option(
            names = {"--dir", "-d"},
            description = "Parent directory for the new project. Default: current directory.",
            defaultValue = ".")
    private @Nullable File parentDir;

    @Override
    public Integer call() {
        if (projectName == null || projectName.isBlank()) {
            System.err.println("ERROR: Project name is required.");
            return 2;
        }

        final Path parent = (parentDir != null ? parentDir : new File("."))
                .toPath()
                .toAbsolutePath()
                .normalize();
        final String type = adapterType != null ? adapterType : "opcua";

        try {
            new ProjectScaffolder().scaffold(parent, projectName, type);
            System.err.println("Created project: " + parent.resolve(projectName));
            return 0;
        } catch (final IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            return 2;
        }
    }
}
