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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Resolves the `methodName` of a request to one of the methods a browse found on the tag's object.
///
/// Two spellings, told apart by whether the value contains a `;`:
///
///   - **an unadorned name** -- `Start`. Resolves only when exactly one method carries that browse name. A
///     browse name is not unique on an object: a `QualifiedName` is a namespace index plus a string, and OPC
///     10000-3 requires unique browse names only for TypeDefinitionNodes and InstanceDeclarations, "not for
///     other instances". Two Interfaces (§4.9), each published by a different organisation in its own
///     namespace, may both contribute a `Start` to one object. Keying a lookup on the string alone let the
///     later browse result replace the earlier, so a request invoked whichever the server happened to report
///     second -- and if both take the same arguments, nothing revealed the substitution.
///   - **a node id** -- by namespace URI, `nsu=http://vendor.example/Boiler;s=Start`, or by namespace index,
///     `ns=2;s=Start`; either with `;i=42`, `;g=<uuid>` or `;b=<base64>` as the identifier. Names the method
///     node itself, so it is unambiguous whatever the object exposes.
///
/// **Both forms are accepted, and the URI form is what a refusal prints.** An index is a position in the
/// server's namespace table, which is a per-session handle rather than an identity: OPC 10000-5 §5.4.2 says
/// servers should not change indexes but "it might not always be possible" to avoid it, and tells clients to
/// re-verify cached indexes when starting a session. So an index in a request that is stored, retained or
/// replayed can come to mean a different method.
///
/// That hazard is real and is **not** grounds for refusing the form here, because a tag's own node id is
/// parsed with `NodeId.parse` and is therefore written `ns=2;s=Boiler` in every configuration that exists. An
/// operator who writes a tag that way and then has `methodName` refuse the same spelling meets an
/// inconsistency Edge invented, while the exposure stays exactly where it was. Refusing it bought nothing and
/// cost that. The URI form is offered, preferred in every message Edge prints, and documented as the stable
/// one -- the choice is the caller's.
///
/// The `nsu=...;...` spelling is not an invention: it is the string form OPC 10000-6 defines for an
/// ExpandedNodeId, which is also why a `;` is the separator and why a URI containing a raw `;` is not
/// expressible. Parsing is Milo's, so the spelling Edge accepts is the spelling the specification defines.
public final class MethodReference {

    private MethodReference() {}

    /// What a `methodName` resolved to.
    public sealed interface Resolution {

        /// Exactly one method matched.
        record Matched(@NotNull MethodSignature method) implements Resolution {}

        /// No method matched. `reason` names what the object does offer.
        record NoSuchMethod(@NotNull String reason) implements Resolution {}

        /// The name is an unadorned one that several methods carry. `reason` lists their node ids, so an
        /// operator can put one of them in `methodName` and get the method they meant.
        record Ambiguous(@NotNull String reason) implements Resolution {}

        /// The value contains a `;` but is not a node id Edge accepts. `reason` says what was wrong with it.
        record Malformed(@NotNull String reason) implements Resolution {}
    }

    /// Resolves `methodName` against the methods a browse found.
    ///
    /// @param methods       every method the object exposes, in browse order.
    /// @param methodName    the request's `methodName`, unadorned or a node id.
    /// @param namespaceTable the live table, for turning a URI in a node id into this session's index.
    public static @NotNull Resolution resolve(
            final @NotNull List<MethodSignature> methods,
            final @NotNull String methodName,
            final @NotNull NamespaceTable namespaceTable) {

        if (methodName.indexOf(';') < 0) {
            return byName(methods, methodName, namespaceTable);
        }
        return byNodeId(methods, methodName, namespaceTable);
    }

