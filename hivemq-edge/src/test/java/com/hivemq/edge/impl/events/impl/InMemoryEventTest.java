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
package com.hivemq.edge.impl.events.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.edge.impl.events.InMemoryEventImpl;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.edge.modules.api.events.model.EventImpl;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class InMemoryEventTest {

    private static long timestampCounter = 0;

    private static void contiguous(final @Nullable List<Event> list, final boolean asc) {
        if (list == null || list.isEmpty()) {
            fail("Empty list not considered contiguous");
        }
        final int startsAt = Integer.valueOf(list.getFirst().getMessage());
        for (int i = 0; i < list.size(); i++) {
            final Event event = list.get(i);
            assertEquals(
                    (asc ? (startsAt + i) : (startsAt - i)),
                    (int) Integer.valueOf(event.getMessage()),
                    "The event messages weren't entirely contiguous");
        }
    }

    private static void fill(final @NotNull InMemoryEventImpl impl, final int count) {
        final int initialSize = impl.readEvents(null, null).size();
        for (int i = 0; i < count; i++) {
            new EventBuilderImpl(impl::storeEvent)
                    .withMessage(String.valueOf(initialSize + i))
                    .withSeverity(EventImpl.SEVERITY.INFO)
                    .withTimestamp(timestampCounter++)
                    .fire();
        }
    }

    @Test
    public void test_in_memory_roll_logic() throws Exception {
        final InMemoryEventImpl impl = new InMemoryEventImpl(10);

        List<Event> events = impl.readEvents(null, null);

        // -- Test empty list is not null
        assertNotNull(events, "Event list should not be null event when empty");

        // -- Add events within bounds
        fill(impl, 10);
        events = impl.readEvents(null, null);
        assertEquals(10, events.size(), "Should be 10 events");

        // -- Check first 10 are contiguous (9-0)
        contiguous(events, false);

        // -- Add another
        fill(impl, 1);
        events = impl.readEvents(null, null);
        assertEquals(10, events.size(), "Should still be 10 events");

        // -- Check still contiguous having added(10-1)
        contiguous(events, false);

        // -- Add to 1000
        fill(impl, 989);
        events = impl.readEvents(null, null);
        assertEquals(10, events.size(), "Should still be 10 events");

        // -- Check still contiguous having added to 1000 (998-989)
        contiguous(events, false);
    }

    @Test
    public void test_in_memory_limit_logic() throws Exception {
        final InMemoryEventImpl impl = new InMemoryEventImpl(10);

        List<Event> events = impl.readEvents(null, null);

        // -- Test empty list is not null
        assertNotNull(events, "Event list should not be null event when empty");

        // -- Add events within bounds
        fill(impl, 10);
        events = impl.readEvents(null, null);
        assertEquals(10, events.size(), "Should be 10 events");
        contiguous(events, false);

        final List<Event> head = events.subList(0, 2);

        // -- Capture the head 2 of the 10, and compare to limited query, to ensure we're limiting from the correct end

        // Check limits
        events = impl.readEvents(null, 2);
        assertEquals(head, events, "Limited query should match the head of the full search");

        // -- Check still contiguous having added to
        contiguous(events, false);
    }

    @Test
    public void test_in_memory_since_query() {
        final InMemoryEventImpl impl = new InMemoryEventImpl(10);
        fill(impl, 10);

        List<Event> events = impl.readEvents(null, 5);
        assertEquals(5, events.size(), "Should be limited to 5 events");

        // -- Check still since timestamp
        events = impl.readEvents(events.get(events.size() - 1).getTimestamp(), null);
        System.err.println(events);
        assertEquals(4, events.size(), "Should be same 4 events (excl.)");
        long latestTimestamp = events.get(0).getTimestamp();

        // -- Add a new one since the last epoch and query by timestamp (should only return 1)
        fill(impl, 1);
        events = impl.readEvents(latestTimestamp, null);
        assertEquals(1, events.size(), "Epoch query should only return 1 match");
    }

    @Test
    public void test_concurrent_writes() throws Exception {
        final int maxEvents = 100;
        final InMemoryEventImpl impl = new InMemoryEventImpl(maxEvents);
        final int numThreads = 10;
        final int eventsPerThread = 50;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(numThreads);
        final AtomicInteger eventCounter = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < eventsPerThread; i++) {
                        final int eventNum = eventCounter.getAndIncrement();
                        new EventBuilderImpl(impl::storeEvent)
                                .withMessage(String.valueOf(eventNum))
                                .withSeverity(EventImpl.SEVERITY.INFO)
                                .withTimestamp(System.currentTimeMillis())
                                .fire();
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        final List<Event> events = impl.readEvents(null, null);
        assertEquals(maxEvents, events.size(), "Should have exactly maxEvents after concurrent writes");
    }

    @Test
    public void test_concurrent_reads() throws Exception {
        final int maxEvents = 100;
        final InMemoryEventImpl impl = new InMemoryEventImpl(maxEvents);
        fill(impl, maxEvents);

        final int numThreads = 10;
        final int readsPerThread = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(numThreads);
        final AtomicInteger successfulReads = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < readsPerThread; i++) {
                        final List<Event> events = impl.readEvents(null, null);
                        if (events.size() == maxEvents) {
                            successfulReads.incrementAndGet();
                        }
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        assertEquals(numThreads * readsPerThread, successfulReads.get(), "All reads should return consistent data");
    }

    @Test
    public void test_concurrent_reads_and_writes() throws Exception {
        final int maxEvents = 100;
        final InMemoryEventImpl impl = new InMemoryEventImpl(maxEvents);
        fill(impl, maxEvents / 2);

        final int numReaderThreads = 5;
        final int numWriterThreads = 5;
        final int operationsPerThread = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(numReaderThreads + numWriterThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(numReaderThreads + numWriterThreads);
        final AtomicInteger eventCounter = new AtomicInteger(maxEvents / 2);
        final AtomicInteger readErrors = new AtomicInteger(0);

        // Writer threads
        for (int t = 0; t < numWriterThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < operationsPerThread; i++) {
                        final int eventNum = eventCounter.getAndIncrement();
                        new EventBuilderImpl(impl::storeEvent)
                                .withMessage(String.valueOf(eventNum))
                                .withSeverity(EventImpl.SEVERITY.INFO)
                                .withTimestamp(System.currentTimeMillis())
                                .fire();
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Reader threads
        for (int t = 0; t < numReaderThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < operationsPerThread; i++) {
                        try {
                            final List<Event> events = impl.readEvents(null, null);
                            // Verify we got a valid list (not corrupted)
                            if (events == null || events.size() > maxEvents) {
                                readErrors.incrementAndGet();
                            }
                            // Verify all events in the list are valid
                            for (final Event event : events) {
                                if (event == null || event.getMessage() == null) {
                                    readErrors.incrementAndGet();
                                    break;
                                }
                            }
                        } catch (final Exception e) {
                            readErrors.incrementAndGet();
                        }
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        assertEquals(0, readErrors.get(), "No read errors should occur during concurrent access");

        final List<Event> finalEvents = impl.readEvents(null, null);
        assertTrue(finalEvents.size() <= maxEvents, "Final size should not exceed max");
        assertTrue(finalEvents.size() > 0, "Should have some events");
    }

    @Test
    public void test_concurrent_writes_data_integrity() throws Exception {
        final int maxEvents = 1000;
        final InMemoryEventImpl impl = new InMemoryEventImpl(maxEvents);
        final int numThreads = 10;
        final int eventsPerThread = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(numThreads);
        final AtomicInteger eventCounter = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < eventsPerThread; i++) {
                        final int eventNum = eventCounter.getAndIncrement();
                        // Include thread ID in message to verify no data corruption
                        new EventBuilderImpl(impl::storeEvent)
                                .withMessage(threadId + "-" + eventNum)
                                .withSeverity(EventImpl.SEVERITY.INFO)
                                .withTimestamp(System.currentTimeMillis())
                                .fire();
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        final List<Event> events = impl.readEvents(null, null);
        assertEquals(maxEvents, events.size(), "Should have exactly maxEvents");

        // Verify data integrity - all messages should be parseable as "threadId-eventNum"
        for (final Event event : events) {
            final String message = event.getMessage();
            assertNotNull(message, "Message should not be null");
            final String[] parts = message.split("-");
            assertEquals(2, parts.length, "Message format should be 'threadId-eventNum': " + message);
            try {
                Integer.parseInt(parts[0]);
                Integer.parseInt(parts[1]);
            } catch (final NumberFormatException e) {
                fail("Message parts should be parseable integers: " + message);
            }
        }
    }
}
