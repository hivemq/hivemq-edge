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
package com.hivemq.persistence.local.memory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.hivemq.annotations.ExecuteInSingleWriter;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.HiveMQMetrics;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.persistence.NoSessionException;
import com.hivemq.persistence.PersistenceEntry;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionWill;
import com.hivemq.persistence.clientsession.PendingWillMessages;
import com.hivemq.persistence.exception.InvalidSessionExpiryIntervalException;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.MemoryEstimator;
import com.hivemq.util.ThreadPreConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.PERSISTENCE_BUCKET_COUNT;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;
import static com.hivemq.mqtt.message.disconnect.DISCONNECT.SESSION_EXPIRY_NOT_SET;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

/**
 * @author Georg Held
 */
@Singleton
public class ClientSessionMemoryLocalPersistence implements ClientSessionLocalPersistence {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ClientSessionMemoryLocalPersistence.class);

    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull EventLog eventLog;
    private final @NotNull Map<String, PersistenceEntry<ClientSession>> @NotNull [] buckets;
    private final @NotNull AtomicInteger sessionsCount = new AtomicInteger(0);
    private final @NotNull AtomicLong currentMemorySize = new AtomicLong();
    private final int bucketCount;

    @Inject
    ClientSessionMemoryLocalPersistence(
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull MetricsHolder metricsHolder,
            final @NotNull EventLog eventLog,
            final @NotNull InternalConfigurationService internalConfigurationService) {

        this.payloadPersistence = payloadPersistence;
        this.metricsHolder = metricsHolder;
        this.eventLog = eventLog;

        bucketCount = internalConfigurationService.getInteger(PERSISTENCE_BUCKET_COUNT);


        //noinspection unchecked
        buckets = new Map[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            buckets[i] = new HashMap<>();
        }

        metricRegistry.register(HiveMQMetrics.CLIENT_SESSIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name(),
                (Gauge<Long>) currentMemorySize::get);
    }

    private @NotNull Map<String, PersistenceEntry<ClientSession>> getBucket(final int bucketIndex) {
        checkArgument(bucketIndex <= bucketCount, "Bucket must be less or equal than bucketCount");
        return buckets[bucketIndex];
    }

    @Override
    public @Nullable ClientSession getSession(
            final @NotNull String clientId, final int bucketIndex, final boolean checkExpired) {
        return getSession(clientId, bucketIndex, checkExpired, true);
    }

    @Override
    public @Nullable ClientSession getSession(final @NotNull String clientId, final int bucketIndex) {
        return getSession(clientId, bucketIndex, true, true);
    }

    @Override
    public @Nullable ClientSession getSession(final @NotNull String clientId, final boolean checkExpired) {
        final int bucketIndex = BucketUtils.getBucket(clientId, bucketCount);
        return getSession(clientId, bucketIndex, checkExpired, true);
    }

    @Override
    public @Nullable ClientSession getSession(
            final @NotNull String clientId, final boolean checkExpired, final boolean includeWill) {
        final int bucketIndex = BucketUtils.getBucket(clientId, bucketCount);
        return getSession(clientId, bucketIndex, checkExpired, includeWill);
    }

    @Override
    public @Nullable ClientSession getSession(final @NotNull String clientId) {
        final int bucketIndex = BucketUtils.getBucket(clientId, bucketCount);
        return getSession(clientId, bucketIndex, true, true);
    }

    private @Nullable ClientSession getSession(
            final @NotNull String clientId,
            final int bucketIndex,
            final boolean checkExpired,
            final boolean includeWill) {

        final Map<String, PersistenceEntry<ClientSession>> bucket = getBucket(bucketIndex);

        final PersistenceEntry<ClientSession> storedSession = bucket.get(clientId);
        if (storedSession == null) {
            return null;
        }

        final ClientSession clientSession = storedSession.getObject().deepCopy();

        if (checkExpired && clientSession.isExpired(System.currentTimeMillis() - storedSession.getTimestamp())) {
            return null;
        }
        if (includeWill) {
            loadWillPayload(clientSession);
        }
        return clientSession;
    }

    @Override
    public @Nullable Long getTimestamp(final @NotNull String clientId) {
        final int bucketIndex = BucketUtils.getBucket(clientId, bucketCount);
        return getTimestamp(clientId, bucketIndex);
    }

    @Override
    public @Nullable Long getTimestamp(final @NotNull String clientId, final int bucketIndex) {
        final Map<String, PersistenceEntry<ClientSession>> bucket = getBucket(bucketIndex);
        final PersistenceEntry<ClientSession> storedSession = bucket.get(clientId);
        if (storedSession == null) {
            return null;
        }
        return storedSession.getTimestamp();
    }

    @Override
    @ExecuteInSingleWriter
    public void put(
            final @NotNull String clientId,
            final @NotNull ClientSession newClientSession,
            final long timestamp,
            final int bucketIndex) {
        checkNotNull(clientId, "Client id must not be null");
        checkNotNull(newClientSession, "Client session must not be null");
        checkArgument(timestamp > 0, "Timestamp must be greater than 0");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);


        final Map<String, PersistenceEntry<ClientSession>> sessions = getBucket(bucketIndex);
        final ClientSession usedSession = newClientSession.deepCopy();

        sessions.compute(clientId, (ignored, storedSession) -> {

            final boolean addClientIdSize;
            if (storedSession == null) {
                sessionsCount.incrementAndGet();
                addClientIdSize = true;
            } else {
                final ClientSession oldSession = storedSession.getObject();

                currentMemorySize.addAndGet(-storedSession.getEstimatedSize());

                removeWillReference(oldSession);

                final boolean oldSessionIsPersistent = isPersistent(oldSession);
                if (!oldSessionIsPersistent && !oldSession.isConnected()) {
                    sessionsCount.incrementAndGet();
                }
                addClientIdSize = false;
            }

            final ClientSessionWill newWill = newClientSession.getWillPublish();
            if (newWill != null) {
                metricsHolder.getStoredWillMessagesCount().inc();
                payloadPersistence.add(newWill.getPayload(), newWill.getPublishId());
            }

            final PersistenceEntry<ClientSession> newEntry = new PersistenceEntry<>(usedSession, timestamp);
            currentMemorySize.addAndGet(newEntry.getEstimatedSize());
            if (addClientIdSize) {
                currentMemorySize.addAndGet(MemoryEstimator.stringSize(clientId));
            }

            return newEntry;
        });
    }

    @Override
    @ExecuteInSingleWriter
    public @NotNull ClientSession disconnect(
            final @NotNull String clientId,
            final long timestamp,
            final boolean sendWill,
            final int bucketIndex,
            final long sessionExpiryInterval) {

        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, PersistenceEntry<ClientSession>> bucket = getBucket(bucketIndex);

        final ClientSession storedSession = bucket.compute(clientId, (ignored, oldEntry) -> {
            if (oldEntry == null) {
                // we create a tombstone here which will be removed at next cleanup
                final ClientSession clientSession = new ClientSession(false, SESSION_EXPIRE_ON_DISCONNECT);
                final PersistenceEntry<ClientSession> persistenceEntry = new PersistenceEntry<>(clientSession, timestamp);
                currentMemorySize.addAndGet(persistenceEntry.getEstimatedSize() + MemoryEstimator.stringSize(clientId));
                return persistenceEntry;
            }

            currentMemorySize.addAndGet(-oldEntry.getEstimatedSize());
            final ClientSession oldSession = oldEntry.getObject();
            final ClientSession newSession;
            if (sendWill) {
                newSession = oldSession;
            } else {
                removeWillReference(oldSession);
                newSession = oldSession.copyWithoutWill();
            }

            if (sessionExpiryInterval != SESSION_EXPIRY_NOT_SET) {
                newSession.setSessionExpiryIntervalSec(sessionExpiryInterval);
            }

            if (newSession.isConnected() && !isPersistent(newSession)) {
                sessionsCount.decrementAndGet();
            }

            newSession.setConnected(false);
            loadWillPayload(newSession, false);

            final PersistenceEntry<ClientSession> newEntry = new PersistenceEntry<>(newSession, timestamp);
            currentMemorySize.addAndGet(newEntry.getEstimatedSize());
            return newEntry;
        }).getObject().deepCopy();


        loadWillPayload(storedSession);
        return storedSession;
    }

    @Override
    public @NotNull Set<String> getAllClients(final int bucketIndex) {

        final Map<String, PersistenceEntry<ClientSession>> bucket = getBucket(bucketIndex);

        return ImmutableSet.copyOf(bucket.keySet());
    }

    @Override
    @ExecuteInSingleWriter
    public void removeWithTimestamp(final @NotNull String clientId, final int bucketIndex) {

        final Map<String, PersistenceEntry<ClientSession>> bucket = getBucket(bucketIndex);
        final PersistenceEntry<ClientSession> remove = bucket.remove(clientId);
        if (remove != null) {
            final ClientSession clientSession = remove.getObject();
            if (isPersistent(clientSession) || clientSession.isConnected()) {
                sessionsCount.decrementAndGet();
            }
            removeWillReference(clientSession);
            currentMemorySize.addAndGet(-(remove.getEstimatedSize() + MemoryEstimator.stringSize(clientId)));
        }
    }

    @Override
    @ExecuteInSingleWriter
    public @NotNull Set<String> cleanUp(final int bucketIndex) {
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, PersistenceEntry<ClientSession>> bucket = getBucket(bucketIndex);

        final long currentTimeMillis = System.currentTimeMillis();
        final Iterator<Map.Entry<String, PersistenceEntry<ClientSession>>> iterator = bucket.entrySet().iterator();

        final ImmutableSet.Builder<String> expiredClientIds = ImmutableSet.builder();

        while (iterator.hasNext()) {
            final Map.Entry<String, PersistenceEntry<ClientSession>> entry = iterator.next();
            final PersistenceEntry<ClientSession> storedEntry = entry.getValue();

            final long timestamp = storedEntry.getTimestamp();
            final ClientSession clientSession = storedEntry.getObject();

            final long sessionExpiryInterval = clientSession.getSessionExpiryIntervalSec();

            if (clientSession.isExpired(currentTimeMillis - timestamp)) {

                if (sessionExpiryInterval > SESSION_EXPIRE_ON_DISCONNECT) {
                    sessionsCount.decrementAndGet();
                }
                eventLog.clientSessionExpired(timestamp + sessionExpiryInterval * 1000, entry.getKey());
                expiredClientIds.add(entry.getKey());
                currentMemorySize.addAndGet(-(storedEntry.getEstimatedSize() + MemoryEstimator.stringSize(entry.getKey())));
                iterator.remove();
            }
        }
        return expiredClientIds.build();
    }

    @Override
    public @NotNull Set<String> getDisconnectedClients(final int bucketIndex) {

        final Map<String, PersistenceEntry<ClientSession>> bucket = getBucket(bucketIndex);
        final long currentTimeMillis = System.currentTimeMillis();

        return bucket.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().getObject().isConnected())
                .filter(entry -> entry.getValue().getObject().getSessionExpiryIntervalSec() > 0)
                .filter(entry -> !entry.getValue().getObject().isExpired(currentTimeMillis - entry.getValue().getTimestamp()))
                .map(Map.Entry::getKey)
                .collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public int getSessionsCount() {
        return sessionsCount.get();
    }

    @Override
    @ExecuteInSingleWriter
    public void setSessionExpiryInterval(
            final @NotNull String clientId, final long sessionExpiryInterval, final int bucketIndex) {
        checkNotNull(clientId, "Client Id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);


        if (sessionExpiryInterval < 0) {
            throw new InvalidSessionExpiryIntervalException("Invalid session expiry interval " + sessionExpiryInterval);
        }

        final Map<String, PersistenceEntry<ClientSession>> bucket = getBucket(bucketIndex);

        bucket.compute(clientId, (ignored, storedSession) -> {


            if (storedSession == null) {
                throw NoSessionException.INSTANCE;
            }

            final ClientSession clientSession = storedSession.getObject();

            // is tombstone?
            if (!clientSession.isConnected() && !isPersistent(clientSession)) {
                throw NoSessionException.INSTANCE;
            }

            clientSession.setSessionExpiryIntervalSec(sessionExpiryInterval);

            return new PersistenceEntry<>(clientSession, storedSession.getTimestamp());
        });
    }

    @Override
    public @NotNull Map<String, PendingWillMessages.PendingWill> getPendingWills(
            final int bucketIndex) {


        final Map<String, PersistenceEntry<ClientSession>> bucket = getBucket(bucketIndex);

        return bucket.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().getObject().isConnected())
                .filter(entry -> entry.getValue().getObject().getWillPublish() != null)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> {

                    final PersistenceEntry<ClientSession> storedSession = entry.getValue();
                    final ClientSessionWill willPublish = storedSession.getObject().getWillPublish();

                    return new PendingWillMessages.PendingWill(Math.min(willPublish.getDelayInterval(),
                            storedSession.getObject().getSessionExpiryIntervalSec()),
                            willPublish.getDelayInterval());
                }));
    }

    @Override
    @ExecuteInSingleWriter
    public @Nullable PersistenceEntry<ClientSession> deleteWill(final @NotNull String clientId, final int bucketIndex) {
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, PersistenceEntry<ClientSession>> bucket = getBucket(bucketIndex);

        final PersistenceEntry<ClientSession> persistenceEntry =
                bucket.computeIfPresent(clientId, (ignored, oldEntry) -> {
                    final ClientSession oldSession = oldEntry.getObject();

                    // Just to be save, we do nothing
                    if (oldSession.isConnected()) {
                        return oldEntry;
                    }
                    currentMemorySize.addAndGet(-oldEntry.getEstimatedSize());

                    removeWillReference(oldSession);

                    final PersistenceEntry<ClientSession> newEntry = new PersistenceEntry<>(oldSession.copyWithoutWill(), oldEntry.getTimestamp());
                    currentMemorySize.addAndGet(newEntry.getEstimatedSize());
                    return newEntry;
                });

        if (persistenceEntry == null) {
            return null;
        }

        final ClientSession session = persistenceEntry.getObject();
        if (session.isConnected()) {
            return null;
        }
        return new PersistenceEntry<>(session.deepCopy(), persistenceEntry.getTimestamp());
    }

    // in contrast to the file persistence method we already have everything in memory. The sizing and pagination are ignored.
    @Override
    public @NotNull BucketChunkResult<Map<String, ClientSession>> getAllClientsChunk(
            final int bucketIndex, final @Nullable String ignored, final int alsoIgnored) {

        final long currentTimeMillis = System.currentTimeMillis();
        final Map<String, PersistenceEntry<ClientSession>> bucket = getBucket(bucketIndex);

        final Map<String, ClientSession> sessions =
                bucket.entrySet()
                        .stream()
                        .filter(entry -> {
                            final PersistenceEntry<ClientSession> value = entry.getValue();
                            return !value.getObject().isExpired(currentTimeMillis - value.getTimestamp());
                        })
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                                entry -> entry.getValue().getObject().copyWithoutWill()));

        return new BucketChunkResult<>(sessions, true, null, bucketIndex);
    }

    @Override
    @ExecuteInSingleWriter
    public void closeDB(final int bucketIndex) {
        getBucket(bucketIndex).clear();
        //happens for every bucket, but its faster than calculating all sizes
        //and decrementing the memory and count every time,
        sessionsCount.set(0);
        currentMemorySize.set(0);
    }

    private void removeWillReference(final @NotNull ClientSession clientSession) {
        final ClientSessionWill willPublish = clientSession.getWillPublish();
        if (willPublish == null) {
            return;
        }
        metricsHolder.getStoredWillMessagesCount().dec();
        payloadPersistence.decrementReferenceCounter(willPublish.getPublishId());
    }

    private void loadWillPayload(final @NotNull ClientSession clientSession) {
        loadWillPayload(clientSession, true);
    }

    private void loadWillPayload(final @NotNull ClientSession clientSession, final boolean setPayload) {
        final ClientSessionWill willPublish = clientSession.getWillPublish();
        if (willPublish == null) {
            return;
        }
        if (willPublish.getPayload() != null) {
            return;
        }
        final byte[] payload = payloadPersistence.get(willPublish.getPublishId());
        if (payload == null) {
            clientSession.setWillPublish(null);
            log.warn("Will Payload for payloadid {} not found", willPublish.getPublishId());
            return;
        }
        if (setPayload) {
            willPublish.getMqttWillPublish().setPayload(payload);
        }
    }

    private static boolean isPersistent(final @NotNull ClientSession clientSession) {
        return clientSession.getSessionExpiryIntervalSec() > SESSION_EXPIRE_ON_DISCONNECT;
    }
}
