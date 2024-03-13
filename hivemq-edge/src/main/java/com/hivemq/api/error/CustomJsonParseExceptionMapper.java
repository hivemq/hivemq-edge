package com.hivemq.api.error;

import com.fasterxml.jackson.core.JsonParseException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.ErrorResponseUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class CustomJsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    @Override
    public @NotNull Response toResponse(final JsonParseException exception) {
        final String originalMessage = exception.getOriginalMessage();
        if (originalMessage != null) {
            return ErrorResponseUtil.invalidInput("Unable to parse JSON body: " + originalMessage);
        } else {
            return ErrorResponseUtil.invalidInput("Unable to parse JSON body, please check the input format.");
        }
    }
}
