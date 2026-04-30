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
package com.hivemq.pulse.status;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Constructs {@link Status} instances. Provided by HiveMQ Edge to consumers that need to publish status updates
 * but should not depend on the concrete {@code Status} implementation.
 */
public interface StatusFactory {

    @NotNull
    Status create(
            @NotNull Status.ActivationStatus activationStatus,
            @NotNull Status.ConnectionStatus connectionStatus,
            @NotNull List<String> errorMessages);

    default @NotNull Status create(
            final @NotNull Status.ActivationStatus activationStatus,
            final @NotNull Status.ConnectionStatus connectionStatus) {
        return create(activationStatus, connectionStatus, List.of());
    }
}
