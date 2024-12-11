package com.hivemq.api.errors.adapters;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;

import java.util.List;

public class AdapterTypeNotFoundError extends ProblemDetails {
    public AdapterTypeNotFoundError(String error) {
        super(
                "AdapterTypeNotFound",
                "Adapter type not found",
                "No adapter type found with the provided id",
                HttpStatus.NOT_FOUND_404,
                List.of(new Error(error)));
    }
}
