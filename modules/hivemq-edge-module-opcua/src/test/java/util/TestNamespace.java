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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static util.EmbeddedOpcUaServerExtension.NS_URI;

public class TestNamespace extends ManagedNamespaceWithLifecycle {

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

            dynamicFolder = new UaFolderNode(getNodeContext(),
                    folderNodeId,
                    newQualifiedName("DynamicFolder"),
                    LocalizedText.english("DynamicFolder"));

            getNodeManager().addNode(dynamicFolder);

            // Make sure our new folder shows up under the server's Objects folder.
            dynamicFolder.addReference(new Reference(dynamicFolder.getNodeId(),
                    NodeIds.Organizes,
                    NodeIds.ObjectsFolder.expanded(),
                    false));

            testFolder = new UaFolderNode(getNodeContext(),
                    folderNodeId,
                    newQualifiedName("TestFolder"),
                    LocalizedText.english("TestFolder"));

            getNodeManager().addNode(testFolder);

            // Make sure our new folder shows up under the server's Objects folder.
            testFolder.addReference(new Reference(testFolder.getNodeId(),
                    NodeIds.Organizes,
                    NodeIds.ObjectsFolder.expanded(),
                    false));

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
        final UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext()).setNodeId(nodeId)
                .setAccessLevel(AccessLevel.READ_WRITE)
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(NodeIds.BaseDataVariableType)
                .build();

        node.setValue(new DataValue(new Variant(initialValue)));

        node.getFilterChain()
                .addLast(AttributeFilters.getValue(ctx -> new DataValue(new Variant(valueCallback.get()))));

        getNodeManager().addNode(node);
        requireNonNull(dynamicFolder).addOrganizes(node);
    }

    private @NotNull String addTestNode(
            final @NotNull String name,
            final @NotNull NodeId typeId,
            final @NotNull Supplier<Object> valueCallback,
            final @NotNull NodeId nodeId) {
        final UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext()).setNodeId(nodeId)
                .setAccessLevel(AccessLevel.READ_WRITE)
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(NodeIds.BaseDataVariableType)
                .build();

        node.setValue(new DataValue(new Variant(null)));

        node.getFilterChain()
                .addLast(AttributeFilters.getValue(ctx -> new DataValue(new Variant(valueCallback.get()))));

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
}
