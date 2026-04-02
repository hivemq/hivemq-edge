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
package com.hivemq.edge.knappogue;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.Nullable;

/**
 * Singleton that holds the latest workspace layout received from a compiled config.
 *
 * <p>The workspace layout is stored by {@link CompiledConfigApplier} whenever a compiled config is
 * applied successfully. It is served to the monitoring frontend via the workspace REST endpoint.
 */
@Singleton
public class WorkspaceHolder {

    private @Nullable JsonNode workspace;

    @Inject
    public WorkspaceHolder() {}

    public synchronized @Nullable JsonNode getWorkspace() {
        return workspace;
    }

    public synchronized void setWorkspace(final @Nullable JsonNode workspace) {
        this.workspace = workspace;
    }
}
