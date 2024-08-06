package com.hivemq.bridge.config;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Objects;

public class BridgeWebsocketConfig {

    private final @NotNull String path;
    private final @NotNull String subProtocol;

    public BridgeWebsocketConfig(final @NotNull String path, final @NotNull String subProtocol) {
        this.path = path;
        this.subProtocol = subProtocol;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getSubProtocol() {
        return subProtocol;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BridgeWebsocketConfig that = (BridgeWebsocketConfig) o;
        return Objects.equals(path, that.path) && Objects.equals(subProtocol, that.subProtocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, subProtocol);
    }
}
