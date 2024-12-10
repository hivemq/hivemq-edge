package com.hivemq.api.errors.bridge;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ErrorWithParameter;
import com.hivemq.http.error.ErrorsWithParameter;

import java.util.List;

public class BridgeFailedSchemaValidationError extends ErrorsWithParameter {
    public BridgeFailedSchemaValidationError(List<ErrorWithParameter> errors) {
        super(
                "BridgeFailedSchemaValidation",
                "The provided bridge configuration could not be validated",
                "The provided bridge configuration could not be validated",
                HttpStatus.BAD_REQUEST_400,
                errors);
    }
}
