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
package com.hivemq.protocols.v2.browse;

import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.model.ResolvedAttributes;
import org.jetbrains.annotations.NotNull;

/**
 * One fully assembled result of a two-phase browse — the {@link ProtocolAdapterBrowseEngine}'s output for a
 * single discovered, selectable variable. It pairs the DISCOVER-phase {@link BrowseResultEntry} (node, kind,
 * selectable flag, browse name) with the RESOLVE-phase {@link ResolvedAttributes} (datatype, access,
 * description), the node's {@code path} assembled from its ancestors' browse names, and the deduplicated
 * default {@code tagName} derived from that path.
 *
 * @param entry      the DISCOVER-phase entry for this node.
 * @param path       the node's path, e.g. {@code /Plant/Line1/Temperature}.
 * @param tagName    the deduplicated default tag name derived from the path, e.g. {@code plant-line1-temperature}.
 * @param attributes the RESOLVE-phase device attributes (datatype, access, description).
 */
public record BrowsedNode(
        @NotNull BrowseResultEntry entry,
        @NotNull String path,
        @NotNull String tagName,
        @NotNull ResolvedAttributes attributes) {}
