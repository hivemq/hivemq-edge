package com.hivemq.edge.modules.api.events;

import com.hivemq.edge.modules.api.events.model.Event;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
public interface EventListener {

    void eventFired(@NotNull final Event event);
}
