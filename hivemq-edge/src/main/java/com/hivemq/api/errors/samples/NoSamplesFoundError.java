package com.hivemq.api.errors.samples;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ErrorsWithoutParameter;

import java.util.List;

public class NoSamplesFoundError extends ErrorsWithoutParameter {
    public NoSamplesFoundError(String error) {
        super(
                "NoSamplesFound",
                "No samples found",
                "No samples found",
                HttpStatus.NOT_FOUND_404,
                List.of(new Error(error)));
    }
}
