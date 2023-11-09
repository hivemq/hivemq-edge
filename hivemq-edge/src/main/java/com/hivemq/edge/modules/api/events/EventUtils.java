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
