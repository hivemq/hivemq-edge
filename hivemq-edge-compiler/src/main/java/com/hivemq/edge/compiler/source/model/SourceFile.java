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
package com.hivemq.edge.compiler.source.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parsed representation of a single YAML file. A file may contain any combination of the top-level sections.
 *
 * <p>Adapter config fields ({@code type}, {@code id}, {@code name}, {@code connection}) are present when the file is an
 * adapter manifest. Entity lists ({@code deviceTags}, {@code tags}, {@code northbound}, {@code dataCombiners}) may
 * appear in any file including the adapter manifest.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceFile {

    /** Set after parsing — not from YAML. */
    public @Nullable Path path;

    // ── Adapter config fields ─────────────────────────────────────────────────
    /** Adapter type discriminator — e.g. {@code "opcua"}, {@code "bacnetip"}. Present only in adapter manifests. */
    public @Nullable String type;

    /** Adapter id — must match the containing directory name. Present only in adapter manifests. */
    public @Nullable String id;

    public @Nullable String name;

    /** Connection config — opaque map, passed through to the compiled output. */
    public @Nullable Map<String, Object> connection;

    // ── Entity lists ──────────────────────────────────────────────────────────
    public @NotNull List<SourceDeviceTag> deviceTags = List.of();
    public @NotNull List<SourceTag> tags = List.of();
    public @NotNull List<SourceNorthboundMapping> northbound = List.of();
    public @NotNull List<SourceDataCombiner> dataCombiners = List.of();

    /** Returns true if this file contains adapter manifest fields (i.e. has a {@code type} field). */
    public boolean isAdapterManifest() {
        return type != null;
    }

    /** Returns true if this file contains at least one entity (device tag, tag, mapping, or combiner). */
    public boolean hasEntities() {
        return !deviceTags.isEmpty() || !tags.isEmpty() || !northbound.isEmpty() || !dataCombiners.isEmpty();
    }
}
