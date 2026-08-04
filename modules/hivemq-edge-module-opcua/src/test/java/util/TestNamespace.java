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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.items.EventItem;
import org.eclipse.milo.opcua.sdk.server.items.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.methods.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.model.objects.AlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilters;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.DiagnosticInfo;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
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

    private final @NotNull AtomicInteger refreshBracketCount = new AtomicInteger();

    /**
     * How many {@code RefreshStart}/{@code RefreshEnd} events the server has emitted.
     * <p>
     * The visible consequence of a successful {@code ConditionRefresh}. The call itself cannot be observed
     * here: OPC 10000-9 §5.5.7 fixes its ObjectId as the well-known {@code ConditionType} in namespace 0, so
     * it is handled by Milo's own namespace and never reaches this one's method handlers.
     */
    public int refreshBracketCount() {
        return refreshBracketCount.get();
    }

    /**
     * Counts the refresh bracket at the server, independently of any client subscription — so a test can
     * assert the refresh happened even though the adapter deliberately drops these events.
     */
    public void observeRefreshEvents() {
        getServer().getEventNotifier().register(event -> {
            final NodeId eventType = event.getEventType();
            if (NodeIds.RefreshStartEventType.equals(eventType) || NodeIds.RefreshEndEventType.equals(eventType)) {
                refreshBracketCount.incrementAndGet();
            }
        });
    }

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
        return addConditionNode(name, nodeIdPart, NodeIds.AlarmConditionType);
    }

    /**
     * Adds a condition of a specific type, so a test can exercise the type verification — including a
     * deliberate mismatch.
     */
    public @NotNull String addConditionNode(
            final @NotNull String name, final long nodeIdPart, final @NotNull NodeId typeDefinition) {
        final NodeId nodeId = newNodeId(nodeIdPart);
        final UaObjectNode node = new UaObjectNode.UaObjectNodeBuilder(getNodeContext())
                .setNodeId(nodeId)
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setTypeDefinition(typeDefinition)
                .build();

        // Deliberately NOT an event notifier. ConditionType defines no EventNotifier attribute, so a
        // conformant server does not let a client subscribe to a condition directly. An earlier version of
        // this harness set the SubscribeToEvents bit here, which made a wrong implementation look correct.
        getNodeManager().addNode(node);
        requireNonNull(testFolder).addOrganizes(node);

        // Wired the way OPC 10000-9 §6.2/§6.3 prescribes: the area notifier has a ConditionSource beneath it
        // by HasEventSource, and the condition hangs off that source by HasCondition. The condition itself is
        // NOT the target of HasEventSource -- an earlier version of this harness attached it there directly,
        // which made the adapter's walk look correct while it was skipping the source entirely.
        final NodeId source = conditionSourceFor(name, nodeIdPart);
        final UaNode sourceNode = getNodeManager().get(source);
        if (sourceNode != null) {
            sourceNode.addReference(new Reference(source, NodeIds.HasCondition, nodeId.expanded(), true));
        }
        node.addReference(new Reference(nodeId, NodeIds.HasCondition, source.expanded(), false));

        return nodeId.toParseableString();
    }

    /**
     * Adds a condition wired the way the specification does <em>not</em> describe: {@code HasEventSource}
     * straight from the area notifier to the condition, with no ConditionSource in between.
     * <p>
     * Servers do this, and it is what an earlier version of this harness modelled exclusively. Kept as a
     * distinct case so the resolver's fallback has coverage: the conformant path is what
     * {@link #addConditionNode} now builds, and a fix that only handled that one would silently strand every
     * device laid out like this.
     */
    public @NotNull String addDirectlyAttachedConditionNode(final @NotNull String name, final long nodeIdPart) {
        final NodeId nodeId = newNodeId(nodeIdPart);
        final UaObjectNode node = new UaObjectNode.UaObjectNodeBuilder(getNodeContext())
                .setNodeId(nodeId)
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setTypeDefinition(NodeIds.AlarmConditionType)
                .build();
        getNodeManager().addNode(node);
        requireNonNull(testFolder).addOrganizes(node);

        final NodeId notifier = areaNotifier();
        node.addReference(new Reference(nodeId, NodeIds.HasEventSource, notifier.expanded(), false));
        final UaNode notifierNode = getNodeManager().get(notifier);
        if (notifierNode != null) {
            notifierNode.addReference(new Reference(notifier, NodeIds.HasEventSource, nodeId.expanded(), true));
        }
        return nodeId.toParseableString();
    }

    /**
     * Adds a condition with no path to any notifier — nothing can be subscribed for it.
     * <p>
     * Servers do under-populate the inverse references the walk relies on, which is precisely why the tag
     * definition has an explicit notifier field. This models that server.
     */
    public @NotNull String addOrphanConditionNode(final @NotNull String name, final long nodeIdPart) {
        final NodeId nodeId = newNodeId(nodeIdPart);
        final UaObjectNode node = new UaObjectNode.UaObjectNodeBuilder(getNodeContext())
                .setNodeId(nodeId)
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setTypeDefinition(NodeIds.AlarmConditionType)
                .build();
        getNodeManager().addNode(node);
        requireNonNull(testFolder).addOrganizes(node);
        return nodeId.toParseableString();
    }

    /**
     * The ConditionSource a condition hangs off — the node that sits in the notifier hierarchy.
     * <p>
     * Modelled as a Variable rather than an Object on purpose: §6.3's own worked example makes
     * {@code LevelMeasurement}, a Variable, a ConditionSource, and a walk that masks its browse to Objects
     * would step straight past it. Deliberately not an event notifier either, so the walk has to continue up
     * to the area rather than stopping here.
     *
     * @param name       the condition's name, used to derive the source's.
     * @param nodeIdPart the condition's identifier part; the source takes a distinct one derived from it.
     */
    private synchronized @NotNull NodeId conditionSourceFor(final @NotNull String name, final long nodeIdPart) {
        final NodeId sourceId = newNodeId(nodeIdPart + 100_000L);
        if (getNodeManager().get(sourceId) == null) {
            final UaVariableNode source = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                    .setNodeId(sourceId)
                    .setBrowseName(newQualifiedName(name + "Source"))
                    .setDisplayName(LocalizedText.english(name + "Source"))
                    .setDataType(NodeIds.Double)
                    .setTypeDefinition(NodeIds.BaseDataVariableType)
                    .build();
            source.setValue(new DataValue(new Variant(0.0)));
            getNodeManager().addNode(source);
            requireNonNull(testFolder).addOrganizes(source);

            // area --HasEventSource--> source, which is the leg §5.12 requires the ConditionSource to be the
            // target of. The walk reverses it to get from the source up to the area.
            final NodeId notifier = areaNotifier();
            final UaNode notifierNode = getNodeManager().get(notifier);
            if (notifierNode != null) {
                notifierNode.addReference(new Reference(notifier, NodeIds.HasEventSource, sourceId.expanded(), true));
            }
            source.addReference(new Reference(sourceId, NodeIds.HasEventSource, notifier.expanded(), false));
        }
        return sourceId;
    }

    /**
     * The area notifier every condition in this namespace hangs off, created on first use.
     * <p>
     * Real servers expose areas (a plant, a line, a machine) as notifiers and place conditions beneath them.
     * Modelling one here is what makes the notifier walk testable at all.
     */
    public synchronized @NotNull NodeId areaNotifier() {
        final NodeId notifierId = newNodeId("AreaNotifier");
        if (getNodeManager().get(notifierId) == null) {
            final UaObjectNode notifier = new UaObjectNode.UaObjectNodeBuilder(getNodeContext())
                    .setNodeId(notifierId)
                    .setBrowseName(newQualifiedName("AreaNotifier"))
                    .setDisplayName(LocalizedText.english("AreaNotifier"))
                    .setTypeDefinition(NodeIds.BaseObjectType)
                    .build();
            // SubscribeToEvents: this is the node a client may place an event MonitoredItem on.
            notifier.setEventNotifier(ubyte(1));
            getNodeManager().addNode(notifier);
            requireNonNull(testFolder).addOrganizes(notifier);

            // Server --HasNotifier--> area, so a walk upward from the area reaches the Server object too.
            notifier.addReference(new Reference(notifierId, NodeIds.HasNotifier, NodeIds.Server.expanded(), false));
        }
        return notifierId;
    }

    /**
     * One invocation of a condition method, as the server saw it.
     *
     * @param methodName   which method was called — Acknowledge or Confirm.
     * @param eventId      the transition the caller is responding to.
     * @param comment      the free text that came with it, flattened for convenience.
     * @param rawComment   the comment argument as it arrived. Null when the argument itself was absent;
     *                     otherwise the {@code LocalizedText}, whose own null-ness carries the §5.7.3
     *                     leave-alone-versus-erase distinction that {@code comment} loses.
     */
    public record MethodCall(
            @NotNull String methodName,
            @NotNull ByteString eventId,
            @NotNull String comment,
            @Nullable Double duration,
            @Nullable LocalizedText rawComment) {}

    private final @NotNull List<MethodCall> methodCalls = new CopyOnWriteArrayList<>();

    /**
     * Every condition method invoked on this namespace so far, in order.
     */
    public @NotNull List<MethodCall> methodCalls() {
        return List.copyOf(methodCalls);
    }

    /**
     * The calls a test itself provoked, with {@code ConditionRefresh} filtered out.
     * <p>
     * Edge asks for a refresh whenever it establishes a subscription, so that call is present in every test
     * that subscribes a condition and says nothing about what the test did. A test asserting on a write it
     * made wants this; a test asserting on the refresh itself wants {@link #methodCalls()}.
     */
    public @NotNull List<MethodCall> methodCallsExcludingRefresh() {
        return methodCalls.stream()
                .filter(call -> !"ConditionRefresh".equals(call.methodName()))
                .toList();
    }

    /**
     * Adds a condition that can be acknowledged and confirmed.
     * <p>
     * Beyond being an event source, the node carries the two standard {@code AcknowledgeableConditionType}
     * methods, each with a handler that records the arguments. Recording rather than modelling is deliberate:
     * what a southbound test needs to establish is that Edge invoked the right method on the right condition
     * with the {@code EventId} it was given, and a real state machine here would obscure that.
     *
     * @param name       the browse name of the condition object.
     * @param nodeIdPart the identifier part of its node id.
     * @return the node id, in the parseable form a tag definition uses.
     */
    /**
     * Adds a condition exposing only the base methods, with none of the {@code "2"} variants.
     * <p>
     * Models a server implementing the original method set. Both forms are Optional and independent in the
     * specification, so this is a conformant server rather than a deficient one — and it is the case where a
     * user's comment cannot be recorded, which is what the fallback exists for.
     */
    public @NotNull String addConditionNodeWithoutCommentedMethods(final @NotNull String name, final long nodeIdPart) {
        final String conditionNodeId = addConditionNode(name, nodeIdPart);
        final NodeId nodeId = NodeId.parse(conditionNodeId);

        long offset = 1000;
        for (final String methodName :
                List.of("Acknowledge", "Confirm", "AddComment", "Suppress", "Unsuppress", "Reset")) {
            addConditionMethod(nodeId, methodName, nodeIdPart + offset);
            offset += 1000;
        }
        return conditionNodeId;
    }

    public @NotNull String addAcknowledgeableConditionNode(final @NotNull String name, final long nodeIdPart) {
        final String conditionNodeId = addConditionNode(name, nodeIdPart);
        final NodeId nodeId = NodeId.parse(conditionNodeId);

        // Every method a condition exposes, so a test can exercise the whole southbound surface. The offsets
        // only have to be distinct; they carry no meaning.
        long offset = 1000;
        for (final String methodName : List.of(
                "Acknowledge",
                "Confirm",
                "AddComment",
                "Enable",
                "Disable",
                "Silence",
                "Suppress",
                "Unsuppress",
                "RemoveFromService",
                "PlaceInService",
                "Reset",
                // The "2" variants, which take a Comment where their base form takes nothing. A server may
                // expose either or both -- see addConditionNodeWithoutCommentedMethods for the other case.
                "Suppress2",
                "Unsuppress2",
                "RemoveFromService2",
                "PlaceInService2",
                "Reset2",
                // Defined on ConditionType, so a server offers it on its condition instances. Edge calls it
                // after every (re)connect to recover the current alarm picture.
                "ConditionRefresh")) {
            addConditionMethod(nodeId, methodName, nodeIdPart + offset);
            offset += 1000;
        }

        // Shelving lives on a nested ShelvingState object rather than on the condition, so a test that only
        // hung these off the condition would not exercise the descent the adapter has to make.
        final NodeId shelvingStateId = newNodeId(nodeIdPart + 500);
        final UaObjectNode shelvingState = new UaObjectNode.UaObjectNodeBuilder(getNodeContext())
                .setNodeId(shelvingStateId)
                .setBrowseName(new QualifiedName(0, "ShelvingState"))
                .setDisplayName(LocalizedText.english("ShelvingState"))
                .setTypeDefinition(NodeIds.ShelvedStateMachineType)
                .build();
        getNodeManager().addNode(shelvingState);
        final UaNode conditionNode = getNodeManager().get(nodeId);
        if (conditionNode != null) {
            conditionNode.addReference(new Reference(
                    nodeId, NodeIds.HasComponent, shelvingState.getNodeId().expanded(), true));
        }
        for (final String methodName :
                List.of("Unshelve", "OneShotShelve", "TimedShelve", "Unshelve2", "OneShotShelve2", "TimedShelve2")) {
            addConditionMethod(shelvingStateId, methodName, nodeIdPart + offset);
            offset += 1000;
        }

        return conditionNodeId;
    }

    /**
     * Exposes one method on a node, with a handler that records the call.
     * <p>
     * Only the instance's own method node is created, which is the form every server offers and the one the
     * adapter prefers. A server that exposes no condition instance at all is modelled separately by
     * {@link #addConditionNodeWithoutMethods}.
     */
    private void addConditionMethod(
            final @NotNull NodeId parentNodeId, final @NotNull String methodName, final long instanceNodeIdPart) {

        final MethodInvocationHandler handler = (context, request) -> {
            final Variant[] arguments = request.getInputArguments();
            // Arguments are found by type rather than by position, because the position differs per method:
            // Acknowledge(EventId, Comment) puts the comment second, Suppress2(Comment) first, and
            // TimedShelve2(ShelvingTime, Comment) second again. Each of the three types appears at most once
            // in any signature, so a type search is unambiguous and survives adding further variants.
            methodCalls.add(new MethodCall(
                    methodName,
                    firstOfType(arguments, ByteString.class, ByteString.NULL_VALUE),
                    String.valueOf(firstOfType(arguments, LocalizedText.class, LocalizedText.NULL_VALUE)
                            .getText()),
                    firstOfType(arguments, Double.class, null),
                    firstOfType(arguments, LocalizedText.class, null)));
            return new CallMethodResult(StatusCode.GOOD, new StatusCode[0], new DiagnosticInfo[0], new Variant[0]);
        };

        final UaMethodNode method = UaMethodNode.builder(getNodeContext())
                .setNodeId(newNodeId(instanceNodeIdPart))
                .setBrowseName(new QualifiedName(0, methodName))
                .setDisplayName(LocalizedText.english(methodName))
                .setExecutable(true)
                .setUserExecutable(true)
                .build();
        method.setInvocationHandler(handler);
        getNodeManager().addNode(method);

        final UaNode parent = getNodeManager().get(parentNodeId);
        if (parent != null) {
            parent.addReference(new Reference(
                    parentNodeId, NodeIds.HasComponent, method.getNodeId().expanded(), true));
        }
    }

    /**
     * The first argument of the given type, or {@code fallback} when the signature has none.
     * <p>
     * Positional lookup was wrong once the {@code "2"} variants arrived: the comment is argument 1 of
     * {@code Acknowledge(EventId, Comment)} and of {@code TimedShelve2(ShelvingTime, Comment)}, but argument
     * 0 of {@code Suppress2(Comment)}. Each of {@code ByteString}, {@code LocalizedText} and {@code Double}
     * appears at most once in any of these signatures, so searching by type is unambiguous — and it does not
     * need revisiting when the next variant is added.
     */
    private static <T> T firstOfType(
            final @Nullable Variant[] arguments, final @NotNull Class<T> type, final T fallback) {
        if (arguments == null) {
            return fallback;
        }
        for (final Variant argument : arguments) {
            if (argument != null && type.isInstance(argument.getValue())) {
                return type.cast(argument.getValue());
            }
        }
        return fallback;
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
    /**
     * A distinct node standing in for the process variable the condition watches. Derived from the condition
     * so a test needs no extra setup, and deliberately never equal to it.
     */
    private @NotNull NodeId sourceNodeFor(final @NotNull NodeId conditionNodeId) {
        return newNodeId("source-of-" + conditionNodeId.toParseableString());
    }

    /**
     * The ConditionSource a condition's events name, as a parseable node id.
     * <p>
     * A test that filters on {@code SourceNode} needs the node id, not the {@code SourceName} string — the
     * two differ by the namespace prefix, and a filter given the bare name silently matches nothing.
     */
    public @NotNull String sourceNodeIdOf(final @NotNull String conditionNodeId) {
        return sourceNodeFor(NodeId.parse(conditionNodeId)).toParseableString();
    }

    /**
     * Fires a {@code RefreshRequiredEventType} — the server telling clients their alarm picture may be stale.
     * <p>
     * A real server sends this when it resynchronises with the system beneath it, or after an event queue
     * overflowed and drained. There is no state to model: the event's whole meaning is the request it makes
     * of the client, so the harness only has to deliver it and let the adapter's reaction be observed through
     * {@link #refreshBracketCount()}.
     * <p>
     * Fired from the area notifier rather than a condition, which is where it originates — it reports on the
     * server's own health, not on any one alarm.
     */
    public void fireRefreshRequired() {
        try {
            final BaseEventTypeNode event =
                    getServer().getEventFactory().createEvent(areaNotifier(), NodeIds.RefreshRequiredEventType);
            event.setEventId(new ByteString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
            event.setEventType(NodeIds.RefreshRequiredEventType);
            event.setSourceNode(areaNotifier());
            event.setSourceName("AreaNotifier");
            event.setTime(DateTime.now());
            event.setReceiveTime(DateTime.now());
            event.setMessage(LocalizedText.english("A condition refresh is required"));
            event.setSeverity(ushort(500));

            getServer().getEventNotifier().fire(event);
        } catch (final UaException e) {
            throw new IllegalStateException("Could not create the RefreshRequired event", e);
        }
    }

    public @NotNull ByteString fireAlarm(
            final @NotNull NodeId conditionNodeId,
            final @NotNull String message,
            final int severity,
            final boolean active) {
        try {
            // The event node IS the condition instance: its NodeId is the ConditionId a client filters on.
            // Creating it with a fresh random id — as an earlier version did — leaves the event with no field
            // that identifies the condition, and makes a correct ConditionId filter match nothing.
            final AlarmConditionTypeNode event = (AlarmConditionTypeNode)
                    getServer().getEventFactory().createEvent(conditionNodeId, NodeIds.AlarmConditionType);

            final ByteString eventId =
                    new ByteString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            event.setEventId(eventId);
            event.setEventType(NodeIds.AlarmConditionType);
            // SourceNode is the ConditionSource — what the alarm is ABOUT — not the condition itself. Real
            // devices set a sensor or machine here, so the harness must not conflate the two.
            event.setSourceNode(sourceNodeFor(conditionNodeId));
            event.setSourceName("source-of-" + conditionNodeId.toParseableString());
            event.setTime(DateTime.now());
            event.setReceiveTime(DateTime.now());
            event.setMessage(LocalizedText.english(message));
            event.setSeverity(ushort(severity));
            event.setRetain(active);
            event.setActiveState(LocalizedText.english(active ? "Active" : "Inactive"));
            event.setAckedState(LocalizedText.english("Unacknowledged"));

            // The Boolean half of each two-state field. Mandatory on TwoStateVariableType (OPC 10000-9
            // Table 1) and reachable only by the two-element browse path ['ActiveState','Id'], so a harness
            // that set only the display text could not tell a working Id selection from a broken one.
            final var activeStateNode = event.getActiveStateNode();
            if (activeStateNode != null) {
                activeStateNode.setId(active);
            }
            final var ackedStateNode = event.getAckedStateNode();
            if (ackedStateNode != null) {
                ackedStateNode.setId(false);
            }

            getServer().getEventNotifier().fire(event);
            return eventId;
        } catch (final UaException e) {
            throw new IllegalStateException("Could not create the alarm event", e);
        }
    }
}
