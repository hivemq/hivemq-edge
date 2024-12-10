package com.hivemq.api.errors.adapters;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ErrorWithParameter;
import com.hivemq.http.error.ErrorsWithParameter;
import com.hivemq.http.error.ErrorsWithoutParameter;

import java.util.List;

public class AdapterFailedSchemaValidationError extends ErrorsWithParameter {
    public AdapterFailedSchemaValidationError(List<ErrorWithParameter> errors) {
        super(
                "AdapterFailedValidation",
                "Adapter failed validation",
                "The provided adapter was invalid",
                HttpStatus.BAD_REQUEST_400,
                errors);
    }
}
