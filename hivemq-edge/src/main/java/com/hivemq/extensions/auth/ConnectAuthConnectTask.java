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
package com.hivemq.extensions.auth;

import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extensions.client.ClientAuthenticators;
import com.hivemq.extensions.services.auth.WrappedAuthenticatorProvider;

/**
 * @author Silvio Giebl
 */
public class ConnectAuthConnectTask extends AbstractEnhancedAuthTask<AuthConnectInput, ConnectAuthOutput> {

    public ConnectAuthConnectTask(
            final @NotNull WrappedAuthenticatorProvider wrappedAuthenticatorProvider,
            final @NotNull AuthenticatorProviderInput authenticatorProviderInput,
            final @NotNull String extensionId,
            final @NotNull ClientAuthenticators clientAuthenticators) {

        super(wrappedAuthenticatorProvider, authenticatorProviderInput, extensionId, clientAuthenticators);
    }

    @Override
    void call(
            final @NotNull EnhancedAuthenticator authenticator,
            final @NotNull AuthConnectInput input,
            final @NotNull ConnectAuthOutput output) {

        authenticator.onConnect(input, output);
    }
}
