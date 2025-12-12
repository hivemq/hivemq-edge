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
import com.hivemq.edge.model.HiveMQEdgeRemoteConfiguration;
import com.hivemq.edge.model.HiveMQEdgeRemoteConnectivityException;
import com.hivemq.edge.model.HiveMQEdgeRemoteEvent;
import com.hivemq.edge.model.HiveMQEdgeRemoteServices;
import com.hivemq.http.HttpConstants;
import com.hivemq.http.core.HttpResponse;
import com.hivemq.http.core.HttpUrlConnectionClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

public class HiveMQEdgeHttpServiceImpl {

    static final @NotNull String SERVICE_DISCOVERY_URL =
            "https://raw.githubusercontent.com/hivemq/hivemq-edge/master/ext/remote-endpoints.txt";

    private static final @NotNull Map<String, String> POST_CONTENT_TYPE_HEADER =
            Map.of(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.JSON_MIME_TYPE);
    private static final int MAX_ERROR_ATTEMPTS_USAGE_STATS = 10;
    private static final int USAGE_STATS_ERROR_RESET_INTERVAL_MILLIS = 1000 * 60 * 10;
    private static final @NotNull Logger logger = LoggerFactory.getLogger(HiveMQEdgeHttpServiceImpl.class.getName());

    private final boolean activateUsage;
    private final @NotNull AtomicInteger errorCount;
    private final @NotNull Object monitor;
    private final @NotNull String hiveMqEdgeVersion;
    private final @NotNull ObjectMapper mapper;
    private final @NotNull String serviceDiscoveryEndpoint;
    private final @NotNull BlockingQueue<HiveMQEdgeRemoteEvent> usageEventQueue;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private @Nullable Thread cloudClientThread;
    private @Nullable Thread usageClientThread;
    private volatile boolean hasConnectivity;
    private volatile boolean running;
    private volatile @Nullable HiveMQEdgeRemoteConfiguration remoteConfiguration;
    private volatile @Nullable HiveMQEdgeRemoteServices remoteServices;

