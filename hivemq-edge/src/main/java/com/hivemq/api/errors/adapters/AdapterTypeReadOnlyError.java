package com.hivemq.api.errors.adapters;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ErrorsWithoutParameter;

import java.util.List;

public class AdapterTypeReadOnlyError extends ErrorsWithoutParameter {
    public AdapterTypeReadOnlyError(String error) {
        super(
                "AdapterTypeReadOnly",
                "Adapter type doesn't support writing",
                "Adapter type doesn't support writing",
                HttpStatus.NOT_FOUND_404,
                List.of(new Error(error)));
    }
}
