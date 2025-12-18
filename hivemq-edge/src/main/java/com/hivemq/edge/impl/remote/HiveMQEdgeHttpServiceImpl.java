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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.requireNonNull;

public class HiveMQEdgeHttpServiceImpl {

    static final @NotNull String SERVICE_DISCOVERY_URL =
            "https://raw.githubusercontent.com/hivemq/hivemq-edge/master/ext/remote-endpoints.txt";

    private static final @NotNull Map<String, String> POST_CONTENT_TYPE_HEADER =
            Map.of(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.JSON_MIME_TYPE);
    private static final int MAX_ERROR_ATTEMPTS_USAGE_STATS = 10;
    private static final int USAGE_STATS_ERROR_RESET_INTERVAL_MILLIS = 1000 * 60 * 10;
    private static final int EVENT_QUEUE_CAPACITY = 20;
    private static final int EVENT_OFFER_TIMEOUT_MILLIS = 50;
    private static final int MAX_ERROR_BACKOFF_MULTIPLIER = 10;
    private static final int THREAD_JOIN_TIMEOUT_MILLIS = 5000;
    private static final @NotNull Logger logger = LoggerFactory.getLogger(HiveMQEdgeHttpServiceImpl.class);

    private final @NotNull String hiveMqEdgeVersion;
    private final @NotNull ObjectMapper mapper;
    private final @NotNull String serviceDiscoveryEndpoint;
    private final @NotNull BlockingQueue<HiveMQEdgeRemoteEvent> usageEventQueue;
    private final @NotNull Lock configurationLock;
    private final @NotNull Object monitor;
    private final @NotNull AtomicInteger errorCount;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final int retryTimeMillis;
    private final boolean activateUsage;
    private final @NotNull Thread cloudClientThread;
    private volatile boolean running;
    private volatile boolean hasConnectivity;
    private volatile @Nullable HiveMQEdgeRemoteConfiguration remoteConfiguration;
    private volatile @Nullable HiveMQEdgeRemoteServices remoteServices;
    private volatile @Nullable Thread usageClientThread;

    public HiveMQEdgeHttpServiceImpl(
            final @NotNull String hiveMqEdgeVersion,
            final @NotNull ObjectMapper mapper,
            final @NotNull String serviceDiscoveryEndpoint,
            final int connectTimeoutMillis,
            final int readTimeoutMillis,
            final int retryTimeMillis,
            final boolean activateUsage) {
        this.hiveMqEdgeVersion = requireNonNull(hiveMqEdgeVersion, "hiveMqEdgeVersion must not be null");
        this.mapper = requireNonNull(mapper, "mapper must not be null");
        this.serviceDiscoveryEndpoint =
                requireNonNull(serviceDiscoveryEndpoint, "serviceDiscoveryEndpoint must not be null");
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.retryTimeMillis = retryTimeMillis;
        this.activateUsage = activateUsage;
        this.errorCount = new AtomicInteger();
        this.monitor = new Object();
        this.configurationLock = new ReentrantLock();
        this.usageEventQueue = new LinkedBlockingDeque<>(EVENT_QUEUE_CAPACITY);
        this.running = true;
        this.cloudClientThread = new Thread(this::runCloudClientLoop, "remote-configuration-monitor");
        this.cloudClientThread.setDaemon(true);
        this.cloudClientThread.setPriority(Thread.MIN_PRIORITY);
        this.cloudClientThread.start();
    }