    public HiveMQEdgeHttpServiceImpl(
            final @NotNull String hiveMqEdgeVersion,
            final @NotNull ObjectMapper mapper,
            final @NotNull String serviceDiscoveryEndpoint,
            final int connectTimeoutMillis,
            final int readTimeoutMillis,
            final int retryTimeMillis,
            final boolean activateUsage) {
        Preconditions.checkNotNull(hiveMqEdgeVersion);
        Preconditions.checkNotNull(mapper);
        Preconditions.checkNotNull(serviceDiscoveryEndpoint);

        this.hiveMqEdgeVersion = hiveMqEdgeVersion;
        this.mapper = mapper;
        this.serviceDiscoveryEndpoint = serviceDiscoveryEndpoint;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.activateUsage = activateUsage;
        this.errorCount = new AtomicInteger();
        this.monitor = new Object();
        this.usageEventQueue = new LinkedBlockingDeque<>(20);
        this.cloudClientThread = new Thread(() -> {
            logger.debug("Starting Remote HTTP Configuration Service..");
            while (running) {
                try {
                    try {
                        remoteServices = httpGet(SERVICE_DISCOVERY_URL, HiveMQEdgeRemoteServices.class);
                        if (logger.isTraceEnabled()) {
                            logger.trace(
                                    "successfully established connection to remote service provider {}, online -> {}",
                                    serviceDiscoveryEndpoint,
                                    remoteServices);
                        }
                    } catch (final HiveMQEdgeRemoteConnectivityException e) {
                        hasConnectivity = false;
                        logger.debug(
                                "connection to http provider {} could not be established, using offline information",
                                serviceDiscoveryEndpoint);
                    }
                    if (remoteServices != null && validRemote(requireNonNull(remoteServices).getConfigEndpoint())) {
                        checkStatus();
                        if (remoteConfiguration == null) {
                            synchronized (this) {
                                if (remoteConfiguration == null) {
                                    remoteConfiguration = httpGet(requireNonNull(remoteServices).getConfigEndpoint(),
                                            HiveMQEdgeRemoteConfiguration.class);
                                }
                            }
                        }
                        if (activateUsage && usageClientThread == null) {
                            initUsage();
                        }
                    }
                    synchronized (monitor) {
                        monitor.wait(retryTimeMillis);
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (final Exception e) {
                    if (running) {
                        synchronized (monitor) {
                            try {
                                monitor.wait((long) retryTimeMillis * Math.min(errorCount.incrementAndGet(), 10));
                            } catch (final InterruptedException ex) {
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

    private static boolean validRemote(final @Nullable String remote) {
        if (remote == null || remote.trim().isEmpty()) {
            return false;
        }
        try {
            final URI ignored = URI.create(remote);
            return true;
        } catch (final Throwable e) {
            return false;
        }
    }

    private void initUsage() {
        if (usageClientThread == null) {
            final AtomicInteger usageErrorCount = new AtomicInteger();
            usageClientThread = new Thread(() -> {
                logger.debug("Starting Remote HTTP Usage Service..");
                long lastAttempt = 0;
                while (running) {
                    try {
                        boolean attempt = true;
                        final HiveMQEdgeRemoteEvent event = usageEventQueue.take();
                        if (usageErrorCount.get() > MAX_ERROR_ATTEMPTS_USAGE_STATS) {
                            if ((lastAttempt <
                                    (System.currentTimeMillis() - USAGE_STATS_ERROR_RESET_INTERVAL_MILLIS))) {
                                if (logger.isTraceEnabled()) {
                                    logger.trace("Error state reset interval exceeded, retry");
                                }
                            } else {
                                attempt = false;
                            }
                        }
                        attempt &= isOnline();
                        if (attempt) {
                            lastAttempt = System.currentTimeMillis();
                            if (validRemote(requireNonNull(remoteServices).getUsageEndpoint())) {
                                httpPost(requireNonNull(remoteServices).getUsageEndpoint(), event);
                                usageErrorCount.set(0);
                            }
                        }
                    } catch (final HiveMQEdgeRemoteConnectivityException e) {
                        usageErrorCount.incrementAndGet();
                        logger.trace("Communication error {} reporting usage event", usageErrorCount.get(), e);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (final Exception e) {
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
            if (usageClientThread != null) {
                usageClientThread.interrupt();
            }
        } catch (final Exception ignore) {
            // no-op
        } finally {
            usageClientThread = null;
        }
        synchronized (monitor) {
            monitor.notifyAll();
        }
        cloudClientThread = null;
    }

    public @NotNull Optional<HiveMQEdgeRemoteConfiguration> getRemoteConfiguration() {
        return Optional.ofNullable(remoteConfiguration);
    }

    public void fireEvent(final @NotNull HiveMQEdgeRemoteEvent event, final boolean queueIfOffline) {
        try {
            if (!activateUsage) {
                return;
            }
            final boolean process = isOnline() || queueIfOffline;
            if (logger.isTraceEnabled()) {
                logger.trace("Firing event {}, online ? {}", event.getEventType(), process);
            }
            if (process) {
                //-- only enqueue data when we know we can drain it (and this may change over time)
                event.setEdgeVersion(hiveMqEdgeVersion);
                if (!usageEventQueue.offer(event, 50, TimeUnit.MILLISECONDS)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Tracking-usage queue blocked, and discarded result");
                    }
                }
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            if (logger.isDebugEnabled()) {
                logger.debug("Could not enqueue usage data -> {}", e.getMessage());
            }
        }
    }

    private void checkResponse(final @NotNull String url, final @Nullable HttpResponse response)
            throws HiveMQEdgeRemoteConnectivityException {
        if (response == null) {
            throw new HiveMQEdgeRemoteConnectivityException("remote service [" + url + "] failed to respond <null>");
        } else {
            if (response.isError()) {
                throw new HiveMQEdgeRemoteConnectivityException("remote service [" +
                        response.getRequestUrl() +
                        "] failed (" +
                        response.getStatusCode() +
                        " - " +
                        response.getStatusMessage() +
                        ")");
            }
            if (response.getContentLength() <= 0) {
                throw new HiveMQEdgeRemoteConnectivityException("remote service [" +
                        response.getRequestUrl() +
                        "] failed with no expected content (" +
                        response.getStatusCode() +
                        " - " +
                        response.getStatusMessage() +
                        ")");
            }
        }
    }

    public boolean isOnline() {
        return remoteServices != null && hasConnectivity && running;
    }

    private void checkStatus() {
        try {
            if (validRemote(requireNonNull(remoteServices).getConfigEndpoint())) {
                final HttpResponse response = HttpUrlConnectionClient.head(Map.of(),
                        requireNonNull(remoteServices).getConfigEndpoint(),
                        readTimeoutMillis);
                hasConnectivity = !response.isError();
                if (logger.isTraceEnabled()) {
                    logger.trace("successfully established connection to http provider {}, online",
                            serviceDiscoveryEndpoint);
                }
                if (hasConnectivity && activateUsage) {
                    fireEvent(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_PING), false);
                }
            } else {
                hasConnectivity = false;
            }
        } catch (final IOException e) {
            hasConnectivity = false;
        }
    }

    private <T> @NotNull T httpGet(final @NotNull String url, final @NotNull Class<? extends T> cls)
            throws HiveMQEdgeRemoteConnectivityException {
        try {
            final HttpResponse response =
                    HttpUrlConnectionClient.get(Map.of(), url, connectTimeoutMillis, readTimeoutMillis);
            if (logger.isTraceEnabled()) {
                logger.trace("obtaining cloud service object from {} -> {}", url, response);
            }
            checkResponse(url, response);
            return mapper.readValue(response.getResponseBody(), cls);
        } catch (final IOException e) {
            throw new HiveMQEdgeRemoteConnectivityException("error in http socket connection", e);
        }
    }

    private void httpPost(final @NotNull String url, final @NotNull Object jsonPostObject)
            throws HiveMQEdgeRemoteConnectivityException {
        try {
            final String jsonBody = mapper.writeValueAsString(jsonPostObject);
            try (final InputStream is = new ByteArrayInputStream(jsonBody.getBytes())) {
                final long start = System.currentTimeMillis();
                final HttpResponse response = HttpUrlConnectionClient.post(POST_CONTENT_TYPE_HEADER,
                        url,
                        is,
                        connectTimeoutMillis,
                        readTimeoutMillis);
                if (logger.isTraceEnabled()) {
                    logger.trace("post to http service object {} -> {} in {}",
                            url,
                            response.getStatusCode(),
                            System.currentTimeMillis() - start);
                }
            }
        } catch (final IOException e) {
            throw new HiveMQEdgeRemoteConnectivityException("error sending request;", e);
        }
    }
}
