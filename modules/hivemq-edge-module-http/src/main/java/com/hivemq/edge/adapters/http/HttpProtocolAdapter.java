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
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.adapters.http.model.HttpData;
import com.hivemq.edge.modules.adapters.impl.AbstractPollingProtocolAdapter;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.http.core.HttpConstants;
import com.hivemq.http.core.HttpUtils;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
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

/**
 * @author HiveMQ Adapter Generator
 */
public class HttpProtocolAdapter extends AbstractPollingProtocolAdapter<HttpAdapterConfig, HttpData> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(HttpProtocolAdapter.class);
    private volatile @Nullable HttpClient httpClient = null;
    static final String RESPONSE_DATA = "httpResponseData";

    public HttpProtocolAdapter(final @NotNull ProtocolAdapterInformation adapterInformation,
                             final @NotNull HttpAdapterConfig adapterConfig,
                             final @NotNull MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    protected CompletableFuture<ProtocolAdapterStartOutput> startInternal(final @NotNull ProtocolAdapterStartOutput output) {
        try {
            setConnectionStatus(ConnectionStatus.STATELESS);
            if(httpClient == null){
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
    protected CompletableFuture<Void> stopInternal() {
        httpClient = null;
        return CompletableFuture.completedFuture(null);
    }

    protected void initializeHttpRequest(@NotNull final HttpAdapterConfig config){
        if(HttpUtils.validHttpOrHttpsUrl(config.getUrl())){
            //initialize client
            HttpClient.Builder builder = HttpClient.newBuilder();
            builder.version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(config.getHttpConnectTimeout()));
            if(config.isAllowUntrustedCertificates()) {
                builder.sslContext(createTrustAllContext());
            }
            httpClient = builder.build();
            startPolling(new Sampler(config));
        } else {
            setErrorConnectionStatus(null, "Invalid URL supplied");
        }
    }

    private static boolean isSuccessStatusCode(final int statusCode){
        return statusCode >= 200 && statusCode <= 299;
    }

    protected CompletableFuture<?> captureDataSample(final @NotNull HttpData data){
        boolean publishData = isSuccessStatusCode(data.getHttpStatusCode()) || !adapterConfig.isHttpPublishSuccessStatusCodeOnly();
        setConnectionStatus(isSuccessStatusCode(data.getHttpStatusCode()) ? ConnectionStatus.STATELESS : ConnectionStatus.ERROR);
        if (publishData) {
           return super.captureDataSample(data);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<HttpData> onSamplerInvoked(final HttpAdapterConfig config) {
        if(httpClient != null){
            switch (config.getHttpRequestMethod()){
                case GET:
                    return httpGet(config);
                case POST:
                    return httpPost(config);
                case PUT:
                    return httpPut(config);
            }
        }
        return null;
    }

    protected CompletableFuture<HttpData> httpPut(@NotNull final HttpAdapterConfig config){
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString(config.getHttpRequestBody()));
        builder.header(HttpConstants.CONTENT_TYPE_HEADER,
                config.getHttpRequestBodyContentType().getContentType());
        return executeInternal(config, builder);
    }

    protected CompletableFuture<HttpData> httpPost(@NotNull final HttpAdapterConfig config){
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(config.getHttpRequestBody()));
        builder.header(HttpConstants.CONTENT_TYPE_HEADER,
                config.getHttpRequestBodyContentType().getContentType());
        return executeInternal(config, builder);
    }

    protected CompletableFuture<HttpData> httpGet(@NotNull final HttpAdapterConfig config){
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .GET();
        return executeInternal(config, builder);
    }

    protected CompletableFuture<HttpData> executeInternal(@NotNull final HttpAdapterConfig config, @NotNull final HttpRequest.Builder builder) {
        builder.uri(URI.create(config.getUrl()));
        //-- Ensure we apply a reasonable timeout so we don't hang threads
        Integer timeout = config.getHttpConnectTimeout();
        timeout = timeout == null ? HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS : timeout;
        timeout = Math.max(timeout, HttpAdapterConstants.MAX_TIMEOUT_SECONDS);
        builder.timeout(Duration.ofSeconds(timeout));
        builder.setHeader(HttpConstants.USER_AGENT_HEADER,
                String.format(HiveMQEdgeConstants.CLIENT_AGENT_PROPERTY_VALUE, HiveMQEdgeConstants.VERSION));
        if (config.getHttpHeaders() != null && !config.getHttpHeaders().isEmpty()) {
            config.getHttpHeaders().stream().forEach(hv -> builder.setHeader(hv.getName(), hv.getValue()));
        }
        HttpRequest request = builder.build();
        CompletableFuture<HttpResponse<String>> responseFuture =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        return responseFuture.thenApply(response -> readResponse(config, response));
    }

    protected HttpData readResponse(@NotNull final HttpAdapterConfig config, final @NotNull HttpResponse<String> response){
        Object payloadData = null;
        String responseContentType = null;
        if(isSuccessStatusCode(response.statusCode())){
            String bodyData = response.body() == null ? null : response.body();
            //-- if the content type is json, then apply the JSON to the output data,
            //-- else encode using base64 (as we dont know what the content is).
            if(bodyData != null){
                responseContentType = response.headers().firstValue(HttpConstants.CONTENT_TYPE_HEADER).orElse(null);
                if(HttpConstants.JSON_MIME_TYPE.equals(responseContentType)) {
                    try {
                        payloadData = objectMapper.readTree(bodyData);
                    } catch (Exception e){
                        log.warn("Error encountered marshalling HTTP response data to json", e);
                        if(log.isDebugEnabled()){
                            log.debug("Invalid json data was [{}]", bodyData);
                        }
                    }
                } else {
                    if(responseContentType == null){
                        responseContentType = HttpConstants.PLAIN_MIME_TYPE;
                    }
                    String base64 = Base64.getEncoder().encodeToString(bodyData.getBytes(StandardCharsets.UTF_8));
                    payloadData = String.format(HttpConstants.BASE64_ENCODED_VALUE, responseContentType, base64);
                }
            }
        }
        HttpData data = new HttpData(
                new AbstractProtocolAdapterConfig.Subscription(config.getDestination(), config.getQos()),
                adapterConfig.getUrl(),
                response.statusCode(),
                responseContentType);
        data.addDataPoint(RESPONSE_DATA, payloadData);
        return data;
    }

   protected SSLContext createTrustAllContext()  {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            X509ExtendedTrustManager trustManager = new X509ExtendedTrustManager() {
                @Override
                public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s) {
                }
                @Override
                public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s) {
                }
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                @Override
                public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s, final Socket socket) {
                }
                @Override
                public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s, final Socket socket) {
                }
                @Override
                public void checkClientTrusted(
                        final X509Certificate[] x509Certificates,
                        final String s,
                        final SSLEngine sslEngine) {
                }
                @Override
                public void checkServerTrusted(
                        final X509Certificate[] x509Certificates,
                        final String s,
                        final SSLEngine sslEngine) {
                }
            };
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            return sslContext;
        } catch(NoSuchAlgorithmException | KeyManagementException e){
            throw new RuntimeException(e);
        }
   }
}
