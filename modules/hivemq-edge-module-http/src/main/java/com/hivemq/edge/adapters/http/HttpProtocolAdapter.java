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
package com.hivemq.edge.adapters.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig;
import com.hivemq.edge.adapters.http.model.HttpData;
import com.hivemq.edge.adapters.http.mqtt2http.JsonSchema;
import com.hivemq.edge.adapters.http.tag.HttpTag;
import com.hivemq.edge.adapters.http.tag.HttpTagDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;
import static com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.JSON_MIME_TYPE;
import static com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.PLAIN_MIME_TYPE;

public class HttpProtocolAdapter implements BatchPollingProtocolAdapter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(HttpProtocolAdapter.class);

    private static final @NotNull String CONTENT_TYPE_HEADER = "Content-Type";
    private static final @NotNull String BASE64_ENCODED_VALUE = "data:%s;base64,%s";
    private static final @NotNull String USER_AGENT_HEADER = "User-Agent";
    private static final @NotNull String RESPONSE_DATA = "httpResponseData";

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull HttpSpecificAdapterConfig adapterConfig;
    private final @NotNull List<HttpTag> tags;
    private final @NotNull String version;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ModuleServices moduleServices;
    private final @NotNull AdapterFactories adapterFactories;
    private final @NotNull String adapterId;
    private final @Nullable ObjectMapper objectMapper;

    private volatile @Nullable HttpClient httpClient = null;

    public HttpProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<HttpSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.tags = input.getTags().stream().map(tag -> (HttpTag)tag).toList();
        this.version = input.getVersion();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.moduleServices = input.moduleServices();
        this.adapterFactories = input.adapterFactories();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input,
            final @NotNull ProtocolAdapterStartOutput output) {
        try {
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.STATELESS);
            if (httpClient == null) {
                final HttpClient.Builder builder = HttpClient.newBuilder();
                builder.version(HttpClient.Version.HTTP_1_1)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(adapterConfig.getHttpConnectTimeoutSeconds()));
                if (adapterConfig.isAllowUntrustedCertificates()) {
                    builder.sslContext(createTrustAllContext());
                }
                httpClient = builder.build();
            }
            output.startedSuccessfully();
        } catch (final Exception e) {
            output.failStart(e, "Unable to start http protocol adapter.");
        }
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        httpClient = null;
        output.stoppedSuccessfully();
    }

    @Override
    public void destroy() {
        log.debug("Destroying HTTP adapter with id '{}'", adapterId);
        // Clear the HTTP client reference to allow GC to clean up the internal executor
        httpClient = null;
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(
            final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {

        final HttpClient httpClient = this.httpClient;
        if (httpClient == null) {
            pollingOutput.fail(new ProtocolAdapterException(), "No response was created, because the client is null.");
            return;
        }

        final List<CompletableFuture<HttpData>> pollingFutures =
                tags.stream().map(tag -> pollHttp(httpClient, tag)).toList();

        CompletableFuture.allOf(pollingFutures.toArray(new CompletableFuture[]{}))
                .whenComplete((result, throwable) -> {
                    if(throwable != null) {
                        pollingOutput.fail(throwable, "Error while polling tags.");
                    } else {
                        try {
                            for (final CompletableFuture<HttpData> future : pollingFutures) {
                                    final var data = future.get();
                                    // Update connection status to ERROR if HTTP request failed
                                    if (!data.isSuccessStatusCode()) {
                                        protocolAdapterState.setConnectionStatus(ERROR);
                                    }
                                    // FSM manages STATELESS/CONNECTED status automatically
                                    if (data.isSuccessStatusCode() ||
                                            !adapterConfig.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()) {
                                        data.getDataPoints().forEach(pollingOutput::addDataPoint);
                                    }
                            }
                            pollingOutput.finish();
                        } catch (final InterruptedException | ExecutionException e) {
                            pollingOutput.fail(e, "Exception while accessing data of completed future.");
                        }
                    }
                });
    }

    private CompletableFuture<HttpData> pollHttp(
            final @NotNull HttpClient httpClient,
            final @NotNull HttpTag httpTag) {

        final HttpRequest.Builder builder = HttpRequest.newBuilder();
        final String url = httpTag.getDefinition().getUrl();
        final HttpTagDefinition tagDef = httpTag.getDefinition();
        builder.uri(URI.create(url));

        builder.timeout(Duration.ofSeconds(httpTag.getDefinition().getHttpRequestTimeoutSeconds()));
        builder.setHeader(USER_AGENT_HEADER, String.format("HiveMQ-Edge; %s", version));

        tagDef.getHttpHeaders().forEach(hv -> builder.setHeader(hv.getName(), hv.getValue()));

        switch (tagDef.getHttpRequestMethod()) {
            case GET:
                builder.GET();
                break;
            case POST:
                if (tagDef.getHttpRequestBody() != null) {
                    builder.POST(HttpRequest.BodyPublishers.ofString(tagDef.getHttpRequestBody()));
                } else {
                    builder.POST(HttpRequest.BodyPublishers.ofString(""));
                }
                builder.header(CONTENT_TYPE_HEADER, tagDef.getHttpRequestBodyContentType().getMimeType());
                break;
            case PUT:
                if (tagDef.getHttpRequestBody() != null) {
                    builder.PUT(HttpRequest.BodyPublishers.ofString(tagDef.getHttpRequestBody()));
                } else {
                    builder.PUT(HttpRequest.BodyPublishers.ofString(""));
                }
                builder.header(CONTENT_TYPE_HEADER, tagDef.getHttpRequestBodyContentType().getMimeType());
                break;
            default:
                return CompletableFuture
                            .failedFuture(
                                new IllegalStateException("There was an unexpected value present in the request config: " + tagDef.getHttpRequestMethod()));
        }

        return httpClient
                    .sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenApply(httpResponse -> getHttpData(httpResponse, url, httpTag.getName()));
    }

    private @NotNull HttpData getHttpData(final HttpResponse<String> httpResponse, final String url,
                                          final @NotNull String tagName) {
        Object payloadData = null;
        String responseContentType = null;

        if (isSuccessStatusCode(httpResponse.statusCode())) {
            final String bodyData = httpResponse.body();
            //-- if the content type is json, then apply the JSON to the output data,
            //-- else encode using base64 (as we dont know what the content is).
            if (bodyData != null) {
                responseContentType = httpResponse.headers().firstValue(CONTENT_TYPE_HEADER).orElse(null);
                responseContentType = adapterConfig.getHttpToMqttConfig().isAssertResponseIsJson() ?
                        JSON_MIME_TYPE :
                        responseContentType;
                if (JSON_MIME_TYPE.equals(responseContentType)) {
                    try {
                        payloadData = objectMapper.readTree(bodyData);
                    } catch (final Exception e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Invalid JSON data was [{}]", bodyData);
                        }
                        moduleServices.eventService()
                                .createAdapterEvent(adapterId, adapterInformation.getProtocolId())
                                .withSeverity(Event.SEVERITY.WARN)
                                .withMessage(String.format(
                                        "Http response on adapter '%s' could not be parsed as JSON data.",
                                        adapterId))
                                .fire();
                        throw new RuntimeException("unable to parse JSON data from HTTP response");
                    }
                } else {
                    if (responseContentType == null) {
                        responseContentType = PLAIN_MIME_TYPE;
                    }
                    final String base64 =
                            Base64.getEncoder().encodeToString(bodyData.getBytes(StandardCharsets.UTF_8));
                    payloadData = String.format(BASE64_ENCODED_VALUE, responseContentType, base64);
                }
            }
        }

        final HttpData data = new HttpData(url,
                httpResponse.statusCode(),
                responseContentType,
                adapterFactories.dataPointFactory());
        //When the body is empty, just include the metadata
        if (payloadData != null) {
            data.addDataPoint(tagName, payloadData);
        }
        return data;
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getHttpToMqttConfig().getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getHttpToMqttConfig().getMaxPollingErrorsBeforeRemoval();
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input, final @NotNull TagSchemaCreationOutput output) {
        output.finish(JsonSchema.createJsonSchema());
    }

    private static boolean isSuccessStatusCode(final int statusCode) {
        return statusCode >= 200 && statusCode <= 299;
    }

    protected @NotNull SSLContext createTrustAllContext() {
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            final X509ExtendedTrustManager trustManager = new X509ExtendedTrustManager() {
                @Override
                public void checkClientTrusted(
                        final X509Certificate @NotNull [] x509Certificates, final @NotNull String s) {
                }

                @Override
                public void checkServerTrusted(
                        final X509Certificate @NotNull [] x509Certificates, final @NotNull String s) {
                }

                @Override
                public X509Certificate @NotNull [] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(
                        final X509Certificate @NotNull [] x509Certificates,
                        final @NotNull String s,
                        final @NotNull Socket socket) {
                }

                @Override
                public void checkServerTrusted(
                        final X509Certificate @NotNull [] x509Certificates,
                        final @NotNull String s,
                        final @NotNull Socket socket) {
                }

                @Override
                public void checkClientTrusted(
                        final X509Certificate @NotNull [] x509Certificates,
                        final @NotNull String s,
                        final @NotNull SSLEngine sslEngine) {
                }

                @Override
                public void checkServerTrusted(
                        final X509Certificate @NotNull [] x509Certificates,
                        final @NotNull String s,
                        final @NotNull SSLEngine sslEngine) {
                }
            };
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            return sslContext;
        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}
