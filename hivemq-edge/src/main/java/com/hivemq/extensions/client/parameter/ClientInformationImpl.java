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
package com.hivemq.extensions.client.parameter;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@Immutable
public class ClientInformationImpl implements ClientInformation {

    @NotNull
    private final String clientId;

    public ClientInformationImpl(final @NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "client id must never be null");
        this.clientId = clientId;
    }

    @NotNull
    @Override
    public String getClientId() {
        return clientId;
    }
}
