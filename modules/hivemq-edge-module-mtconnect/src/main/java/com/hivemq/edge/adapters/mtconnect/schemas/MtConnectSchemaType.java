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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum MtConnectSchemaType {
    Assets,
    Devices,
    Error,
    Streams,
    ;

    private static final @NotNull Map<@NotNull String, @NotNull MtConnectSchemaType> TYPE_MAP =
            Stream.of(values()).collect(Collectors.toMap(MtConnectSchemaType::name, Function.identity()));
    private final @NotNull String rootNodeName = "MTConnect" + name();

    public static @Nullable MtConnectSchemaType of(@NotNull final String type) {
        return TYPE_MAP.get(type);
    }

    public @NotNull String getRootNodeName() {
        return rootNodeName;
    }
}
