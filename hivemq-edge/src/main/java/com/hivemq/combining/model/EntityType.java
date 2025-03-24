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
package com.hivemq.combining.model;

import org.jetbrains.annotations.NotNull;

public enum EntityType {

    ADAPTER,
    DEVICE,
    BRIDGE,
    EDGE_BROKER;

    public static @NotNull EntityType fromModel(@NotNull final com.hivemq.edge.api.model.EntityType type) {
        switch (type) {
            case ADAPTER -> {
                return ADAPTER;
            }
            case DEVICE -> {
                return DEVICE;
            }
            case BRIDGE -> {
                return BRIDGE;
            }
            case EDGE_BROKER -> {
                return EDGE_BROKER;
            }
        }
        throw new IllegalArgumentException();
    }

    public @NotNull com.hivemq.edge.api.model.EntityType toModel() {
        switch (this) {
            case ADAPTER -> {
                return com.hivemq.edge.api.model.EntityType.ADAPTER;
            }
            case DEVICE -> {
                return com.hivemq.edge.api.model.EntityType.DEVICE;
            }
            case BRIDGE -> {
                return com.hivemq.edge.api.model.EntityType.BRIDGE;
            }
            case EDGE_BROKER -> {
                return com.hivemq.edge.api.model.EntityType.EDGE_BROKER;
            }
        }
        throw new IllegalArgumentException();
    }


}
