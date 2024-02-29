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

import com.hivemq.api.model.core.Payload;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.nio.charset.StandardCharsets;

/**
 * @author Simon L Johnson
 */
public class EventUtils {

    public static Payload generateErrorPayload(final @Nullable Throwable throwable){
        Payload payload = null;
        if(throwable != null){
            payload = Payload.from(Payload.ContentType.PLAIN_TEXT,
                    ExceptionUtils.getStackTrace(throwable));
        }
        return payload;
    }

    public static Payload generateJsonPayload(final @Nullable byte[] arr){
        Payload payload = null;
        if(arr != null){
            payload = Payload.from(Payload.ContentType.JSON,
                    new String(arr, StandardCharsets.UTF_8));
        }
        return payload;
    }
}
