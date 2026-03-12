/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.browse;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import com.hivemq.adapter.sdk.api.discovery.BrowseException;
import com.hivemq.adapter.sdk.api.discovery.BrowsedNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Browses an OPC-UA address space and collects variable nodes with their attributes.
 * Builds {@link BrowsedNode} records with informational fields and generated defaults.
 */
public class OpcUaNodeBrowser {

    private static final long TIMEOUT_SECONDS = 120;
    private static final int READ_BATCH_SIZE = 100;

    // OPC-UA attribute IDs
    private static final int ATTRIBUTE_DATA_TYPE = 14;
    private static final int ATTRIBUTE_ACCESS_LEVEL = 17;
    private static final int ATTRIBUTE_DESCRIPTION = 21;

    private final @NotNull OpcUaClient client;
    private final @NotNull String adapterId;

    public OpcUaNodeBrowser(final @NotNull OpcUaClient client, final @NotNull String adapterId) {
        this.client = client;
        this.adapterId = adapterId;
    }

    /**
     * Browse the OPC-UA address space starting from the given root node.
     *
     * @param rootNodeId the OPC-UA node ID to start from, or null for ObjectsFolder (i=85)
     * @param maxDepth   maximum depth (0 = unlimited)
     * @return list of discovered variable nodes
     * @throws BrowseException if the operation fails
     */
    public @NotNull List<BrowsedNode> browse(final @Nullable String rootNodeId, final int maxDepth)
            throws BrowseException {
        final NodeId browseRoot;
        if (rootNodeId == null || rootNodeId.isBlank()) {
            browseRoot = NodeIds.ObjectsFolder;
        } else {
            final Optional<NodeId> parsed = NodeId.parseSafe(rootNodeId);
            if (parsed.isEmpty()) {
                throw new BrowseException("Invalid OPC-UA node ID: '" + rootNodeId + "'");
            }
            browseRoot = parsed.get();
        }

        try {
            // Phase 1: Browse and collect variable node references with their paths
            final List<DiscoveredVariable> variables = new ArrayList<>();
            browseRecursive(browseRoot, "", maxDepth == 0 ? Integer.MAX_VALUE : maxDepth, variables)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (variables.isEmpty()) {
                return List.of();
            }

            // Phase 2: Batch-read attributes (DataType, AccessLevel, Description)
            return readAttributesAndBuild(variables);
        } catch (final ExecutionException e) {
            throw new BrowseException("Browse operation failed", e.getCause());
        } catch (final TimeoutException e) {
            throw new BrowseException("Browse operation timed out after " + TIMEOUT_SECONDS + " seconds", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrowseException("Browse operation interrupted", e);
        }
    }

    private @NotNull CompletableFuture<Void> browseRecursive(
            final @NotNull NodeId browseRoot,
            final @NotNull String currentPath,
            final int remainingDepth,
            final @NotNull List<DiscoveredVariable> variables) {
        return client.browseAsync(new BrowseDescription(
                        browseRoot,
                        BrowseDirection.Forward,
                        NodeIds.HierarchicalReferences,
                        true,
                        uint(0),
                        uint(BrowseResultMask.All.getValue())))
                .thenCompose(browseResult -> handleBrowseResult(browseResult, currentPath, remainingDepth, variables));
    }

    private @NotNull CompletableFuture<Void> handleBrowseResult(
            final @NotNull BrowseResult browseResult,
            final @NotNull String currentPath,
            final int remainingDepth,
            final @NotNull List<DiscoveredVariable> variables) {
        final var childFutures = new ArrayList<CompletableFuture<Void>>();
        final var references = new ArrayList<ReferenceDescription>();
        final var continuationPoints = new ArrayList<ByteString>();

        if (browseResult.getReferences() != null) {
            Collections.addAll(references, browseResult.getReferences());
        }
        if (browseResult.getContinuationPoint() != null) {
            continuationPoints.add(browseResult.getContinuationPoint());
        }

        final NamespaceTable nsTable = client.getNamespaceTable();

        for (final ReferenceDescription rd : references) {
            final String browseName =
                    rd.getBrowseName() != null ? rd.getBrowseName().getName() : "";
            final String childPath = currentPath + "/" + (browseName != null ? browseName : "");

            if (rd.getNodeClass() == NodeClass.Variable) {
                // Collect variable node
                rd.getNodeId().toNodeId(nsTable).ifPresent(nodeId -> {
                    final int nsIndex = nodeId.getNamespaceIndex().intValue();
                    final String nsUri =
                            nsIndex < nsTable.toArray().length ? nsTable.get(nsIndex) : String.valueOf(nsIndex);
                    variables.add(new DiscoveredVariable(
                            nodeId,
                            childPath,
                            nsUri != null ? nsUri : "",
                            nsIndex,
                            browseName != null ? browseName : ""));
                });
            }

            // Recurse into non-leaf nodes
            if (remainingDepth > 1) {
                rd.getNodeId()
                        .toNodeId(nsTable)
                        .ifPresent(childNodeId -> childFutures.add(
                                browseRecursive(childNodeId, childPath, remainingDepth - 1, variables)));
            }
        }

        // Handle continuation points
        if (!continuationPoints.isEmpty()) {
            final var validCont =
                    continuationPoints.stream().filter(ct -> ct.bytes() != null).toList();
            if (!validCont.isEmpty()) {
                childFutures.add(
                        client.browseNextAsync(false, continuationPoints).thenCompose(nextResult -> {
                            final var allFutures = new ArrayList<CompletableFuture<Void>>();
                            if (nextResult.getResults() != null) {
                                for (final BrowseResult result : nextResult.getResults()) {
                                    if (result != null) {
                                        allFutures.add(
                                                handleBrowseResult(result, currentPath, remainingDepth, variables));
                                    }
                                }
                            }
                            return CompletableFuture.allOf(allFutures.toArray(CompletableFuture[]::new));
                        }));
            }
        }

        return CompletableFuture.allOf(childFutures.toArray(CompletableFuture[]::new));
    }

    private @NotNull List<BrowsedNode> readAttributesAndBuild(final @NotNull List<DiscoveredVariable> variables)
            throws BrowseException {
        final List<BrowsedNode> result = new ArrayList<>();

        // Process in batches of READ_BATCH_SIZE
        for (int offset = 0; offset < variables.size(); offset += READ_BATCH_SIZE) {
            final int end = Math.min(offset + READ_BATCH_SIZE, variables.size());
            final List<DiscoveredVariable> batch = variables.subList(offset, end);

            // Build read requests: 3 attributes per node (DataType, AccessLevel, Description)
            final List<ReadValueId> readValueIds = new ArrayList<>();
            for (final DiscoveredVariable var : batch) {
                readValueIds.add(new ReadValueId(var.nodeId, uint(ATTRIBUTE_DATA_TYPE), null, null));
                readValueIds.add(new ReadValueId(var.nodeId, uint(ATTRIBUTE_ACCESS_LEVEL), null, null));
                readValueIds.add(new ReadValueId(var.nodeId, uint(ATTRIBUTE_DESCRIPTION), null, null));
            }

            try {
                final DataValue[] values = client.readAsync(0.0, TimestampsToReturn.Neither, readValueIds)
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .getResults();

                for (int i = 0; i < batch.size(); i++) {
                    final DiscoveredVariable var = batch.get(i);
                    final DataValue dataTypeValue = values[i * 3];
                    final DataValue accessLevelValue = values[i * 3 + 1];
                    final DataValue descriptionValue = values[i * 3 + 2];

                    final String dataType = resolveDataTypeName(dataTypeValue);
                    final String accessLevel = resolveAccessLevel(accessLevelValue);
                    final String description = resolveDescription(descriptionValue);

                    result.add(new BrowsedNode(
                            var.path,
                            var.namespaceUri,
                            var.namespaceIndex,
                            var.nodeId.toParseableString(),
                            dataType,
                            accessLevel,
                            description,
                            generateTagNameDefault(var.browseName),
                            description,
                            generateNorthboundTopicDefault(var.path),
                            generateSouthboundTopicDefault(var.path)));
                }
            } catch (final ExecutionException e) {
                throw new BrowseException("Failed to read node attributes", e.getCause());
            } catch (final TimeoutException e) {
                throw new BrowseException("Attribute read timed out after " + TIMEOUT_SECONDS + " seconds", e);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BrowseException("Attribute read interrupted", e);
            }
        }

        return result;
    }

    // --- Attribute resolution ---

    private @NotNull String resolveDataTypeName(final @NotNull DataValue dataTypeValue) {
        if (dataTypeValue.getValue().getValue() instanceof final NodeId dataTypeNodeId) {
            // Try to resolve the data type NodeId to a human-readable name
            return resolveBuiltinDataTypeName(dataTypeNodeId);
        }
        return "Unknown";
    }

    private @NotNull String resolveBuiltinDataTypeName(final @NotNull NodeId dataTypeNodeId) {
        // Common OPC-UA built-in data types (namespace 0)
        if (dataTypeNodeId.getNamespaceIndex().intValue() == 0) {
            final Object id = dataTypeNodeId.getIdentifier();
            if (id instanceof final Number num) {
                return switch (num.intValue()) {
                    case 1 -> "Boolean";
                    case 2 -> "SByte";
                    case 3 -> "Byte";
                    case 4 -> "Int16";
                    case 5 -> "UInt16";
                    case 6 -> "Int32";
                    case 7 -> "UInt32";
                    case 8 -> "Int64";
                    case 9 -> "UInt64";
                    case 10 -> "Float";
                    case 11 -> "Double";
                    case 12 -> "String";
                    case 13 -> "DateTime";
                    case 14 -> "Guid";
                    case 15 -> "ByteString";
                    case 22 -> "ExtensionObject";
                    case 24 -> "BaseDataType";
                    case 26 -> "Number";
                    case 28 -> "Integer";
                    case 29 -> "UInteger";
                    default -> dataTypeNodeId.toParseableString();
                };
            }
        }
        return dataTypeNodeId.toParseableString();
    }

    private @NotNull String resolveAccessLevel(final @NotNull DataValue accessLevelValue) {
        if (accessLevelValue.getValue().getValue() instanceof final UByte accessByte) {
            final int level = accessByte.intValue();
            final boolean readable = (level & 0x01) != 0;
            final boolean writable = (level & 0x02) != 0;
            if (readable && writable) return "READ_WRITE";
            if (readable) return "READ";
            if (writable) return "WRITE";
            return "NONE";
        }
        return "READ";
    }

    private @Nullable String resolveDescription(final @NotNull DataValue descriptionValue) {
        if (descriptionValue.getValue().getValue() instanceof final LocalizedText text) {
            return text.getText();
        }
        return null;
    }

    // --- Default generation ---

    @NotNull
    String generateTagNameDefault(final @NotNull String browseName) {
        // {adapterId}-{sanitizedBrowseName} — lowercase, non-alphanumeric → -, collapse consecutive, strip edges
        return adapterId + "-" + sanitize(browseName);
    }

    @NotNull
    String generateNorthboundTopicDefault(final @NotNull String path) {
        // {adapterId}/{sanitizedPath}
        return adapterId + "/" + sanitizePath(path);
    }

    @NotNull
    String generateSouthboundTopicDefault(final @NotNull String path) {
        // {adapterId}/write/{sanitizedPath}
        return adapterId + "/write/" + sanitizePath(path);
    }

    /**
     * Sanitize a single name: lowercase, non-alphanumeric to '-', collapse consecutive dashes, strip edges.
     */
    static @NotNull String sanitize(final @NotNull String input) {
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    /**
     * Sanitize a path: strip leading '/', sanitize each segment, join with '/'.
     */
    static @NotNull String sanitizePath(final @NotNull String path) {
        final String stripped = path.startsWith("/") ? path.substring(1) : path;
        if (stripped.isEmpty()) return "";
        final String[] segments = stripped.split("/");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(sanitize(segments[i]));
        }
        return sb.toString();
    }

    private record DiscoveredVariable(
            @NotNull NodeId nodeId,
            @NotNull String path,
            @NotNull String namespaceUri,
            int namespaceIndex,
            @NotNull String browseName) {}
}
