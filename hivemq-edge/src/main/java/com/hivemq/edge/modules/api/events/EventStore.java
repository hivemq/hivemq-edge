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
package com.hivemq.edge.modules.api.events;

import com.hivemq.edge.modules.events.model.Event;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

/**
 * @author Simon L Johnson
 */
public interface EventStore {

    void storeEvent(@NotNull Event event);

    @NotNull List<Event> readEvents(@NotNull Long since, @NotNull Integer limit);
}
