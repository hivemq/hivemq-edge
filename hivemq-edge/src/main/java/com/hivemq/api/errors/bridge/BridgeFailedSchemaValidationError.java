package com.hivemq.api.errors.bridge;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.Errors;

import java.util.List;

public class BridgeFailedSchemaValidationError extends Errors {
    public BridgeFailedSchemaValidationError(List<Error> errors) {
        super(
                "BridgeFailedSchemaValidation",
                "The provided bridge configuration could not be validated",
                "The provided bridge configuration could not be validated",
                HttpStatus.BAD_REQUEST_400,
                errors);
    }
}
