package com.hivemq.api.errors.adapters;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;

import java.util.List;

public class AdapterFailedValidationError extends ProblemDetails {
    public AdapterFailedValidationError(String error) {
        super(
                "AdapterFailedValidation",
                "Adapter failed validation",
                "The provided adapter was invalid",
                HttpStatus.NOT_FOUND_404,
                List.of(new Error(error)));
    }
}
