package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ProblemDetails;

import java.util.List;

public class UnsupportedMediaTypeError extends ProblemDetails {
    public UnsupportedMediaTypeError() {
        super(
                "UnsupportedMediaTypeError",
                "Unsupported MediaType",
                "Unsupported MediaType",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE_415,
                List.of());
    }
}
