package com.hivemq.api.errors.adapters;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.Errors;

import java.util.List;

public class DomainTagNotFoundError extends Errors {
    public DomainTagNotFoundError(String error) {
        super(
                "TagNotFound",
                "No tag found with the provided id",
                "No tag found with the provided id",
                HttpStatus.NOT_FOUND_404,
                List.of(new Error(error)));
    }
}