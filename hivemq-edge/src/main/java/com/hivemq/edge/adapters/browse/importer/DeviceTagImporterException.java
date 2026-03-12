/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.edge.adapters.browse.importer;

import com.hivemq.edge.adapters.browse.validate.ValidationError;
import java.util.List;
import org.jetbrains.annotations.NotNull;

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
