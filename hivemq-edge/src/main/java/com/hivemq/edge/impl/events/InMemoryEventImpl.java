package com.hivemq.edge.impl.events;

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.edge.modules.api.events.EventStore;
import com.hivemq.edge.modules.api.events.model.Event;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
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
        this.inMemoryEventList
                = new RollingList<>(InternalConfigurations.EDGE_RUNTIME_MAX_EVENTS_IN_INMEMORY_LIST.get());
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
        Stream<Event> stream  = events.stream().sorted(Comparator.comparing(Event::getTimestamp));
        if(since != null){
            stream = stream.filter(event -> since > event.getTimestamp());
        }
        if(limit != null){
            stream = stream.limit(limit);
        }
        return stream.collect(Collectors.toUnmodifiableList());
    }
}
