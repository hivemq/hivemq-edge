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
package com.hivemq.api.errors;

import java.net.URI;
import org.jetbrains.annotations.NotNull;

public abstract class ErrorFactory {
    private static final String CLASS_PREFIX = "com.hivemq";
    private static final String TYPE_PREFIX = "https://hivemq.com";

    protected ErrorFactory() {}

    protected static @NotNull URI type(final @NotNull Class<?> clazz) {
        return URI.create(
                TYPE_PREFIX + clazz.getName().substring(CLASS_PREFIX.length()).replace(".", "/"));
    }
}
