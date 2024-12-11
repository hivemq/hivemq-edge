package com.hivemq.api.errors.adapters;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;

import java.util.List;

public class AdapterNotFound403Error extends ProblemDetails {
    public AdapterNotFound403Error(String error) {
        super(
                "AdapterNotFound",
                "No adapter found with the provided id",
                "No adapter found with the provided id",
                HttpStatus.FORBIDDEN_403,
                List.of(new Error(error)));
    }
}
