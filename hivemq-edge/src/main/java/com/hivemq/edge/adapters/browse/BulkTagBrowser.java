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

    /**
     * Resolve a node ID against the device's current namespace table using the stable namespace URI.
     * If the device's namespace index for the given URI has changed since the file was exported,
     * the returned node ID will contain the updated index.
     *
     * <p>Protocols that do not have namespace concepts (Modbus, EtherNet/IP, etc.) should return
     * the {@code nodeId} unchanged.
     *
     * @param nodeId       the node identifier from the import file (e.g. {@code ns=2;s=Temperature})
     * @param namespaceUri the stable namespace URI from the import file (e.g. {@code urn:milo:hello-world})
     * @return the node ID with the namespace index resolved against the live device, or the original
     *         nodeId if resolution is not applicable or namespaceUri is null/empty
     * @throws BrowseException if the namespace URI cannot be resolved (e.g. unknown URI)
     */
    default @NotNull String resolveNodeId(final @NotNull String nodeId, final @Nullable String namespaceUri)
            throws BrowseException {
        return nodeId;
    }
}
