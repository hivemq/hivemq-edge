/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.file.v2;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown when a file's raw bytes cannot be converted to the value shape the tag's content type declares — for
 * example a file declared as JSON whose content is not valid JSON. The poll catches it and reports a per-node
 * error, so the tag reflects the failure and the next scheduled poll is the retry.
 */
public class MappingException extends RuntimeException {

    /**
     * @param message a human-readable description of what could not be mapped.
     */
    public MappingException(final @NotNull String message) {
        super(message);
    }

    @Override
    public synchronized @NotNull Throwable fillInStackTrace() {
        return this;
    }
}
