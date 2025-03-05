package com.hivemq.edge.adapters.mtconnect.schemas;

import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import org.jetbrains.annotations.NotNull;

public class MtConnectSchemaValidationEventHandler implements ValidationEventHandler {
    @Override
    public boolean handleEvent(final @NotNull ValidationEvent validationEvent) {
        return false;
    }
}
