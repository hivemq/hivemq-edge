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
package com.hivemq.edge.adapters.opcua.condition;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks that a condition node really is the type its tag declares.
 * <p>
 * The declaration decides what the tag publishes, so a wrong one is not a cosmetic error: the select clause
 * would ask for fields the device does not have, and the schema would promise them. Checking at subscribe time
 * turns that into a clear failure for one tag instead of a subscription that quietly returns nulls.
 * <p>
 * Declaring a <em>supertype</em> passes. A tag declaring {@code AlarmConditionType} against a device offering
 * {@code ExclusiveLevelAlarmType} is projecting a narrower view of a richer condition, which is legitimate and
 * keeps configuration robust when a device is upgraded.
 */
public final class ConditionTypeVerifier {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ConditionTypeVerifier.class);

    /**
     * How far to follow {@code HasSubtype} before giving up. The standard tree is four deep at most, so ten
     * leaves ample room for a vendor hierarchy while still ending a cycle.
     */
    private static final int MAX_SUBTYPE_WALK_DEPTH = 10;

    private ConditionTypeVerifier() {}

    /** The outcome of checking one tag against its device. */
    public sealed interface Result {

        /** The device's type satisfies the declaration. */
        record Verified(@NotNull OpcuaConditionType deviceType) implements Result {}

        /**
         * The tag must not be subscribed. Carries a reason fit for a log line and an adapter event — the
         * operator needs to know which tag, what was declared and what was found.
         */
        record Rejected(@NotNull String reason) implements Result {}
    }

    /**
     * Reads the node's type definition and compares it with the declaration.
     * <p>
     * Never completes exceptionally: a browse that fails, a node with no type definition, and a vendor type
     * outside the standard tree are all reported as {@link Result.Rejected} rather than thrown. A condition
     * tag that cannot be verified must fail alone, not take the adapter's start with it.
     *
     * @param client        the connected client.
     * @param conditionNode the node the tag points at.
     * @param declaredType  the type the tag claims it is.
     * @param tagName       used only to phrase the rejection.
     */
    public static @NotNull CompletableFuture<Result> verify(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId conditionNode,
            final @NotNull OpcuaConditionType declaredType,
            final @NotNull String tagName) {

        final BrowseDescription browse = new BrowseDescription(
                conditionNode,
                BrowseDirection.Forward,
                NodeIds.HasTypeDefinition,
                false,
                uint(NodeClass.ObjectType.getValue()),
                uint(BrowseResultMask.All.getValue()));

        return Browsing.browseAll(client, browse)
                .thenCompose(references -> compare(client, references, declaredType, tagName))
                .exceptionally(throwable -> new Result.Rejected(
                        "could not read the type of '" + tagName + "' from the device: " + throwable.getMessage()));
    }

    private static @NotNull CompletableFuture<Result> compare(
            final @NotNull OpcUaClient client,
            final @NotNull List<ReferenceDescription> references,
            final @NotNull OpcuaConditionType declaredType,
            final @NotNull String tagName) {

        final Optional<String> deviceTypeName = typeNameOf(references);
        if (deviceTypeName.isEmpty()) {
            // Rejected, not waved through. The specification does permit a server to keep condition
            // instances out of the AddressSpace (§4.3), so an empty answer is not proof the tag is wrong --
            // but it is not proof the tag is right either, and a verifier that treats "I could not check"
            // as "it is fine" has stopped verifying. Failing here is the safe direction: the operator is
            // told plainly, and a real device that provokes it is worth investigating rather than guessing
            // about. The escape hatch is to correct the declaration or name the type the device does expose.
            return CompletableFuture.completedFuture(new Result.Rejected("tag '" + tagName
                    + "' is declared as a condition of type '"
                    + declaredType.browseName()
                    + "', but the device returned no type definition for that node, so the declaration could "
                    + "not be verified. Either the node is not a condition, or this server does not expose "
                    + "its condition instances in the address space — which OPC 10000-9 §4.3 permits. If the "
                    + "node is right and the server is one of those, please report it: the case is known to "
                    + "be possible but has not been seen on a real device"));
        }

        final Optional<OpcuaConditionType> deviceType = OpcuaConditionType.fromBrowseName(deviceTypeName.get());
        if (deviceType.isPresent()) {
            log.debug(
                    "Tag '{}': the device reports type '{}', which is a standard condition type.",
                    tagName,
                    deviceTypeName.get());
            return CompletableFuture.completedFuture(
                    decide(declaredType, deviceType.get(), deviceTypeName.get(), tagName));
        }

        // Not a name this build knows, which OPC 10000-9 §5.5 says to expect: "It is expected that vendors or
        // other standardisation groups will define additional ConditionTypes deriving from the common base
        // types defined in this part." So ask the server what it derives from rather than refusing.
        final Optional<NodeId> typeNode = typeNodeOf(client, references, deviceTypeName.get());
        if (typeNode.isEmpty()) {
            return CompletableFuture.completedFuture(new Result.Rejected("tag '"
                    + tagName
                    + "' points at a node of type '"
                    + deviceTypeName.get()
                    + "', which is not a standard OPC UA condition type, and the server gave no node id for that "
                    + "type, so its ancestry could not be followed"));
        }

        log.debug(
                "Tag '{}': the device reports type '{}' ({}), which is not a standard condition type. Following "
                        + "HasSubtype upwards to find one it derives from.",
                tagName,
                deviceTypeName.get(),
                typeNode.get());

        return walkToStandardType(client, typeNode.get(), tagName, 0).thenApply(ancestor -> {
            if (ancestor == null) {
                return new Result.Rejected("tag '"
                        + tagName
                        + "' points at a node of type '"
                        + deviceTypeName.get()
                        + "', and none of the types it derives from is a standard OPC UA condition type either. "
                        + "Edge cannot tell what fields such a condition carries. If this server's type does "
                        + "derive from a standard one, please report it — the ancestry it reports is in the "
                        + "adapter's debug log");
            }
            log.info(
                    "Tag '{}': the device's type '{}' is vendor-specific; it derives from the standard type '{}', "
                            + "which is what the tag is verified against and what decides the fields published.",
                    tagName,
                    deviceTypeName.get(),
                    ancestor.browseName());
            return decide(declaredType, ancestor, deviceTypeName.get(), tagName);
        });
    }

    /** Whether the declaration is satisfied by the standard type the device turned out to be. */
    private static @NotNull Result decide(
            final @NotNull OpcuaConditionType declaredType,
            final @NotNull OpcuaConditionType deviceType,
            final @NotNull String reportedName,
            final @NotNull String tagName) {

        if (!declaredType.isSatisfiedBy(deviceType)) {
            return new Result.Rejected("tag '" + tagName
                    + "' is declared as '"
                    + declaredType.browseName()
                    + "' but the device offers '"
                    + reportedName
                    + "', which does not derive from it");
        }
        return new Result.Verified(deviceType);
    }

    /**
     * Follows {@code HasSubtype} upwards until a standard condition type is reached, or null if none is.
     * <p>
     * Inverse because the reference points downward — a supertype <em>has</em> subtypes — so getting from a
     * vendor type to what it derives from means following them backwards. This is the same walk
     * {@link OpcuaConditionType#isSatisfiedBy} performs, except in the server's address space rather than in
     * Edge's own table: once a standard ancestor is found, that table takes over.
     * <p>
     * Bounded like the notifier walk, and for the same reason: a server that reports a cycle, or a hierarchy
     * deeper than any real one, must fail cleanly rather than browse forever.
     */
    private static @NotNull CompletableFuture<OpcuaConditionType> walkToStandardType(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId from,
            final @NotNull String tagName,
            final int depth) {

        if (depth >= MAX_SUBTYPE_WALK_DEPTH) {
            log.debug(
                    "Tag '{}': gave up following HasSubtype after {} steps, last at {}. A hierarchy this deep is "
                            + "more likely a cycle than a real one.",
                    tagName,
                    depth,
                    from);
            return CompletableFuture.completedFuture(null);
        }

        final BrowseDescription browse = new BrowseDescription(
                from,
                BrowseDirection.Inverse,
                NodeIds.HasSubtype,
                false,
                uint(NodeClass.ObjectType.getValue()),
                uint(BrowseResultMask.All.getValue()));

        return Browsing.browseAll(client, browse).thenCompose(references -> {
            if (references.isEmpty()) {
                log.debug("Tag '{}': {} reports no supertype, so the walk ends here.", tagName, from);
                return CompletableFuture.completedFuture(null);
            }
            // A standard ancestor at this level wins outright; otherwise keep climbing the first vendor one.
            // Multiple inheritance is not a thing for ObjectTypes, so "first" is a formality in practice.
            NodeId next = null;
            for (final ReferenceDescription reference : references) {
                final QualifiedName browseName = reference.getBrowseName();
                if (browseName == null || browseName.getName() == null) {
                    continue;
                }
                final Optional<OpcuaConditionType> standard = OpcuaConditionType.fromBrowseName(browseName.getName());
                if (standard.isPresent()) {
                    log.debug(
                            "Tag '{}': step {} reached '{}', a standard condition type.",
                            tagName,
                            depth + 1,
                            browseName.getName());
                    return CompletableFuture.completedFuture(standard.get());
                }
                if (next == null) {
                    next = reference
                            .getNodeId()
                            .toNodeId(client.getNamespaceTable())
                            .orElse(null);
                    log.debug(
                            "Tag '{}': step {} reached '{}', also not standard; continuing upwards.",
                            tagName,
                            depth + 1,
                            browseName.getName());
                }
            }
            return next == null
                    ? CompletableFuture.completedFuture(null)
                    : walkToStandardType(client, next, tagName, depth + 1);
        });
    }

    /** The node id the server gave for the type it reported, needed to browse that type's own references. */
    private static @NotNull Optional<NodeId> typeNodeOf(
            final @NotNull OpcUaClient client,
            final @NotNull List<ReferenceDescription> references,
            final @NotNull String reportedName) {

        for (final ReferenceDescription reference : references) {
            final QualifiedName browseName = reference.getBrowseName();
            if (browseName != null && reportedName.equals(browseName.getName())) {
                return reference.getNodeId().toNodeId(client.getNamespaceTable());
            }
        }
        return Optional.empty();
    }

    /**
     * The device's type name, preferring the standard namespace.
     * <p>
     * Unlike the method lookup in {@code ConditionUpdateWriter}, a name outside namespace 0 is not a
     * collision here: a vendor subtype legitimately lives in the vendor's own namespace, and reporting it is
     * how {@link #compare} produces its "not a standard OPC UA condition type" message. But a node can carry
     * more than one {@code HasTypeDefinition} reference, and if one of them is a standard type that is the
     * one worth comparing against — so namespace 0 wins when both are present, rather than whichever the
     * server happened to list first.
     */
    private static @NotNull Optional<String> typeNameOf(final @NotNull List<ReferenceDescription> references) {
        Optional<String> vendorName = Optional.empty();
        for (final ReferenceDescription reference : references) {
            final QualifiedName browseName = reference.getBrowseName();
            if (browseName == null || browseName.getName() == null) {
                continue;
            }
            if (browseName.getNamespaceIndex() != null
                    && browseName.getNamespaceIndex().intValue() == 0) {
                return Optional.of(browseName.getName());
            }
            if (vendorName.isEmpty()) {
                vendorName = Optional.of(browseName.getName());
            }
        }
        return vendorName;
    }
}
