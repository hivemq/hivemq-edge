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
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;
import static util.EmbeddedOpcUaServerExtension.NS_URI;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.items.EventItem;
import org.eclipse.milo.opcua.sdk.server.items.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.model.objects.AlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilters;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        final UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(nodeId)
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

    public @NotNull String addArrayNode(
            final @NotNull String name,
            final @NotNull NodeId typeId,
            final @NotNull Supplier<Object> valueCallback,
            final long nodeIdPart,
            final int dimension) {
        return addTestArrayNode(name, typeId, valueCallback, newNodeId(nodeIdPart), dimension);
    }

    // ── Conditions ──────────────────────────────────────────────────────────────────────────────────────
    //
    // A condition is an object that emits events when its state changes. Making one subscribable takes two
    // separate things, and having only the first is the silent failure mode:
    //
    //  1. The EventNotifier attribute must have the SubscribeToEvents bit. This is what lets a client create
    //     an event-mode MonitoredItem on the node.
    //  2. The namespace owning the node must register those items with the server's EventNotifier, by
    //     implementing onEventItemsCreated/Deleted (see below). This is what decides whether a fired event
    //     is delivered at all.
    //
    // With only (1) the subscription is accepted with status Good and then never receives anything: the
    // items are created and immediately forgotten. Nothing reports an error, which makes a namespace missing
    // (2) look like a broken client.

    @Override
    public void onEventItemsCreated(final @NotNull List<EventItem> eventItems) {
        // An EventItem is itself an EventListener. Milo does no source-node filtering when an event is
        // fired — it notifies every registered listener and lets each one apply its own WhereClause — so
        // registering here is exactly what makes events reach a client subscribed to a node of ours.
        eventItems.forEach(item -> getServer().getEventNotifier().register(item));
    }

    @Override
    public void onEventItemsDeleted(final @NotNull List<EventItem> eventItems) {
        eventItems.forEach(item -> getServer().getEventNotifier().unregister(item));
    }

    /**
     * Adds an object that can emit condition events.
     *
     * @param name       the browse name of the condition object.
     * @param nodeIdPart the identifier part of its node id.
     * @return the node id, in the parseable form a tag definition uses.
     */
    public @NotNull String addConditionNode(final @NotNull String name, final long nodeIdPart) {
        final NodeId nodeId = newNodeId(nodeIdPart);
        final UaObjectNode node = new UaObjectNode.UaObjectNodeBuilder(getNodeContext())
                .setNodeId(nodeId)
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setTypeDefinition(NodeIds.BaseObjectType)
                .build();

        // SubscribeToEvents — without this the node is not a valid event source.
        node.setEventNotifier(ubyte(1));

        getNodeManager().addNode(node);
        requireNonNull(testFolder).addOrganizes(node);
        return nodeId.toParseableString();
    }

    /**
     * Fires one alarm event from the given condition node, as a server would when the condition changes
     * state. The fields set here are the ones a client selects for a condition tag.
     *
     * @param conditionNodeId the node the event originates from.
     * @param message         the alarm message.
     * @param severity        the alarm severity (1-1000).
     * @param active          whether the alarm is active after this transition.
     * @return the {@code EventId} of the fired event — the token identifying this transition.
     */
    public @NotNull ByteString fireAlarm(
            final @NotNull NodeId conditionNodeId,
            final @NotNull String message,
            final int severity,
            final boolean active) {
        try {
            final AlarmConditionTypeNode event = (AlarmConditionTypeNode)
                    getServer().getEventFactory().createEvent(newNodeId(UUID.randomUUID()), NodeIds.AlarmConditionType);

            final ByteString eventId =
                    new ByteString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            event.setEventId(eventId);
            event.setEventType(NodeIds.AlarmConditionType);
            event.setSourceNode(conditionNodeId);
            event.setSourceName(conditionNodeId.toParseableString());
            event.setTime(DateTime.now());
            event.setReceiveTime(DateTime.now());
            event.setMessage(LocalizedText.english(message));
            event.setSeverity(ushort(severity));
            event.setRetain(active);
            event.setActiveState(LocalizedText.english(active ? "Active" : "Inactive"));
            event.setAckedState(LocalizedText.english("Unacknowledged"));

            getServer().getEventNotifier().fire(event);
            return eventId;
        } catch (final UaException e) {
            throw new IllegalStateException("Could not create the alarm event", e);
        }
    }
}
