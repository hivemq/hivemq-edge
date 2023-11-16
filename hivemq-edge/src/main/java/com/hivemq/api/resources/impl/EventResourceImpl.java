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

import com.hivemq.api.AbstractApi;
import com.hivemq.api.model.events.EventList;
import com.hivemq.api.resources.EventApi;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

/**
 * @author Simon L Johnson
 */
public class EventResourceImpl extends AbstractApi implements EventApi {

    private final @NotNull EventService eventService;

    @Inject
    public EventResourceImpl(final @NotNull EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public Response listEvents(final Integer limit, final Long timestamp) {
        return Response.ok(new EventList(eventService.readEvents(timestamp, limit))).build();
    }
}
