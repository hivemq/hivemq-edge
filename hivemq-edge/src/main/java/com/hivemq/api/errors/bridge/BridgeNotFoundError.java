package com.hivemq.api.errors.bridge;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.Errors;

import java.util.List;

public class BridgeNotFoundError extends Errors {
    public BridgeNotFoundError(String error) {
        super(
                "BridgeNotFound",
                "Bridge was not found",
                "No bridge with the given id was found",
                HttpStatus.NOT_FOUND_404,
                List.of(new Error(error)));
    }
}
