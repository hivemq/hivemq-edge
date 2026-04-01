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

import com.hivemq.edge.compiler.cli.CompileCommand;
import com.hivemq.edge.compiler.cli.NewCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "edge-compiler",
        description = "HiveMQ Edge configuration compiler.",
        subcommands = {CompileCommand.class, NewCommand.class},
        mixinStandardHelpOptions = true)
public class CompilerMain {

    public static void main(final String[] args) {
        final int exitCode = new CommandLine(new CompilerMain()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Entry point for use by the {@code edge --compile} seam. Does not call {@link System#exit} — returns the exit
     * code instead so the caller can decide whether to exit.
     */
    public static int run(final String[] args) {
        return new CommandLine(new CompilerMain()).execute(args);
    }
}
