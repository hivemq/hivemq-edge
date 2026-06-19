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
package util;

import static java.util.Objects.requireNonNull;
import static util.EmbeddedOpcUaServerExtension.NS_URI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.items.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilters;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestNamespace extends ManagedNamespaceWithLifecycle {

    private static final @NotNull NodeId[] LARGE_TREE_DATA_TYPES = {
        NodeIds.Boolean, NodeIds.Int32, NodeIds.Int64, NodeIds.Double, NodeIds.String
    };
    private final @NotNull SubscriptionModel subscriptionModel;
    private @Nullable UaFolderNode dynamicFolder;
    private @Nullable UaFolderNode testFolder;

    public TestNamespace(final @NotNull OpcUaServer server) {
        super(server, NS_URI);
        subscriptionModel = new SubscriptionModel(server, this);
        getLifecycleManager().addLifecycle(subscriptionModel);
        getLifecycleManager().addStartupTask(() -> {
            // Create a "HelloWorld" folder and add it to the node manager
            final NodeId folderNodeId = newNodeId("TestFolder");

            dynamicFolder = new UaFolderNode(
                    getNodeContext(),
                    folderNodeId,
                    newQualifiedName("DynamicFolder"),
                    LocalizedText.english("DynamicFolder"));

            getNodeManager().addNode(dynamicFolder);

            // Make sure our new folder shows up under the server's Objects folder.
            dynamicFolder.addReference(new Reference(
                    dynamicFolder.getNodeId(), NodeIds.Organizes, NodeIds.ObjectsFolder.expanded(), false));

            testFolder = new UaFolderNode(
                    getNodeContext(),
                    folderNodeId,
                    newQualifiedName("TestFolder"),
                    LocalizedText.english("TestFolder"));

            getNodeManager().addNode(testFolder);

            // Make sure our new folder shows up under the server's Objects folder.
            testFolder.addReference(
                    new Reference(testFolder.getNodeId(), NodeIds.Organizes, NodeIds.ObjectsFolder.expanded(), false));

            addDynamicNodes();
        });
    }

    @Override
    public void onDataItemsCreated(final @NotNull List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(final @NotNull List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(final @NotNull List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(final @NotNull List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    private void addDynamicNodes() {
        final Random random = new Random();
        addDefaultNode("Bool", NodeIds.Boolean, true, random::nextBoolean, newNodeId(10));
        addDefaultNode("Int32", NodeIds.Int32, 50, random::nextInt, newNodeId(11));
        addDefaultNode("Int64", NodeIds.Int64, 5000, random::nextLong, newNodeId(12));
        addDefaultNode("Double", NodeIds.Double, 123.4d, random::nextDouble, newNodeId(13));
        addDefaultNode("String", NodeIds.String, "abc", () -> DateTime.now().toString(), newNodeId("abc"));
    }

    private void addDefaultNode(
            final @NotNull String name,
            final @NotNull NodeId typeId,
            final @NotNull Object initialValue,
            final @NotNull Supplier<Object> valueCallback,
            final @NotNull NodeId nodeId) {
        final UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(nodeId)
                .setAccessLevel(AccessLevel.READ_WRITE)
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(NodeIds.BaseDataVariableType)
                .build();

        node.setValue(new DataValue(new Variant(initialValue)));

        node.getFilterChain().addLast(AttributeFilters.getValue(_ -> new DataValue(new Variant(valueCallback.get()))));

        getNodeManager().addNode(node);
        requireNonNull(dynamicFolder).addOrganizes(node);
    }

    private @NotNull String addTestNode(
            final @NotNull String name,
            final @NotNull NodeId typeId,
            final @NotNull Supplier<Object> valueCallback,
            final @NotNull NodeId nodeId) {
        final UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(nodeId)
                .setAccessLevel(AccessLevel.READ_WRITE)
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(NodeIds.BaseDataVariableType)
                .build();

        node.setValue(new DataValue(new Variant(null)));

        node.getFilterChain().addLast(AttributeFilters.getValue(_ -> new DataValue(new Variant(valueCallback.get()))));

        getNodeManager().addNode(node);
        requireNonNull(dynamicFolder).addOrganizes(node);

        return nodeId.toParseableString();
    }

    private @NotNull String addTestArrayNode(
            final @NotNull String name,
            final @NotNull NodeId typeId,
            final @NotNull Supplier<Object> valueCallback,
            final @NotNull NodeId nodeId,
            final int dimension) {
        final UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(nodeId)
                .setAccessLevel(AccessLevel.READ_WRITE)
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setArrayDimensions(new UInteger[] {UInteger.valueOf(dimension)})
                .setTypeDefinition(NodeIds.BaseDataVariableType)
                .build();

        node.setValue(new DataValue(new Variant(null)));

        node.getFilterChain().addLast(AttributeFilters.getValue(_ -> new DataValue(new Variant(valueCallback.get()))));

        getNodeManager().addNode(node);
        requireNonNull(dynamicFolder).addOrganizes(node);

        return nodeId.toParseableString();
    }

    public @NotNull String addNode(
            final @NotNull String name,
            final @NotNull NodeId typeId,
            final @NotNull Supplier<Object> valueCallback,
            final long nodeIdPart) {
        return addTestNode(name, typeId, valueCallback, newNodeId(nodeIdPart));
    }

    public @NotNull String addArrayNode(
            final @NotNull String name,
            final @NotNull NodeId typeId,
            final @NotNull Supplier<Object> valueCallback,
            final long nodeIdPart,
            final int dimension) {
        return addTestArrayNode(name, typeId, valueCallback, newNodeId(nodeIdPart), dimension);
    }

    /**
     * Grow a deterministic, deep, branching address space under a fresh {@code LargeRoot} folder (organized
     * under Objects): {@code breadth} child folders per level, {@code depth} levels, {@code varsPerFolder}
     * variables per folder with rotating datatypes. Two structural torture cases are seeded so a browse engine
     * is really tested: one variable organized under <i>two</i> folders (the visited-set must dedup it to a
     * single discovery), and a back-reference from the last folder to the root (a cycle the walk must terminate
     * on rather than loop). Nodes are added to the running server's node manager, so call this after startup.
     *
     * @return the expected shape — every unique variable with its declared datatype, the shared node, and the
     *         total folder count — so a test can assert completeness exactly.
     */
    public @NotNull LargeTree growLargeTree(final int breadth, final int depth, final int varsPerFolder) {
        final long[] counter = {0};
        final List<ExpectedVar> variables = new ArrayList<>();
        final List<UaFolderNode> folders = new ArrayList<>();

        final NodeId rootId = newNodeId("large/root");
        final UaFolderNode root = makeFolder(rootId, "LargeRoot");
        root.addReference(new Reference(rootId, NodeIds.Organizes, NodeIds.ObjectsFolder.expanded(), false));
        folders.add(root);

        buildLevel(root, "", depth, breadth, varsPerFolder, counter, variables, folders);

        // torture case 1 — one variable organized under two distinct folders: must be discovered exactly once.
        // Breadth-first discovery reaches it first under the root (folders.getFirst()), so its path is "/shared".
        final NodeId sharedId = newNodeId("large/shared/" + counter[0]++);
        final UaVariableNode shared = makeVariable(sharedId, "shared", NodeIds.Double, folders.getFirst());
        folders.getLast().addOrganizes(shared);
        variables.add(new ExpectedVar(sharedId.toParseableString(), NodeIds.Double.toParseableString(), "/shared"));

        // torture case 2 — a forward back-edge from the last folder to the root: the walk must terminate
        final UaFolderNode last = folders.getLast();
        last.addReference(new Reference(last.getNodeId(), NodeIds.Organizes, rootId.expanded(), true));

        return new LargeTree(
                rootId.toParseableString(), List.copyOf(variables), sharedId.toParseableString(), folders.size());
    }

    private void buildLevel(
            final @NotNull UaFolderNode parent,
            final @NotNull String parentPath,
            final int depth,
            final int breadth,
            final int varsPerFolder,
            final long @NotNull [] counter,
            final @NotNull List<ExpectedVar> variables,
            final @NotNull List<UaFolderNode> folders) {
        for (int i = 0; i < varsPerFolder; i++) {
            final long n = counter[0]++;
            final NodeId typeId = LARGE_TREE_DATA_TYPES[(int) (n % LARGE_TREE_DATA_TYPES.length)];
            final NodeId id = newNodeId("large/v/" + n);
            final String name = "v" + n;
            makeVariable(id, name, typeId, parent);
            variables.add(new ExpectedVar(id.toParseableString(), typeId.toParseableString(), parentPath + "/" + name));
        }
        if (depth <= 1) {
            return;
        }
        for (int b = 0; b < breadth; b++) {
            final long n = counter[0]++;
            final NodeId id = newNodeId("large/f/" + n);
            final String name = "f" + n;
            final UaFolderNode child = makeFolder(id, name);
            parent.addOrganizes(child);
            folders.add(child);
            buildLevel(child, parentPath + "/" + name, depth - 1, breadth, varsPerFolder, counter, variables, folders);
        }
    }

    private @NotNull UaFolderNode makeFolder(final @NotNull NodeId nodeId, final @NotNull String name) {
        final UaFolderNode folder =
                new UaFolderNode(getNodeContext(), nodeId, newQualifiedName(name), LocalizedText.english(name));
        getNodeManager().addNode(folder);
        return folder;
    }

    private @NotNull UaVariableNode makeVariable(
            final @NotNull NodeId nodeId,
            final @NotNull String name,
            final @NotNull NodeId typeId,
            final @NotNull UaFolderNode parent) {
        final UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(nodeId)
                .setAccessLevel(AccessLevel.READ_WRITE)
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(NodeIds.BaseDataVariableType)
                .build();
        node.setValue(new DataValue(new Variant(null)));
        getNodeManager().addNode(node);
        parent.addOrganizes(node);
        return node;
    }

    /**
     * One expected variable: its parseable node id, the parseable id of its declared datatype, and its path
     * (assembled from browse names, root contributing no segment — e.g. {@code "/f3/v17"}).
     */
    public record ExpectedVar(
            @NotNull String nodeId,
            @NotNull String dataTypeId,
            @NotNull String path) {}

    /**
     * The expected shape of a {@link #growLargeTree} address space.
     */
    public record LargeTree(
            @NotNull String rootNodeId,
            @NotNull List<ExpectedVar> variables,
            @NotNull String sharedNodeId,
            int folderCount) {}
}
