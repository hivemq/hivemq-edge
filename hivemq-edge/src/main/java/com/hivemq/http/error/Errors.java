package com.hivemq.http.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class Errors {

    @JsonProperty("errors")
    private final @NotNull List<@NotNull Error> errors;

    public Errors(final @NotNull Error... errors) {
        this.errors = Arrays.asList(errors);
    }

    @JsonCreator
    public Errors(@JsonProperty("errors") final @NotNull List<Error> errors) {
        this.errors = errors;
    }

    public @NotNull List<@NotNull Error> getErrors() {
        return errors;
    }

}
