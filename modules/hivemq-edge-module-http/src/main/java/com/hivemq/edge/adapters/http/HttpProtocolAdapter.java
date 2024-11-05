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

import com.fasterxml.jackson.databind.JsonNode;
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
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.adapters.http.config.BidirectionalHttpAdapterConfig;
import com.hivemq.edge.adapters.http.config.HttpAdapterConfig;
import com.hivemq.edge.adapters.http.config.http2mqtt.HttpToMqttMapping;
import com.hivemq.edge.adapters.http.config.mqtt2http.MqttToHttpMapping;
import com.hivemq.edge.adapters.http.model.HttpData;
import com.hivemq.edge.adapters.http.mqtt2http.HttpPayload;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.STATELESS;
import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.JSON_MIME_TYPE;
import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.PLAIN_MIME_TYPE;

/**
 * @author HiveMQ Adapter Generator
 */
public class HttpProtocolAdapter
        implements PollingProtocolAdapter<HttpToMqttMapping>, WritingProtocolAdapter<MqttToHttpMapping> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(HttpProtocolAdapter.class);

    private static final @NotNull String CONTENT_TYPE_HEADER = "Content-Type";
    private static final @NotNull String BASE64_ENCODED_VALUE = "data:%s;base64,%s";
    private static final @NotNull String USER_AGENT_HEADER = "User-Agent";
    private static final @NotNull String RESPONSE_DATA = "httpResponseData";

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull HttpAdapterConfig adapterConfig;
    private final @NotNull String version;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ModuleServices moduleServices;
    private final @NotNull AdapterFactories adapterFactories;

    private volatile @Nullable HttpClient httpClient = null;
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();

    public HttpProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<HttpAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.version = input.getVersion();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.moduleServices = input.moduleServices();
        this.adapterFactories = input.adapterFactories();
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        try {
            protocolAdapterState.setConnectionStatus(STATELESS);
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
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(
            final @NotNull PollingInput<HttpToMqttMapping> pollingInput, final @NotNull PollingOutput pollingOutput) {

        if (httpClient == null) {
            pollingOutput.fail(new ProtocolAdapterException(), "No response was created, because the client is null.");
            return;
        }

        final HttpToMqttMapping httpToMqttMapping = pollingInput.getPollingContext();

        // first resolve the tag
        final String tagName = pollingInput.getPollingContext().getTagName();
        adapterConfig.getTags().stream()
                .filter(tag -> tag.getName().equals(pollingInput.getPollingContext().getTagName()))
                .findFirst()
                .ifPresentOrElse(
                        def -> pollHttp(pollingOutput, def, httpToMqttMapping),
                        () -> pollingOutput.fail("Polling for protocol adapter failed because the used tag '" +
                                pollingInput.getPollingContext().getTagName() +
                                "' was not found. For the polling to work the tag must be created via REST API or the UI.")
                );

    }

    private void pollHttp(
            @NotNull PollingOutput pollingOutput,
            HttpTag httpTag,
            HttpToMqttMapping httpToMqttMapping) {
        final HttpRequest.Builder builder = HttpRequest.newBuilder();
        final String url = httpTag.getDefinition().getUrl();
        builder.uri(URI.create(url));
        builder.timeout(Duration.ofSeconds(httpToMqttMapping.getHttpRequestTimeoutSeconds()));
        builder.setHeader(USER_AGENT_HEADER, String.format("HiveMQ-Edge; %s", version));

        httpToMqttMapping.getHttpHeaders().forEach(hv -> builder.setHeader(hv.getName(), hv.getValue()));

        switch (httpToMqttMapping.getHttpRequestMethod()) {
            case GET:
                builder.GET();
                break;
            case POST:
                builder.POST(HttpRequest.BodyPublishers.ofString(httpToMqttMapping.getHttpRequestBody()));
                builder.header(CONTENT_TYPE_HEADER, httpToMqttMapping.getHttpRequestBodyContentType().getMimeType());
                break;
            case PUT:
                builder.PUT(HttpRequest.BodyPublishers.ofString(httpToMqttMapping.getHttpRequestBody()));
                builder.header(CONTENT_TYPE_HEADER, httpToMqttMapping.getHttpRequestBodyContentType().getMimeType());
                break;
            default:
                pollingOutput.fail(new IllegalStateException("Unexpected value: " +
                                httpToMqttMapping.getHttpRequestMethod()),
                        "There was an unexpected value present in the request config: " +
                                httpToMqttMapping.getHttpRequestMethod());
                return;
        }

        final CompletableFuture<HttpResponse<String>> responseFuture =
                httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());

        final CompletableFuture<HttpData> dataFuture = responseFuture.thenApply(httpResponse -> {
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
                                    .createAdapterEvent(adapterConfig.getId(), adapterInformation.getProtocolId())
                                    .withSeverity(Event.SEVERITY.WARN)
                                    .withMessage(String.format(
                                            "Http response on adapter '%s' could not be parsed as JSON data.",
                                            adapterConfig.getId()))
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

            final HttpData data = new HttpData(httpToMqttMapping, url,
                    httpResponse.statusCode(),
                    responseContentType,
                    adapterFactories.dataPointFactory());
            //When the body is empty, just include the metadata
            if (payloadData != null) {
                data.addDataPoint(RESPONSE_DATA, payloadData);
            }
            return data;
        });

        dataFuture.whenComplete((data, throwable) -> {
            if (throwable != null) {
                pollingOutput.fail(throwable, null);
                return;
            }

            if (data.isSuccessStatusCode()) {
                protocolAdapterState.setConnectionStatus(STATELESS);
            } else {
                protocolAdapterState.setConnectionStatus(ERROR);
            }

            if (data.isSuccessStatusCode() ||
                    !adapterConfig.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()) {
                data.getDataPoints().forEach(pollingOutput::addDataPoint);
            }
            pollingOutput.finish();
        });
    }

    @Override
    public @NotNull List<HttpToMqttMapping> getPollingContexts() {
        return List.copyOf(adapterConfig.getHttpToMqttConfig().getMappings());
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
    public void write(final @NotNull WritingInput writingInput, final @NotNull WritingOutput writingOutput) {
        if (httpClient == null) {
            writingOutput.fail(new ProtocolAdapterException(), "No response was created, because the client is null.");
            return;
        }
        final MqttToHttpMapping mqttToHttpMapping = (MqttToHttpMapping) writingInput.getWritingContext();

        final String tagName = mqttToHttpMapping.getTagName();
        final HttpTag httpTag;
        adapterConfig.getTags().stream()
                .filter(tag -> tag.getName().equals(mqttToHttpMapping.getTagName()))
                .findFirst()
                .ifPresentOrElse(
                        def -> writeHttp(writingInput, writingOutput, def, mqttToHttpMapping),
                        () -> writingOutput.fail("Writing for protocol adapter failed because the used tag '" +
                                mqttToHttpMapping.getTagName() +
                                "' was not found. For the polling to work the tag must be created via REST API or the UI.")
                );
    }

    private void writeHttp(
            @NotNull WritingInput writingInput,
            @NotNull WritingOutput writingOutput,
            @NotNull HttpTag httpTag,
            @NotNull MqttToHttpMapping mqttToHttpMapping) {
        final String url = httpTag.getDefinition().getUrl();

        final HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(URI.create(url));
        builder.timeout(Duration.ofSeconds(mqttToHttpMapping.getHttpRequestTimeoutSeconds()));
        builder.setHeader(USER_AGENT_HEADER, String.format("HiveMQ-Edge; %s", version));
        mqttToHttpMapping.getHttpHeaders().forEach(hv -> builder.setHeader(hv.getName(), hv.getValue()));

        final HttpPayload httpPayload = (HttpPayload) writingInput.getWritingPayload();
        final String payloadAsString = httpPayload.getValue().toString();

        switch (mqttToHttpMapping.getHttpRequestMethod()) {
            case POST:
                builder.POST(HttpRequest.BodyPublishers.ofString(payloadAsString));
                builder.header(CONTENT_TYPE_HEADER, JSON_MIME_TYPE);
                break;
            case PUT:
                builder.PUT(HttpRequest.BodyPublishers.ofString(payloadAsString));
                builder.header(CONTENT_TYPE_HEADER, JSON_MIME_TYPE);
                break;
            default:
                writingOutput.fail(new IllegalStateException("Unsupported request method: " +
                                mqttToHttpMapping.getHttpRequestMethod()),
                        "There was an unexpected value present in the request config: " +
                                mqttToHttpMapping.getHttpRequestMethod());
                return;
        }

        final CompletableFuture<HttpResponse<String>> responseFuture =
                httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());

        responseFuture.thenAccept(httpResponse -> {
            if (isSuccessStatusCode(httpResponse.statusCode())) {
                writingOutput.finish();
            } else {
                writingOutput.fail(String.format(
                        "Forwarding a message to url '%s' failed with status code '%d", url,
                        httpResponse.statusCode()));
            }
        });
    }

    @Override
    public @NotNull List<MqttToHttpMapping> getWritingContexts() {
        if (adapterConfig instanceof BidirectionalHttpAdapterConfig) {
            return ((BidirectionalHttpAdapterConfig) adapterConfig).getMqttToHttpConfig().getMappings();
        }
        return Collections.emptyList();
    }

    @Override
    public void createTagSchema(final @NotNull TagSchemaCreationInput input, final @NotNull TagSchemaCreationOutput output) {
        output.finish(JsonSchema.createJsonSchema());
    }

    @Override
    public @NotNull Class<? extends WritingPayload> getMqttPayloadClass() {
        return HttpPayload.class;
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
