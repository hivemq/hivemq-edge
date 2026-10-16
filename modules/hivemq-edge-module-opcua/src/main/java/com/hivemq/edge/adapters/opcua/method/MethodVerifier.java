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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
        /// `complete` is false when the walk did not finish: the methods listed are real, but a level of the
        /// type hierarchy could not be read, so more may exist. {@link MethodCache} evicts such a result the
        /// way it evicts an {@link Unverifiable} one, so the next request climbs again.
        ///
        /// **Why a flag rather than a failure.** Returning `Unverifiable` for a failed climb would undo a
        /// deliberate fallback: a server that refuses to let a client browse types at all is served the
        /// instance-only list, and that is the correct answer for it, not an error. The flag keeps both --
        /// the partial list is used, and it is not mistaken for the whole truth for the rest of the session.
        ///
        /// Found by Miguel in round 2 of EDG-1087: `43923822f` fixed this one level down, on the object's own
        /// argument reads, and the type climb kept turning a timeout into "no such method" until the session
        /// ended. Build one with {@link #of} or {@link #partial}, not the canonical constructor.
        record Verified(@NotNull List<MethodSignature> methods, boolean complete) implements Result {

            /// The ordinary case: everything the device has to say about this object was read.
            static @NotNull Verified of(final @NotNull List<MethodSignature> methods) {
                return new Verified(methods, true);
            }

            /// What was read before a level of the type hierarchy could not be. Not cached.
            static @NotNull Verified partial(final @NotNull List<MethodSignature> methods) {
                return new Verified(methods, false);
            }
        }

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

        // Forward HasComponent references: that is how a method hangs off its object (OPC 10000-3 §4.5.3).
        // Subtypes of HasComponent are included -- a server may use a more specific reference type, and
        // excluding them would hide methods that are genuinely there.
        final BrowseDescription browse = methodsOf(objectNode);

        // The node class is checked before the browse, not inferred from it. A variable or a method named by
        // mistake has no HasComponent methods either, so browsing alone would report it as an object with none
        // -- a usable tag that can never serve a request, and a rejection the operator never sees. Naming the
        // mistake costs one read.
        // Traced end to end, deliberately and at every narrowing step. The symptom this answers is a device that
        // yields its method when this code is called directly and no methods when the same code is called from
        // inside the running adapter -- so the question is never "is the logic right" but "where between those
        // two contexts does the count become zero". Every count below is therefore a point where two runs of the
        // same build can be compared line by line, which is what makes one round of measurement enough.
        //
        // Deliberately verbose: an extra line costs a second of reading, a missing one costs a lab deploy and a
        // QA round. Kept in the product rather than removed afterwards, so the next device that behaves oddly
        // needs no new build. Reported by QA as P12 on EDG-1100 and again on EDG-1116.
        final long startedAt = System.nanoTime();
        log.debug("Tag '{}': reading the methods of {}. Session: {}", tagName, objectNode, describeSession(client));

        return confirmObject(client, objectNode, tagName)
                .thenCompose(rejection -> {
                    if (rejection != null) {
                        log.debug("Tag '{}': {} is not an object, so there is nothing to browse", tagName, objectNode);
                        return CompletableFuture.completedFuture(rejection);
                    }
                    return Browsing.browseAll(client, browse)
                            .thenCompose(references -> {
                                // The decisive count. An empty list here and a non-empty one in a direct run
                                // localises the fault to the browse rather than to anything downstream of it.
                                log.debug(
                                        "Tag '{}': browsing {} for HasComponent returned {} reference(s): {}",
                                        tagName,
                                        objectNode,
                                        references.size(),
                                        describeReferences(references));
                                return readSignatures(client, references, objectNode, false, objectNode, tagName);
                            })
                            .thenCompose(fromInstance -> {
                                log.debug(
                                        "Tag '{}': the object itself yielded {}",
                                        tagName,
                                        describeOutcome(fromInstance));
                                return addInheritedMethods(client, objectNode, fromInstance, tagName);
                            });
                })
                .whenComplete((result, throwable) -> log.debug(
                        "Tag '{}': reading the methods of {} ended after {} ms as {}",
                        tagName,
                        objectNode,
                        (System.nanoTime() - startedAt) / 1_000_000L,
                        throwable != null ? "a throw: " + throwable : describeOutcome(result)))
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

    // region Tracing helpers
    //
    // Formatting only, and each one answers a question a two-run comparison asks. They swallow their own
    // failures: a trace that throws while describing a state would replace the problem being investigated with
    // one of its own.

    /// The session this browse will use, or why it could not be named.
    ///
    /// Present because the fault being chased may be a browse issued against a session that is up but not yet
    /// usable, and that is invisible in a reference count alone. A failure to read it is itself the answer, so
    /// it is reported rather than thrown -- `getSession` throws when there is no session, which is the state
    /// most worth seeing in the log.
    private static @NotNull String describeSession(final @NotNull OpcUaClient client) {
        try {
            final var session = client.getSession();
            return session.getSessionName() + " / " + session.getSessionId();
        } catch (final Exception e) {
            return "none: " + e;
        }
    }

    /// Every browsed reference with the two fields that decide its fate: its node class and its name.
    private static @NotNull String describeReferences(final @NotNull List<ReferenceDescription> references) {
        if (references.isEmpty()) {
            return "none";
        }
        final StringBuilder described = new StringBuilder();
        for (final ReferenceDescription reference : references) {
            if (!described.isEmpty()) {
                described.append(", ");
            }
            described
                    .append(reference.getBrowseName())
                    .append(" [")
                    .append(reference.getNodeClass())
                    .append(" ")
                    .append(reference.getNodeId())
                    .append("]");
        }
        return described.toString();
    }

    /// An argument list by name, so a count that is right with names that are wrong is still visible.
    private static @NotNull String describeArguments(final Argument @NotNull [] arguments) {
        if (arguments.length == 0) {
            return "none";
        }
        final StringBuilder described = new StringBuilder();
        for (final Argument argument : arguments) {
            if (!described.isEmpty()) {
                described.append(", ");
            }
            described.append(argument.getName()).append(": ").append(argument.getDataType());
        }
        return described.toString();
    }

    /// A verification outcome in one line: which kind, how many methods, and their names.
    private static @NotNull String describeOutcome(final @Nullable Result result) {
        return switch (result) {
            case null -> "nothing";
            case final Result.Verified verified ->
                "verified, "
                        + verified.methods().size()
                        + " method(s) ["
                        + verified.methods().stream()
                                .map(method -> method.browseName() + " " + method.origin())
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("none")
                        + "], complete="
                        + verified.complete();
            case final Result.Rejected rejected -> "rejected: " + rejected.reason();
            case final Result.Unverifiable unverifiable -> "unverifiable: " + unverifiable.reason();
        };
    }

    // endregion

    /// A browse for the methods hanging off a node, asking the server to filter nothing.
    ///
    /// **Why the node-class filter is not used.** `BrowseDescription` can ask the server to return only Method
    /// nodes, and OPC 10000-4 §5.8.2 is unambiguous that it must: "Only TargetNodes with the selected
    /// NodeClasses are returned". A Siemens S7-1500 (firmware V02.08.03) does not honour it for a user method
    /// defined in an instance DB -- it reports that node's class as **Object**, so a mask of `Method` returns
    /// nothing while an unfiltered browse lists the method correctly, and below the method node it ignores the
    /// mask entirely. With the filter in place no user-written method on that PLC could be found: the object
    /// resolved to zero methods, the generated schema admitted no arguments, and every call was quarantined
    /// while the adapter stayed connected and said nothing above DEBUG.
    ///
    /// That is the device family this feature exists for, and the firmware is fielded, so Edge filters instead
    /// of the server. The cost is a few more references per object, discarded in {@link #isMethod}. Other
    /// clients -- UaExpert, asyncua -- never met this because they browse unfiltered too.
    ///
    /// Reported by QA as P12 on EDG-1100, with a 16-cell browse matrix and an 8-mask characterisation.
    ///
    /// Package-private so the mask can be asserted without a server: a device that misreports a node class
    /// cannot be imitated by the embedded one, since Milo derives the browse's node class from the real node.
    static @NotNull BrowseDescription methodsOf(final @NotNull NodeId node) {
        return new BrowseDescription(
                node,
                BrowseDirection.Forward,
                NodeIds.HasComponent,
                true,
                uint(0),
                uint(BrowseResultMask.All.getValue()));
    }

    /// Whether a browsed reference is a method, by what the server reported about it.
    ///
    /// The filter the server was asked not to apply. A reference whose node class is unknown is kept rather
    /// than dropped: {@link #readSignature} resolves the node and discards anything that is not a method node,
    /// so a server that under-reports here costs a round trip rather than a method. The S7-1500 reports
    /// `Object` for a method node, which is exactly the case this cannot be strict about -- so "unknown" and
    /// "says Object" are both resolved rather than refused, and only a node class that is positively something
    /// else is dropped here.
    ///
    /// Package-private for the same reason as {@link #methodsOf}: this is the half of the decision that an
    /// embedded server cannot exercise, because it reports node classes correctly.
    static boolean isMethod(final @NotNull ReferenceDescription reference) {
        final NodeClass nodeClass = reference.getNodeClass();
        return nodeClass == NodeClass.Method || nodeClass == NodeClass.Object || nodeClass == NodeClass.Unspecified;
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
    /// @param owner    the node that was browsed: the tag's own object, or a type reached by climbing.
    /// @param onAType  whether `owner` is a type. When it is, each method's modelling rule decides whether it
    ///                 is static (so a call names the type) or a declaration (so a call names the object).
    /// @param objectId what a `Call` names as its object for a method found on the **object**: the tag's node.
    ///
    /// Package-private so the outcome a failed read produces can be asserted without a server: whether it is
    /// durable or retried is the question this method answers, and reaching it through `verify` needs a device
    /// that fails in a particular way mid-read.
    static @NotNull CompletableFuture<Result> readSignatures(
            final @NotNull OpcUaClient client,
            final @NotNull List<ReferenceDescription> references,
            final @NotNull NodeId owner,
            final boolean onAType,
            final @NotNull NodeId objectId,
            final @NotNull String tagName) {

        final List<CompletableFuture<MethodSignature>> pending = new ArrayList<>(references.size());
        for (final ReferenceDescription reference : references) {
            // The node-class filter the server was asked not to apply -- see methodsOf. Applied before the
            // node is resolved, so a Variable sitting beside a method costs nothing rather than a round trip.
            //
            // Logged per reference, kept or dropped. This is the one place a method the server did report can
            // still be discarded, and the S7-1500 is exactly the device that makes the decision non-obvious:
            // it calls a method node an Object.
            final boolean kept = isMethod(reference);
            log.debug(
                    "Tag '{}': reference {} named '{}' of class {} on {} -- {}",
                    tagName,
                    reference.getNodeId(),
                    reference.getBrowseName(),
                    reference.getNodeClass(),
                    owner,
                    kept ? "resolving it" : "dropped, its class is positively not a method");
            if (kept) {
                pending.add(readSignature(client, reference, onAType, owner, objectId));
            }
        }
        log.debug(
                "Tag '{}': {} of {} reference(s) on {} will be resolved",
                tagName,
                pending.size(),
                references.size(),
                owner);

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
                    return (Result) Result.Verified.of(List.copyOf(methods));
                })
                // Unverifiable, not Rejected. A read that failed says nothing about the tag -- a timeout, an
                // access refusal, a Bad_TooManyOperations from a busy server -- so it is not a verdict to keep.
                // This used to be Rejected, which the cache keeps for the session, so one timed-out argument
                // read made a tag unusable until the session was replaced.
                //
                // The inconsistency is what settles it rather than any doctrine about retrying: the *object*
                // browse one level up already answers a transport failure with Unverifiable and is retried.
                // Same device, same class of fault, and the two differed only in which code path met it.
                //
                // No promise about *when* a retry happens is made or intended. The cache simply does not keep
                // this outcome, so the next request that finds no entry asks again; a reconnect clears
                // everything regardless. See MethodCache.
                .exceptionally(throwable -> new Result.Unverifiable("the methods of tag '"
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
            final boolean onAType,
            final @NotNull NodeId owner,
            final @NotNull NodeId objectId) {

        // The qualified name travels into the signature, namespace and all: two methods named `Start` in
        // different namespaces are two different methods, and a vendor's own namespace is exactly where
        // vendor-specific methods sit beside the standard ones. A BrowseName with no string half cannot
        // describe a callable method, so it is dropped here rather than carried as a nameless signature.
        final QualifiedName browseName = reference.getBrowseName();
        if (browseName == null || browseName.getName() == null) {
            log.debug("Reference {} on {} has no usable browse name, so it is dropped", reference.getNodeId(), owner);
            return CompletableFuture.completedFuture(null);
        }

        return client.getAddressSpace()
                .toNodeIdAsync(reference.getNodeId())
                .thenCompose(
                        nodeId -> client.getAddressSpace().getNodeAsync(nodeId).thenCompose(node -> {
                            if (!(node instanceof final UaMethodNode methodNode)) {
                                // The second of the two places a browsed reference is discarded. Together with
                                // the node-class filter these account for every method the server reported and
                                // Edge did not keep, which is the accounting a zero-method object needs.
                                log.debug(
                                        "Method '{}' on {} resolved to {}, which is not a method node, so it is "
                                                + "dropped",
                                        browseName,
                                        owner,
                                        node.getNodeClass());
                                return CompletableFuture.completedFuture((MethodSignature) null);
                            }
                            log.debug(
                                    "Method '{}' on {} resolved to node {}; reading its arguments",
                                    browseName,
                                    owner,
                                    nodeId);
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
                            if (!onAType) {
                                return inputs.thenCombine(outputs, (in, out) -> {
                                    log.debug(
                                            "Method '{}' on object {} declares {} input(s) {} and {} output(s) {}",
                                            browseName,
                                            owner,
                                            in.length,
                                            describeArguments(in),
                                            out.length,
                                            describeArguments(out));
                                    final ResolvedTypes types = builtinTypesOf(client, in, out);
                                    return new MethodSignature(
                                            browseName,
                                            nodeId,
                                            objectId,
                                            MethodSignature.Origin.OBJECT,
                                            List.of(in),
                                            List.of(out),
                                            types.builtins(),
                                            types.resolved());
                                });
                            }
                            return hasModellingRule(client, nodeId)
                                    .thenCompose(declared -> inputs.thenCombine(outputs, (in, out) -> {
                                        log.debug(
                                                "Method '{}' on type {} is {} and declares {} input(s) {} and {} "
                                                        + "output(s) {}",
                                                browseName,
                                                owner,
                                                declared ? "a declaration, called on the object" : "static",
                                                in.length,
                                                describeArguments(in),
                                                out.length,
                                                describeArguments(out));
                                        final ResolvedTypes types = builtinTypesOf(client, in, out);
                                        return new MethodSignature(
                                                browseName,
                                                nodeId,
                                                // A static method is invoked by naming the type; a declaration
                                                // describes a method of the instance, so a call names the object.
                                                declared ? objectId : owner,
                                                declared
                                                        ? MethodSignature.Origin.TYPE_DECLARATION
                                                        : MethodSignature.Origin.TYPE_STATIC,
                                                List.of(in),
                                                List.of(out),
                                                types.builtins(),
                                                types.resolved());
                                    }));
                        }));
    }

    /// Whether a node on a type carries a modelling rule, making it a declaration rather than a static method.
    ///
    /// A node with no modelling rule "is neither considered for instantiation of a type nor for subtyping" (OPC
    /// 10000-3 section 6.4.4.2), so it exists on the type alone -- the static method of section 4.8. One
    /// carrying Mandatory or Optional describes what instances have instead.
    ///
    /// **A failed read is treated as "declared", which is the safe direction.** Not knowing, and guessing
    /// static, would name the type as a call's object and run something against the class; guessing declared
    /// calls the object, where a device that does not have the method simply refuses. So the uncertain case
    /// costs a refusal rather than an action on the wrong node.
    private static @NotNull CompletableFuture<Boolean> hasModellingRule(
            final @NotNull OpcUaClient client, final @NotNull NodeId method) {

        final BrowseDescription browse = new BrowseDescription(
                method,
                BrowseDirection.Forward,
                NodeIds.HasModellingRule,
                false,
                uint(0),
                uint(BrowseResultMask.All.getValue()));

        return Browsing.browseAll(client, browse)
                .thenApply(references -> !references.isEmpty())
                .exceptionally(throwable -> {
                    log.debug(
                            "Could not read the modelling rule of method {}, so it is taken as a declaration "
                                    + "and called on the object: {}",
                            method,
                            Browsing.describeException(throwable));
                    return true;
                });
    }

    /// The builtin type each argument's DataType reduces to: from the local table where that suffices, and
    /// through the server's DataType hierarchy for the rest.
    ///
    /// Covers the outputs as well as the inputs: an output declared `Duration` rendered as `any` in the response
    /// schema for the same reason a `Duration` input could not be encoded at all.
    ///
    /// **Why the lookup table alone is not enough.** `OpcUaDataType.fromNodeId` knows the 25 builtin ids and
    /// nothing else, so it answers null for a **Simple DataType** -- a subtype of a builtin, which OPC 10000-3
    /// section 5.8.2 says is "handled on the wire like the Built-in DataType, i.e. they cannot be distinguished
    /// on the wire from their Built-in supertypes". `Duration` is the specification's own example, and the
    /// standard type for a timeout argument; `UtcTime`, `LocaleId`, `Counter`, `IntegerId`, `Index` and
    /// `NumericRange` are others. Every method taking one was refused with "a type this version cannot encode"
    /// -- for a type that encodes exactly like the Double or UInt32 it is.
    ///
    /// The client's DataType tree does that walk, which is what the ordinary value-write path has always used
    /// ({@code JsonToOpcUAConverter}, {@code JsonSchemaGenerator}). The method path reached for the table
    /// instead, which was the whole of that defect. Resolved here because this is where a client exists: the
    /// argument converter and the schema generator both work from a {@link MethodSignature} alone.
    ///
    /// **Why the table is nevertheless consulted first.** It needs no server, so what it answers cannot be lost
    /// to a server that does not. Reading the tree first and starting from nothing meant one unreadable
    /// hierarchy discarded the types of arguments that never needed it.
    ///
    /// A type reducing to no builtin is left out, and is then refused at conversion -- a custom structure,
    /// which stays deliberately unencodable in this release.
    ///
    /// Reported by Miguel as M6 on EDG-1087.
    private static @NotNull ResolvedTypes builtinTypesOf(
            final @NotNull OpcUaClient client, final Argument @NotNull [] inputs, final Argument @NotNull [] outputs) {

        // Seeded from the local table first, and the order is what makes a failed read survivable. Every
        // argument whose DataType *is* one of the 25 builtins is answered here, from a lookup that needs no
        // server. Only a Simple DataType -- a subtype that encodes like its builtin supertype -- needs the
        // hierarchy below.
        //
        // Resolving in the other order is the defect this closes: the tree was read first and a failure
        // returned an empty map, so one unreadable hierarchy discarded the answers for arguments that never
        // needed it. An `Int32` input became unencodable, the signature was nevertheless reported fully
        // verified, and the cache kept it -- leaving a method taking a plain integer uncallable until the
        // adapter restarted.
        final Map<NodeId, OpcUaDataType> builtins =
                new LinkedHashMap<>(MethodSignature.builtinsOf(List.of(inputs), List.of(outputs)));

        // Every argument already answered by the table needs no server at all, so a hierarchy that cannot be
        // read costs only the Simple DataTypes -- and `resolved=false` says so, which is what stops that
        // moment's failure becoming a fact about the method. See ResolvedTypes.
        if (builtins.size() == countDistinctDataTypes(inputs, outputs)) {
            // Every DataType was a builtin. The hierarchy can say nothing further, so it is not read: on a
            // device that serves metadata slowly that is a round trip saved per method, for an answer already
            // in hand.
            return new ResolvedTypes(builtins, true);
        }

        final DataTypeTree tree;
        try {
            tree = client.getDataTypeTree();
        } catch (final UaException | RuntimeException e) {
            log.warn(
                    "Could not read the DataType hierarchy; arguments of a Simple DataType stay unresolved, "
                            + "the builtin ones are unaffected: {}",
                    e.getMessage());
            return new ResolvedTypes(builtins, false);
        }
        boolean resolved = true;
        for (final Argument argument : concat(inputs, outputs)) {
            final NodeId dataType = argument.getDataType();
            if (dataType == null || builtins.containsKey(dataType)) {
                continue;
            }
            try {
                final OpcUaDataType builtin = tree.getBuiltinType(dataType);
                if (builtin != null) {
                    builtins.put(dataType, builtin);
                }
                // A null answer is the tree saying this type reduces to no builtin -- a genuine structure,
                // which this release does not encode. That is durable, so `resolved` stays true and the
                // signature may be cached: asking again would learn the same thing.
            } catch (final RuntimeException e) {
                // The tree refusing to answer about one type, as distinct from answering "none". Same state as
                // a tree that could not be built at all: asked, not answered.
                log.debug("Could not reduce DataType {} to a builtin type: {}", dataType, e.getMessage());
                resolved = false;
            }
        }
        return new ResolvedTypes(builtins, resolved);
    }

    /// How many distinct DataTypes the arguments name, so a signature needing no hierarchy can skip reading it.
    private static int countDistinctDataTypes(final Argument @NotNull [] inputs, final Argument @NotNull [] outputs) {
        final Set<NodeId> dataTypes = new LinkedHashSet<>();
        for (final Argument argument : concat(inputs, outputs)) {
            final NodeId dataType = argument.getDataType();
            if (dataType != null) {
                dataTypes.add(dataType);
            }
        }
        return dataTypes.size();
    }

    /// The builtin type of each argument's DataType, and whether every lookup that was needed actually answered.
    ///
    /// **The flag is the whole point of this record.** Without it a type missing from the map has two causes a
    /// caller cannot tell apart: the hierarchy answered "this reduces to no builtin", which is a genuine
    /// structure and durable for the session; or the hierarchy could not be read, which is a statement about
    /// the moment and nothing about the method. Both left the type absent, both produced the same refusal --
    /// "has a type this version cannot encode" -- and the signature was reported fully verified either way, so
    /// the cache kept it and one failed read made a perfectly callable method uncallable until the adapter
    /// restarted. A `Duration` argument is the standard case: an alias for a Double, and until the hierarchy is
    /// read we do not even know it is an alias rather than a structure, so "cannot encode" is a conclusion we
    /// are not entitled to.
    ///
    /// `resolved=false` therefore makes the signature incomplete, and `MethodCache` declines to remember an
    /// incomplete one, so the next request reads the hierarchy again.
    record ResolvedTypes(@NotNull Map<NodeId, OpcUaDataType> builtins, boolean resolved) {}

    private static @NotNull List<Argument> concat(final Argument @NotNull [] first, final Argument @NotNull [] second) {

        final List<Argument> all = new ArrayList<>(first.length + second.length);
        all.addAll(List.of(first));
        all.addAll(List.of(second));
        return all;
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
                        ? CompletableFuture.completedFuture(new Climb(verified.methods(), true))
                        : collectUpwards(client, type, objectNode, verified.methods(), tagName, 0))
                .thenApply(climb -> {
                    warnWhenNoneWereFound(climb.methods(), objectNode, tagName);
                    // A climb that could not read a level is not cached, so the next request tries again --
                    // see Result.Verified. The methods it did find are still served.
                    //
                    // A method whose DataType lookups did not all answer counts the same way, and for the same
                    // reason: its arguments are refused at conversion, and remembering that would turn one
                    // unreadable hierarchy into a permanent property of a method that is perfectly callable.
                    final boolean typesResolved = climb.methods().stream().allMatch(MethodSignature::typesResolved);
                    if (!typesResolved) {
                        log.debug(
                                "Tag '{}': a DataType lookup did not answer, so this result is not remembered and "
                                        + "the next request reads the hierarchy again",
                                tagName);
                    }
                    return (Result)
                            (climb.complete() && typesResolved
                                    ? Result.Verified.of(climb.methods())
                                    : Result.Verified.partial(climb.methods()));
                })
                .exceptionally(throwable -> {
                    log.debug(
                            "Tag '{}': could not read the methods of its type, so only the ones its object "
                                    + "exposes are known: {}",
                            tagName,
                            Browsing.describeException(throwable));
                    // Partial for the same reason: the type lookup failing is a failure to reach the device,
                    // not the device saying this object's type has nothing to add.
                    return Result.Verified.partial(verified.methods());
                });
    }

    /// Warns when a method tag's object turned out to expose nothing callable.
    ///
    /// **Why this is a WARN rather than a DEBUG.** The outcome is a tag that looks healthy and can never serve
    /// a request: the adapter stays connected, the generated schema admits no arguments, and every call is
    /// quarantined by the validation gate. An operator sees a configured tag, a connected device, the method
    /// plainly visible in another OPC-UA client, and calls that vanish -- with nothing above DEBUG to explain
    /// it. This class's own comments already name "a usable tag that can never serve a request" as the outcome
    /// to avoid; said at DEBUG, that warning reached nobody.
    ///
    /// Not a rejection, deliberately. An object may legitimately expose no methods, and the methods an object
    /// exposes can be added later without the tag changing -- so this is reported rather than refused. QA
    /// raised it alongside the node-class filter (P12 on EDG-1100), and it is the half that survives whatever
    /// a given server does with that filter.
    private static void warnWhenNoneWereFound(
            final @NotNull List<MethodSignature> methods,
            final @NotNull NodeId objectNode,
            final @NotNull String tagName) {

        if (methods.isEmpty()) {
            log.warn(
                    "Method tag '{}' points at node {}, which exposes no callable methods -- neither on the "
                            + "object nor on its type. Every request against this tag will be refused. Check "
                            + "that the node is the object that owns the methods, and that the server lists "
                            + "them as components of it.",
                    tagName,
                    objectNode);
        }
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
    private static @NotNull CompletableFuture<Climb> collectUpwards(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId type,
            final @NotNull NodeId objectNode,
            final @NotNull List<MethodSignature> known,
            final @NotNull String tagName,
            final int depth) {

        if (depth >= MAX_TYPE_WALK_DEPTH) {
            // Bounded for the reason the condition walk is: a server reporting a cycle, or a hierarchy deeper
            // than any real one, must stop rather than browse forever.
            //
            // Complete, deliberately: the device answered every level we asked about. Stopping here is our
            // rule rather than its failure, so re-climbing on the next request would learn the same thing and
            // pay for it again.
            log.debug(
                    "Tag '{}': gave up climbing the type hierarchy after {} levels, last at {}. A hierarchy this "
                            + "deep is more likely a cycle than a real one.",
                    tagName,
                    depth,
                    type);
            return CompletableFuture.completedFuture(new Climb(known, true));
        }

        // Unfiltered for the same reason as the instance browse -- see methodsOf. A server whose node-class
        // filter hides a method on an object hides it on the type too, and a static method lives only there.
        final BrowseDescription methods = methodsOf(type);

        log.debug(
                "Tag '{}': climbing to type {} at level {}, {} method(s) known so far",
                tagName,
                type,
                depth,
                known.size());

        return Browsing.browseAll(client, methods)
                .thenCompose(references -> {
                    log.debug(
                            "Tag '{}': browsing type {} for HasComponent returned {} reference(s): {}",
                            tagName,
                            type,
                            references.size(),
                            describeReferences(references));
                    return readSignatures(client, references, type, true, objectNode, tagName);
                })
                .thenCompose(result -> {
                    final List<MethodSignature> merged = result instanceof final Result.Verified verified
                            ? keepingNearest(known, verified.methods())
                            : known;
                    // A level the device could not answer about makes the whole climb incomplete, and that
                    // travels up through every recursion rather than being decided at the top. Without it a
                    // single Bad_Timeout mid-climb produced a cacheable "these are all the methods", hiding
                    // every static method and inherited declaration until the session ended.
                    final boolean levelRead = !(result instanceof Result.Unverifiable);
                    log.debug(
                            "Tag '{}': type {} at level {} contributed {}; {} method(s) known after merging, level "
                                    + "{}",
                            tagName,
                            type,
                            depth,
                            describeOutcome(result),
                            merged.size(),
                            levelRead ? "read" : "NOT readable, so the climb is incomplete");
                    return supertypeOf(client, type)
                            .thenCompose(supertype -> supertype == null
                                    ? CompletableFuture.completedFuture(new Climb(List.copyOf(merged), levelRead))
                                    : collectUpwards(client, supertype, objectNode, merged, tagName, depth + 1)
                                            .thenApply(above -> above.andThisLevel(levelRead)));
                });
    }

    /// What a climb up the type hierarchy found, and whether it got to read every level it tried.
    ///
    /// Exists because the climb's answer is two things: the methods, and whether the list can be trusted as
    /// whole. Returning only the list made the second invisible, so the caller cached a partial answer as a
    /// complete one.
    /// Package-private rather than private so {@link #andThisLevel} can be tested directly. Combining a flag
    /// up a recursion is the kind of thing that looks obviously right and is wrong by one level, and the
    /// alternative test needs a server willing to fail exactly one level of a type hierarchy on demand.
    record Climb(@NotNull List<MethodSignature> methods, boolean complete) {

        /// The same methods, incomplete if this level could not be read or anything above it already was.
        @NotNull
        Climb andThisLevel(final boolean levelRead) {
            // `this` when the level was read, which keeps an earlier failure because `this` is then already
            // partial -- the combination is idempotent in that direction rather than needing to test `complete`.
            return levelRead ? this : new Climb(methods, false);
        }
    }

    /// Adds the further-away methods whose names no nearer **type level** already provides.
    ///
    /// Two levels of a type hierarchy may declare the same name; the nearer declaration is the one a subtype
    /// specialised, so it supersedes the one above it. An instance keeps the browse name of the declaration it
    /// derives from (OPC 10000-3 section 6.2.6), and a subtype may not remove an inherited argument, retype one
    /// declared with a concrete DataType, or add a mandatory one (section 6.4.4.4.1) -- so a nearer node of the
    /// same name is the same method specialised, not a different method sharing a string.
    ///
    /// **A method the object itself declares no longer suppresses the type's declaration of that name**, which
    /// it used to. Both are kept, and {@link MethodReference} decides between them: a bare `methodName` matching
    /// one object method and one non-static declaration resolves to the object's, because that is the node the
    /// device actually has. Dropping the declaration here instead made the preference a side effect of merge
    /// order rather than a stated rule, and left resolution unable to tell that pairing -- the ordinary shape of
    /// a Mandatory declaration and its instance -- from two genuinely rival methods.
    ///
    /// **Compared on the qualified name, namespace and all.** Specialisation keeps the browse name of the
    /// declaration it derives from, namespace included, so only an equal qualified name is the same method one
    /// level down. Comparing the string halves made two unrelated methods one: a subtype's `1:Start` discarded a
    /// supertype's `2:Start`, and because the node-id lookup searches this same list, the discarded one was
    /// then unreachable by node id as well -- a method the server exposes, reported as absent. A genuine
    /// cross-namespace clash now survives to resolution, which reports a bare name matching both as ambiguous
    /// and names the node ids.
    ///
    /// Package-private so the rule can be tested without a server: the climb around it needs a live address
    /// space, and this is the part that decides what a caller can reach.
    static @NotNull List<MethodSignature> keepingNearest(
            final @NotNull List<MethodSignature> nearer, final @NotNull List<MethodSignature> further) {

        final List<MethodSignature> merged = new ArrayList<>(nearer);
        for (final MethodSignature candidate : further) {
            final boolean shadowedByANearerTypeLevel = nearer.stream()
                    .anyMatch(method -> method.origin() != MethodSignature.Origin.OBJECT
                            && method.browseName().equals(candidate.browseName()));
            if (!shadowedByANearerTypeLevel) {
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
