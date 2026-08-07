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
package com.hivemq.protocols.v2.southbound;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Turns one MQTT {@link PUBLISH} read from the durable client queue into the {@link DataPoint} a southbound write
 * carries — the payload seam between the broker side and the write path. The MQTT-intake task supplies the real
 * implementation (payload parsing per the southbound mapping); until then tests supply simple ones.
 * <p>
 * Returning {@code null} (or throwing) marks the publish <b>untranslatable</b>: the backlog dead-letters it and
 * moves on — an unparseable command must never wedge the queue. Invoked outside the backlog's monitor, on the
 * thread that completed the read.
 */
@FunctionalInterface
public interface SouthboundPublishTranslator {

    /**
     * @param publish the MQTT publish read from the tag's queue.
     * @return the value to write, or {@code null} when the payload cannot be translated (dead-letters the publish).
     */
    @Nullable
    DataPoint translate(@NotNull PUBLISH publish);
}
