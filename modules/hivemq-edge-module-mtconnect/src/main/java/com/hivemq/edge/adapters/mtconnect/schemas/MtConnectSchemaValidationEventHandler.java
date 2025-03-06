/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
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