    /// An unadorned name: resolves when it is unique, or when the only collision is a declaration of itself.
    ///
    /// **The exception, and why it is not a special case so much as the ordinary shape.** A type declares a
    /// method Mandatory, every instance is required to have its own node for it (OPC 10000-3 section
    /// 6.4.4.4.1), and both get browsed -- so the same name appears twice for an entirely conventional device.
    /// Those two are not rivals: the declaration on the type is the template and the object's node is the real
    /// method, with the device's own arguments. Resolving to the object's node is therefore not a guess.
    ///
    /// Refused, by contrast, when the pair is anything else: two methods on the object (two Interfaces each
    /// contributing one), or an object method sharing a name with a **static** type method. The latter are
    /// genuinely different things -- one acts on the device, the other on the class -- so a bare name cannot say
    /// which, and the caller is asked for a node id.
    private static @NotNull Resolution byName(
            final @NotNull List<MethodSignature> methods,
            final @NotNull String methodName,
            final @NotNull NamespaceTable namespaceTable) {

        final List<MethodSignature> matches = new ArrayList<>(2);
        for (final MethodSignature method : methods) {
            if (method.name().equals(methodName)) {
                matches.add(method);
            }
        }
        if (matches.size() == 1) {
            return new Resolution.Matched(matches.get(0));
        }
        final MethodSignature itsOwn = theObjectsOwnAmongADeclarationOfIt(matches);
        if (itsOwn != null) {
            return new Resolution.Matched(itsOwn);
        }
        if (matches.isEmpty()) {
            return new Resolution.NoSuchMethod(
                    "has no method '" + methodName + "'. It offers: " + offered(methods, namespaceTable));
        }
        // Refused rather than resolved by browse order. A request has no way to say which of the two it means,
        // so naming them is the only answer that does not guess -- and each node id printed here is a value
        // that can be pasted straight into `methodName`.
        final StringBuilder nodeIds = new StringBuilder();
        for (final MethodSignature method : matches) {
            if (!nodeIds.isEmpty()) {
                nodeIds.append(", ");
            }
            nodeIds.append(render(method.nodeId(), namespaceTable));
        }
        return new Resolution.Ambiguous("has "
                + matches.size()
                + " methods named '"
                + methodName
                + "', so the name does not say which to call. Put one of these node ids in 'methodName' "
                + "instead: "
                + nodeIds);
    }

    /// A node id: names the method node itself.
    private static @NotNull Resolution byNodeId(
            final @NotNull List<MethodSignature> methods,
            final @NotNull String methodName,
            final @NotNull NamespaceTable namespaceTable) {

        final ExpandedNodeId parsed;
        try {
            parsed = ExpandedNodeId.parse(methodName);
        } catch (final RuntimeException e) {
            return new Resolution.Malformed("'"
                    + methodName
                    + "' is neither a plain method name nor a node id: "
                    + e.getMessage()
                    + ". A node id is written nsu=<namespace uri>;<identifier> or ns=<index>;<identifier>, for "
                    + "example nsu=http://example.org/Boiler;s=Start");
        }
        // An index is accepted as well as a URI. It is the weaker spelling -- see the class note -- but it is
        // the spelling a tag's own node id uses, so refusing it here would be an inconsistency of Edge's own
        // making rather than a protection. `toNodeId` resolves either: a URI through the live table, an index
        // as itself.
        final NodeId wanted = parsed.toNodeId(namespaceTable).orElse(null);
        if (wanted == null) {
            // Only reachable for the URI form; an index needs no lookup, so it cannot fail this way.
            return new Resolution.NoSuchMethod("is on a server that does not publish the namespace '"
                    + parsed.getNamespaceUri()
                    + "', so '"
                    + methodName
                    + "' names no node on it. It offers: "
                    + offered(methods, namespaceTable));
        }
        for (final MethodSignature method : methods) {
            if (method.nodeId().equals(wanted)) {
                return new Resolution.Matched(method);
            }
        }
        return new Resolution.NoSuchMethod("exposes no method with node id "
                + render(wanted, namespaceTable)
                + ". It offers: "
                + offered(methods, namespaceTable));
    }

