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
package com.hivemq.edge.impl.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hivemq.edge.model.HiveMQEdgeRemoteEvent;
import com.hivemq.edge.model.HiveMQEdgeRemoteConfiguration;
import com.hivemq.edge.model.HiveMQEdgeRemoteConnectivityException;
import com.hivemq.edge.model.HiveMQEdgeRemoteServices;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.HttpConstants;
import com.hivemq.http.core.HttpResponse;
import com.hivemq.http.core.HttpUrlConnectionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HiveMQEdgeHttpServiceImpl {

    static Logger logger = LoggerFactory.getLogger(HiveMQEdgeHttpServiceImpl.class.getName());
    static final String SERVICE_DISCOVERY_URL = "https://raw.githubusercontent.com/hivemq/hivemq-edge/master/ext/remote-endpoints.txt";
    protected String serviceDiscoveryEndpoint;
    protected String hiveMqEdgeVersion;
    protected int connectTimeoutMillis;
    protected int readTimeoutMillis;
    protected int retryTimeMillis;
    protected AtomicInteger errorCount = new AtomicInteger();
    protected long lastRequestTime;

    protected final boolean activateUsage;

    protected ObjectMapper mapper;
    protected volatile boolean hasConnectivity;
    private Thread cloudClientThread;
    private Thread usageClientThread;
    private volatile boolean running = false;
    private Object monitor = new Object();
    private volatile HiveMQEdgeRemoteConfiguration remoteConfiguration;
    private volatile HiveMQEdgeRemoteServices remoteServices;
    private BlockingQueue<HiveMQEdgeRemoteEvent> usageEventQueue = new LinkedBlockingDeque<>(20);
    static final int MAX_ERROR_ATTEMPTS_USAGE_STATS = 10;
    static final int USAGE_STATS_ERROR_RESET_INTERVAL_MILLIS = 1000 * 60 * 10;

    public HiveMQEdgeHttpServiceImpl(
            final @NotNull String hiveMqEdgeVersion,
            final @NotNull ObjectMapper mapper,
            final @NotNull String serviceDiscoveryEndpoint,
            int connectTimeoutMillis,
            int readTimeoutMillis,
            int retryTimeMillis,
            boolean activateUsage) {
        Preconditions.checkNotNull(hiveMqEdgeVersion);
        Preconditions.checkNotNull(mapper);
        Preconditions.checkNotNull(serviceDiscoveryEndpoint);

        this.hiveMqEdgeVersion = hiveMqEdgeVersion;
        this.mapper = mapper;
        this.serviceDiscoveryEndpoint = serviceDiscoveryEndpoint;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.retryTimeMillis = Math.max(2000, retryTimeMillis);
        this.activateUsage = activateUsage;
        initMonitor();
    }

    private void initMonitor() {
        cloudClientThread = new Thread(() -> {
            logger.debug("Starting Remote HTTP Configuration Service..");
            while (running) {
                try {
                    loadRemoteServices();
                    if(remoteServices != null &&
                            validRemote(remoteServices.getConfigEndpoint())){
                        checkStatus();
                        if (remoteConfiguration == null) {
                            loadConfigurationInternal();
                        }
                        if(activateUsage && usageClientThread == null){
                            initUsage();
                        }
                    }
                    synchronized (monitor) {
                        monitor.wait(retryTimeMillis);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    if(running){
                        synchronized (monitor) {
                            try {
                                monitor.wait(retryTimeMillis * Math.min(errorCount.incrementAndGet(), 10));
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
            }
        }, "remote-configuration-monitor");
        cloudClientThread.setDaemon(true);
        cloudClientThread.setPriority(Thread.MIN_PRIORITY);
        cloudClientThread.start();
        running = true;
    }

    private void initUsage() {
        if(usageClientThread == null){
            AtomicInteger usageErrorCount = new AtomicInteger();
            usageClientThread = new Thread(() -> {
                logger.debug("Starting Remote HTTP Usage Service..");
                long lastAttempt = 0;
                while (running) {
                    try {
                        boolean attempt = true;
                        HiveMQEdgeRemoteEvent event = usageEventQueue.take();
                        if(usageErrorCount.get() > MAX_ERROR_ATTEMPTS_USAGE_STATS){
                            if((lastAttempt < (System.currentTimeMillis() - USAGE_STATS_ERROR_RESET_INTERVAL_MILLIS))) {
                                if (logger.isTraceEnabled()) {
                                    logger.trace("Error state reset interval exceeded, retry");
                                }
                            } else {
                                attempt = false;
                            }
                        }
                        attempt &= isOnline();
                        if(attempt){
                            lastAttempt = System.currentTimeMillis();
                            if(validRemote(remoteServices.getUsageEndpoint())){
                                httpPost(remoteServices.getUsageEndpoint(), Void.class, event, 1000, 1000);
                                usageErrorCount.set(0);
                            }
                        }
                    }
                    catch (HiveMQEdgeRemoteConnectivityException e) {
                        usageErrorCount.incrementAndGet();
                        logger.trace("Communication error {} reporting usage event", usageErrorCount.get(), e);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    catch(Exception e){
                        usageErrorCount.incrementAndGet();
                        //-- ensure we only break for unchecked
                    }
                }
            }, "remote-usage-monitor");
            usageClientThread.setDaemon(true);
            usageClientThread.setPriority(Thread.MIN_PRIORITY);
            usageClientThread.start();
        }
    }

    public void stop() {
        running = false;
        remoteConfiguration = null;
        remoteServices = null;
        try {
            if(usageClientThread != null){
                usageClientThread.interrupt();
            }
        } catch(Exception e){
        } finally {
            usageClientThread = null;
        }
        synchronized (monitor) {
            monitor.notifyAll();
        }
        cloudClientThread = null;
    }

    public Optional<HiveMQEdgeRemoteConfiguration> getRemoteConfiguration() {
        return Optional.ofNullable(remoteConfiguration);
    }

    public void fireEvent(final HiveMQEdgeRemoteEvent event, boolean queueIfOffline){
        try {
            if(!activateUsage) return;
            boolean process = isOnline() || queueIfOffline;
            if(logger.isTraceEnabled()){
                logger.trace("Firing event {}, online ? {}", event.getEventType(), process);
            }
            if(process){
                //-- only enqueue data when we know we can drain it (and this may change over time)
                event.setEdgeVersion(hiveMqEdgeVersion);
                if(!usageEventQueue.offer(event, 50, TimeUnit.MILLISECONDS)){
                    if(logger.isTraceEnabled()){
                        logger.trace("Tracking-usage queue blocked, and discarded result");
                    }
                }
            }
        } catch (InterruptedException e) {
            if(logger.isDebugEnabled()){
                logger.debug("Could not enqueue usage data -> {}", e.getMessage());
            }
        }
    }

    private HiveMQEdgeRemoteConfiguration loadConfigurationInternal() throws HiveMQEdgeRemoteConnectivityException {
        if (remoteConfiguration == null) {
            synchronized (this) {
                if (remoteConfiguration == null) {
                    remoteConfiguration = httpGet(remoteServices.getConfigEndpoint(), HiveMQEdgeRemoteConfiguration.class);
                }
            }
        }
        return remoteConfiguration;
    }

    private void checkResponse(HttpResponse response, boolean expectPayload)
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
        return remoteServices != null && hasConnectivity && running;
    }

    private void checkStatus() {
        try {
            if(validRemote(remoteServices.getConfigEndpoint())){
                HttpResponse response =
                        HttpUrlConnectionClient.head(getHeaders(), remoteServices.getConfigEndpoint(), readTimeoutMillis);
                hasConnectivity = !response.isError();
                if (logger.isTraceEnabled()) {
                    logger.trace("successfully established connection to http provider {}, online",
                            serviceDiscoveryEndpoint);
                }
                if(hasConnectivity && activateUsage){
                    HiveMQEdgeRemoteEvent event = new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_PING);
                    fireEvent(event, false);
                }
            } else {
                hasConnectivity = false;
            }
        } catch (IOException e) {
            hasConnectivity = false;
        } finally {
            updateLastAttempt();
        }
    }

    private void loadRemoteServices() {
        try {
            remoteServices = httpGet(SERVICE_DISCOVERY_URL, HiveMQEdgeRemoteServices.class);
            if (logger.isTraceEnabled()) {
                logger.trace("successfully established connection to remote service provider {}, online -> {}",
                        serviceDiscoveryEndpoint, remoteServices);
            }
        } catch (HiveMQEdgeRemoteConnectivityException e) {
            hasConnectivity = false;
            logger.debug("connection to http provider {} could not be established, using offline information",
                    serviceDiscoveryEndpoint);
        } finally {
            updateLastAttempt();
        }
    }

    private void updateLastAttempt() {
        lastRequestTime = System.currentTimeMillis();
    }

    private Map<String, String> getHeaders() {
        Map<String, String> map = new HashMap<>();
        return map;
    }

    private <T> T httpGet(String url, Class<? extends T> cls) throws HiveMQEdgeRemoteConnectivityException {
        try {
            HttpResponse response =
                    HttpUrlConnectionClient.get(getHeaders(), url, connectTimeoutMillis, readTimeoutMillis);
            if (logger.isTraceEnabled()) {
                logger.trace("obtaining cloud service object from {} -> {}", url, response);
            }
            checkResponse(response, true);
            return mapper.readValue(response.getResponseBody(), cls);
        } catch (IOException e) {
            throw new HiveMQEdgeRemoteConnectivityException("error in http socket connection", e);
        } finally {
            updateLastAttempt();
        }
    }

    private <T> T httpPost(String url, Class<? extends T> cls, Object jsonPostObject, int connectTimeoutMillis, int readTimeoutMillis)
            throws HiveMQEdgeRemoteConnectivityException {
        try {
            String jsonBody = mapper.writeValueAsString(jsonPostObject);
            try (InputStream is = new ByteArrayInputStream(jsonBody.getBytes())) {
                Map<String, String> headers = getHeaders();
                headers.put(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.JSON_MIME_TYPE);
                headers.put(HttpConstants.CONTENT_ENCODING_HEADER, HttpConstants.DEFAULT_CHARSET.toString());
                long start = System.currentTimeMillis();
                HttpResponse response =
                        HttpUrlConnectionClient.post(headers, url, is, connectTimeoutMillis, readTimeoutMillis);
                if (logger.isTraceEnabled()) {
                    logger.trace("post to http service object {} -> {} in {}", url, response.getStatusCode(), System.currentTimeMillis() - start);
                }
                if (cls == Void.class) {
                    return null;
                } else {
                    checkResponse(response, true);
                    byte[] b = response.getResponseBody();
                    return mapper.readValue(b, cls);
                }
            }
        } catch (IOException e) {
            throw new HiveMQEdgeRemoteConnectivityException("error sending request;", e);
        } finally {
            updateLastAttempt();
        }
    }

    private <T> List<T> httpGetList(String url, Class<? extends T> cls) throws HiveMQEdgeRemoteConnectivityException {
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

    private static boolean validRemote(@NotNull String remote){
        if(remote == null || remote.trim().length() == 0){
            return false;
        }
        try {
            URL url = new URL(remote);
            return true;
        } catch(MalformedURLException e){
            return false;
        }
    }
}
