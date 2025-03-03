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
package com.hivemq.combining.runtime;

import com.hivemq.combining.model.DataIdentifierReference;
import org.jetbrains.annotations.NotNull;

public class SourceSanitizer {

    public static @NotNull String sanitize(final @NotNull DataIdentifierReference dataIdentifierReference) {
        return dataIdentifierReference.type() + ":" + sanitizeId(dataIdentifierReference.id());
    }

    private static @NotNull String sanitizeId(final @NotNull String id){
        final String escaped = id.replaceAll("//.", "/");
        return escaped;
    }
}
