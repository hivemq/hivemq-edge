package com.hivemq.edge.modules.api.events;

import com.hivemq.edge.modules.api.events.model.Event;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;

/**
 * @author Simon L Johnson
 */
public interface EventService {

    void fireEvent(final @NotNull Event event);

    List<Event> readEvents(final @Nullable Long sinceTimestamp, final @Nullable Integer limit);


}
