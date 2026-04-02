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
package com.hivemq.api.resources.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.edge.knappogue.WorkspaceHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * REST endpoint that serves the workspace layout stored in {@link WorkspaceHolder}.
 *
 * <p>The workspace layout is embedded in a compiled config by the edge-compiler and stored by
 * {@link com.hivemq.edge.knappogue.CompiledConfigApplier} when a config is applied. It is then
 * served here so that monitoring frontends can reconstruct the same node arrangement that was
 * defined in the authoring tool.
 *
 * <p>{@code GET /api/v1/management/workspace} — returns the workspace JSON, or 204 if no compiled
 * config with a workspace has been applied yet.
 */
@Singleton
@Path("/api/v1/management/workspace")
public class WorkspaceResource {

    private final @NotNull WorkspaceHolder workspaceHolder;

    @Inject
    public WorkspaceResource(final @NotNull WorkspaceHolder workspaceHolder) {
        this.workspaceHolder = workspaceHolder;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public @NotNull Response getWorkspace() {
        final @Nullable JsonNode workspace = workspaceHolder.getWorkspace();
        if (workspace == null) {
            return Response.noContent().build();
        }
        return Response.ok(workspace).build();
    }
}
