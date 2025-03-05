package com.hivemq.edge.adapters.mtconnect.schemas;

import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import org.jetbrains.annotations.NotNull;

/**
 * The MTConnect schema validation event handler invalidates all the validation events
 * with severity ERROR and FATAL_ERROR.
 */
public class MtConnectSchemaValidationEventHandler implements ValidationEventHandler {
    @Override
    public boolean handleEvent(final @NotNull ValidationEvent validationEvent) {
        switch (validationEvent.getSeverity()) {
            case ValidationEvent.ERROR:
            case ValidationEvent.FATAL_ERROR:
                return false;
            default:
                return true;
        }
    }
}
