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
package com.hivemq.edge.adapters.etherip;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assumptions;

/// A minimal EtherNet/IP device on the loopback address, port 44818, that answers the requests of the `etherip`
// library:
/// RegisterSession, and SendRRData carrying a CIP multi-request of Read Tag services, each answered with the same
/// INT value. `goOffline()` simulates an outage: it closes every connection and refuses new ones.
///
/// The port is fixed because `etherip` always connects to 44818 and has no way to change it.
final class FakeEtherNetIpDevice implements AutoCloseable {

    static final @NotNull String HOST = InetAddress.getLoopbackAddress().getHostAddress();
    static final int PORT = 44818;

    private static final int COMMAND_REGISTER_SESSION = 0x65;
    private static final int COMMAND_UNREGISTER_SESSION = 0x66;
    private static final int COMMAND_SEND_RR_DATA = 0x6F;
    private static final int HEADER_SIZE = 24;
    private static final int SESSION = 0x12345678;
    /// Offset of the multi-request service count in a SendRRData request body, see [#sendRRDataReply].
    private static final int MULTI_REQUEST_COUNT_OFFSET = 32;
    private static final int CIP_STATUS_PATH_DESTINATION_UNKNOWN = 0x05;

    private final short tagValue;
    private final @NotNull List<Socket> connections = new CopyOnWriteArrayList<>();
    private final @NotNull AtomicInteger sessionCount = new AtomicInteger();
    private final @NotNull AtomicBoolean answerWithCipError = new AtomicBoolean();
    private volatile @Nullable ServerSocket serverSocket;

    private FakeEtherNetIpDevice(final short tagValue) {
        this.tagValue = tagValue;
    }

    /// Creates the device offline; skips the calling test if port 44818 is taken on this machine.
    static @NotNull FakeEtherNetIpDevice createOrSkip(final short tagValue) {
        try (ServerSocket probe = bind()) {
            probe.getLocalPort();
        } catch (final IOException e) {
            Assumptions.abort("Port " + PORT + " is not available: " + e);
        }
        return new FakeEtherNetIpDevice(tagValue);
    }

