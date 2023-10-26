package com.hivemq.api.resources.impl;

import com.google.common.collect.ImmutableList;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.model.core.Payload;
import com.hivemq.api.model.events.EventList;
import com.hivemq.api.resources.EventApi;
import com.hivemq.api.utils.LoremIpsum;
import com.hivemq.edge.model.TypeIdentifier;
import com.hivemq.edge.modules.api.events.model.Event;

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
            Payload payload = null;
            if(ThreadLocalRandom.current().nextBoolean()){
                if(i % 2 == 0){
                    payload = Payload.from(Payload.ContentType.JSON, "{\n" + "    \"attribute1\": \"value\",\n" + "    \"attribute2\": 10\n" + "}");
                } else {
                    payload = Payload.from(Payload.ContentType.PLAIN_TEXT, LoremIpsum.generate(89));
                }
            }
            builder.add(new Event(
                    TypeIdentifier.generate(TypeIdentifier.TYPE.EVENT),
                    Event.SEVERITY.values()[ThreadLocalRandom.current().nextInt(0, Event.SEVERITY.values().length) - 1],
                    LoremIpsum.generate(15),
                    payload,
                    time - (i * 1000),
                    TypeIdentifier.generate(TypeIdentifier.TYPE.ADAPTER),
                    null));
        }
        return Response.ok(new EventList(builder.build())).build();
    }
}