    private static boolean isValidUri(final @Nullable String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }
        try {
            final URI ignored = URI.create(uri);
            return true;
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    private void runCloudClientLoop() {
        logger.debug("Starting Remote HTTP Configuration Service..");
        while (running) {
            try {
                fetchRemoteServices();
                final HiveMQEdgeRemoteServices services = remoteServices;
                if (services != null && isValidUri(services.getConfigEndpoint())) {
                    checkConnectivityStatus(services);
                    fetchRemoteConfigurationIfNeeded(services);
                    startUsageThreadIfNeeded();
                }
                waitForNextRetry(retryTimeMillis);
            } catch (final Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                handleCloudClientError();
            }
        }
    }

    private void fetchRemoteServices() {
        try {
            remoteServices = httpGet(serviceDiscoveryEndpoint, HiveMQEdgeRemoteServices.class);
            if (logger.isTraceEnabled()) {
                logger.trace("Successfully established connection to remote service provider {}, online -> {}",
                        serviceDiscoveryEndpoint,
                        remoteServices);
            }
        } catch (final HiveMQEdgeRemoteConnectivityException e) {
            hasConnectivity = false;
            logger.debug("Connection to http provider {} could not be established, using offline information",
                    serviceDiscoveryEndpoint);
        }
    }

    private void checkConnectivityStatus(final @NotNull HiveMQEdgeRemoteServices services) {
        try {
            final HttpResponse response =
                    HttpUrlConnectionClient.head(Map.of(), services.getConfigEndpoint(), readTimeoutMillis);
            hasConnectivity = !response.isError();
            if (logger.isTraceEnabled()) {
                logger.trace("Successfully established connection to http provider {}, online",
                        serviceDiscoveryEndpoint);
            }
            if (hasConnectivity && activateUsage) {
                fireEvent(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_PING), false);
            }
        } catch (final IOException e) {
            hasConnectivity = false;
        }
    }

    private void fetchRemoteConfigurationIfNeeded(final @NotNull HiveMQEdgeRemoteServices services)
            throws HiveMQEdgeRemoteConnectivityException {
        if (remoteConfiguration != null || !running) {
            return;
        }
        configurationLock.lock();
        try {
            if (remoteConfiguration == null && running) {
                remoteConfiguration = httpGet(services.getConfigEndpoint(), HiveMQEdgeRemoteConfiguration.class);
            }
        } finally {
            configurationLock.unlock();
        }
    }

    private void startUsageThreadIfNeeded() {
        if (activateUsage && usageClientThread == null) {
            synchronized (monitor) {
                if (usageClientThread == null) {
                    usageClientThread = new Thread(this::runUsageClientLoop, "remote-usage-monitor");
                    usageClientThread.setDaemon(true);
                    usageClientThread.setPriority(Thread.MIN_PRIORITY);
                    usageClientThread.start();
                }
            }
        }
    }

