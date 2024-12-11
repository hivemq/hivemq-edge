package com.hivemq.api.errors.adapters;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;

import java.util.List;

public class AdapterNotFoundError extends ProblemDetails {
    public AdapterNotFoundError(String error) {
        super(
                "AdapterNotFound",
                "No adapter found with the provided id",
                "No adapter found with the provided id",
                HttpStatus.NOT_FOUND_404,
                List.of(new Error(error)));
    }
}
