package com.hivemq.api.errors.adapters;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.Errors;

import java.util.List;

public class AdapterOperationNotSupportedError extends Errors {
    public AdapterOperationNotSupportedError(String error) {
        super(
                "AdapterTypeOperationNotSupported",
                "Adapter type doesn't support the operation",
                "Adapter type doesn't support the operation",
                HttpStatus.NOT_FOUND_404,
                List.of(new Error(error)));
    }
}