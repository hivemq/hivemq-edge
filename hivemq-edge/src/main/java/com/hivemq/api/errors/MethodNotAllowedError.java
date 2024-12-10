package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Errors;

import java.util.List;

public class MethodNotAllowedError extends Errors {
    public MethodNotAllowedError() {
        super(
                "MethodNotAllowedError",
                "Method not allowed",
                "Method not allowed",
                HttpStatus.METHOD_NOT_ALLOWED_405,
                List.of());
    }
}