    /// A node id as a caller should write it back: the namespace by URI when the server publishes one.
    ///
    /// **Why not `toParseableString`.** That renders the namespace as an **index**, and every message Edge
    /// prints is a value an operator is being invited to paste into `methodName`. An index is a per-session
    /// handle (see the class note), so handing one back as the canonical spelling teaches the weaker form --
    /// and the three places that printed one had each reached for the same convenient method, which is how
    /// they drifted from what the parser documents. One helper now, so they cannot drift again.
    ///
    /// Falls back to the index form when the table has no URI for the namespace, which is the honest answer:
    /// it is what the node id is, and it still parses.
    ///
    /// **The table may not know the namespace at all**, and then `expanded` throws rather than answering --
    /// `IllegalStateException: namespace index=7 not found`. That reaches here from the refusal message for a
    /// request naming an index the server does not publish: the resolution had already decided correctly that
    /// no method matches, and only the wording of the refusal failed. The throw escaped into the write path,
    /// which answered nothing and held the request's writer until it timed out 30 seconds later, so further
    /// requests for that tag waited behind a message that was already known to be unanswerable. Caught here, at
    /// the one place that turns a node id into text for a caller.
    private static @NotNull String render(final @NotNull NodeId nodeId, final @NotNull NamespaceTable namespaceTable) {

        try {
            final ExpandedNodeId expanded = nodeId.expanded(namespaceTable);
            return expanded.getNamespaceUri() != null ? expanded.toParseableString() : nodeId.toParseableString();
        } catch (final RuntimeException e) {
            return nodeId.toParseableString();
        }
    }

    /// The object's own method when the only other match is a non-static declaration of it, else null.
    ///
    /// Exactly two matches, one from the object and one from the type carrying a modelling rule. Any other
    /// shape -- a different count, two object methods, or a static type method among them -- is a collision a
    /// bare name cannot settle, and is left to the caller's node id.
    private static @Nullable MethodSignature theObjectsOwnAmongADeclarationOfIt(
            final @NotNull List<MethodSignature> matches) {

        if (matches.size() != 2) {
            return null;
        }
        final MethodSignature first = matches.get(0);
        final MethodSignature second = matches.get(1);
        if (first.origin() == MethodSignature.Origin.OBJECT
                && second.origin() == MethodSignature.Origin.TYPE_DECLARATION) {
            return first;
        }
        if (second.origin() == MethodSignature.Origin.OBJECT
                && first.origin() == MethodSignature.Origin.TYPE_DECLARATION) {
            return second;
        }
        return null;
    }

    /// The object's methods, for a refusal message: the browse name of each, and its node id where the name
    /// alone would not get the caller there.
    private static @NotNull String offered(
            final @NotNull List<MethodSignature> methods, final @NotNull NamespaceTable namespaceTable) {

        if (methods.isEmpty()) {
            return "none";
        }
        final StringBuilder offered = new StringBuilder();
        for (final MethodSignature method : methods) {
            if (!offered.isEmpty()) {
                offered.append(", ");
            }
            offered.append(method.name());
            if (isAmbiguous(methods, method.name())) {
                offered.append(" (")
                        .append(render(method.nodeId(), namespaceTable))
                        .append(')');
            }
        }
        return offered.toString();
    }

    /// Whether a bare name would be refused, which is the only reason to print a node id beside it.
    ///
    /// Not simply "appears more than once": a name carried by the object's own method and by its type's
    /// declaration of that method resolves to the object's, so annotating it would offer a node id for a name
    /// that works. {@link #byName} and this must agree, or a refusal lists node ids the caller does not need
    /// while a genuine collision goes unannotated.
    private static boolean isAmbiguous(final @NotNull List<MethodSignature> methods, final @Nullable String name) {
        final List<MethodSignature> matches = new ArrayList<>(2);
        for (final MethodSignature method : methods) {
            if (method.name().equals(name)) {
                matches.add(method);
            }
        }
        return matches.size() > 1 && theObjectsOwnAmongADeclarationOfIt(matches) == null;
    }
}
