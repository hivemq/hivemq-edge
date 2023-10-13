package com.hivemq.api.resources.impl;

import com.google.common.collect.ImmutableList;
import com.hivemq.api.AbstractApi;
import com.hivemq.edge.model.TypeIdentifier;
import com.hivemq.api.model.events.Event;
import com.hivemq.api.model.events.EventList;
import com.hivemq.api.resources.EventApi;
import com.hivemq.api.utils.LoremIpsum;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Simon L Johnson
 */
public class EventResourceImpl extends AbstractApi implements EventApi {


    static final int DEFAULT_MAX = 200;

    @Inject
    public EventResourceImpl() {
    }

    @Override
    public Response listEvents(Integer limit, final Long timestamp) {

        //-- Todo back with SPI
        if(timestamp != null){
            limit = Math.min(limit, 4);
        }
        long time = System.currentTimeMillis();
        ImmutableList.Builder<Event> builder = new ImmutableList.Builder<>();
        for (int i = 0; i < DEFAULT_MAX && i < limit; i++){
            Object payload = null;
            String contentType = null;
            if(ThreadLocalRandom.current().nextBoolean()){
                if(i % 2 == 0){
                    contentType = "application/json";
                    payload = "{\n" + "    \"attribute1\": \"value\",\n" + "    \"attribute2\": 10\n" + "}";
                } else {
                    contentType = "text/plain";
                    payload = "This is some plain text in here";
                }
            }
            builder.add(new Event(
                    TypeIdentifier.generate(TypeIdentifier.TYPE.EVENT),
                    Event.SEVERITY.INFO,
                    LoremIpsum.generate(15),
                    contentType,
                    payload,
                    time - (i * 1000),
                    TypeIdentifier.generate(TypeIdentifier.TYPE.ADAPTER),
                    null));
        }
        return Response.ok(new EventList(builder.build())).build();
    }
}
