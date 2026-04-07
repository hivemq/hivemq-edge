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
package com.hivemq.edge.lsp;

import org.eclipse.lsp4j.launch.LSPLauncher;

/**
 * Entry point for the HiveMQ Edge Language Server. Connects to the LSP client via stdin/stdout.
 *
 * <p>Start with:
 * <pre>{@code
 * java -jar hivemq-edge-lsp-all.jar
 * }</pre>
 */
public class HiveMQEdgeLspLauncher {

    public static void main(final String[] args) throws Exception {
        final HiveMQEdgeLspServer server = new HiveMQEdgeLspServer();
        final var launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);
        server.connect(launcher.getRemoteProxy());
        launcher.startListening().get();
    }
}
