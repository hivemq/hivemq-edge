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
package com.hivemq.bootstrap.factories;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.publish.PUBLISH;

public class EventServiceResult {

    private final boolean preventPublish;
    private final @Nullable PUBLISH createdPublish;

    private EventServiceResult(final boolean preventPublish, @Nullable final PUBLISH createdPublish) {
        this.preventPublish = preventPublish;
        this.createdPublish = createdPublish;
    }


    public static @NotNull EventServiceResult preventPublishing() {
        return new EventServiceResult(true, null);
    }

    public static @NotNull EventServiceResult allowPublishing(final @NotNull PUBLISH createdPublish) {
        return new EventServiceResult(false, createdPublish);
    }


    public boolean isPreventPublish() {
        return preventPublish;
    }

    public @Nullable PUBLISH getModifiedPublish() {
        return createdPublish;
    }
}
