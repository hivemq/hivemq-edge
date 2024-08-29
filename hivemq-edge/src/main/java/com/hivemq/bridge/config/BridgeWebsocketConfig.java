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
