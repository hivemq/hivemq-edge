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
package com.hivemq.edge.adapters.opcua.util;

import com.hivemq.edge.adapters.opcua.method.Guids;
import java.util.Objects;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;

/// The one place the OPC UA adapter turns node id text into a node id, and a node id back into text.
///
/// Every node id an operator or a caller writes goes through here: a tag's `node`, `notifierNode`,
/// `sourceNode` and `conditionNode`, a browse root, a `methodName` given as a node id, and a NodeId or
/// ExpandedNodeId value in a write or a method argument. So all of them accept the same spellings and refuse
/// the same mistakes, with the same message.
///
/// ## The spellings
///
///   - **namespace by URI** -- `nsu=http://vendor.example/Boiler;s=Start`, the string form OPC 10000-6 defines
///     for an ExpandedNodeId. Stable: the URI names the namespace whatever its position in the server's table.
///   - **namespace by index** -- `ns=2;s=Start`, or `i=85` for namespace 0. A position in the server's
///     namespace table, which OPC 10000-5 §5.4.2 allows to change between sessions.
///
/// Either with `i=`, `s=`, `g=` or `b=` as the identifier. Text is parsed by Milo's `ExpandedNodeId.parse`,
/// which reads both, so the spelling accepted is the spelling the specification defines; a URI containing a
/// raw `;` is therefore not expressible.
///
/// ## The one check Edge adds
///
/// Milo reads a `g=` identifier with `UUID.fromString`, which zero-fills a short group into a *different*,
/// valid Guid, so a caller who drops a digit reaches a node they did not name. A `g=` identifier is therefore
/// required to be 8-4-4-4-12 hexadecimal digits before Milo sees it. See {@link Guids}.
///
/// ## What a namespace means where
///
/// A node id that addresses a node -- a tag, a browse root, a method -- and a NodeId value are both resolved
/// against the session's namespace table with {@link #resolve}, so the server receives this session's index
/// whichever spelling was written. An ExpandedNodeId value carries its URI to the server unchanged, so
/// {@link #expanded} checks only the spelling.
public final class NodeIdText {

    /// How a node id is written, appended to every refusal of one.
    public static final @NotNull String HOW_TO_WRITE = "A node id is written nsu=<namespace uri>;<identifier> or "
            + "ns=<index>;<identifier>, for example nsu=http://example.org/Boiler;s=Temperature";

    private NodeIdText() {}

    /// A node id that names a namespace URI the connected server does not publish.
    public static final class UnpublishedNamespaceException extends IllegalArgumentException {

        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private final @NotNull String namespaceUri;

        private UnpublishedNamespaceException(final @NotNull String text, final @NotNull String namespaceUri) {
            super("'" + text + "' names the namespace '" + namespaceUri + "', which the server does not publish");
            this.namespaceUri = namespaceUri;
        }

        public @NotNull String namespaceUri() {
            return namespaceUri;
        }
    }

    /// Any node id, as written: the syntax checked, nothing resolved.
    ///
    /// @throws IllegalArgumentException when `text` is not a node id. The message is written for an operator.
    public static @NotNull ExpandedNodeId expanded(final @NotNull String text) {
        try {
            requireCanonicalGuid(text);
            return ExpandedNodeId.parse(text);
        } catch (final RuntimeException e) {
            throw new IllegalArgumentException(
                    "'" + text + "' is not a valid OPC UA node id: " + reason(e) + ". " + HOW_TO_WRITE, e);
        }
    }

    /// The node a node id names on this session, the namespace as this session's index.
    ///
    /// @throws UnpublishedNamespaceException when `text` names a namespace URI the server does not publish.
    /// @throws IllegalArgumentException      when `text` is not a node id, or names a node on another server.
    public static @NotNull NodeId resolve(final @NotNull String text, final @NotNull NamespaceTable namespaceTable) {
        final ExpandedNodeId parsed = expanded(text);
        requireThisServer(text, parsed);
        // Empty only for the URI form: an index resolves as itself, without consulting the table.
        return parsed.toNodeId(namespaceTable)
                .orElseThrow(() ->
                        new UnpublishedNamespaceException(text, Objects.requireNonNull(parsed.getNamespaceUri())));
    }

    /// A node id as a caller should write it back: the namespace by URI when the table has one.
    ///
    /// **Why not `toParseableString`.** That renders the namespace as an index, and a node id Edge prints is a
    /// value an operator is invited to paste back into a configuration or a request. Handing back the
    /// per-session index teaches the weaker form.
    ///
    /// Falls back to the index form when the table has no URI for the namespace, which is the honest answer:
    /// it is what the node id is, and it still parses.
    ///
    /// **The table may not know the namespace at all**, and then Milo's `expanded` throws rather than answering
    /// -- `IllegalStateException: namespace index=7 not found`. That once reached a refusal message for a method
    /// request naming an unpublished index: the throw escaped into the write path, which answered nothing and
    /// held the request's writer until it timed out 30 seconds later. Caught here, so printing a node id never
    /// fails.
    public static @NotNull String render(final @NotNull NodeId nodeId, final @NotNull NamespaceTable namespaceTable) {
        try {
            final ExpandedNodeId expanded = nodeId.expanded(namespaceTable);
            return expanded.getNamespaceUri() != null ? expanded.toParseableString() : nodeId.toParseableString();
        } catch (final RuntimeException e) {
            return nodeId.toParseableString();
        }
    }

    /// A key under which two spellings of one node compare equal on this session: the index form of the node
    /// when `text` resolves, else `text` itself, which then matches only itself. Never throws.
    public static @NotNull String matchKey(final @NotNull String text, final @NotNull NamespaceTable namespaceTable) {
        try {
            return resolve(text, namespaceTable).toParseableString();
        } catch (final RuntimeException e) {
            return text;
        }
    }

    /// Refuses a `g=` identifier that is not 8-4-4-4-12 hexadecimal digits. The identifier follows the
    /// `svr=`, `svu=`, `nsu=` and `ns=` prefixes, each ended by the first `;` after it.
    private static void requireCanonicalGuid(final @NotNull String text) {
        int start = 0;
        while (text.startsWith("svr=", start)
                || text.startsWith("svu=", start)
                || text.startsWith("nsu=", start)
                || text.startsWith("ns=", start)) {
            final int separator = text.indexOf(';', start);
            if (separator < 0) {
                return;
            }
            start = separator + 1;
        }
        if (text.startsWith("g=", start)) {
            Guids.requireCanonical(text.substring(start + 2));
        }
    }

    private static void requireThisServer(final @NotNull String text, final @NotNull ExpandedNodeId parsed) {
        if (!parsed.isLocal()) {
            throw new IllegalArgumentException(
                    "'" + text + "' names a node on another server (svr=), and Edge addresses only the server "
                            + "it is connected to");
        }
    }

    /// Milo wraps its complaint as `UaRuntimeException: java.lang.IllegalArgumentException: ...`; the innermost
    /// message is the one that says what was wrong.
    private static @NotNull String reason(final @NotNull Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        final String message = cause.getMessage();
        return message != null && !message.isBlank()
                ? message
                : cause.getClass().getSimpleName();
    }
}
