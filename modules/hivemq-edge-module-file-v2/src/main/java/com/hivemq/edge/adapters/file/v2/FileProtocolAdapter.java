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
package com.hivemq.edge.adapters.file.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.template.AbstractProtocolAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The v2 File adapter runtime — an actor built on {@link AbstractProtocolAdapter}. It reproduces the v1 File
 * adapter's behavior: a stateless, northbound poll-only reader that reads a file's content on each scheduled poll
 * and reports it as a reused v1 value. It owns no connection (the File adapter is stateless, so {@code doConnect}
 * acknowledges immediately) and no client library, and it never writes, browses, or subscribes — its type
 * advertises an empty capability set, so the framework never issues those commands.
 * <p>
 * The 64 KB file-size cap and the five content-type decode rules are carried over verbatim from the v1 adapter. A
 * poll failure — a missing or unreadable file, an oversized file, or content that cannot be decoded — is reported
 * as a per-node error; the framework returns the tag to its poll interval, counts the failure, and the next
 * scheduled poll is the retry (there is no auto-removal after repeated errors, unlike v1).
 */
public final class FileProtocolAdapter extends AbstractProtocolAdapter {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(FileProtocolAdapter.class);

    /**
     * The maximum size, in bytes, of a file this adapter will read — carried over verbatim from the v1 adapter.
     */
    private static final int MAX_FILE_SIZE_BYTES = 64_000;

    /**
     * @param input  everything this adapter instance is constructed from.
     * @param output the framework's state-and-event reporter.
     */
    public FileProtocolAdapter(final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
        super(input, output);
        // The File adapter has no adapter-level settings; parse tolerantly so an absent or empty section is accepted.
        FileAdapterConfiguration.parse(input.adapterConfig());
    }

    @Override
    protected void doStart() {
        // No resources to allocate.
        output.started();
    }

    @Override
    protected void doStop() {
        // No resources to release.
        output.stopped();
    }

    @Override
    protected void doConnect() {
        // The File adapter is stateless — there is no connection to open; acknowledge immediately.
        output.connected();
    }

    @Override
    protected void doDisconnect() {
        output.disconnected();
    }

    @Override
    protected void doPoll(final @NotNull Node node) {
        if (!(node instanceof final FileNode fileNode)) {
            output.nodeError(node, "the file adapter received a node of an unexpected type", false);
            return;
        }
        final Path path;
        try {
            path = Path.of(fileNode.filePath());
        } catch (final InvalidPathException e) {
            output.nodeError(node, "The configured file path is invalid: " + e.getMessage(), false);
            return;
        }
        try {
            final byte[] fileContent = readAtMostMaxFileSize(path);
            if (fileContent.length > MAX_FILE_SIZE_BYTES) {
                output.nodeError(
                        node,
                        "File '" + path.toAbsolutePath() + "' exceeds the limit '" + MAX_FILE_SIZE_BYTES + "'.",
                        false);
                return;
            }
            final Object value = fileNode.contentType().map(fileContent);
            if (value == null) {
                output.nodeError(
                        node, "Failed to map the content of file '" + fileNode.filePath() + "' to a value.", false);
                return;
            }
            output.dataPoint(node, toDataPoint(fileNode, value));
        } catch (final IOException e) {
            LOG.debug("An exception occurred while reading the file '{}'.", fileNode.filePath(), e);
            output.nodeError(
                    node,
                    "An exception occurred while reading the file '" + fileNode.filePath() + "': " + e.getMessage(),
                    false);
        } catch (final MappingException e) {
            LOG.debug(
                    "An exception occurred while converting the content of file '{}' to a value.",
                    fileNode.filePath(),
                    e);
            output.nodeError(
                    node,
                    "An exception occurred while converting the content of file '"
                            + fileNode.filePath()
                            + "': "
                            + e.getMessage(),
                    false);
        }
    }

    private static byte @NotNull [] readAtMostMaxFileSize(final @NotNull Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            // Reading one byte beyond the cap makes the size check authoritative even when the file grows mid-poll.
            return inputStream.readNBytes(MAX_FILE_SIZE_BYTES + 1);
        }
    }

    private @NotNull DataPoint toDataPoint(final @NotNull FileNode fileNode, final @NotNull Object value) {
        // The framework stamps the owning tag's name onto the value, so the node id is a stable placeholder here.
        final String tagName = fileNode.nodeId();
        return switch (value) {
            case final JsonNode jsonNode -> dataPointFactory.create(tagName, jsonNode);
            case final String text -> dataPointFactory.create(tagName, text);
            case final byte[] bytes -> dataPointFactory.create(tagName, bytes);
            default -> dataPointFactory.create(tagName, value.toString());
        };
    }

    @Override
    protected void doAddSubscription(final @NotNull Node node) {
        // The File adapter does not advertise the SUBSCRIPTIONS capability, so the framework never calls this; report
        // a per-node error defensively should it ever be invoked.
        output.nodeError(node, "the file adapter does not support subscriptions", false);
    }

    @Override
    protected void doWrite(final @NotNull Node node, final @NotNull DataPoint value) {
        // The File adapter does not advertise the WRITE capability, so the framework never calls this; report a failed
        // write defensively should it ever be invoked.
        output.writeResult(node, false, "the file adapter does not support writing");
    }
}
