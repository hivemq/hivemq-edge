package com.hivemq.edge.modules.api.events;

import com.hivemq.edge.modules.api.events.model.Event;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

/**
 * @author Simon L Johnson
 */
public interface EventStore {

    void storeEvent(@NotNull Event event);

    List<Event> readEvents(@NotNull Long since, @NotNull Integer limit);
}
