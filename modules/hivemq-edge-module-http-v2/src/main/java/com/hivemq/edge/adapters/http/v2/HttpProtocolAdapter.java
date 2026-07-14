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
package com.hivemq.edge.adapters.http.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.template.AbstractProtocolAdapter;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The v2 HTTP(s) adapter runtime — an actor built on {@link AbstractProtocolAdapter}. It reproduces the v1 HTTP
 * adapter's behavior: a northbound poll-only reader that issues per-tag HTTP(s) requests (GET/POST/PUT with custom
 * headers, per-tag body and timeout) and reports each response as a reused v1 value, applying the same success-status
 * filtering and JSON-or-Base64 response decoding. It owns the JDK {@link HttpClient} across
 * {@link #doConnect()}/{@link #doDisconnect()} (optionally trusting any TLS certificate), and it never writes,
 * browses, or subscribes — its type advertises an empty capability set.
 * <p>
 * Per-cycle parallelism is preserved: {@link #doPoll(Node)} fires {@link HttpClient#sendAsync} and returns
 * immediately; the completion handler reports the result through the thread-safe {@code output} façade from the
 * client's completion thread, so a hung endpoint (bounded by its per-request timeout) never wedges the dispatch
 * thread. A poll failure is reported as a per-node error; the framework returns the tag to its poll interval, counts
 * the failure, and the next scheduled poll is the retry (there is no auto-removal after repeated errors, unlike v1).
 */
public final class HttpProtocolAdapter extends AbstractProtocolAdapter {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(HttpProtocolAdapter.class);

    private static final @NotNull String CONTENT_TYPE_HEADER = "Content-Type";
    private static final @NotNull String USER_AGENT_HEADER = "User-Agent";
    private static final @NotNull String JSON_MIME_TYPE = "application/json";
    private static final @NotNull String PLAIN_MIME_TYPE = "text/plain";

    private final @NotNull HttpAdapterConfiguration configuration;
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull String userAgent = "HiveMQ-Edge; " + HttpProtocolAdapterInformation.INSTANCE.version();

    private volatile @Nullable HttpClient httpClient;

    /**
     * @param input  everything this adapter instance is constructed from.
     * @param output the framework's state-and-event reporter.
     */
    public HttpProtocolAdapter(final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
        super(input, output);
        this.configuration = HttpAdapterConfiguration.parse(input.adapterConfig(), objectMapper);
    }

    @Override
    protected void doStart() {
        // No resources to allocate before connecting.
        output.started();
    }

    @Override
    protected void doStop() {
        shutdownHttpClient();
        output.stopped();
    }

    @Override
    protected void doConnect() {
        shutdownHttpClient();
        try {
            final HttpClient.Builder builder = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(configuration.httpConnectTimeoutSeconds()));
            if (configuration.allowUntrustedCertificates()) {
                builder.sslContext(createTrustAllContext());
            }
            httpClient = builder.build();
            output.connected();
        } catch (final Exception e) {
            output.error(ErrorScope.CONNECTION, "Unable to build the HTTP client: " + e.getMessage());
        }
    }

    @Override
    protected void doDisconnect() {
        shutdownHttpClient();
        output.disconnected();
    }

    @Override
    protected void doPoll(final @NotNull Node node) {
        if (!(node instanceof final HttpNode httpNode)) {
            output.nodeError(node, "the http adapter received a node of an unexpected type", false);
            return;
        }
        final HttpClient client = this.httpClient;
        if (client == null) {
            output.nodeError(node, "the http client is not initialized", false);
            return;
        }
        final HttpRequest request;
        try {
            request = buildRequest(httpNode);
        } catch (final RuntimeException e) {
            output.nodeError(
                    node, "could not build the HTTP request for '" + httpNode.url() + "': " + e.getMessage(), false);
            return;
        }
        // Fire the request and return immediately; the completion handler reports the result from the client's own
        // thread through the thread-safe output façade. The returned future is intentionally not awaited here — this is
        // what preserves v1's per-cycle parallelism, and the dispatch thread does not block on it.
        final var unused = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, throwable) -> {
                    if (httpClient != client) {
                        return;
                    }
                    try {
                        if (throwable != null) {
                            output.nodeError(
                                    node,
                                    "the HTTP request to '" + httpNode.url() + "' failed: " + describe(throwable),
                                    false);
                        } else {
                            handleResponse(httpNode, node, response);
                        }
                    } catch (final RuntimeException e) {
                        LOG.debug("Could not process the HTTP response from '{}'.", httpNode.url(), e);
                        output.nodeError(
                                node,
                                "the HTTP response from '" + httpNode.url() + "' could not be processed: "
                                        + e.getMessage(),
                                false);
                    }
                });
    }

    private @NotNull HttpRequest buildRequest(final @NotNull HttpNode node) {
        final HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(node.url()))
                .timeout(Duration.ofSeconds(node.httpRequestTimeoutSeconds()))
                .setHeader(USER_AGENT_HEADER, userAgent);
        node.httpHeaders().forEach(header -> builder.setHeader(header.name(), header.value()));
        switch (node.httpRequestMethod()) {
            case GET -> builder.GET();
            case POST -> {
                builder.POST(HttpRequest.BodyPublishers.ofString(bodyOrEmpty(node)));
                builder.setHeader(
                        CONTENT_TYPE_HEADER, node.httpRequestBodyContentType().getMimeType());
            }
            case PUT -> {
                builder.PUT(HttpRequest.BodyPublishers.ofString(bodyOrEmpty(node)));
                builder.setHeader(
                        CONTENT_TYPE_HEADER, node.httpRequestBodyContentType().getMimeType());
            }
        }
        return builder.build();
    }

    private static @NotNull String bodyOrEmpty(final @NotNull HttpNode node) {
        final String body = node.httpRequestBody();
        return body != null ? body : "";
    }

    private void handleResponse(
            final @NotNull HttpNode node, final @NotNull Node key, final @NotNull HttpResponse<String> response) {
        final int status = response.statusCode();
        final boolean success = status >= 200 && status <= 299;
        if (!success && configuration.httpPublishSuccessStatusCodeOnly()) {
            output.nodeError(key, "the HTTP request to '" + node.url() + "' returned status " + status, false);
            return;
        }
        final Object payload;
        try {
            payload = decodePayload(response);
        } catch (final JsonProcessingException e) {
            LOG.debug("Invalid JSON data from '{}'.", node.url(), e);
            output.nodeError(key, "the HTTP response from '" + node.url() + "' could not be parsed as JSON", false);
            return;
        }
        if (payload == null) {
            // The response carried no body; nothing to publish and the next scheduled poll is the retry.
            return;
        }
        output.dataPoint(key, toDataPoint(node, payload));
    }

    private @Nullable Object decodePayload(final @NotNull HttpResponse<String> response)
            throws JsonProcessingException {
        final String body = response.body();
        if (body == null) {
            return null;
        }
        String contentType = response.headers().firstValue(CONTENT_TYPE_HEADER).orElse(null);
        if (configuration.assertResponseIsJson()) {
            contentType = JSON_MIME_TYPE;
        }
        if (JSON_MIME_TYPE.equals(contentType)) {
            return objectMapper.readTree(body);
        }
        final String effectiveContentType = contentType != null ? contentType : PLAIN_MIME_TYPE;
        final String base64 = Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));
        return "data:" + effectiveContentType + ";base64," + base64;
    }

    private @NotNull DataPoint toDataPoint(final @NotNull HttpNode node, final @NotNull Object payload) {
        // The framework stamps the owning tag's name onto the value, so the node id is a stable placeholder here.
        final String tagName = node.nodeId();
        if (payload instanceof final JsonNode jsonNode) {
            return dataPointFactory.create(tagName, jsonNode);
        }
        return dataPointFactory.create(tagName, payload.toString());
    }

    private void shutdownHttpClient() {
        final HttpClient client = httpClient;
        httpClient = null;
        if (client != null) {
            // Cancels in-flight polls without blocking the adapter dispatcher during disconnect or reconnect.
            client.shutdownNow();
        }
    }

    @Override
    protected void doAddSubscription(final @NotNull Node node) {
        // The HTTP adapter does not advertise the SUBSCRIPTIONS capability, so the framework never calls this; report a
        // per-node error defensively should it ever be invoked.
        output.nodeError(node, "the http adapter does not support subscriptions", false);
    }

    @Override
    protected void doWrite(final @NotNull Node node, final @NotNull DataPoint value) {
        // The HTTP adapter does not advertise the WRITE capability, so the framework never calls this; report a failed
        // write defensively should it ever be invoked.
        output.writeResult(node, false, "the http adapter does not support writing");
    }

    private static @NotNull String describe(final @NotNull Throwable throwable) {
        Throwable cause = throwable;
        if (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        final String message = cause.getMessage();
        return message != null
                ? cause.getClass().getSimpleName() + ": " + message
                : cause.getClass().getSimpleName();
    }

    /**
     * Build a {@link SSLContext} that trusts any certificate, for {@code allowUntrustedCertificates} — carried over
     * verbatim from the v1 adapter. Gated behind the configuration flag, which defaults to {@code false}.
     *
     * @return the trust-all TLS context.
     */
    private static @NotNull SSLContext createTrustAllContext() {
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            final X509ExtendedTrustManager trustManager = new X509ExtendedTrustManager() {
                @Override
                public void checkClientTrusted(
                        final X509Certificate @NotNull [] chain, final @NotNull String authType) {}

                @Override
                public void checkServerTrusted(
                        final X509Certificate @NotNull [] chain, final @NotNull String authType) {}

                @Override
                public X509Certificate @NotNull [] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(
                        final X509Certificate @NotNull [] chain,
                        final @NotNull String authType,
                        final @NotNull Socket socket) {}

                @Override
                public void checkServerTrusted(
                        final X509Certificate @NotNull [] chain,
                        final @NotNull String authType,
                        final @NotNull Socket socket) {}

                @Override
                public void checkClientTrusted(
                        final X509Certificate @NotNull [] chain,
                        final @NotNull String authType,
                        final @NotNull SSLEngine engine) {}

                @Override
                public void checkServerTrusted(
                        final X509Certificate @NotNull [] chain,
                        final @NotNull String authType,
                        final @NotNull SSLEngine engine) {}
            };
            sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());
            return sslContext;
        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException("Unable to build a trust-all TLS context: " + e.getMessage(), e);
        }
    }
}
