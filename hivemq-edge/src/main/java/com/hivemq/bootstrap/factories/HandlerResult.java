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

import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.publish.PUBLISH;

public class HandlerResult {

    private final boolean preventPublish;
    private final @Nullable PUBLISH modifiedPublish;

    public HandlerResult(boolean preventPublish, @Nullable PUBLISH modifiedPublish) {
        this.preventPublish = preventPublish;
        this.modifiedPublish = modifiedPublish;
    }

    public boolean isPreventPublish() {
        return preventPublish;
    }

    public @Nullable PUBLISH getModifiedPublish() {
        return modifiedPublish;
    }
}
