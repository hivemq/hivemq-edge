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
package com.hivemq.edge.impl.events;

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.edge.modules.api.events.EventStore;
import com.hivemq.edge.modules.api.events.model.Event;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.util.IntMap;
import com.hivemq.util.RollingList;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple rolling list implementation optimized for fast writes but slow reads as requires sort on read. (Handled
 * out of lock).
 * @author Simon L Johnson
 */
@Singleton
public class InMemoryEventImpl implements EventStore {
    private @NotNull RollingList<Event> inMemoryEventList;
    private volatile ReadWriteLock lock = new ReentrantReadWriteLock();

    @Inject
    public InMemoryEventImpl() {
        //optimize for quick write slow read (sort)
        this(InternalConfigurations.EDGE_RUNTIME_MAX_EVENTS_IN_INMEMORY_LIST.get());
    }

    public InMemoryEventImpl(final int max) {
        this.inMemoryEventList = new RollingList<>(max);
    }

    public void storeEvent(final Event event) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            inMemoryEventList.add(event);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<Event> readEvents(@Nullable Long since, @Nullable Integer limit){
        Lock readLock = lock.writeLock();
        List<Event> events;
        try {
            readLock.lock();
            events = new ArrayList<>(inMemoryEventList);
        } finally {
            readLock.unlock();
        }
        Stream<Event> stream  = events.stream().sorted(Comparator.comparing(Event::getTimestamp).reversed());
        if(since != null){
            stream = stream.filter(event -> since < event.getTimestamp());
        }
        if(limit != null){
            stream = stream.limit(limit);
        }
        return stream.collect(Collectors.toUnmodifiableList());
    }
}
