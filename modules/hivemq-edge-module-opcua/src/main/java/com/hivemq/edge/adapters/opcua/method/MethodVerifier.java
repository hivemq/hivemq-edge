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
package com.hivemq.edge.adapters.opcua.method;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import com.hivemq.edge.adapters.opcua.browse.Browsing;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;

/// Reads the methods a `METHOD` tag's object exposes, so they can be called and described.
///
/// A method tag names an object, and every request against it names one of that object's methods. What a call
/// needs is each method's NodeId and the order of its arguments, and what schema generation needs is each
/// argument's name and type -- both come from the same browse, so both are read once here.
///
/// **Why this cannot be skipped.** A condition's methods are standard: the specification fixes their NodeIds,
/// so {@code ConditionUpdateWriter} can call one even against a server that keeps the condition itself out of
/// its address space. A user-defined method has no such table, so its NodeId exists only on the server and a
/// browse is the only way to learn it. An unverified method tag is therefore **unusable** rather than merely
/// undescribed, which is the opposite of a value tag -- there, a failed browse costs metadata and the tag still
/// works.
public final class MethodVerifier {

    private MethodVerifier() {}

    /// What a verification attempt concluded, in the same three-way shape the condition verifiers use: the
    /// difference between "this tag is wrong" and "the device did not answer" is the difference between an
    /// operator checking their configuration and checking their server.
    public sealed interface Result {

        /// The object was browsed and its methods read. Empty when the object genuinely exposes none, which is
        /// a usable tag that can never serve a request -- recorded rather than rejected, because the methods an
        /// object exposes may be added later without the tag changing.
        ///
        /// A list rather than a map keyed on the browse name, because that name is not unique on an object: OPC
        /// 10000-3 requires unique browse names only for TypeDefinitionNodes and InstanceDeclarations, "not for
        /// other instances", so two Interfaces may each contribute a `Start`. A map silently kept whichever the
        /// browse reported last. {@link MethodReference} resolves a request's `methodName` against this list
        // and
        /// refuses an unadorned name that several methods carry.
        record Verified(@NotNull List<MethodSignature> methods) implements Result {}

        /// The tag is wrong about the device: the node is absent, or is not an object. No retry will help.
        record Rejected(@NotNull String reason) implements Result {}

        /// The device could not be asked -- a transport failure, a refusal, a timeout. The tag may be correct;
        /// the answer is unknown, which is a different instruction for the operator than a rejection.
        record Unverifiable(@NotNull String reason) implements Result {}
    }

    /// Browses the object for its methods and reads each one's argument lists.
    ///
    /// @param client     the connected client.
    /// @param objectNode the node the tag points at, already parsed.
    /// @param tagName    used only to phrase the outcome.
    public static @NotNull CompletableFuture<Result> verify(
            final @NotNull OpcUaClient client, final @NotNull NodeId objectNode, final @NotNull String tagName) {

        // Forward HasComponent references with the Method node class: that is how a method hangs off its
        // object (OPC 10000-3 §4.5.3). Subtypes of HasComponent are included -- a server may use a more
        // specific reference type, and excluding them would hide methods that are genuinely there.
        final BrowseDescription browse = new BrowseDescription(
                objectNode,
                BrowseDirection.Forward,
                NodeIds.HasComponent,
                true,
                uint(NodeClass.Method.getValue()),
                uint(BrowseResultMask.All.getValue()));

        // The node class is checked before the browse, not inferred from it. A variable or a method named by
        // mistake has no HasComponent methods either, so browsing alone would report it as an object with none
        // -- a usable tag that can never serve a request, and a rejection the operator never sees. Naming the
        // mistake costs one read.
        return confirmObject(client, objectNode, tagName)
                .thenCompose(rejection -> {
                    if (rejection != null) {
                        return CompletableFuture.completedFuture(rejection);
                    }
                    return Browsing.browseAll(client, browse)
                            .thenCompose(references -> readSignatures(client, references, tagName));
                })
                .exceptionally(throwable -> {
                    // A node the server does not expose is a rejection here, not the "unverifiable" it is for
                    // a condition. OPC 10000-9 §4.3 lets a server keep condition *instances* hidden and
                    // deliver them through events instead, which is why a condition tag survives it. An
                    // object whose methods are to be called has no such dispensation: if it is not in the
                    // address space, there is nothing to browse and no NodeId to call.
                    if (Browsing.nodeNotInAddressSpace(throwable)) {
                        return new Result.Rejected("the server does not expose node "
                                + objectNode
                                + " in its address space, so the methods of tag '"
                                + tagName
                                + "' cannot be read -- and without them there is nothing to call");
                    }
                    return new Result.Unverifiable("could not read the methods of '"
                            + tagName
                            + "' from the device: "
                            + Browsing.describeException(throwable));
                });
    }