    private static @NotNull ServerSocket bind() throws IOException {
        final ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), PORT));
        return socket;
    }

    synchronized void goOnline() {
        if (serverSocket != null) {
            return;
        }
        try {
            final ServerSocket listening = bind();
            serverSocket = listening;
            Thread.ofVirtual().name("fake-eip-accept").start(() -> acceptLoop(listening));
        } catch (final IOException e) {
            throw new IllegalStateException("Fake device could not listen on port " + PORT, e);
        }
    }

    synchronized void goOffline() {
        final ServerSocket listening = serverSocket;
        serverSocket = null;
        closeQuietly(listening);
        for (final Socket connection : connections) {
            closeQuietly(connection);
        }
        connections.clear();
    }

    /// Answers Read Tag requests with a CIP error status instead of the value, as a device does for an unknown tag.
    void answerWithCipError(final boolean enabled) {
        answerWithCipError.set(enabled);
    }

    /// Sessions registered since the device was created. TCP connections that never register a session, such as
    /// the adapter's connection check, do not count.
    int sessionCount() {
        return sessionCount.get();
    }

    /// Waits until the client has closed every connection, and returns whether it did within the timeout.
    boolean awaitNoOpenConnections(final @NotNull Duration timeout) throws InterruptedException {
        final long deadline = System.nanoTime() + timeout.toNanos();
        while (connections.stream().anyMatch(socket -> !socket.isClosed())) {
            if (System.nanoTime() > deadline) {
                return false;
            }
            Thread.sleep(10);
        }
        return true;
    }

    @Override
    public void close() {
        goOffline();
    }

    private void acceptLoop(final @NotNull ServerSocket listening) {
        while (!listening.isClosed()) {
            try {
                final Socket connection = listening.accept();
                connections.add(connection);
                Thread.ofVirtual().name("fake-eip-connection").start(() -> serve(connection));
            } catch (final IOException e) {
                return;
            }
        }
    }

    private void serve(final @NotNull Socket connection) {
        try (connection) {
            final DataInputStream in = new DataInputStream(connection.getInputStream());
            final OutputStream out = connection.getOutputStream();
            while (true) {
                final byte[] header = new byte[HEADER_SIZE];
                in.readFully(header);
                final ByteBuffer headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
                final int command = headerBuffer.getShort(0) & 0xFFFF;
                final byte[] body = new byte[headerBuffer.getShort(2) & 0xFFFF];
                in.readFully(body);
                final byte[] context = new byte[8];
                System.arraycopy(header, 12, context, 0, 8);
                final byte[] reply =
                        switch (command) {
                            case COMMAND_REGISTER_SESSION -> {
                                sessionCount.incrementAndGet();
                                yield encapsulate(command, context, new byte[] {1, 0, 0, 0});
                            }
                            case COMMAND_SEND_RR_DATA -> encapsulate(command, context, sendRRDataReply(body));
                            case COMMAND_UNREGISTER_SESSION -> null;
                            default -> throw new IOException("Unexpected command " + command);
                        };
                if (reply != null) {
                    // -- One write per reply: etherip mistakes a partial header for a complete reply.
                    out.write(reply);
                    out.flush();
                }
            }
        } catch (final EOFException e) {
            // -- The client closed the connection.
        } catch (final IOException e) {
            // -- goOffline() closed the connection.
        } finally {
            connections.remove(connection);
        }
    }

    private static byte @NotNull [] encapsulate(
            final int command, final byte @NotNull [] context, final byte @NotNull [] body) {
        final ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + body.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) command);
        buffer.putShort((short) body.length);
        buffer.putInt(SESSION);
        buffer.putInt(0); // -- status
        buffer.put(context);
        buffer.putInt(0); // -- options
        buffer.put(body);
        return buffer.array();
    }

    /// Answers an Unconnected Send of a CIP multi-request with one Read Tag reply per requested tag.
    /// The request body is: interface handle (4), timeout (2), item count (2), null address item (4),
    /// data item header (4), Unconnected Send service and path (6), tick time (2), message size (2),
    /// Multiple Service Packet service and path (6), then the service count at offset 32.
    private byte @NotNull [] sendRRDataReply(final byte @NotNull [] request) {
        final ByteBuffer requestBuffer = ByteBuffer.wrap(request).order(ByteOrder.LITTLE_ENDIAN);
        final int serviceCount = requestBuffer.getShort(MULTI_REQUEST_COUNT_OFFSET) & 0xFFFF;
        final byte[] readReply = readTagReply();

        // -- Multiple Service Packet reply: count, one offset per reply (relative to the count), the replies.
        final ByteBuffer multi = ByteBuffer.allocate(2 + 2 * serviceCount + serviceCount * readReply.length)
                .order(ByteOrder.LITTLE_ENDIAN);
        multi.putShort((short) serviceCount);
        for (int i = 0; i < serviceCount; i++) {
            multi.putShort((short) (2 + 2 * serviceCount + i * readReply.length));
        }
        for (int i = 0; i < serviceCount; i++) {
            multi.put(readReply);
        }

        final ByteArrayOutputStream messageRouterReply = new ByteArrayOutputStream();
        messageRouterReply.writeBytes(new byte[] {(byte) 0x8A, 0, 0, 0}); // -- reply to service 0x0A, status 0
        messageRouterReply.writeBytes(multi.array());
        final byte[] cip = messageRouterReply.toByteArray();

        final ByteBuffer body = ByteBuffer.allocate(16 + cip.length).order(ByteOrder.LITTLE_ENDIAN);
        body.putInt(0); // -- interface handle
        body.putShort((short) 0); // -- timeout
        body.putShort((short) 2); // -- item count
        body.putShort((short) 0); // -- null address item
        body.putShort((short) 0);
        body.putShort((short) 0x00B2); // -- unconnected data item
        body.putShort((short) cip.length);
        body.put(cip);
        return body.array();
    }

    private byte @NotNull [] readTagReply() {
        if (answerWithCipError.get()) {
            return new byte[] {(byte) 0xCC, 0, CIP_STATUS_PATH_DESTINATION_UNKNOWN, 0};
        }
        final ByteBuffer reply = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        reply.put((byte) 0xCC).put((byte) 0).put((byte) 0).put((byte) 0); // -- reply to Read Tag 0x4C, status 0
        reply.putShort((short) 0x00C3); // -- CIP type INT
        reply.putShort(tagValue);
        return reply.array();
    }

    private static void closeQuietly(final @Nullable AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (final Exception ignored) {
            // -- Closing is best effort.
        }
    }
}
