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
package com.hivemq.api.auth.oidc;

import com.nimbusds.oauth2.sdk.http.HTTPRequestSender;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.http.ReadOnlyHTTPRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sends a Nimbus {@link ReadOnlyHTTPRequest} and reads the response body through a fixed byte cap.
 * <p>
 * Nimbus's default {@code HTTPRequest.send()} reads the whole response into an unbounded buffer, so a
 * hostile or broken Identity Provider could grow the heap without limit. This sender reads at most
 * {@code maxBodyBytes} and aborts the read once that is exceeded, so discovery and token-exchange calls
 * are bounded in memory the same way JWKS retrieval already is via {@code DefaultResourceRetriever}.
 * <p>
 * Connect and read timeouts are taken from the request Nimbus builds. Redirects are disabled: an
 * Identity Provider endpoint should answer directly, and following redirects would reopen an unbounded,
 * attacker-steerable fetch.
 */
final class BoundedHttpRequestSender implements HTTPRequestSender {

    private final int maxBodyBytes;

    BoundedHttpRequestSender(final int maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    @Override
    public @NotNull HTTPResponse send(final @NotNull ReadOnlyHTTPRequest request) throws IOException {
        final URL url = request.getURL();
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(request.getConnectTimeout());
            connection.setReadTimeout(request.getReadTimeout());
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(request.getMethod().name());
            for (final Map.Entry<String, List<String>> header :
                    request.getHeaderMap().entrySet()) {
                for (final String value : header.getValue()) {
                    connection.addRequestProperty(header.getKey(), value);
                }
            }
            writeBody(connection, request.getBody());

            final int statusCode = connection.getResponseCode();
            final byte[] body = readBounded(bodyStream(connection));

            final HTTPResponse response = new HTTPResponse(statusCode);
            response.setStatusMessage(connection.getResponseMessage());
            for (final Map.Entry<String, List<String>> header :
                    connection.getHeaderFields().entrySet()) {
                if (header.getKey() != null) {
                    response.setHeader(header.getKey(), header.getValue().toArray(new String[0]));
                }
            }
            if (body.length > 0) {
                response.setBody(new String(body, StandardCharsets.UTF_8));
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private static void writeBody(final @NotNull HttpURLConnection connection, final @Nullable String body)
            throws IOException {
        if (body == null || body.isEmpty()) {
            return;
        }
        connection.setDoOutput(true);
        connection.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

    /** The body stream on success, or the error stream on an error status (which may still carry JSON). */
    private static @Nullable InputStream bodyStream(final @NotNull HttpURLConnection connection) throws IOException {
        if (connection.getResponseCode() >= HttpsURLConnection.HTTP_BAD_REQUEST) {
            return connection.getErrorStream();
        }
        return connection.getInputStream();
    }

    /**
     * Reads the stream into a byte array, failing as soon as more than {@code maxBodyBytes} would be
     * read — the limit is enforced while reading, not after buffering the whole response.
     */
    private byte @NotNull [] readBounded(final @Nullable InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        try (final InputStream stream = in) {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            final byte[] chunk = new byte[8192];
            int total = 0;
            int read;
            while ((read = stream.read(chunk)) != -1) {
                total += read;
                if (total > maxBodyBytes) {
                    throw new IOException("Identity Provider response exceeds the " + maxBodyBytes
                            + " byte limit; aborting to bound memory.");
                }
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }
}
