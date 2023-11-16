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
package com.hivemq.bootstrap.ioc;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extension.sdk.api.services.publish.RetainedMessageStore;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.services.auth.Authenticators;

import javax.inject.Inject;

public class Extensions {

    private final @NotNull HiveMQExtensions hivemqExtensions;
    private final @NotNull Authenticators authenticators;
    private final @NotNull RetainedMessageStore retainedMessageStore;
    private final @NotNull ClientService clientService;
    private final @NotNull PublishService publishService;

    @Inject
    public Extensions(
            final @NotNull HiveMQExtensions hivemqExtensions,
            final @NotNull Authenticators authenticators,
            final @NotNull RetainedMessageStore retainedMessageStore,
            final @NotNull ClientService clientService,
            final @NotNull PublishService publishService) {
        this.hivemqExtensions = hivemqExtensions;
        this.authenticators = authenticators;
        this.retainedMessageStore = retainedMessageStore;
        this.clientService = clientService;
        this.publishService = publishService;
    }

    public @NotNull HiveMQExtensions hivemqExtensions() {
        return hivemqExtensions;
    }

    public @NotNull Authenticators authenticators() {
        return authenticators;
    }

    public @NotNull RetainedMessageStore retainedMessageStore() {
        return retainedMessageStore;
    }

    public @NotNull ClientService clientService() {
        return clientService;
    }

    public @NotNull PublishService publishService() {
        return publishService;
    }
}