    private void runUsageClientLoop() {
        logger.debug("Starting Remote HTTP Usage Service..");
        final AtomicInteger usageErrorCount = new AtomicInteger();
        long lastAttempt = 0;

        while (running) {
            try {
                final HiveMQEdgeRemoteEvent event = usageEventQueue.take();
                final boolean shouldAttempt = shouldAttemptUsageRequest(usageErrorCount, lastAttempt);

                if (shouldAttempt && isOnline()) {
                    lastAttempt = System.currentTimeMillis();
                    sendUsageEvent(event, usageErrorCount);
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final HiveMQEdgeRemoteConnectivityException e) {
                usageErrorCount.incrementAndGet();
                logger.trace("Communication error {} reporting usage event", usageErrorCount.get(), e);
            } catch (final Exception e) {
                usageErrorCount.incrementAndGet();
            }
        }
    }

    private boolean shouldAttemptUsageRequest(final @NotNull AtomicInteger usageErrorCount, final long lastAttempt) {
        if (usageErrorCount.get() <= MAX_ERROR_ATTEMPTS_USAGE_STATS) {
            return true;
        }
        final boolean resetIntervalExceeded =
                lastAttempt < (System.currentTimeMillis() - USAGE_STATS_ERROR_RESET_INTERVAL_MILLIS);
        if (resetIntervalExceeded && logger.isTraceEnabled()) {
            logger.trace("Error state reset interval exceeded, retry");
        }
        return resetIntervalExceeded;
    }

    private void sendUsageEvent(
            final @NotNull HiveMQEdgeRemoteEvent event,
            final @NotNull AtomicInteger usageErrorCount)
            throws HiveMQEdgeRemoteConnectivityException {
        final HiveMQEdgeRemoteServices services = remoteServices;
        if (services != null && isValidUri(services.getUsageEndpoint())) {
            httpPost(services.getUsageEndpoint(), event);
            usageErrorCount.set(0);
        }
    }

    private void handleCloudClientError() {
        if (!running) {
            return;
        }
        final int backoffMultiplier = Math.min(errorCount.incrementAndGet(), MAX_ERROR_BACKOFF_MULTIPLIER);
        waitForNextRetry((long) retryTimeMillis * backoffMultiplier);
    }

    private void waitForNextRetry(final long waitTimeMillis) {
        synchronized (monitor) {
            try {
                monitor.wait(waitTimeMillis);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        running = false;
        remoteConfiguration = null;
        remoteServices = null;
        synchronized (monitor) {
            monitor.notifyAll();
        }
        stopThread(usageClientThread, "usage client");
        stopThread(cloudClientThread, "cloud client");
    }

    private void stopThread(final @Nullable Thread thread, final @NotNull String threadName) {
        if (thread == null) {
            return;
        }
        thread.interrupt();
        try {
            thread.join(THREAD_JOIN_TIMEOUT_MILLIS);
            if (thread.isAlive()) {
                logger.warn("Thread {} did not terminate within timeout", threadName);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("Interrupted while waiting for {} thread to stop", threadName);
        }
    }

    public @NotNull Optional<HiveMQEdgeRemoteConfiguration> getRemoteConfiguration() {
        return Optional.ofNullable(remoteConfiguration);
    }

    public void fireEvent(final @NotNull HiveMQEdgeRemoteEvent event, final boolean queueIfOffline) {
        if (!activateUsage) {
            return;
        }
        final boolean shouldProcess = isOnline() || queueIfOffline;
        if (logger.isTraceEnabled()) {
            logger.trace("Firing event {}, online ? {}", event.getEventType(), shouldProcess);
        }
        if (shouldProcess) {
            event.setEdgeVersion(hiveMqEdgeVersion);
            enqueueEvent(event);
        }
    }

    private void enqueueEvent(final @NotNull HiveMQEdgeRemoteEvent event) {
        try {
            if (!usageEventQueue.offer(event, EVENT_OFFER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Tracking-usage queue blocked, and discarded result");
                }
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("Could not enqueue usage data -> {}", e.getMessage());
        }
    }

    public boolean isOnline() {
        return remoteServices != null && hasConnectivity && running;
    }

    private <T> @NotNull T httpGet(final @NotNull String url, final @NotNull Class<? extends T> cls)
            throws HiveMQEdgeRemoteConnectivityException {
        try {
            final HttpResponse response =
                    HttpUrlConnectionClient.get(Map.of(), url, connectTimeoutMillis, readTimeoutMillis);
            if (logger.isTraceEnabled()) {
                logger.trace("Obtaining cloud service object from {} -> {}", url, response);
            }
            checkResponse(url, response);
            return mapper.readValue(response.getResponseBody(), cls);
        } catch (final IOException e) {
            throw new HiveMQEdgeRemoteConnectivityException("Error in http socket connection", e);
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
                    logger.trace("Post to http service object {} -> {} in {}",
                            url,
                            response.getStatusCode(),
                            System.currentTimeMillis() - start);
                }
            }
        } catch (final IOException e) {
            throw new HiveMQEdgeRemoteConnectivityException("Error sending request", e);
        }
    }

    private void checkResponse(final @NotNull String url, final @Nullable HttpResponse response)
            throws HiveMQEdgeRemoteConnectivityException {
        if (response == null) {
            throw new HiveMQEdgeRemoteConnectivityException("Remote service [" + url + "] failed to respond <null>");
        }
        if (response.isError()) {
            throw new HiveMQEdgeRemoteConnectivityException("Remote service [" +
                    response.getRequestUrl() +
                    "] failed (" +
                    response.getStatusCode() +
                    " - " +
                    response.getStatusMessage() +
                    ")");
        }
        if (response.getContentLength() <= 0) {
            throw new HiveMQEdgeRemoteConnectivityException("Remote service [" +
                    response.getRequestUrl() +
                    "] failed with no expected content (" +
                    response.getStatusCode() +
                    " - " +
                    response.getStatusMessage() +
                    ")");
        }
    }
}
