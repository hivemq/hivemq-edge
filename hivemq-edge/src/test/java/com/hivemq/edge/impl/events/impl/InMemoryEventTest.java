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

import com.hivemq.edge.impl.events.InMemoryEventImpl;
import com.hivemq.edge.modules.api.events.model.Event;
import com.hivemq.util.RollingList;
import com.hivemq.util.Strings;
import net.openhft.hashing.LongHashFunction;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Simon L Johnson
 */
public class InMemoryEventTest {


    @Test
    public void test_in_memory_roll_logic() throws Exception {

        InMemoryEventImpl impl = new InMemoryEventImpl(10);

        List<Event> events = impl.readEvents(null, null);

        //-- Test empty list is not null
        Assert.assertNotNull("Event list should not be null event when empty", events);

        //-- Add events within bounds
        fill(impl, 10);
        events = impl.readEvents(null, null);
        Assert.assertEquals("Should be 10 events", 10, events.size());

        //-- Check first 10 are contiguous (9-0)
        contiguous(events, false);

        //-- Add another
        fill(impl, 1);
        events = impl.readEvents(null, null);
        Assert.assertEquals("Should still be 10 events", 10, events.size());

        //-- Check still contiguous having added(10-1)
        contiguous(events, false);

        //-- Add to 1000
        fill(impl, 989);
        events = impl.readEvents(null, null);
        Assert.assertEquals("Should still be 10 events", 10, events.size());

        //-- Check still contiguous having added to 1000 (998-989)
        contiguous(events, false);
    }

    @Test
    public void test_in_memory_limit_logic() throws Exception {

        InMemoryEventImpl impl = new InMemoryEventImpl(10);

        List<Event> events = impl.readEvents(null, null);

        //-- Test empty list is not null
        Assert.assertNotNull("Event list should not be null event when empty", events);

        //-- Add events within bounds
        fill(impl, 10);
        events = impl.readEvents(null, null);
        Assert.assertEquals("Should be 10 events", 10, events.size());
        contiguous(events, false);

        List<Event> head = events.subList(0, 2);

        //-- Capture the head 2 of the 10, and compare to limited query, to ensure we're limiting from the correct end

        //Check limits
        events = impl.readEvents(null, 2);
        Assert.assertEquals("Limited query should match the head of the full search", head, events);

        //-- Check still contiguous having added to
        contiguous(events, false);
    }

    @Test
    public void test_in_memory_since_query() {

        InMemoryEventImpl impl = new InMemoryEventImpl(10);
        fill(impl, 10);

        List<Event> events = impl.readEvents(null, 5);
        Assert.assertEquals("Should be limited to 5 events", 5, events.size());

        //-- Check still since timestamp
        events = impl.readEvents(events.get(events.size() - 1).getTimestamp(), null);
        System.err.println(events);
        Assert.assertEquals("Should be same 4 events (excl.)", 4, events.size());
        long latestTimestamp = events.get(0).getTimestamp();

        //-- Add a new one since the last epoch and query by timestamp (should only return 1)
        fill(impl, 1);
        events = impl.readEvents(latestTimestamp, null);
        Assert.assertEquals("Epoch query should only return 1 match", 1, events.size());
    }

    private static void contiguous(List<Event> list, boolean asc){
        if(list == null || list.isEmpty()) {
            Assert.fail("Empty list not considered contiguous");
        }
        int startsAt = Integer.valueOf(list.get(0).getMessage());
        for (int i = 0; i < list.size(); i++){
            Event event = list.get(i);
            Assert.assertEquals("The event messages weren't entirely contiguous",
                    (asc ? (startsAt + i) : (startsAt - i)), (int) Integer.valueOf(event.getMessage()));
        }
    }

    private static void fill(InMemoryEventImpl impl, int count){
        int initialSize = impl.readEvents(null, null).size();
        for (int i = 0; i < count; i++){
            Event.Builder builder = new Event.Builder().withMessage((initialSize + i) + "").
                    withSeverity(Event.SEVERITY.INFO).withTimestamp(System.currentTimeMillis());
            impl.storeEvent(builder.build());
            try {
                Thread.sleep(1);
            } catch(Exception e){}
        }
    }
}
