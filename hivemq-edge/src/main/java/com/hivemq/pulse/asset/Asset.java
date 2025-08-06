package com.hivemq.pulse.asset;


import com.hivemq.extension.sdk.api.annotations.NotNull;

public record Asset(@NotNull String id, @NotNull String topic, @NotNull String name, @NotNull String jsonSchema) {
}