    /// Confirms the tag's node is an object, yielding a rejection when it is not and null when it is.
    ///
    /// A failure to read the node at all is left to propagate: the caller's handler already distinguishes an
    /// absent node from an unanswered server, and duplicating that judgement here would let the two drift.
    private static @NotNull CompletableFuture<Result> confirmObject(
            final @NotNull OpcUaClient client, final @NotNull NodeId objectNode, final @NotNull String tagName) {

        return client.getAddressSpace().getNodeAsync(objectNode).thenApply(node -> {
            if (node instanceof UaObjectNode) {
                return null;
            }
            return new Result.Rejected("node "
                    + objectNode
                    + " of tag '"
                    + tagName
                    + "' is a "
                    + node.getNodeClass()
                    + ", not an Object. A method tag names the object whose methods are called, and only an "
                    + "object has methods");
        });
    }

    /// Reads the input and output argument lists of every method the browse found.
    private static @NotNull CompletableFuture<Result> readSignatures(
            final @NotNull OpcUaClient client,
            final @NotNull List<ReferenceDescription> references,
            final @NotNull String tagName) {

        final List<CompletableFuture<MethodSignature>> pending = new ArrayList<>(references.size());
        for (final ReferenceDescription reference : references) {
            pending.add(readSignature(client, reference));
        }

        return CompletableFuture.allOf(pending.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> {
                    // Browse order is kept so that a generated schema lists arguments in the order the server
                    // reported its methods, which is stable for a given server and makes a schema diff
                    // readable when the device changes.
                    //
                    // Every method is kept, including two that share a browse name. Keying on the name dropped
                    // one of them, and which one survived depended on browse order.
                    final List<MethodSignature> methods = new ArrayList<>(pending.size());
                    for (final CompletableFuture<MethodSignature> future : pending) {
                        final MethodSignature signature = future.join();
                        if (signature != null) {
                            methods.add(signature);
                        }
                    }
                    return (Result) new Result.Verified(List.copyOf(methods));
                })
                .exceptionally(throwable -> new Result.Unverifiable("could not read the arguments of a method of '"
                        + tagName
                        + "': "
                        + Browsing.describeException(throwable)));
    }

    /// Reads one method's NodeId and argument lists.
    ///
    /// Yields null for a reference the address space cannot resolve to a method node, rather than failing the
    /// whole tag: one unreadable method among several is a gap in what the tag can do, and rejecting the tag
    /// would take the other methods down with it.
    private static @NotNull CompletableFuture<MethodSignature> readSignature(
            final @NotNull OpcUaClient client, final @NotNull ReferenceDescription reference) {

        final QualifiedName browseName = reference.getBrowseName();
        if (browseName == null || browseName.getName() == null) {
            return CompletableFuture.completedFuture(null);
        }
        final String name = browseName.getName();

        return client.getAddressSpace()
                .toNodeIdAsync(reference.getNodeId())
                .thenCompose(
                        nodeId -> client.getAddressSpace().getNodeAsync(nodeId).thenCompose(node -> {
                            if (!(node instanceof final UaMethodNode methodNode)) {
                                return CompletableFuture.completedFuture((MethodSignature) null);
                            }
                            // An absent InputArguments property reads as an empty array rather than an error, which
                            // is Milo's own behaviour and the right one: a method taking no arguments and one whose
                            // property the server omits are indistinguishable over the wire, and both are callable.
                            // The difference shows only in how much the generated schema can say.
                            final CompletableFuture<Argument[]> inputs =
                                    methodNode.readInputArgumentsAsync().exceptionally(t -> new Argument[0]);
                            final CompletableFuture<Argument[]> outputs =
                                    methodNode.readOutputArgumentsAsync().exceptionally(t -> new Argument[0]);
                            return inputs.thenCombine(
                                    outputs, (in, out) -> new MethodSignature(name, nodeId, List.of(in), List.of(out)));
                        }))
                .exceptionally(throwable -> null);
    }
}
