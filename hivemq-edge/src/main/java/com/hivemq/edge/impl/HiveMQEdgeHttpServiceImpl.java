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
package com.hivemq.edge.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.model.HiveMQEdgeRemoteConfiguration;
import com.hivemq.edge.model.HiveMQEdgeRemoteConnectivityException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.HttpConstants;
import com.hivemq.http.core.HttpResponse;
import com.hivemq.http.core.HttpUrlConnectionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class HiveMQEdgeHttpServiceImpl {

    static Logger logger = LoggerFactory.getLogger(HiveMQEdgeHttpServiceImpl.class.getName());
    protected String serviceDiscoveryEndpoint;
    protected int connectTimeoutMillis;
    protected int readTimeoutMillis;
    protected int retryTimeMillis;
    protected AtomicInteger errorCount = new AtomicInteger();
    protected long lastRequestTime;

    protected ObjectMapper mapper;
    protected volatile boolean hasConnectivity;
    private Thread cloudClientMonitor;
    private volatile boolean running = false;
    private Object monitor = new Object();
    private volatile HiveMQEdgeRemoteConfiguration remoteConfiguration;

    public HiveMQEdgeHttpServiceImpl(
            final @NotNull ObjectMapper mapper,
            final @NotNull String serviceDiscoveryEndpoint,
            int connectTimeoutMillis,
            int readTimeoutMillis,
            int retryTimeMillis) {
        this.mapper = mapper;
        this.serviceDiscoveryEndpoint = serviceDiscoveryEndpoint;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.retryTimeMillis = Math.max(2000, retryTimeMillis);
        initMonitor();
    }

    protected void initMonitor() {
        cloudClientMonitor = new Thread(() -> {
            logger.debug("Starting Remote HTTP Configuration Service..");
            while (running) {
                try {
                    checkStatus();
                    if (remoteConfiguration == null) {
                        loadConfigurationInternal();
                    }
                    synchronized (monitor) {
                        monitor.wait(retryTimeMillis);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    synchronized (monitor) {
                        try {
                            monitor.wait(retryTimeMillis * Math.min(errorCount.incrementAndGet(), 10));
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }, "remote-configuration-monitor");
        cloudClientMonitor.setDaemon(true);
        cloudClientMonitor.setPriority(Thread.MIN_PRIORITY);
        cloudClientMonitor.start();
        running = true;
    }

    public void stop() {
        running = false;
        remoteConfiguration = null;
        synchronized (monitor) {
            monitor.notifyAll();
        }
        cloudClientMonitor = null;
    }

    public Optional<HiveMQEdgeRemoteConfiguration> getRemoteConfiguration() {
        return Optional.ofNullable(remoteConfiguration);
    }

    protected HiveMQEdgeRemoteConfiguration loadConfigurationInternal() throws HiveMQEdgeRemoteConnectivityException {
        if (remoteConfiguration == null) {
            synchronized (this) {
                if (remoteConfiguration == null) {
                    remoteConfiguration = httpGet(serviceDiscoveryEndpoint, HiveMQEdgeRemoteConfiguration.class);
                }
            }
        }
        return remoteConfiguration;
    }

    protected void checkResponse(HttpResponse response, boolean expectPayload)
            throws HiveMQEdgeRemoteConnectivityException {
        if (response == null) {
            throw new HiveMQEdgeRemoteConnectivityException("remote service [" +
                    response.getRequestUrl() +
                    "] failed to respond <null>");
        }
        if (response.isError()) {
            throw new HiveMQEdgeRemoteConnectivityException("remote service [" +
                    response.getRequestUrl() +
                    "] failed (" +
                    response.getStatusCode() +
                    " - " +
                    response.getStatusMessage() +
                    ")");
        }
        if (expectPayload && response.getContentLength() <= 0) {
            throw new HiveMQEdgeRemoteConnectivityException("remote service [" +
                    response.getRequestUrl() +
                    "] failed with no expected content (" +
                    response.getStatusCode() +
                    " - " +
                    response.getStatusMessage() +
                    ")");
        }
    }

    public boolean isOnline() {
        return hasConnectivity && running;
    }

    protected void checkStatus() {
        try {
            HttpResponse response =
                    HttpUrlConnectionClient.head(getHeaders(), serviceDiscoveryEndpoint, readTimeoutMillis);
            hasConnectivity = !response.isError();
            if (logger.isDebugEnabled()) {
                logger.trace("successfully established connection to http provider {}, online",
                        serviceDiscoveryEndpoint);
            }
        } catch (IOException e) {
            hasConnectivity = false;
            logger.trace("connection to http provider {} could not be established, using offline information",
                    serviceDiscoveryEndpoint);
        } finally {
            updateLastAttempt();
        }
    }

    protected void updateLastAttempt() {
        lastRequestTime = System.currentTimeMillis();
    }

    protected Map<String, String> getHeaders() {
        Map<String, String> map = new HashMap<>();
        return map;
    }

    protected <T> T httpGet(String url, Class<? extends T> cls) throws HiveMQEdgeRemoteConnectivityException {
        try {
            HttpResponse response =
                    HttpUrlConnectionClient.get(getHeaders(), url, connectTimeoutMillis, readTimeoutMillis);
            if (logger.isDebugEnabled()) {
                logger.debug("obtaining cloud service object from {} -> {}", url, response);
            }
            checkResponse(response, true);
            return mapper.readValue(response.getResponseBody(), cls);
        } catch (IOException e) {
            throw new HiveMQEdgeRemoteConnectivityException("error in http socket connection", e);
        } finally {
            updateLastAttempt();
        }
    }

    protected <T> T httpPost(String url, Class<? extends T> cls, Object jsonPostObject)
            throws HiveMQEdgeRemoteConnectivityException {
        try {
            String jsonBody = mapper.writeValueAsString(jsonPostObject);
            try (InputStream is = new ByteArrayInputStream(jsonBody.getBytes())) {
                Map<String, String> headers = getHeaders();
                headers.put(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.JSON_MIME_TYPE);
                headers.put(HttpConstants.CONTENT_ENCODING_HEADER, HttpConstants.DEFAULT_CHARSET.toString());
                HttpResponse response =
                        HttpUrlConnectionClient.post(headers, url, is, connectTimeoutMillis, readTimeoutMillis);
                if (logger.isDebugEnabled()) {
                    logger.debug("post to http service object from {} {} -> {}", url, jsonBody, response);
                }
                if (cls == Void.class) {
                    return null;
                } else {
                    checkResponse(response, true);
                    byte[] b = response.getResponseBody();
                    logger.error("response is -> {}", new String(b));
                    return mapper.readValue(b, cls);
                }
            }
        } catch (IOException e) {
            throw new HiveMQEdgeRemoteConnectivityException("error reading response body;", e);
        } finally {
            updateLastAttempt();
        }
    }

    protected <T> List<T> httpGetList(String url, Class<? extends T> cls) throws HiveMQEdgeRemoteConnectivityException {
        try {
            HttpResponse response =
                    HttpUrlConnectionClient.get(getHeaders(), url, connectTimeoutMillis, readTimeoutMillis);
            logger.debug("obtaining cloud service list from {} -> {} of type {}", url, response, cls.getName());
            checkResponse(response, true);
            return mapper.readValue(response.getResponseBody(),
                    mapper.getTypeFactory().constructCollectionType(List.class, cls));
        } catch (IOException e) {
            throw new HiveMQEdgeRemoteConnectivityException("error in http socket connection", e);
        } finally {
            updateLastAttempt();
        }
    }
}
