package com.hivemq.edge.adapters.browse.importer;

import com.hivemq.edge.adapters.browse.validate.ValidationError;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DeviceTagImporterException extends Exception {
    private final @NotNull List<ValidationError> errors;

    public DeviceTagImporterException(final @NotNull List<ValidationError> errors) {
        super("Import validation failed with " + errors.size() + " errors");
        this.errors = errors;
    }

    public @NotNull List<ValidationError> getErrors() {
        return errors;
    }
}
