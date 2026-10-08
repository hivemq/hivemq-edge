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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final @NotNull Logger log = LoggerFactory.getLogger(MethodVerifier.class);

    /// How many levels of the ObjectType hierarchy to climb before giving up. The standard tree is four deep at
    /// most, so ten leaves room for a vendor hierarchy while still stopping a server that reports a cycle. The
    /// same bound {@code ConditionTypeVerifier} uses for the same walk.
    private static final int MAX_TYPE_WALK_DEPTH = 10;

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
                            .thenCompose(references -> readSignatures(client, references, objectNode, tagName))
                            .thenCompose(
                                    fromInstance -> addInheritedMethods(client, objectNode, fromInstance, tagName));
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
    ///
    /// @param objectId what a `Call` to one of these methods names as its object -- the tag's own node for the
    ///                 methods the object exposes, and the ObjectType for those found by climbing to it.
    private static @NotNull CompletableFuture<Result> readSignatures(
            final @NotNull OpcUaClient client,
            final @NotNull List<ReferenceDescription> references,
            final @NotNull NodeId objectId,
            final @NotNull String tagName) {

        final List<CompletableFuture<MethodSignature>> pending = new ArrayList<>(references.size());
        for (final ReferenceDescription reference : references) {
            pending.add(readSignature(client, reference, objectId));
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
                // Rejected rather than Unverifiable, which the cache drops so the next request asks again. In
                // Edge-1 a failure is final: a tag whose methods could not be described stays unusable for the
                // session rather than retrying per request. The message names the method and the cause, so an
                // operator can tell an access refusal from a server that went away.
                .exceptionally(throwable -> new Result.Rejected("the methods of tag '"
                        + tagName
                        + "' could not be described, so no request against it can be turned into a call: "
                        + Browsing.describeException(throwable)));
    }

    /// Reads one method's NodeId and argument lists.
    ///
    /// Yields null for a reference that is not a method node, which is a gap in what the tag can do rather than
    /// a fault -- the other methods stay callable.
    ///
    /// A **failure** is not tolerated and propagates, so {@link #readSignatures} rejects the tag. Everything
    /// here was previously swallowed: an unreadable argument list became "this method takes no arguments", and
    /// the method was then offered with a signature that was not its own.
    private static @NotNull CompletableFuture<MethodSignature> readSignature(
            final @NotNull OpcUaClient client,
            final @NotNull ReferenceDescription reference,
            final @NotNull NodeId objectId) {

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
                            // An absent arguments property means the method takes none: OPC 10000-3 section
                            // 5.7 makes the property optional and says "a Property that is not provided
                            // indicates that there are no input arguments or output arguments", so that is how
                            // an argument-less method is normally modelled. Milo reports it as Bad_NotFound.
                            //
                            // Every other failure propagates. It used to be caught here and recorded as an
                            // empty list, which claimed the method takes no arguments: the schema said so and
                            // admitted no properties, so every correct request was refused for the session with
                            // nothing to distinguish it from a method that genuinely takes none.
                            final CompletableFuture<Argument[]> inputs =
                                    emptyWhenAbsent(methodNode.readInputArgumentsAsync());
                            final CompletableFuture<Argument[]> outputs =
                                    emptyWhenAbsent(methodNode.readOutputArgumentsAsync());
                            return inputs.thenCombine(
                                    outputs,
                                    (in, out) ->
                                            new MethodSignature(name, nodeId, objectId, List.of(in), List.of(out)));
                        }));
    }

    /// Reads an arguments property, treating "the server does not expose it" as "the method has none".
    ///
    /// The one failure that is an answer about the method. Every other -- a timeout, an access refusal, a
    /// transport failure -- propagates, and `readSignatures` rejects the tag.
    static @NotNull CompletableFuture<Argument[]> emptyWhenAbsent(final @NotNull CompletableFuture<Argument[]> read) {

        return read.exceptionallyCompose(throwable -> Browsing.notFound(throwable)
                ? CompletableFuture.completedFuture(new Argument[0])
                : CompletableFuture.failedFuture(throwable));
    }

    /// Adds the methods the object's type exposes but the object itself does not.
    ///
    /// **Why this is needed.** A method need not be mirrored onto the instances of its type. OPC 10000-3
    /// section 4.5.3 describes one whose scope is the ObjectType, "similar to static methods of a class"; such a
    /// method carries no modelling rule, so section 6.2.2 keeps it off every instance. OPC 10000-4's Call
    /// service expects a client to find it anyway: the object id may name "the Object or ObjectType on which the
    /// Method is invoked", and for an Object the method may hang off that Object, "the ObjectType of the Object
    /// or a super type of that ObjectType". Browsing the instance alone therefore reported a method the server
    /// supports as absent.
    ///
    /// Mandatory and Optional declarations *are* mirrored -- section 6.4.4.4.1 requires a similar node on the
    /// instance -- so this adds nothing for them, which is why the common case worked without it.
    ///
    /// **Nearest wins.** A method found on the instance shadows one of the same name further up. That is sound
    /// rather than a guess: section 6.2.6 fixes an instance's browse name to the declaration's, and section
    /// 6.4.4.4.1's subtype rules forbid removing or retyping an inherited argument, so the nearer node is the
    /// same method specialised rather than a different one. Calling it needs the instance's own node id.
    ///
    /// **Tolerant.** A failed climb yields the methods already found rather than failing the tag. Browsing a
    /// server's type nodes is not guaranteed -- a server may restrict access to them -- and a tag that works
    /// today must not start failing because an addition could not complete. The cost is one browse per level,
    /// paid once per tag per session.
    private static @NotNull CompletableFuture<Result> addInheritedMethods(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId objectNode,
            final @NotNull Result fromInstance,
            final @NotNull String tagName) {

        // Only a verified result is extended. A rejection or an unverifiable outcome is about the tag's own
        // node, and climbing above it would answer a question nobody asked.
        if (!(fromInstance instanceof final Result.Verified verified)) {
            return CompletableFuture.completedFuture(fromInstance);
        }

        return typeOf(client, objectNode, tagName)
                .thenCompose(type -> type == null
                        ? CompletableFuture.completedFuture(verified.methods())
                        : collectUpwards(client, type, verified.methods(), tagName, 0))
                .thenApply(methods -> (Result) new Result.Verified(methods))
                .exceptionally(throwable -> {
                    log.debug(
                            "Tag '{}': could not read the methods of its type, so only the ones its object "
                                    + "exposes are known: {}",
                            tagName,
                            Browsing.describeException(throwable));
                    return fromInstance;
                });
    }

    /// The object's ObjectType, or null when the server reports none.
    private static @NotNull CompletableFuture<NodeId> typeOf(
            final @NotNull OpcUaClient client, final @NotNull NodeId objectNode, final @NotNull String tagName) {

        final BrowseDescription browse = new BrowseDescription(
                objectNode,
                BrowseDirection.Forward,
                NodeIds.HasTypeDefinition,
                false,
                uint(NodeClass.ObjectType.getValue()),
                uint(BrowseResultMask.All.getValue()));

        return Browsing.browseAll(client, browse).thenCompose(references -> {
            for (final ReferenceDescription reference : references) {
                final NodeId type = reference
                        .getNodeId()
                        .toNodeId(client.getNamespaceTable())
                        .orElse(null);
                if (type != null) {
                    return CompletableFuture.completedFuture(type);
                }
            }
            log.debug("Tag '{}': {} reports no ObjectType, so there is nothing to inherit.", tagName, objectNode);
            return CompletableFuture.completedFuture(null);
        });
    }

    /// Gathers each level's methods, keeping what is already known, then climbs to the supertype.
    ///
    /// `HasSubtype` points downward -- a supertype *has* subtypes -- so climbing means following it inversely,
    /// the same walk {@code ConditionTypeVerifier} performs for condition types.
    private static @NotNull CompletableFuture<List<MethodSignature>> collectUpwards(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId type,
            final @NotNull List<MethodSignature> known,
            final @NotNull String tagName,
            final int depth) {

        if (depth >= MAX_TYPE_WALK_DEPTH) {
            // Bounded for the reason the condition walk is: a server reporting a cycle, or a hierarchy deeper
            // than any real one, must stop rather than browse forever.
            log.debug(
                    "Tag '{}': gave up climbing the type hierarchy after {} levels, last at {}. A hierarchy this "
                            + "deep is more likely a cycle than a real one.",
                    tagName,
                    depth,
                    type);
            return CompletableFuture.completedFuture(known);
        }

        final BrowseDescription methods = new BrowseDescription(
                type,
                BrowseDirection.Forward,
                NodeIds.HasComponent,
                true,
                uint(NodeClass.Method.getValue()),
                uint(BrowseResultMask.All.getValue()));

        return Browsing.browseAll(client, methods)
                .thenCompose(references -> readSignatures(client, references, type, tagName))
                .thenCompose(result -> {
                    final List<MethodSignature> merged = result instanceof final Result.Verified verified
                            ? keepingNearest(known, verified.methods())
                            : known;
                    return supertypeOf(client, type)
                            .thenCompose(supertype -> supertype == null
                                    ? CompletableFuture.completedFuture(List.copyOf(merged))
                                    : collectUpwards(client, supertype, merged, tagName, depth + 1));
                });
    }

    /// Adds the further-away methods whose names nothing nearer already provides.
    ///
    /// **Why nearest wins is sound rather than a guess.** An instance keeps the browse name of the declaration
    /// it derives from (OPC 10000-3 section 6.2.6), and a subtype may not remove an inherited argument, retype
    /// one declared with a concrete DataType, or add a mandatory one (section 6.4.4.4.1). So a nearer node of
    /// the same name is the same method, specialised -- not a different method that happens to share a string.
    /// Calling it needs the nearer node's own id, which is what keeping it does.
    ///
    /// Package-private so the rule can be tested without a server: the climb around it needs a live address
    /// space, and this is the part that decides what a caller can reach.
    static @NotNull List<MethodSignature> keepingNearest(
            final @NotNull List<MethodSignature> nearer, final @NotNull List<MethodSignature> further) {

        final List<MethodSignature> merged = new ArrayList<>(nearer);
        for (final MethodSignature candidate : further) {
            if (nearer.stream().noneMatch(method -> method.name().equals(candidate.name()))) {
                merged.add(candidate);
            }
        }
        return List.copyOf(merged);
    }

    /// The type this type derives from, or null at the top of the hierarchy.
    private static @NotNull CompletableFuture<NodeId> supertypeOf(
            final @NotNull OpcUaClient client, final @NotNull NodeId type) {

        final BrowseDescription browse = new BrowseDescription(
                type,
                BrowseDirection.Inverse,
                NodeIds.HasSubtype,
                false,
                uint(NodeClass.ObjectType.getValue()),
                uint(BrowseResultMask.All.getValue()));

        return Browsing.browseAll(client, browse).thenApply(references -> {
            for (final ReferenceDescription reference : references) {
                final NodeId supertype = reference
                        .getNodeId()
                        .toNodeId(client.getNamespaceTable())
                        .orElse(null);
                if (supertype != null) {
                    return supertype;
                }
            }
            return null;
        });
    }
}
