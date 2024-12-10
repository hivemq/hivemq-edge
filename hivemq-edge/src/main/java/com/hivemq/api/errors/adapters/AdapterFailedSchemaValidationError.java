package com.hivemq.api.errors.adapters;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.Errors;
import com.hivemq.http.error.Errors;

import java.util.List;

public class AdapterFailedSchemaValidationError extends Errors {
    public AdapterFailedSchemaValidationError(List<Error> errors) {
        super(
                "AdapterFailedValidation",
                "Adapter failed validation",
                "The provided adapter was invalid",
                HttpStatus.BAD_REQUEST_400,
                errors);
    }
}
