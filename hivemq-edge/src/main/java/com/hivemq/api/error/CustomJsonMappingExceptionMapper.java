package com.hivemq.api.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.ErrorResponseUtil;

import javax.annotation.Priority;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

@Priority(1)
public class CustomJsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {

    @Override
    public @NotNull Response toResponse(final @NotNull JsonMappingException exception) {
        final String originalMessage = exception.getOriginalMessage();
        if (originalMessage != null) {
            return ErrorResponseUtil.invalidInput("Unable to parse JSON body: " + originalMessage);
        } else {
            return ErrorResponseUtil.invalidInput("Unable to parse JSON body, please check the input format.");
        }
    }
}
