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
package com.hivemq.api.resources.impl;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.model.events.EventList;
import com.hivemq.edge.api.EventsApi;
import org.jetbrains.annotations.NotNull;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/**
 * @author Simon L Johnson
 */
public class EventResourceImpl extends AbstractApi implements EventsApi {

    private final @NotNull EventService eventService;

    @Inject
    public EventResourceImpl(final @NotNull EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public @NotNull Response getEvents(final @NotNull Integer limit, final @NotNull Long timestamp) {
        return Response.ok(new EventList(eventService.readEvents(timestamp, limit))).build();
    }
}
