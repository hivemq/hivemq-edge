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
package com.hivemq.bridge;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.publish.PUBLISH;

public class BridgeMessage {

    private final @NotNull PUBLISH publish;
    private final @NotNull String forwarderId;

    public BridgeMessage(final @NotNull PUBLISH publish,
                         final @NotNull String forwarderId) {
        this.publish = publish;
        this.forwarderId = forwarderId;
    }

    public @NotNull PUBLISH getPublish() {
        return publish;
    }

    public @NotNull String getForwarderId() {
        return forwarderId;
    }
}
