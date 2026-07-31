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
import org.jetbrains.annotations.Nullable;

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

        return client.browseAsync(browse).handle((result, throwable) -> {
            if (throwable != null) {
                return new Result.Rejected(
                        "could not read the type of '" + tagName + "' from the device: " + throwable.getMessage());
            }
            return compare(result.getReferences(), declaredType, tagName);
        });
    }

    private static @NotNull Result compare(
            final @Nullable ReferenceDescription @Nullable [] references,
            final @NotNull OpcuaConditionType declaredType,
            final @NotNull String tagName) {

        final Optional<String> deviceTypeName = typeNameOf(references);
        if (deviceTypeName.isEmpty()) {
            return new Result.Rejected("tag '" + tagName
                    + "' is declared as a condition of type '"
                    + declaredType.browseName()
                    + "', but the node has no type definition — it is probably not a condition");
        }

        final Optional<OpcuaConditionType> deviceType = OpcuaConditionType.fromBrowseName(deviceTypeName.get());
        if (deviceType.isEmpty()) {
            // A vendor subtype is not in the standard tree, so it cannot be placed in the hierarchy from here.
            // Rejecting is the honest answer: the tag names a type this build cannot reason about.
            return new Result.Rejected(
                    "tag '" + tagName
                            + "' points at a node of type '"
                            + deviceTypeName.get()
                            + "', which is not a standard OPC UA condition type. Declare the nearest standard type it derives from");
        }

        if (!declaredType.isSatisfiedBy(deviceType.get())) {
            return new Result.Rejected("tag '" + tagName
                    + "' is declared as '"
                    + declaredType.browseName()
                    + "' but the device offers '"
                    + deviceType.get().browseName()
                    + "', which does not derive from it");
        }

        return new Result.Verified(deviceType.get());
    }

    private static @NotNull Optional<String> typeNameOf(final @Nullable ReferenceDescription @Nullable [] references) {
        if (references == null) {
            return Optional.empty();
        }
        for (final ReferenceDescription reference : references) {
            final QualifiedName browseName = reference.getBrowseName();
            if (browseName != null && browseName.getName() != null) {
                return Optional.of(browseName.getName());
            }
        }
        return Optional.empty();
    }
}
