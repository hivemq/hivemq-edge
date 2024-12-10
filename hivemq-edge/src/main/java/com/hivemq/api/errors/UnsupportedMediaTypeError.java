package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ErrorsWithoutParameter;

import java.util.List;

public class UnsupportedMediaTypeError extends ErrorsWithoutParameter {
    public UnsupportedMediaTypeError() {
        super(
                "UnsupportedMediaTypeError",
                "Unsupported MediaType",
                "Unsupported MediaType",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE_415,
                List.of());
    }
}
