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
import com.hivemq.edge.modules.adapters.ProtocolAdapterException;
import com.hivemq.edge.modules.adapters.impl.AbstractProtocolAdapter;
import com.hivemq.edge.modules.adapters.params.NodeTree;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterDiscoveryInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterDiscoveryOutput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.adapters.params.impl.ProtocolAdapterPollingInputImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishBuilder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.HttpConstants;
import com.hivemq.http.core.HttpUtils;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author HiveMQ Adapter Generator
 */
public class HttpProtocolAdapter extends AbstractProtocolAdapter<HttpAdapterConfig> {

    private static final Logger log = LoggerFactory.getLogger(HttpProtocolAdapter.class);
    private volatile Object lock = new Object();
    private AtomicBoolean connected = new AtomicBoolean(false);
    private HttpClient httpClient = null;

    public HttpProtocolAdapter(final @NotNull ProtocolAdapterInformation adapterInformation,
                             final @NotNull HttpAdapterConfig adapterConfig,
                             final @NotNull MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    public CompletableFuture<Void> start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        try {
            bindServices(input.moduleServices());
            if(httpClient == null){
                synchronized (lock) {
                    if (httpClient == null) {
                        initializeHttpRequest(adapterConfig);
                        output.startedSuccessfully("Successfully connected");
                    }
                }
            }
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            output.failStart(e, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Void> stop() {
        try {
            //-- Stop polling jobs
            protocolAdapterPollingService.getPollingJobsForAdapter(getId()).stream().forEach(
                    protocolAdapterPollingService::stopPolling);
        } finally {
            connected.set(false);
            httpClient = null;
        }
        return CompletableFuture.completedFuture(null);
    }

    protected void initializeHttpRequest(@NotNull final HttpAdapterConfig config){
        initStartAttempt();
        if(HttpUtils.validHttpOrHttpsUrl(config.getUrl())){
            //initialize client
            httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(config.getHttpConnectTimeout()))
                    .build();
            connected.set(true);
            startPolling(new HttpRequestPoller(config));
        } else {
            connected.set(false);
            setLastErrorMessage("Invalid URL supplied");
        }
    }

    private void startPolling(final @NotNull HttpRequestPoller httpRequestPoller) {
        protocolAdapterPollingService.schedulePolling(this, httpRequestPoller);
    }

    @Override
    public CompletableFuture<Void> close() {
        return stop();
    }

    @Override
    public @NotNull Status status() {
        return connected.get() ? Status.CONNECTED : Status.DISCONNECTED;
    }

    private static boolean isSuccessStatusCode(final int statusCode){
        return statusCode >= 200 && statusCode <= 299;
    }

    protected void captured(final @NotNull HttpData data) throws ProtocolAdapterException {
        boolean publishData = isSuccessStatusCode(data.getHttpStatusCode()) || !adapterConfig.isHttpPublishSuccessStatusCodeOnly();
        if (publishData) {
            final ProtocolAdapterPublishBuilder publishBuilder = adapterPublishService.publish()
                    .withTopic(data.getTopic())
                    .withPayload(convertToJson(data))
                    .withQoS(data.getQos());
            final CompletableFuture<PublishReturnCode> publishFuture = publishBuilder.send();
            publishFuture.thenAccept(publishReturnCode -> {
                protocolAdapterMetricsHelper.incrementReadPublishSuccess();
            }).exceptionally(throwable -> {
                protocolAdapterMetricsHelper.incrementReadPublishFailure();
                log.warn("Error Publishing Http Payload", throwable);
                return null;
            });
        }
    }

    class HttpRequestPoller extends ProtocolAdapterPollingInputImpl {

        private final HttpAdapterConfig config;

        public HttpRequestPoller(final @NotNull HttpAdapterConfig config) {
            super(config.getPublishingInterval(),
                    config.getPublishingInterval(),
                    TimeUnit.MILLISECONDS,
                    config.getMaxPollingErrorsBeforeRemoval());
            this.config = config;
        }

        @Override
        public void execute() throws Exception {
            HttpData data = executeHttpRequest(config);
            if (data != null) {
                captured(data);
            }
        }
    }

    protected HttpData executeHttpRequest(@NotNull final HttpAdapterConfig config)
            throws IOException, InterruptedException {
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

    protected HttpData httpPut(@NotNull final HttpAdapterConfig config)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString(config.getHttpRequestBody()));
        builder.header(HttpConstants.CONTENT_TYPE_HEADER,
                config.getHttpRequestBodyContentType().getContentType());
        return executeInternal(config, builder);
    }

    protected HttpData httpPost(@NotNull final HttpAdapterConfig config)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(config.getHttpRequestBody()));
        builder.header(HttpConstants.CONTENT_TYPE_HEADER,
                config.getHttpRequestBodyContentType().getContentType());
        return executeInternal(config, builder);
    }

    protected HttpData httpGet(@NotNull final HttpAdapterConfig config)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .GET();
        return executeInternal(config, builder);
    }

    protected HttpData executeInternal(@NotNull final HttpAdapterConfig config, @NotNull final HttpRequest.Builder builder)
            throws IOException, InterruptedException {
        builder.uri(URI.create(config.getUrl()));
        //-- Ensure we apply a reasonable timeout so we don't hang threads
        Integer timeout = config.getHttpConnectTimeout();
        timeout = timeout == null ? HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS : timeout;
        timeout = Math.max(timeout, HttpAdapterConstants.MAX_TIMEOUT_SECONDS);
        builder.timeout(Duration.ofSeconds(timeout));
        builder.setHeader(HttpConstants.USER_AGENT_HEADER,
                String.format(HiveMQEdgeConstants.CLIENT_AGENT_PROPERTY_VALUE, HiveMQEdgeConstants.VERSION));
        if(config.getHttpHeaders() != null && !config.getHttpHeaders().isEmpty()){
            config.getHttpHeaders().stream().
                    forEach(hv -> builder.setHeader(hv.getName(), hv.getValue()));
        }
        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseContentType = response.headers().firstValue(HttpConstants.CONTENT_TYPE_HEADER).orElse(null);
        String bodyData = response.body() == null ? null : response.body();
        Object payloadData = null;
        //-- if the content type is json, then apply the JSON to the output data,
        //-- else encode using base64 (as we dont know what the content is).
        if(bodyData != null){
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

        HttpData data = new HttpData(adapterConfig.getUrl(),
                response.statusCode(),
                responseContentType,
                payloadData,
                config.getDestination(),
                config.getQos());
        return data;
    }
}
