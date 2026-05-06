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
package com.hivemq.edge.integration.api.message;

import java.util.List;
import org.jetbrains.annotations.NotNull;

// TODO DISCUSS: This is a every general concept that should be in a more general api, not in the special pulse api

/**
 * A read-only view of an incoming MQTT publish handed to the Pulse Agent integration.
 */
public interface IncomingMessage {

    @NotNull
    String topic();

    byte @NotNull [] payload();

    @NotNull
    String uniqueId();

    long timestamp();

    @NotNull
    List<UserProperty> userProperties();

    interface UserProperty {

        @NotNull
        String name();

        @NotNull
        String value();
    }
}
