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
///   - **a node id** -- `nsu=http://vendor.example/Boiler;s=Start`, or `;i=42`, `;g=<uuid>`, `;b=<base64>`.
///     Names the method node itself, so it is unambiguous whatever the object exposes.
///
/// **Why the qualified form carries a URI and not a namespace index.** An index is a position in the server's
/// namespace table, which is a per-session handle rather than an identity: OPC 10000-5 §5.4.2 says servers
/// should not change indexes but "it might not always be possible" to avoid it, and tells clients to re-verify
/// cached indexes when starting a session. A request is written once and may be stored, retained or replayed,
/// so an index in it would silently come to mean a different method -- the very fault this resolution exists to
/// prevent. A URI is stable, and the index is looked up from it against the live table on each call. This is
/// the same trade the adapter already makes for tag node ids, which store a URI and are re-indexed at connect.
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
            return byName(methods, methodName);
        }
        return byNodeId(methods, methodName, namespaceTable);
    }

    /// An unadorned name: resolves only when it is unique among the object's methods.
    private static @NotNull Resolution byName(
            final @NotNull List<MethodSignature> methods, final @NotNull String methodName) {

        final List<MethodSignature> matches = new ArrayList<>(2);
        for (final MethodSignature method : methods) {
            if (method.name().equals(methodName)) {
                matches.add(method);
            }
        }
        if (matches.size() == 1) {
            return new Resolution.Matched(matches.get(0));
        }
        if (matches.isEmpty()) {
            return new Resolution.NoSuchMethod("has no method '" + methodName + "'. It offers: " + offered(methods));
        }
        // Refused rather than resolved by browse order. A request has no way to say which of the two it means,
        // so naming them is the only answer that does not guess -- and each node id printed here is a value
        // that can be pasted straight into `methodName`.
        final StringBuilder nodeIds = new StringBuilder();
        for (final MethodSignature method : matches) {
            if (!nodeIds.isEmpty()) {
                nodeIds.append(", ");
            }
            nodeIds.append(method.nodeId().toParseableString());
        }
        return new Resolution.Ambiguous("has "
                + matches.size()
                + " methods named '"
                + methodName
                + "', so the name does not say which to call. Name one of these node ids in 'methodName' "
                + "instead, as nsu=<namespace uri>;<identifier>: "
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
                    + ". A node id is written nsu=<namespace uri>;<identifier>, for example "
                    + "nsu=http://example.org/Boiler;s=Start");
        }
        // An index is deliberately not accepted, even though Milo parses it. `ns=2;s=Start` means whatever
        // position 2 holds in this session's namespace table, and a server may renumber the table between
        // sessions -- so a stored or retained request carrying an index can come to name a different method.
        if (parsed.getNamespaceUri() == null) {
            return new Resolution.Malformed("'"
                    + methodName
                    + "' identifies a namespace by index. An index is only meaningful for one session, because "
                    + "a server may renumber its namespace table, so name the namespace by URI instead: "
                    + "nsu=<namespace uri>;<identifier>");
        }
        final NodeId wanted = parsed.toNodeId(namespaceTable).orElse(null);
        if (wanted == null) {
            return new Resolution.NoSuchMethod("is on a server that does not publish the namespace '"
                    + parsed.getNamespaceUri()
                    + "', so '"
                    + methodName
                    + "' names no node on it. It offers: "
                    + offered(methods));
        }
        for (final MethodSignature method : methods) {
            if (method.nodeId().equals(wanted)) {
                return new Resolution.Matched(method);
            }
        }
        return new Resolution.NoSuchMethod(
                "exposes no method with node id " + wanted.toParseableString() + ". It offers: " + offered(methods));
    }

    /// The object's methods, for a refusal message: the browse name of each, and its node id where the name
    /// alone would not get the caller there.
    private static @NotNull String offered(final @NotNull List<MethodSignature> methods) {
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
                offered.append(" (").append(method.nodeId().toParseableString()).append(')');
            }
        }
        return offered.toString();
    }

    private static boolean isAmbiguous(final @NotNull List<MethodSignature> methods, final @Nullable String name) {
        int seen = 0;
        for (final MethodSignature method : methods) {
            if (method.name().equals(name)) {
                seen++;
            }
        }
        return seen > 1;
    }
}
