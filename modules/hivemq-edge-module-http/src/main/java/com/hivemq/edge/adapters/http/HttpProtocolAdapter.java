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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.adapters.http.model.HttpData;
import com.hivemq.edge.modules.adapters.PollingProtocolAdapter;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.factories.AdapterFactories;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterState;
import com.hivemq.edge.modules.api.events.model.Event;
import com.hivemq.edge.modules.api.events.model.EventBuilder;
import com.hivemq.edge.modules.config.AdapterSubscription;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.http.HttpConstants;
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
import java.util.concurrent.CompletableFuture;

import static com.hivemq.edge.HiveMQEdgeConstants.CLIENT_AGENT_PROPERTY_VALUE;

/**
 * @author HiveMQ Adapter Generator
 */
public class HttpProtocolAdapter implements PollingProtocolAdapter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(HttpProtocolAdapter.class);

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull HttpAdapterConfig adapterConfig;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull String version;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ModuleServices moduleServices;
    private final @NotNull AdapterFactories adapterFactories;

    private volatile @Nullable HttpClient httpClient = null;
    protected @NotNull
    final Object lock = new Object();
    static final @NotNull String RESPONSE_DATA = "httpResponseData";
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();

    public HttpProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull HttpAdapterConfig adapterConfig,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull String version,
            final @NotNull ProtocolAdapterInput<HttpAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = adapterConfig;
        this.metricRegistry = metricRegistry;
        this.version = version;
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.moduleServices = input.moduleServices();
        this.adapterFactories = input.adapterFactories();
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public @NotNull CompletableFuture<ProtocolAdapterStartOutput> start(
            @NotNull final ProtocolAdapterStartInput input, @NotNull final ProtocolAdapterStartOutput output) {
        try {
            protocolAdapterState.setConnectionStatus(ProtocolAdapter.ConnectionStatus.STATELESS);
            if (httpClient == null) {
                synchronized (lock) {
                    if (httpClient == null) {
                        initializeHttpRequest(adapterConfig);
                    }
                }
            }
            return CompletableFuture.completedFuture(output);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> stop() {
        httpClient = null;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public @NotNull ConnectionStatus getConnectionStatus() {
        return protocolAdapterState.getConnectionStatus();
    }

    @Override
    public @NotNull RuntimeStatus getRuntimeStatus() {
        return protocolAdapterState.getRuntimeStatus();
    }

    @Override
    public @Nullable String getErrorMessage() {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<ProtocolAdapterDataSample> poll() {
        if (httpClient != null) {
            final CompletableFuture<HttpData> dataFuture;
            switch (adapterConfig.getHttpRequestMethod()) {
                case GET:
                    dataFuture = httpGet(adapterConfig);
                    break;
                case POST:
                    dataFuture = httpPost(adapterConfig);
                    break;
                case PUT:
                    dataFuture = httpPut(adapterConfig);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + adapterConfig.getHttpRequestMethod());
            }

            return dataFuture.thenApply((data) -> {
                boolean publishData = isSuccessStatusCode(data.getHttpStatusCode()) ||
                        !adapterConfig.isHttpPublishSuccessStatusCodeOnly();
                protocolAdapterState.setConnectionStatus(isSuccessStatusCode(data.getHttpStatusCode()) ?
                        ProtocolAdapter.ConnectionStatus.STATELESS :
                        ProtocolAdapter.ConnectionStatus.ERROR);
                if (publishData) {
                    return data;
                }
                return null;
            });
        }
        return CompletableFuture.failedFuture(new RuntimeException("No response was created."));
    }


    // duplicated should likely go to Edge via a interface


    protected void initializeHttpRequest(@NotNull final HttpAdapterConfig config) {
        if (HttpUtils.validHttpOrHttpsUrl(config.getUrl())) {
            //initialize client
            HttpClient.Builder builder = HttpClient.newBuilder();
            builder.version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(config.getHttpConnectTimeout()));
            if (config.isAllowUntrustedCertificates()) {
                builder.sslContext(createTrustAllContext());
            }
            httpClient = builder.build();
        } else {
            protocolAdapterState.setErrorConnectionStatus(adapterConfig.getId(),
                    adapterInformation.getProtocolId(),
                    null,
                    "Invalid URL supplied");
        }
    }

    private static boolean isSuccessStatusCode(final int statusCode) {
        return statusCode >= 200 && statusCode <= 299;
    }

    protected @NotNull CompletableFuture<HttpData> httpPut(@NotNull final HttpAdapterConfig config) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder().PUT(HttpRequest.BodyPublishers.ofString(config.getHttpRequestBody()));
        builder.header(HttpConstants.CONTENT_TYPE_HEADER, config.getHttpRequestBodyContentType().getContentType());
        return executeInternal(config, builder);
    }

    protected @NotNull CompletableFuture<HttpData> httpPost(@NotNull final HttpAdapterConfig config) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(config.getHttpRequestBody()));
        builder.header(HttpConstants.CONTENT_TYPE_HEADER, config.getHttpRequestBodyContentType().getContentType());
        return executeInternal(config, builder);
    }

    protected @NotNull CompletableFuture<HttpData> httpGet(@NotNull final HttpAdapterConfig config) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().GET();
        return executeInternal(config, builder);
    }

    protected @NotNull CompletableFuture<HttpData> executeInternal(
            @NotNull final HttpAdapterConfig config, @NotNull final HttpRequest.Builder builder) {
        builder.uri(URI.create(config.getUrl()));
        //-- Ensure we apply a reasonable timeout so we don't hang threads
        Integer timeout = config.getHttpConnectTimeout();
        timeout = timeout == null ? HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS : timeout;
        timeout = Math.max(timeout, HttpAdapterConstants.MAX_TIMEOUT_SECONDS);
        builder.timeout(Duration.ofSeconds(timeout));
        builder.setHeader(HttpConstants.USER_AGENT_HEADER, String.format(CLIENT_AGENT_PROPERTY_VALUE, version));
        if (config.getHttpHeaders() != null && !config.getHttpHeaders().isEmpty()) {
            config.getHttpHeaders().stream().forEach(hv -> builder.setHeader(hv.getName(), hv.getValue()));
        }
        HttpRequest request = builder.build();
        CompletableFuture<HttpResponse<String>> responseFuture =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        return responseFuture.thenApply(response -> readResponse(config, response));
    }

    protected @NotNull HttpData readResponse(
            @NotNull final HttpAdapterConfig config, final @NotNull HttpResponse<String> response) {
        Object payloadData = null;
        String responseContentType = null;
        if (isSuccessStatusCode(response.statusCode())) {
            String bodyData = response.body() == null ? null : response.body();
            //-- if the content type is json, then apply the JSON to the output data,
            //-- else encode using base64 (as we dont know what the content is).
            if (bodyData != null) {
                responseContentType = response.headers().firstValue(HttpConstants.CONTENT_TYPE_HEADER).orElse(null);
                responseContentType =
                        config.isAssertResponseIsJson() ? HttpConstants.JSON_MIME_TYPE : responseContentType;
                if (HttpConstants.JSON_MIME_TYPE.equals(responseContentType)) {
                    try {
                        payloadData = objectMapper.readTree(bodyData);
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Invalid JSON data was [{}]", bodyData);
                        }
                        moduleServices.eventService()
                                .fireEvent(eventBuilder(Event.SEVERITY.WARN).withMessage(String.format(
                                        "Http response on adapter '%s' could not be parsed as JSON data.",
                                        adapterConfig.getId())).build());
                        throw new RuntimeException("unable to parse JSON data from HTTP response");
                    }
                } else {
                    if (responseContentType == null) {
                        responseContentType = HttpConstants.PLAIN_MIME_TYPE;
                    }
                    String base64 = Base64.getEncoder().encodeToString(bodyData.getBytes(StandardCharsets.UTF_8));
                    payloadData = String.format(HttpConstants.BASE64_ENCODED_VALUE, responseContentType, base64);
                }
            }
        }
        final AdapterSubscription adapterSubscription =
                adapterFactories.adapterSubscriptionFactory().create(config.getDestination(), config.getQos(), null);

        HttpData data = new HttpData(adapterSubscription,
                adapterConfig.getUrl(),
                response.statusCode(),
                responseContentType,
                adapterFactories.dataPointFactory());
        if (payloadData != null) {
            data.addDataPoint(RESPONSE_DATA, payloadData);
        } else {
            //When the body is empty, just include the metadata
            data.addDataPoint(RESPONSE_DATA,
                    new HttpData(adapterSubscription,
                            adapterConfig.getUrl(),
                            response.statusCode(),
                            responseContentType,
                            adapterFactories.dataPointFactory()));
        }
        return data;
    }

    protected @NotNull SSLContext createTrustAllContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            X509ExtendedTrustManager trustManager = new X509ExtendedTrustManager() {
                @Override
                public void checkClientTrusted(
                        final X509Certificate @NotNull [] x509Certificates, final @NotNull String s) {
                }

                @Override
                public void checkServerTrusted(
                        final X509Certificate @NotNull [] x509Certificates, final @NotNull String s) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
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
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull EventBuilder eventBuilder(
            final @NotNull Event.SEVERITY severity) {
        return adapterFactories.eventBuilderFactory()
                .create(adapterConfig.getId(), adapterInformation.getProtocolId())
                .withSeverity(severity);
    }

}
