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
package com.hivemq.edge.adapters.browse;

import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for protocol adapters that support bulk browsing of their device address space.
 * Returns a stream of discovered nodes with informational attributes and generated defaults.
 */
public interface BulkTagBrowser {

    /**
     * Browse the device address space starting from the given root.
     *
     * @param rootId the device-specific identifier to start browsing from, or null for the default root.
     *               Interpretation is protocol-dependent (e.g. an OPC UA node ID, a register type filter,
     *               a tag name prefix).
     * @param maxDepth   maximum depth to traverse (0 = unlimited)
     * @return a stream of discovered variable nodes with informational fields and generated defaults
     * @throws BrowseException if the browse operation fails
     */
    @NotNull
    Stream<BrowsedNode> browse(@Nullable String rootId, int maxDepth) throws BrowseException;
}
