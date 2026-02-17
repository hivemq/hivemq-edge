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
package com.hivemq.configuration.entity;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class UUIDAdapter extends XmlAdapter<String, UUID> {
    @Override
    public @Nullable UUID unmarshal(final @Nullable String v) {
        return v == null ? null : UUID.fromString(v);
    }

    @Override
    public @Nullable String marshal(final @Nullable UUID v) {
        return v == null ? null : v.toString();
    }
}
