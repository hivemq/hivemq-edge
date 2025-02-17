package com.hivemq.combining;

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
